/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.apidocumentation.models

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class JsonSchema(description: Option[String] = None,
                      id: Option[String] = None,
                      `type`: Option[String] = None,
                      example: Option[String] = None,
                      title: Option[String] = None,
                      properties: Map[String, JsonSchema] = Map(),
                      patternProperties: Map[String, JsonSchema] = Map(),
                      items: Option[JsonSchema] = None,
                      required: Seq[String] = Nil,
                      definitions: Map[String, JsonSchema] = Map(),
                      ref: Option[String] = None,
                      enum: Seq[String] = Nil,
                      oneOf: Seq[JsonSchema] = Nil,
                      pattern: Option[String] = None)

object JsonSchema {
  implicit lazy val reads: Reads[JsonSchema] = (
    ( __ \ "description" ).readNullable[String] and
      ( __ \ "id" ).readNullable[String] and
      ( __ \ "type" ).readNullable[String] and
      ( __ \ "example" ).readNullable[String] and
      ( __ \ "title" ).readNullable[String] and
      ( __ \ "properties" ).lazyReadNullable[Map[String,JsonSchema]](Reads.map[JsonSchema]).map(_.getOrElse(Map())) and
      ( __ \ "patternProperties" ).lazyReadNullable[Map[String,JsonSchema]](Reads.map[JsonSchema]).map(_.getOrElse(Map())) and
      ( __ \ "items" ).lazyReadNullable[JsonSchema](JsonSchema.reads) and
      ( __ \ "required" ).readNullable[Seq[String]].map(_.toSeq.flatten) and
      ( __ \ "definitions" ).lazyReadNullable[Map[String,JsonSchema]](Reads.map[JsonSchema]).map(_.getOrElse(Map())) and
      ( __ \ "$ref" ).readNullable[String] and
      ( __ \ "enum" ).readNullable[Seq[String]].map(_.toSeq.flatten) and
      ( __ \ "oneOf" ).lazyReadNullable[Seq[JsonSchema]](Reads.seq[JsonSchema]).map(_.toSeq.flatten) and
      ( __ \ "pattern" ).readNullable[String]
    )(JsonSchema.apply _)
}
