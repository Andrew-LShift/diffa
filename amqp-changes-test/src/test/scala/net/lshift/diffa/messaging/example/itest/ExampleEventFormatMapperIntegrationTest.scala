/**
 * Copyright (C) 2010-2011 LShift Ltd.
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

package net.lshift.diffa.messaging.example.itest

import org.junit.Assert._
import org.junit.Assume.assumeTrue
import org.junit.Test
import net.lshift.diffa.messaging.amqp.{ConnectorHolder, AmqpProducer, AmqpConnectionChecker}
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import net.lshift.diffa.tools.client.{ConfigurationRestClient, DifferencesRestClient}
import net.lshift.diffa.kernel.differencing.MatchState.UNMATCHED
import net.lshift.diffa.kernel.events.VersionID
import org.joda.time.format.ISODateTimeFormat
import net.lshift.diffa.kernel.differencing.{SessionEvent, SessionScope}
import scala.collection.JavaConversions._

/**
 * Integration test for change events over AMQP in an example JSON format.
 */
class ExampleEventFormatMapperIntegrationTest {

  assumeTrue(AmqpConnectionChecker.isConnectionAvailable)

  private val log = LoggerFactory.getLogger(getClass)

  @Test
  def integrationTest() {
    val serverRoot = "http://localhost:19095/diffa-agent"
    val diffClient = new DifferencesRestClient(serverRoot)
    val config = new ConfigurationRestClient(serverRoot)
    val categories = Map("bizDate" -> "date")

    config.declareGroup("g1")
    config.declareEndpoint("upstream",
                           "http://upstream.com", "application/json",
                           "amqp://localhost//queues/exampleChanges", "application/example+json",
                           true, categories)

    config.declareEndpoint("downstream",
                           "http://downstream.com", "application/json",
                           null, null,
                           true, categories)

    config.declarePair("pair", "same", 1, "upstream", "downstream", "g1")

    val connectorHolder = new ConnectorHolder()
    val queueName = "exampleChanges"
    val changeEventProducer = new AmqpProducer(connectorHolder.connector, queueName)

    val sessionId = diffClient.subscribe(SessionScope.forPairs("pair"))

    log.info("Sending change event")
    val changeEvent = IOUtils.toString(getClass.getResourceAsStream("/event.json"))
    changeEventProducer.send(changeEvent)

    val sessionEvents = poll(diffClient, sessionId)
    assertEquals(1, sessionEvents.length)
    val sessionEvent = sessionEvents(0)
    assertEquals(VersionID("pair", "5509a836-ca75-42a4-855a-71893448cc9d"), sessionEvent.objId)
    assertEquals("2011-01-24T00:00:00.000Z", ISODateTimeFormat.dateTime.print(sessionEvent.detectedAt))
    assertEquals(UNMATCHED, sessionEvent.state)
    assertEquals("479", sessionEvent.upstreamVsn)
    assertNull(sessionEvent.downstreamVsn)
  }

  def poll(diffClient: DifferencesRestClient,
           sessionId: String,
           maxAttempts: Int = 10,
           sleepTimeMillis: Int = 1000): Array[SessionEvent] = {
    
    var attempts = 0
    while (attempts < maxAttempts) {
      Thread.sleep(1000)
      val sessionEvents = diffClient.poll(sessionId)
      assertNotNull(sessionEvents)
      if (sessionEvents.length > 0) return sessionEvents
      attempts += 1
    }
    fail("Couldn't retrieve session events after %d attempts".format(maxAttempts))
    Array[SessionEvent]()
  }
}