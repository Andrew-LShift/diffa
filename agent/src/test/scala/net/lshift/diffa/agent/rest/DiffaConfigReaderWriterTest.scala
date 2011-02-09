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
package net.lshift.diffa.agent.rest

import org.junit.Test
import org.junit.Assert._
import scala.collection.JavaConversions._
import net.lshift.diffa.kernel.config._
import net.lshift.diffa.kernel.frontend.DiffaConfig
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

/*
* Test cases for the DiffaConfigReaderWriter.
*/
class DiffaConfigReaderWriterTest {
  @Test
  def roundtrip = {
    val config = new DiffaConfig(
//      properties = Map("diffa.host" -> "localhost:1234"),
      users = List(User("abc", "a@example.com")),
      endpoints = List(
        Endpoint(name = "upstream1", url = "http://localhost:1234", contentType = "application/json",
          inboundUrl = "http://inbound", inboundContentType = "application/xml",
          categories = Map(
            "a" -> new RangeCategoryDescriptor("date", "2009", "2010"),
            "b" -> new SetCategoryDescriptor(Set("a", "b", "c")))),
        Endpoint(name = "downstream1", url = "http://localhost:5432", contentType = "application/json",
          categories = Map(
            "c" -> new PrefixCategoryDescriptor(1, 5, 1),
            "d" -> new PrefixCategoryDescriptor(1, 6, 1)
          ))),
      groups = List(PairGroup("gaa"), PairGroup("gbb")),
      pairs = List(
        PairDef("ab", "same", 5, "upstream1", "downstream1", "gaa"),
        PairDef("ac", "same", 5, "upstream1", "downstream1", "gbb"))
    )

    val readerWriter = new DiffaConfigReaderWriter
    val baos = new ByteArrayOutputStream
    readerWriter.writeTo(config, null, null, null, null, null, baos)

      // TODO: Use XMLDiff to compare these
//    val xml = new String(baos.toByteArray, "UTF-8")
//    assertEquals(
//      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
//      "<diffa-config>" +
//        "<user name=\"abc\" email=\"a@example.com\"/>" +
//        "<endpoint name=\"upstream1\" url=\"http://localhost:1234\" content-type=\"application/json\" " +
//                   "inbound-url=\"http://inbound\" inbound-content-type=\"application/xml\">" +
//          "<range-category name=\"upstream1\" data-type=\"date\" lower=\"2009\" upper=\"2010\"/>" +
//          "<set-category name=\"upstream1\">" +
//            "<value>a</value>" +
//            "<value>b</value>" +
//            "<value>c</value>" +
//          "</set-category>" +
//        "</endpoint>" +
//        "<endpoint name=\"downstream1\" url=\"http://localhost:5432\" content-type=\"application/json\">" +
//          "<prefix-category name=\"downstream1\" prefix-length=\"1\" max-length=\"6\" step=\"1\"/>" +
//          "<prefix-category name=\"downstream1\" prefix-length=\"1\" max-length=\"5\" step=\"1\"/>" +
//        "</endpoint>" +
//        "<group name=\"gaa\">" +
//          "<pair key=\"ab\" upstream=\"upstream1\" downstream=\"downstream1\" version-policy=\"same\" matching-timeout=\"5\"/>" +
//        "</group>" +
//        "<group name=\"gbb\">" +
//          "<pair key=\"ac\" upstream=\"upstream1\" downstream=\"downstream1\" version-policy=\"same\" matching-timeout=\"5\"/>" +
//        "</group>" +
//      "</diffa-config>",
//      xml
//    )

    val newConfig = readerWriter.readFrom(null, null, null, null, null, new ByteArrayInputStream(baos.toByteArray))
    assertEquals(config, newConfig)
  }

}