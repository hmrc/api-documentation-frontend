/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.apidocumentation.views.helpers

import uk.gov.hmrc.apidocumentation.models.{EnumerationValue, JsonSchema}
import org.raml.v2.api.model.v10.datamodel.TypeDeclaration
import org.raml.v2.api.model.v10.methods.Method
import uk.gov.hmrc.apidocumentation.models

import scala.collection.JavaConversions._

case class EnumValue(
                      name: String,
                      description: Option[String] = None
                    )

case class RequestResponseField(
                                 name: String,
                                 title: Option[String],
                                 `type`: String,
                                 typeId: String,
                                 isArray: Boolean,
                                 required: Boolean,
                                 example: String,
                                 description: String,
                                 pattern: String,
                                 depth: Int,
                                 enumValues: Seq[EnumValue],
                                 oneOf: Map[String, Seq[RequestResponseField]]
                               )

trait RequestResponseFields {

  def extractFields(requestResponseBodies: Seq[TypeDeclaration], schemas: Map[String, JsonSchema]): Seq[RequestResponseField] = {
    val fields = for {
      body    <- requestResponseBodies
      schema  <- schema(body, schemas).toSeq
      result  <- extractFields(schema)
    } yield result
    fields
  }

  private def schema(body: TypeDeclaration, schemas: Map[String, JsonSchema]): Option[JsonSchema] = {
    body.`type`() match {
      case json if json.trim.startsWith("{") => Some(schemas(json))
      case _ => None
    }
  }

  private def extractFields(schema: JsonSchema,
                            title: Option[String] = None,
                            fieldName: Option[String] = None,
                            description: Option[String] = None,
                            required: Boolean = false,
                            depth: Int = -1,
                            acc: Seq[RequestResponseField] = Nil,
                            isArray: Boolean = false,
                            isPatternproperty: Boolean = false): Seq[RequestResponseField] = {

    def extractOneOfValues(schema: JsonSchema): Map[String, Seq[RequestResponseField]] = {
      val x: Seq[RequestResponseField] = for {
        oneOf <- schema.oneOf ++ schema.items.map(_.oneOf).getOrElse(Nil)
        (fieldName, definition) <- oneOf.properties
        x <- extractFields(definition, oneOf.title, Some(fieldName), definition.description, schema.required.contains(fieldName), depth + 1).toList
      } yield x

      x.groupBy(_.title.getOrElse(""))
    }

    def extractEnumValues(schema: JsonSchema): Seq[EnumValue] = {
      val enum = schema.enum.map( e => EnumValue(e.value))

      val oneOf: Seq[EnumValue] = schema.oneOf.flatMap { e =>
        e.enum.headOption.map(v => EnumValue(v.value, e.description)).toSeq
      }

      enum ++ oneOf
    }

    val currentField = fieldName match {
      case Some(name) if schema.`type` != "array" =>
        val fieldOrTitle = if (isPatternproperty) schema.title.getOrElse(name) else name
        Some(RequestResponseField(
          name = fieldOrTitle,
          title = title,
          `type` = schema.`type`.getOrElse(""),
          typeId = schema.id.getOrElse(""),
          isArray = isArray,
          required = required,
          example = schema.example.getOrElse(""),
          description = schema.description.orElse(description).getOrElse(""),
          pattern = schema.pattern.getOrElse(""),
          depth = depth,
          enumValues = extractEnumValues(schema),
          oneOf = extractOneOfValues(schema)
        ))

      case _ => None
    }

    schema.`type` match {
      case Some("object") => {
        val propertyFields = for {
          (fieldName, definition) <- schema.properties
          field <- extractFields(definition, title, Some(fieldName), None, schema.required.contains(fieldName), depth+1)
        } yield {
          field
        }

        val patternFields = for {
          (fieldName, definition) <- schema.patternProperties
          field <- extractFields(definition, title, Some(fieldName), None, schema.required.contains(fieldName), depth+1,
            isPatternproperty=true)
        } yield {
          field
        }

        currentField.fold(acc)(acc :+ _) ++ propertyFields ++ patternFields
      }
      case Some("array") => {
        extractFields(schema.items.get, title, fieldName, schema.description, required, depth, acc, true)
      }
      case _ => currentField.fold(acc)(acc :+ _)
    }
  }
}

object ResponseFields extends RequestResponseFields {
  def apply(method: Method, schemas: Map[String, JsonSchema]): Seq[RequestResponseField] = {
    val responseBodies = for {
      response <- Responses.success(method)
      body <- response.body
    } yield {
      body
    }

    extractFields(responseBodies, schemas)
  }
}

object RequestFields extends RequestResponseFields {
  def apply(method: Method, schemas: Map[String, JsonSchema]): Seq[RequestResponseField] = {
    extractFields(method.body, schemas)
  }
}

