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

package org.springframework.boot.test.autoconfigure.web.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Example {@link ConstructorBinding constructor-bound}
 * {@link ConfigurationProperties @ConfigurationProperties} used to test the use of
 * configuration properties scan with sliced test.
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties("example")
public class ExampleProperties {

	private final String name;

	public ExampleProperties(@DefaultValue("test") String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

}
