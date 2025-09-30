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

package org.springframework.boot.http.client.autoconfigure.reactive;

import java.time.Duration;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesSource;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorBuilder;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorSettings;
import org.springframework.http.client.reactive.ClientHttpConnector;

/**
 * Base {@link ConfigurationProperties @ConfigurationProperties} for configuring a
 * {@link ClientHttpConnector}.
 *
 * @author Phillip Webb
 * @since 3.5.0
 * @see ClientHttpConnectorSettings
 */
@ConfigurationPropertiesSource
public abstract class AbstractClientHttpConnectorProperties {

	/**
	 * Handling for HTTP redirects.
	 */
	private @Nullable HttpRedirects redirects;

	/**
	 * Default connect timeout for a client HTTP request.
	 */
	private @Nullable Duration connectTimeout;

	/**
	 * Default read timeout for a client HTTP request.
	 */
	private @Nullable Duration readTimeout;

	/**
	 * Default SSL configuration for a client HTTP request.
	 */
	private final Ssl ssl = new Ssl();

	/**
	 * Default connector used for a client HTTP request.
	 */
	private @Nullable Connector connector;

	public @Nullable HttpRedirects getRedirects() {
		return this.redirects;
	}

	public void setRedirects(@Nullable HttpRedirects redirects) {
		this.redirects = redirects;
	}

	public @Nullable Duration getConnectTimeout() {
		return this.connectTimeout;
	}

	public void setConnectTimeout(@Nullable Duration connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public @Nullable Duration getReadTimeout() {
		return this.readTimeout;
	}

	public void setReadTimeout(@Nullable Duration readTimeout) {
		this.readTimeout = readTimeout;
	}

	public Ssl getSsl() {
		return this.ssl;
	}

	public @Nullable Connector getConnector() {
		return this.connector;
	}

	public void setConnector(@Nullable Connector connector) {
		this.connector = connector;
	}

	/**
	 * SSL configuration.
	 */
	@ConfigurationPropertiesSource
	public static class Ssl {

		/**
		 * SSL bundle to use.
		 */
		private @Nullable String bundle;

		public @Nullable String getBundle() {
			return this.bundle;
		}

		public void setBundle(@Nullable String bundle) {
			this.bundle = bundle;
		}

	}

	/**
	 * Supported factory types.
	 */
	public enum Connector {

		/**
		 * Reactor-Netty.
		 */
		REACTOR(ClientHttpConnectorBuilder::reactor),

		/**
		 * Jetty's HttpClient.
		 */
		JETTY(ClientHttpConnectorBuilder::jetty),

		/**
		 * Apache HttpComponents HttpClient.
		 */
		HTTP_COMPONENTS(ClientHttpConnectorBuilder::httpComponents),

		/**
		 * Java's HttpClient.
		 */
		JDK(ClientHttpConnectorBuilder::jdk);

		private final Supplier<ClientHttpConnectorBuilder<?>> builderSupplier;

		Connector(Supplier<ClientHttpConnectorBuilder<?>> builderSupplier) {
			this.builderSupplier = builderSupplier;
		}

		ClientHttpConnectorBuilder<?> builder() {
			return this.builderSupplier.get();
		}

	}

}
