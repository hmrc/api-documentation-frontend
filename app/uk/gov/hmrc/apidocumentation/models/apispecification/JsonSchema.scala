/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.apidocumentation.models.apispecification

import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.Json.fromJson

import scala.collection.Seq
import scala.collection.immutable.ListMap

/*
 * ListMap is used instead of Map so that when iterating the entries are returned in insertion order. This means
 * that the documentation renders properties in the order defined in the schema rather than in a random order.
 * This makes the documentation more usable.
 */
case class JsonSchema(
    description: Option[String] = None,
    id: Option[String] = None,
    `type`: Option[String] = None,
    example: Option[String] = None,
    title: Option[String] = None,
    properties: ListMap[String, JsonSchema] = ListMap(),        // See above regarding use of ListMap
    patternProperties: ListMap[String, JsonSchema] = ListMap(), // See above regarding use of ListMap
    items: Option[JsonSchema] = None,
    required: Seq[String] = Nil,
    definitions: ListMap[String, JsonSchema] = ListMap(),       // See above regarding use of ListMap
    ref: Option[String] = None,
    enum: Seq[EnumerationValue] = Nil,
    oneOf: Seq[JsonSchema] = Nil,
    pattern: Option[String] = None
  )

object JsonSchema {

  case object JsonSchemaWithReference {

    def unapply(arg: JsonSchema): Boolean = arg match {
      case JsonSchema(description, _, _, _, _, properties, patternProperties, items, _, definitions, ref, _, oneOf, _) =>
        val hasReference: PartialFunction[JsonSchema, Boolean] = {
          case JsonSchemaWithReference() => true
          case _                         => false
        }

        val doesPatternPropertiesHaveReference = patternProperties.exists(v => hasReference(v._2))

        val doesDefinitionsHaveReference = definitions.exists(v => hasReference(v._2))

        val doesPropertiesHaveReference = properties.exists(v => hasReference(v._2))

        val doesItemsHaveReference = items.exists(hasReference)

        val doesOneofHaveReference = oneOf.exists(hasReference)

        val reference = ref.isDefined

        doesPropertiesHaveReference ||
        doesPatternPropertiesHaveReference ||
        doesItemsHaveReference ||
        reference ||
        doesOneofHaveReference ||
        doesDefinitionsHaveReference
    }

  }

  implicit def listMapReads[V](implicit formatV: Reads[V]): Reads[ListMap[String, V]] = new Reads[ListMap[String, V]] {

    def reads(json: JsValue) = json match {
      case JsObject(m) =>
        type Errors = Seq[(JsPath, Seq[JsonValidationError])]

        def locate(e: Errors, key: String) = e.map { case (path, validationError) => (JsPath \ key) ++ path -> validationError }

        m.foldLeft(Right(ListMap.empty): Either[Errors, ListMap[String, V]]) {
          case (acc, (key, value)) => (acc, fromJson[V](value)(formatV)) match {
              case (Right(vs), JsSuccess(v, _)) => Right(vs + (key -> v))
              case (Right(_), JsError(e))       => Left(locate(e, key))
              case (Left(e), _: JsSuccess[_])   => Left(e)
              case (Left(e1), JsError(e2))      => Left(e1 ++ locate(e2, key))
            }
        }.fold(JsError.apply, res => JsSuccess(res))

      case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
    }
  }

  implicit lazy val reads: Reads[JsonSchema] = (
    (__ \ "description").readNullable[String] and
      (__ \ "id").readNullable[String] and
      (__ \ "type").readNullable[String] and
      (__ \ "example").readNullable[String] and
      (__ \ "title").readNullable[String] and
      (__ \ "properties").lazyReadNullable[ListMap[String, JsonSchema]](listMapReads[JsonSchema]).map(_.getOrElse(ListMap())) and
      (__ \ "patternProperties").lazyReadNullable[ListMap[String, JsonSchema]](listMapReads[JsonSchema]).map(_.getOrElse(ListMap())) and
      (__ \ "items").lazyReadNullable[JsonSchema](JsonSchema.reads) and
      (__ \ "required").readNullable[Seq[String]].map(_.toSeq.flatten) and
      (__ \ "definitions").lazyReadNullable[ListMap[String, JsonSchema]](listMapReads[JsonSchema]).map(_.getOrElse(ListMap())) and
      (__ \ """$ref""").readNullable[String] and
      (__ \ "enum").readNullable[Seq[EnumerationValue]].map(_.toSeq.flatten) and
      (__ \ "oneOf").lazyReadNullable[Seq[JsonSchema]](Reads.seq[JsonSchema]).map(_.toSeq.flatten) and
      (__ \ "pattern").readNullable[String]
  )(JsonSchema.apply _)
}
