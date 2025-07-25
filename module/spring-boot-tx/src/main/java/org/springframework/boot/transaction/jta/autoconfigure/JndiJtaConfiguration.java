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

package org.springframework.boot.transaction.jta.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnJndi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.transaction.autoconfigure.TransactionManagerCustomizers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.jta.JtaTransactionManager;

/**
 * JTA Configuration for a JNDI-managed {@link JtaTransactionManager}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Kazuki Shimizu
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(JtaTransactionManager.class)
@ConditionalOnJndi({ JtaTransactionManager.DEFAULT_USER_TRANSACTION_NAME, "java:comp/TransactionManager",
		"java:appserver/TransactionManager", "java:pm/TransactionManager", "java:/TransactionManager" })
@ConditionalOnMissingBean(org.springframework.transaction.TransactionManager.class)
class JndiJtaConfiguration {

	@Bean
	JtaTransactionManager transactionManager(
			ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {
		JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();
		transactionManagerCustomizers.ifAvailable((customizers) -> customizers.customize(jtaTransactionManager));
		return jtaTransactionManager;
	}

}
