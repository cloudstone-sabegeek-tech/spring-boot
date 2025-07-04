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

package org.springframework.boot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.util.Assert;

/**
 * A {@link Runnable} to be used as a {@link Runtime#addShutdownHook(Thread) shutdown
 * hook} to perform graceful shutdown of Spring Boot applications. This hook tracks
 * registered application contexts as well as any actions registered via
 * {@link SpringApplication#getShutdownHandlers()}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Brian Clozel
 */
class SpringApplicationShutdownHook implements Runnable {

	private static final int SLEEP = 50;

	private static final long TIMEOUT = TimeUnit.MINUTES.toMillis(10);

	private static final Log logger = LogFactory.getLog(SpringApplicationShutdownHook.class);

	private final Handlers handlers = new Handlers();

	private final Set<ConfigurableApplicationContext> contexts = new LinkedHashSet<>();

	private final Set<ConfigurableApplicationContext> closedContexts = Collections.newSetFromMap(new WeakHashMap<>());

	private final ApplicationContextClosedListener contextCloseListener = new ApplicationContextClosedListener();

	private final AtomicBoolean shutdownHookAdded = new AtomicBoolean();

	private volatile boolean shutdownHookAdditionEnabled = false;

	private boolean inProgress;

	SpringApplicationShutdownHandlers getHandlers() {
		return this.handlers;
	}

	void enableShutdownHookAddition() {
		this.shutdownHookAdditionEnabled = true;
	}

	void registerApplicationContext(ConfigurableApplicationContext context) {
		addRuntimeShutdownHookIfNecessary();
		synchronized (SpringApplicationShutdownHook.class) {
			assertNotInProgress();
			context.addApplicationListener(this.contextCloseListener);
			this.contexts.add(context);
		}
	}

	private void addRuntimeShutdownHookIfNecessary() {
		if (this.shutdownHookAdditionEnabled && this.shutdownHookAdded.compareAndSet(false, true)) {
			addRuntimeShutdownHook();
		}
	}

	void addRuntimeShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(this, "SpringApplicationShutdownHook"));
	}

	void deregisterFailedApplicationContext(ConfigurableApplicationContext applicationContext) {
		synchronized (SpringApplicationShutdownHook.class) {
			Assert.state(!applicationContext.isActive(), "Cannot unregister active application context");
			SpringApplicationShutdownHook.this.contexts.remove(applicationContext);
		}
	}

	@Override
	public void run() {
		Set<ConfigurableApplicationContext> contexts;
		Set<ConfigurableApplicationContext> closedContexts;
		List<Handler> handlers;
		synchronized (SpringApplicationShutdownHook.class) {
			this.inProgress = true;
			contexts = new LinkedHashSet<>(this.contexts);
			closedContexts = new LinkedHashSet<>(this.closedContexts);
			handlers = new ArrayList<>(this.handlers.getActions());
			Collections.reverse(handlers);
		}
		contexts.forEach(this::closeAndWait);
		closedContexts.forEach(this::closeAndWait);
		handlers.forEach(Handler::run);
	}

	boolean isApplicationContextRegistered(ConfigurableApplicationContext context) {
		synchronized (SpringApplicationShutdownHook.class) {
			return this.contexts.contains(context);
		}
	}

	void reset() {
		synchronized (SpringApplicationShutdownHook.class) {
			this.contexts.clear();
			this.closedContexts.clear();
			this.handlers.getActions().clear();
			this.inProgress = false;
		}
	}

	/**
	 * Call {@link ConfigurableApplicationContext#close()} and wait until the context
	 * becomes inactive. We can't assume that just because the close method returns that
	 * the context is actually inactive. It could be that another thread is still in the
	 * process of disposing beans.
	 * @param context the context to clean
	 */
	private void closeAndWait(ConfigurableApplicationContext context) {
		if (!context.isActive()) {
			return;
		}
		context.close();
		try {
			int waited = 0;
			while (context.isActive()) {
				if (waited > TIMEOUT) {
					throw new TimeoutException();
				}
				Thread.sleep(SLEEP);
				waited += SLEEP;
			}
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			logger.warn("Interrupted waiting for application context " + context + " to become inactive");
		}
		catch (TimeoutException ex) {
			logger.warn("Timed out waiting for application context " + context + " to become inactive", ex);
		}
	}

	private void assertNotInProgress() {
		Assert.state(!SpringApplicationShutdownHook.this.inProgress, "Shutdown in progress");
	}

	/**
	 * The handler actions for this shutdown hook.
	 */
	private final class Handlers implements SpringApplicationShutdownHandlers, Runnable {

		private final Set<Handler> actions = new LinkedHashSet<>();

		@Override
		public void add(Runnable action) {
			Assert.notNull(action, "'action' must not be null");
			addRuntimeShutdownHookIfNecessary();
			synchronized (SpringApplicationShutdownHook.class) {
				assertNotInProgress();
				this.actions.add(new Handler(action));
			}
		}

		@Override
		public void remove(Runnable action) {
			Assert.notNull(action, "'action' must not be null");
			synchronized (SpringApplicationShutdownHook.class) {
				assertNotInProgress();
				this.actions.remove(new Handler(action));
			}
		}

		Set<Handler> getActions() {
			return this.actions;
		}

		@Override
		public void run() {
			SpringApplicationShutdownHook.this.run();
			SpringApplicationShutdownHook.this.reset();
		}

	}

	/**
	 * A single handler that uses object identity for {@link #equals(Object)} and
	 * {@link #hashCode()}.
	 *
	 * @param runnable the handler runner
	 */
	record Handler(Runnable runnable) {

		@Override
		public int hashCode() {
			return System.identityHashCode(this.runnable);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			return this.runnable == ((Handler) obj).runnable;
		}

		void run() {
			this.runnable.run();
		}

	}

	/**
	 * {@link ApplicationListener} to track closed contexts.
	 */
	private final class ApplicationContextClosedListener implements ApplicationListener<ContextClosedEvent> {

		@Override
		public void onApplicationEvent(ContextClosedEvent event) {
			// The ContextClosedEvent is fired at the start of a call to {@code close()}
			// and if that happens in a different thread then the context may still be
			// active. Rather than just removing the context, we add it to a {@code
			// closedContexts} set. This is weak set so that the context can be GC'd once
			// the {@code close()} method returns.
			synchronized (SpringApplicationShutdownHook.class) {
				ApplicationContext applicationContext = event.getApplicationContext();
				SpringApplicationShutdownHook.this.contexts.remove(applicationContext);
				SpringApplicationShutdownHook.this.closedContexts
					.add((ConfigurableApplicationContext) applicationContext);
			}
		}

	}

}
