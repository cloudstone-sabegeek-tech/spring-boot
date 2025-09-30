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

package org.springframework.boot.webclient.autoconfigure.service;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.autoconfigure.reactive.ClientHttpConnectorAutoConfiguration;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorBuilder;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorSettings;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.webclient.WebClientCustomizer;
import org.springframework.boot.webclient.autoconfigure.WebClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.ApiVersionFormatter;
import org.springframework.web.client.ApiVersionInserter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.registry.HttpServiceProxyRegistry;

/**
 * AutoConfiguration for Spring reactive HTTP Service Clients backed by {@link WebClient}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @since 4.0.0
 */
@AutoConfiguration(after = { ClientHttpConnectorAutoConfiguration.class, WebClientAutoConfiguration.class })
@ConditionalOnClass(WebClientAdapter.class)
@ConditionalOnBean(HttpServiceProxyRegistry.class)
@EnableConfigurationProperties(ReactiveHttpClientServiceProperties.class)
public final class ReactiveHttpServiceClientAutoConfiguration implements BeanClassLoaderAware {

	@SuppressWarnings("NullAway.Init")
	private ClassLoader beanClassLoader;

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Bean
	WebClientPropertiesHttpServiceGroupConfigurer webClientPropertiesHttpServiceGroupConfigurer(
			ObjectProvider<SslBundles> sslBundles, ReactiveHttpClientServiceProperties serviceProperties,
			ObjectProvider<ClientHttpConnectorBuilder<?>> clientConnectorBuilder,
			ObjectProvider<ClientHttpConnectorSettings> clientConnectorSettings,
			ObjectProvider<ApiVersionInserter> apiVersionInserter,
			ObjectProvider<ApiVersionFormatter> apiVersionFormatter) {
		return new WebClientPropertiesHttpServiceGroupConfigurer(this.beanClassLoader, sslBundles, serviceProperties,
				clientConnectorBuilder, clientConnectorSettings, apiVersionInserter, apiVersionFormatter);
	}

	@Bean
	WebClientCustomizerHttpServiceGroupConfigurer webClientCustomizerHttpServiceGroupConfigurer(
			ObjectProvider<WebClientCustomizer> customizers) {
		return new WebClientCustomizerHttpServiceGroupConfigurer(customizers);
	}

}
