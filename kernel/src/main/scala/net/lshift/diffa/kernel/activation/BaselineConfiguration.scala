/**
 * Copyright (C) 2010 LShift Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.lshift.diffa.kernel.activation

import org.hibernate.{SessionFactory, SessionFactoryObserver}
import org.slf4j.LoggerFactory
import net.lshift.diffa.kernel.config.{Endpoint, HibernateConfigStore}

/**
 * This creates a baseline data set in the DB once the Hibernate session factory
 * becomes available for the first time.
 */
class BaselineConfiguration extends SessionFactoryObserver {

  private val log = LoggerFactory.getLogger(getClass)

  def sessionFactoryCreated(factory:SessionFactory) = {
    // The config store will not have been constructed at this point in Spring
    // So just create a throw away instance in order to produce this baseline
    val config = new HibernateConfigStore(factory)
    val contentType = "application/json"
    val inboundUrl = "changes"
    val e = Endpoint("json-messaging-endpoint", null, contentType, inboundUrl, true)
    config.createOrUpdateEndpoint(e)

    log.debug("Diffa baseline configuration created")
  }

  def sessionFactoryClosed(factory:SessionFactory) = {}
}