/*
 * Copyright 2020 HM Revenue & Customs
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

import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json

import scala.collection.immutable.ListMap

class JsonSchemaSpec extends WordSpec with Matchers {

  def parseSchema(schema: String) = { Json.parse(schema).as[JsonSchema] }

  "JsonSchema" should {

    "support scalar types" in {
      val actual = parseSchema(
        """
          |{
          |    "$schema": "http://json-schema.org/schema",
          |    "title": "Business name",
          |    "description": "Name of the business",
          |    "type": "string"
          |}
        """.stripMargin)

      actual shouldBe JsonSchema(title = Some("Business name"), description = Some("Name of the business"), `type` = Some("string"))
    }

    "support provision of an example" in {
      val actual = parseSchema(
        """
          |{
          |    "$schema": "http://json-schema.org/schema",
          |    "description": "Name of the business",
          |    "example": "Json Limited",
          |    "type": "string"
          |}
        """.stripMargin)

      actual shouldBe JsonSchema(description = Some("Name of the business"), example = Some("Json Limited"), `type` = Some("string"))
    }

    "support objects with properties" in {
      val actual = parseSchema(
        """
          |{
          |    "$schema": "http://json-schema.org/schema",
          |    "type": "object",
          |    "properties": {
          |        "name": {
          |            "description": "Business name",
          |            "type": "string"
          |        },
          |        "activities": {
          |            "description": "Activities of the business",
          |            "type": "string"
          |        }
          |    }
          |}
        """.stripMargin)

      actual shouldBe JsonSchema(`type` = Some("object"), properties = ListMap(
        "name" -> JsonSchema(description = Some("Business name"), `type` = Some("string")),
        "activities" -> JsonSchema(description = Some("Activities of the business"), `type` = Some("string"))
      ))
    }

    "support objects containing objects" in {
      val actual = parseSchema(
        """
          |{
          |    "$schema": "http://json-schema.org/schema",
          |    "type": "object",
          |    "properties": {
          |        "name": {
          |            "description": "Business name",
          |            "type": "object",
          |            "properties": {
          |              "registeredName" : {
          |                  "description": "The registered name for the business",
          |                  "type": "string"
          |              },
          |              "tradingName": {
          |                  "description": "The name the business trades by",
          |                  "type": "string"
          |              }
          |            }
          |        },
          |        "activities": {
          |            "description": "Activities of the business",
          |            "type": "string"
          |        }
          |    }
          |}
        """.stripMargin)

      actual shouldBe JsonSchema(`type` = Some("object"),
        properties = ListMap(
          "name" -> JsonSchema(description = Some("Business name"), `type` = Some("object"),
            properties = ListMap(
              "registeredName" -> JsonSchema(description = Some("The registered name for the business"), `type` = Some("string")),
              "tradingName" -> JsonSchema(description = Some("The name the business trades by"), `type` = Some("string"))
            )
          ),
          "activities" -> JsonSchema(description = Some("Activities of the business"), `type` = Some("string"))
        )
      )
    }

    "support objects with patternProperties" in {
      val actual = parseSchema(
        """
          |{
          |    "$schema": "http://json-schema.org/schema",
          |    "type": "object",
          |    "patternProperties": {
          |        "[A-Z0-9]{4}-[A-Z0-9]{5}": {
          |            "description": "First",
          |            "type": "string"
          |        },
          |        "\\d{5}-\\d{2}": {
          |            "description": "Second",
          |            "type": "string"
          |        }
          |    }
          |}
        """.stripMargin)

      actual shouldBe JsonSchema(`type` = Some("object"),
        patternProperties = ListMap("[A-Z0-9]{4}-[A-Z0-9]{5}" -> JsonSchema(description = Some("First"), `type` = Some("string")),
          "\\d{5}-\\d{2}" -> JsonSchema(description = Some("Second"), `type` = Some("string")))
      )
    }

    "support specifying required properties" in {
      val actual = parseSchema(
        """
          |{
          |    "$schema": "http://json-schema.org/schema",
          |    "type": "object",
          |    "properties": {
          |        "name": {
          |            "description": "Business name",
          |            "type": "string"
          |        },
          |        "activities": {
          |            "description": "Activities of the business",
          |            "type": "string"
          |        }
          |    },
          |    "required": ["name"]
          |}
        """.stripMargin)

      actual shouldBe JsonSchema(`type` = Some("object"),
        properties = ListMap("name" -> JsonSchema(description = Some("Business name"), `type` = Some("string")),
          "activities" -> JsonSchema(description = Some("Activities of the business"), `type` = Some("string"))),
        required = Seq("name")
      )
    }

    "support arrays" in {
      val actual = parseSchema(
        """
          |{
          |    "$schema": "http://json-schema.org/schema",
          |    "type": "array",
          |    "items": {
          |        "description": "Business name",
          |        "type": "string"
          |    }
          |}
        """.stripMargin)

      actual shouldBe JsonSchema(`type` = Some("array"),
        items = Some(JsonSchema(description = Some("Business name"), `type` = Some("string")))
      )
    }

    "support definitions" in {
      val actual = parseSchema(
        """
          |{
          |    "$schema": "http://json-schema.org/schema",
          |    "definitions": {
          |        "name": {
          |            "description": "Business name",
          |            "type": "string"
          |        },
          |        "activities": {
          |            "description": "Activities of the business",
          |            "type": "string"
          |        }
          |    }
          |}
        """.stripMargin)

      actual shouldBe JsonSchema(definitions = ListMap(
        "name" -> JsonSchema(description = Some("Business name"), `type` = Some("string")),
        "activities" -> JsonSchema(description = Some("Activities of the business"), `type` = Some("string"))
      ))
    }

    "support references" in {
      val actual = parseSchema(
        """
          |{
          |    "$schema": "http://json-schema.org/schema",
          |    "type": "object",
          |    "properties": {
          |        "name": {
          |            "$ref": "#/definitions/name"
          |        }
          |    },
          |    "definitions": {
          |        "name": {
          |            "description": "Business name",
          |            "type": "string"
          |        }
          |    }
          |}
        """.stripMargin)

      actual shouldBe JsonSchema(`type` = Some("object"),
        properties = ListMap(
          "name" -> JsonSchema(ref = Some("#/definitions/name"))
        ),
        definitions = ListMap(
          "name" -> JsonSchema(description = Some("Business name"), `type` = Some("string"))
        )
      )
    }

    "support enums" in {
      val actual = parseSchema(
        """
          |{
          |    "$schema": "http://json-schema.org/schema",
          |    "description": "Transaction type",
          |    "type": "string",
          |    "enum": ["CREDIT", "DEBIT"]
          |}
        """.stripMargin)

      actual shouldBe JsonSchema(
        description = Some("Transaction type"),
        `type` = Some("string"),
        enum = Seq(EnumerationValue("CREDIT"), EnumerationValue("DEBIT")))
    }

    "support oneOf for specifying enums with descriptions" in {
      val actual = parseSchema(
        """
          |{
          |    "$schema": "http://json-schema.org/schema",
          |    "description": "Transaction type",
          |    "oneOf": [
          |        { "enum": ["CREDIT"], "description": "A credit" },
          |        { "enum": ["DEBIT"], "description": "A debit" }
          |    ]
          |}
        """.stripMargin)

      actual shouldBe JsonSchema(description = Some("Transaction type"),
        oneOf = Seq(
          JsonSchema(enum = Seq(EnumerationValue("CREDIT")), description = Some("A credit")),
          JsonSchema(enum = Seq(EnumerationValue("DEBIT")), description = Some("A debit"))
        )
      )
    }

    "support specifying a pattern" in {
      val actual = parseSchema(
        """
          |{
          |    "$schema": "http://json-schema.org/schema",
          |    "description": "Name of the business",
          |    "type": "string",
          |    "pattern": "^[A-Z0-9]{6}$"
          |}
        """.stripMargin)

      actual shouldBe JsonSchema(description = Some("Name of the business"), pattern = Some("^[A-Z0-9]{6}$"), `type` = Some("string"))
    }

    "support specifying an ID for the type" in {
      val actual = parseSchema(
        """
          |{
          |    "$schema": "http://json-schema.org/schema",
          |    "description": "Date",
          |    "type": "string",
          |    "id": "full-date"
          |}
        """.stripMargin)

      actual shouldBe JsonSchema(description = Some("Date"), id = Some("full-date"), `type` = Some("string"))
    }
  }
}
