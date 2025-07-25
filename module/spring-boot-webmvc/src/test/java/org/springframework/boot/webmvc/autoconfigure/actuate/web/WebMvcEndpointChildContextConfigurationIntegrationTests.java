/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.webmvc.autoconfigure.actuate.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.env.ConfigTreePropertySource;
import org.springframework.boot.servlet.autoconfigure.actuate.web.ServletManagementContextAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.tomcat.autoconfigure.actuate.web.server.TomcatServletManagementContextAutoConfiguration;
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration;
import org.springframework.boot.web.server.context.ServerPortInfoApplicationContextInitializer;
import org.springframework.boot.web.server.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersSpec.ExchangeFunction;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link WebMvcEndpointChildContextConfiguration}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class WebMvcEndpointChildContextConfigurationIntegrationTests {

	private final WebApplicationContextRunner runner = new WebApplicationContextRunner(
			AnnotationConfigServletWebServerApplicationContext::new)
		.withConfiguration(AutoConfigurations.of(ManagementContextAutoConfiguration.class,
				TomcatServletWebServerAutoConfiguration.class, TomcatServletManagementContextAutoConfiguration.class,
				ServletManagementContextAutoConfiguration.class, WebEndpointAutoConfiguration.class,
				EndpointAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
				ErrorMvcAutoConfiguration.class))
		.withUserConfiguration(SucceedingEndpoint.class, FailingEndpoint.class, FailingControllerEndpoint.class)
		.withInitializer(new ServerPortInfoApplicationContextInitializer())
		.withPropertyValues("server.port=0", "management.server.port=0", "management.endpoints.web.exposure.include=*",
				"server.error.include-exception=true", "server.error.include-message=always",
				"server.error.include-binding-errors=always");

	@TempDir
	Path temp;

	@Test // gh-17938
	void errorEndpointIsUsedWithEndpoint() {
		this.runner.run(withRestClient((client) -> {
			Map<String, ?> body = client.get()
				.uri("actuator/fail")
				.accept(MediaType.APPLICATION_JSON)
				.exchange(toResponseBody());
			assertThat(body).hasEntrySatisfying("exception",
					(value) -> assertThat(value).asString().contains("IllegalStateException"));
			assertThat(body).hasEntrySatisfying("message",
					(value) -> assertThat(value).asString().contains("Epic Fail"));
		}));
	}

	@Test
	void errorPageAndErrorControllerIncludeDetails() {
		this.runner.withPropertyValues("server.error.include-stacktrace=always", "server.error.include-message=always")
			.run(withRestClient((client) -> {
				Map<String, ?> body = client.get()
					.uri("actuator/fail")
					.accept(MediaType.APPLICATION_JSON)
					.exchange(toResponseBody());
				assertThat(body).hasEntrySatisfying("message",
						(value) -> assertThat(value).asString().contains("Epic Fail"));
				assertThat(body).hasEntrySatisfying("trace",
						(value) -> assertThat(value).asString().contains("java.lang.IllegalStateException: Epic Fail"));
			}));
	}

	@Test
	void errorEndpointIsUsedWithRestControllerEndpoint() {
		this.runner.run(withRestClient((client) -> {
			Map<String, ?> body = client.get()
				.uri("actuator/failController")
				.accept(MediaType.APPLICATION_JSON)
				.exchange(toResponseBody());
			assertThat(body).hasEntrySatisfying("exception",
					(value) -> assertThat(value).asString().contains("IllegalStateException"));
			assertThat(body).hasEntrySatisfying("message",
					(value) -> assertThat(value).asString().contains("Epic Fail"));
		}));
	}

	@Test
	void errorEndpointIsUsedWithRestControllerEndpointOnBindingError() {
		this.runner.run(withRestClient((client) -> {
			Map<String, ?> body = client.post()
				.uri("actuator/failController")
				.body(Collections.singletonMap("content", ""))
				.accept(MediaType.APPLICATION_JSON)
				.exchange(toResponseBody());
			assertThat(body).hasEntrySatisfying("exception",
					(value) -> assertThat(value).asString().contains("MethodArgumentNotValidException"));
			assertThat(body).hasEntrySatisfying("message",
					(value) -> assertThat(value).asString().contains("Validation failed"));
			assertThat(body).hasEntrySatisfying("errors",
					(value) -> assertThat(value).asInstanceOf(InstanceOfAssertFactories.LIST).isNotEmpty());
		}));
	}

	@Test
	void whenManagementServerBasePathIsConfiguredThenEndpointsAreBeneathThatPath() {
		this.runner.withPropertyValues("management.server.base-path:/manage").run(withRestClient((client) -> {
			String body = client.get()
				.uri("manage/actuator/success")
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.body(String.class);
			assertThat(body).isEqualTo("Success");
		}));
	}

	@Test // gh-32941
	void whenManagementServerPortLoadedFromConfigTree() {
		this.runner.withInitializer(this::addConfigTreePropertySource)
			.run((context) -> assertThat(context).hasNotFailed());
	}

	private void addConfigTreePropertySource(ConfigurableApplicationContext applicationContext) {
		try {
			applicationContext.getEnvironment()
				.setConversionService((ConfigurableConversionService) ApplicationConversionService.getSharedInstance());
			Path configtree = this.temp.resolve("configtree");
			Path file = configtree.resolve("management/server/port");
			file.toFile().getParentFile().mkdirs();
			FileCopyUtils.copy("0".getBytes(StandardCharsets.UTF_8), file.toFile());
			ConfigTreePropertySource source = new ConfigTreePropertySource("configtree", configtree);
			applicationContext.getEnvironment().getPropertySources().addFirst(source);
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private ContextConsumer<AssertableWebApplicationContext> withRestClient(Consumer<RestClient> restClient) {
		return (context) -> {
			String port = context.getEnvironment().getProperty("local.management.port");
			RestClient client = RestClient.create("http://localhost:" + port);
			restClient.accept(client);
		};
	}

	private ExchangeFunction<Map<String, ?>> toResponseBody() {
		return ((request, response) -> response.bodyTo(new ParameterizedTypeReference<Map<String, ?>>() {
		}));
	}

	@Endpoint(id = "fail")
	static class FailingEndpoint {

		@ReadOperation
		String fail() {
			throw new IllegalStateException("Epic Fail");
		}

	}

	@Endpoint(id = "success")
	static class SucceedingEndpoint {

		@ReadOperation
		String fail() {
			return "Success";
		}

	}

	@org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint(id = "failController")
	@SuppressWarnings("removal")
	static class FailingControllerEndpoint {

		@GetMapping
		String fail() {
			throw new IllegalStateException("Epic Fail");
		}

		@PostMapping(produces = "application/json")
		@ResponseBody
		String bodyValidation(@Valid @RequestBody TestBody body) {
			return body.getContent();
		}

	}

	public static class TestBody {

		@NotEmpty
		private String content;

		public String getContent() {
			return this.content;
		}

		public void setContent(String content) {
			this.content = content;
		}

	}

}
