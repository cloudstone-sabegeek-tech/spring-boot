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

package org.springframework.boot.actuate.docs.condition;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.condition.ConditionsReportEndpoint;
import org.springframework.boot.actuate.docs.MockMvcEndpointDocumentationTests;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;

/**
 * Tests for generating documentation describing {@link ConditionsReportEndpoint}.
 *
 * @author Andy Wilkinson
 */
class ConditionsReportEndpointDocumentationTests extends MockMvcEndpointDocumentationTests {

	@Test
	void conditions() {
		List<FieldDescriptor> positiveMatchFields = List.of(
				fieldWithPath("").description("Classes and methods with conditions that were matched."),
				fieldWithPath(".*.[].condition").description("Name of the condition."),
				fieldWithPath(".*.[].message").description("Details of why the condition was matched."));
		List<FieldDescriptor> negativeMatchFields = List.of(
				fieldWithPath("").description("Classes and methods with conditions that were not matched."),
				fieldWithPath(".*.notMatched").description("Conditions that were matched."),
				fieldWithPath(".*.notMatched.[].condition").description("Name of the condition."),
				fieldWithPath(".*.notMatched.[].message").description("Details of why the condition was not matched."),
				fieldWithPath(".*.matched").description("Conditions that were matched."),
				fieldWithPath(".*.matched.[].condition").description("Name of the condition.")
					.type(JsonFieldType.STRING)
					.optional(),
				fieldWithPath(".*.matched.[].message").description("Details of why the condition was matched.")
					.type(JsonFieldType.STRING)
					.optional());
		FieldDescriptor unconditionalClassesField = fieldWithPath("contexts.*.unconditionalClasses")
			.description("Names of unconditional auto-configuration classes if any.");
		assertThat(this.mvc.get().uri("/actuator/conditions")).hasStatusOk()
			.apply(MockMvcRestDocumentation.document("conditions",
					preprocessResponse(limit("contexts", getApplicationContext().getId(), "positiveMatches"),
							limit("contexts", getApplicationContext().getId(), "negativeMatches")),
					responseFields(fieldWithPath("contexts").description("Application contexts keyed by id."))
						.andWithPrefix("contexts.*.positiveMatches", positiveMatchFields)
						.andWithPrefix("contexts.*.negativeMatches", negativeMatchFields)
						.and(unconditionalClassesField, parentIdField())));
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		ConditionsReportEndpoint autoConfigurationReportEndpoint(ConfigurableApplicationContext context) {
			ConditionEvaluationReport conditionEvaluationReport = ConditionEvaluationReport
				.get(context.getBeanFactory());
			conditionEvaluationReport
				.recordEvaluationCandidates(List.of(PropertyPlaceholderAutoConfiguration.class.getName()));
			return new ConditionsReportEndpoint(context);
		}

	}

}
