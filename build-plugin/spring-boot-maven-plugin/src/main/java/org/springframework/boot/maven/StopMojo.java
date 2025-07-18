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

package org.springframework.boot.maven;

import java.io.IOException;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Stop an application that has been started by the "start" goal. Typically invoked once a
 * test suite has completed.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@Mojo(name = "stop", requiresProject = true, defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST)
public class StopMojo extends AbstractMojo {

	/**
	 * The Maven project.
	 * @since 1.4.1
	 */
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	/**
	 * The JMX name of the automatically deployed MBean managing the lifecycle of the
	 * application.
	 */
	@Parameter(defaultValue = SpringApplicationAdminClient.DEFAULT_OBJECT_NAME)
	private String jmxName;

	/**
	 * The port to use to look up the platform MBeanServer.
	 */
	@Parameter(defaultValue = "9001")
	private int jmxPort;

	/**
	 * Skip the execution.
	 * @since 1.3.2
	 */
	@Parameter(property = "spring-boot.stop.skip", defaultValue = "false")
	private boolean skip;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (this.skip) {
			getLog().debug("skipping stop as per configuration.");
			return;
		}
		getLog().info("Stopping application...");
		try (JMXConnector connector = SpringApplicationAdminClient.connect(this.jmxPort)) {
			MBeanServerConnection connection = connector.getMBeanServerConnection();
			stop(connection);
		}
		catch (IOException ex) {
			// The response won't be received as the server has died - ignoring
			getLog().debug("Service is not reachable anymore (" + ex.getMessage() + ")");
		}
	}

	private void stop(MBeanServerConnection connection) throws IOException, MojoExecutionException {
		try {
			new SpringApplicationAdminClient(connection, this.jmxName).stop();
		}
		catch (InstanceNotFoundException ex) {
			throw new MojoExecutionException(
					"Spring application lifecycle JMX bean not found. Could not stop application gracefully", ex);
		}
	}

}
