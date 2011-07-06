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
package net.lshift.diffa.messaging.json

import com.sun.jersey.core.util.MultivaluedMapImpl
import net.lshift.diffa.kernel.participants._
import javax.ws.rs.core.MediaType
import com.sun.jersey.api.client.ClientResponse
import net.lshift.diffa.participant.common.JSONHelper
import org.apache.commons.io.IOUtils

/**
 * JSON/REST scanning participant client.
 */

class ScanningParticipantRestClient(scanUrl:String)
    extends AbstractRestClient(scanUrl, "")
    with ScanningParticipantRef {

  def scan(constraints: Seq[QueryConstraint], aggregations: Map[String, CategoryFunction]) = {
    val params = new MultivaluedMapImpl()
    constraints.foreach {
      case sqc:SetQueryConstraint   =>
        sqc.values.foreach(v => params.add(sqc.category, v))
      case rqc:RangeQueryConstraint =>
        params.add(rqc.category + "-start", rqc.lower)
        params.add(rqc.category + "-end", rqc.upper)
      case pc:PrefixQueryConstraint =>
        params.add(pc.category + "-prefix", pc.prefix)
      case nvc:NonValueConstraint =>    // Ignore non-value constraints
    }
    aggregations.foreach { case (k, f) =>
        params.add(k + "-granularity", f.name)
    }

    val jsonEndpoint = resource.queryParams(params).`type`(MediaType.APPLICATION_JSON_TYPE)
    val response = jsonEndpoint.get(classOf[ClientResponse])
    response.getStatus match {
      case 200 => JSONHelper.readQueryResult(response.getEntityInputStream)
      case _   =>
        log.error(response.getStatus + "")
        throw new Exception("Participant scan failed: " + response.getStatus + "\n" + IOUtils.toString(response.getEntityInputStream, "UTF-8"))
    }
  }
}