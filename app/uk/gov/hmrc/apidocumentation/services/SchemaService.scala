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

package uk.gov.hmrc.apidocumentation.services

import java.net.URI

import org.raml.v2.api.model.v10.methods.Method
import play.api.libs.json.Json
import uk.gov.hmrc.apidocumentation.models.JsonSchema

import scala.collection.JavaConversions._
import scala.io.Source

class SchemaService {

  def loadSchemas(basePath: String, raml: RAML): Map[String, JsonSchema] = {

    val schemas = for {
      resource <- raml.flattenedResources
      method <- resource.methods
      schema <- payloadSchemas(method)
    } yield {
      schema -> parseSchema(schema, basePath)
    }

    schemas.toMap
  }

  protected def fetchPlainTextSchema(uri: String): String = {
    Source.fromURL(uri).mkString
  }

  private def fetchSchema(basePath: String, schemaPath: String): JsonSchema = {

    val schemaLocation = if(URI.create(schemaPath).isAbsolute) {
      schemaPath
    } else {
      URI.create(s"${basePath}/${schemaPath}").normalize.toString
    }

    val newBasePath = schemaLocation.substring(0, schemaLocation.lastIndexOf('/'))

    parseSchema(fetchPlainTextSchema(schemaLocation), newBasePath)
  }

  private def payloadSchemas(method: Method): Seq[String] = {
    val requestTypes = method.body.map(_.`type`)
    val responseTypes = method.responses.flatMap(_.body).map(_.`type`)

    (requestTypes ++ responseTypes).filter(_.trim.startsWith("{"))
  }

  private def parseSchema(schema: String, basePath: String): JsonSchema = {
    val jsonSchema = Json.parse(schema).as[JsonSchema]
    if(schema.contains("$ref")) resolveRefs(jsonSchema, basePath, jsonSchema) else jsonSchema
  }

  private def resolveRefs(schema: JsonSchema, basePath: String, enclosingSchema: JsonSchema): JsonSchema = {
    def splitJsonPointer(jsonPointer: String): Seq[String] = {
      jsonPointer.dropWhile(_ == '/').split("/")
    }

    def resolveRefsInSubschemas(subschemas: Map[String, JsonSchema], basePath: String, enclosingSchema: JsonSchema): Map[String, JsonSchema] = {
      subschemas.map { case (name, subSchema) =>
        name -> resolveRefs(subSchema, basePath, enclosingSchema)
      }
    }

    schema.ref match {
      case Some(ref) => {
        val (referredSchemaPath, jsonPointerPathParts) = ref.split('#') match {
          case Array(referredSchemaPath, jsonPointer) => {
            (referredSchemaPath, splitJsonPointer(jsonPointer))
          }
          case Array(referredSchemaPath) => {
            (referredSchemaPath, Nil)
          }
        }

        val referredSchema = referredSchemaPath match {
          case "" => enclosingSchema
          case _ => fetchSchema(basePath, referredSchemaPath)
        }

        val referredSubSchema = findSubschema(jsonPointerPathParts, referredSchema)
        schema.description.fold(referredSubSchema)(description => referredSubSchema.copy(description = Some(description)))
      }
      case _ => {
        val properties = resolveRefsInSubschemas(schema.properties, basePath, enclosingSchema)
        val patternProperties = resolveRefsInSubschemas(schema.patternProperties, basePath, enclosingSchema)
        val definitions = resolveRefsInSubschemas(schema.definitions, basePath, enclosingSchema)
        val items = schema.items.map(resolveRefs(_, basePath, enclosingSchema))

        schema.copy(properties = properties, patternProperties = patternProperties,
          items = items, definitions = definitions, ref = None)
      }
    }
  }

  private def findSubschema(pathParts: Seq[String], schema: JsonSchema): JsonSchema = {
    pathParts match {
      case "definitions" +: pathPart +: Nil => {
        schema.definitions(pathPart)
      }
      case "definitions" +: pathPart +: remainingPathParts => {
        findSubschema(remainingPathParts, schema.definitions(pathPart))
      }
      case pathPart +: Nil => {
        schema.properties(pathPart)
      }
      case pathPart +: remainingPath => {
        findSubschema(remainingPath, schema.properties(pathPart))
      }
      case _ => schema
    }
  }
}

