/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.apidocumentation.views.apispecification.helpers

import uk.gov.hmrc.apidocumentation.models.JsonSchema
import play.api.libs.json.JsValue
import play.api.libs.json.Json

case class EnumValue(
                      name: String,
                      description: Option[String] = None
                    )

case class RequestResponseField2(name: String, `type`: String, typeId: String, isArray: Boolean, required: Boolean, example: Option[String],
                                description: Option[String], pattern: Option[String], depth: Int, enumValues: Seq[EnumValue])

object RequestResponseField2 {
  def extractFields(requestResponseBodies: List[uk.gov.hmrc.apidocumentation.models.apispecification.TypeDeclaration]): Seq[RequestResponseField2] = {
    val fields = for {
      body    <- requestResponseBodies
      schema  <- schema(body)
    } yield {
      extractFields(schema)
    }

    fields.flatten
  }

  private def schema(body: uk.gov.hmrc.apidocumentation.models.apispecification.TypeDeclaration): Option[JsonSchema] = {
    body.`type` match {
      case jsonText if jsonText.trim.startsWith("{") => {
        val json: JsValue = Json.parse(jsonText)
        json.validate[JsonSchema].asOpt
      }
      case _ => None
    }
  }

  private def extractFields(schema: JsonSchema,
                            fieldName: Option[String] = None,
                            description: Option[String] = None,
                            required: Boolean = false,
                            depth: Int = -1,
                            acc: Seq[RequestResponseField2] = Nil,
                            isArray: Boolean = false,
                            isPatternproperty: Boolean = false): Seq[RequestResponseField2] = {

    def extractEnumValues(schema: JsonSchema): Seq[EnumValue] = {

      val enum = schema.enum.map( e => EnumValue(e.value))

      val oneOf = schema.oneOf.map { e =>
        EnumValue(e.enum.headOption.fold("")(_.value), e.description)
      }

      enum ++ oneOf
    }

    val currentField = fieldName match {
      case Some(name) if schema.`type` != "array" => {
        val fieldOrTitle = if (isPatternproperty) schema.title.getOrElse(name) else name
        Some(RequestResponseField2(
          fieldOrTitle,
          schema.`type`.getOrElse(""),
          schema.id.getOrElse(""),
          isArray,
          required,
          schema.example.filter(_.nonEmpty),
          schema.description.orElse(description).filter(_.nonEmpty),
          schema.pattern.filter(_.nonEmpty),
          depth,
          extractEnumValues(schema)))
      }
      case _ => None
    }

    schema.`type` match {
      case Some("object") => {
        val propertyFields = for {
          (fieldName, definition) <- schema.properties
          field <- extractFields(definition, Some(fieldName), None, schema.required.contains(fieldName), depth+1)
        } yield {
          field
        }

        val patternFields = for {
          (fieldName, definition) <- schema.patternProperties
          field <- extractFields(definition, Some(fieldName), None, schema.required.contains(fieldName), depth+1,
            isPatternproperty=true)
        } yield {
          field
        }
        currentField.fold(acc)(acc :+ _) ++ propertyFields ++ patternFields
      }
      case Some("array") => {
        extractFields(schema.items.get, fieldName, schema.description, required, depth, acc, true)
      }
      case _ => {
        currentField.fold(acc)(acc :+ _)
      }
    }
  }

  def requestFields(method: uk.gov.hmrc.apidocumentation.models.apispecification.Method): Seq[RequestResponseField2] = {
    extractFields(method.body)
  }

}

object RequestFields2 {

  def apply(method: uk.gov.hmrc.apidocumentation.models.apispecification.Method): Seq[RequestResponseField2] = RequestResponseField2.requestFields(method)
}

