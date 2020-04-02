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

package unit.uk.gov.hmrc.apidocumentation.views.helpers

import uk.gov.hmrc.apidocumentation.models.JsonSchema
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json
import uk.gov.hmrc.apidocumentation.views.helpers.{EnumValue, RequestFields, RequestResponseField, ResponseFields}
import unit.uk.gov.hmrc.apidocumentation.utils.StringRamlLoader

class PayloadFieldsHelpersSpec extends WordSpec with Matchers {

  def verifyRequestResponseField(field: RequestResponseField, name: String = "", description: String = "",
                                 `type`: String = "", typeId: String = "",
                                 isArray: Boolean = false, example: String = "", pattern: String = "",
                                 required: Boolean = false, depth: Int = 0, enumValues: Seq[EnumValue] = Nil) = {

    field.name shouldBe name
    field.description shouldBe description
    field.`type` shouldBe `type`
    field.typeId shouldBe typeId
    field.isArray shouldBe isArray
    field.pattern shouldBe pattern
    field.example shouldBe example
    field.required shouldBe required
    field.depth shouldBe depth
    field.enumValues shouldBe enumValues
  }

  def schemaMap(schema: String): Map[String, JsonSchema] = {
    if (schema.trim.startsWith("{")) Map(schema -> Json.parse(schema).as[JsonSchema]) else Map()
  }

  def getResponseFields(raml: String): Seq[RequestResponseField] = {
    val ramlApi = new StringRamlLoader().load(raml).get
    val method = ramlApi.resources().get(0).methods().get(0)
    val schema = method.responses.get(0).body.get(0).`type`

    ResponseFields(method, schemaMap(schema))
  }

  def getRequestFields(raml: String): Seq[RequestResponseField] = {
    val ramlApi = new StringRamlLoader().load(raml).get
    val method = ramlApi.resources().get(0).methods().get(0)
    val schema = method.body.get(0).`type`

    RequestFields(method, schemaMap(schema))
  }

  "ResponseFields helper" should {

    "return the response fields defined in the JSON schema" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |/cust:
          |  get:
          |    responses:
          |      200:
          |        body:
          |          application/json:
          |            type: |
          |              {
          |                "$schema": "http://json-schema.org/draft-04/schema#",
          |                "description": "National Insurance annual summary",
          |                "type": "object",
          |                "properties": {
          |                  "totalNICableEarnings" : {
          |                    "description": "Total NICable earnings",
          |                    "type": "number",
          |                    "example": "10.00"
          |                  },
          |                  "maxNICsReached":  {
          |                    "description": "A flag",
          |                    "type": "boolean",
          |                    "example": "false"
          |                  },
          |                  "nino": {
          |                    "description": "National Insurance number",
          |                    "type": "string",
          |                    "pattern": "^[A-Z]{2}[0-9]{6}[A-Z]$",
          |                    "example": "QQ123456A"
          |                  }
          |                },
          |                "required": [ "maxNICsReached", "nino" ]
          |              }
          |""".stripMargin
      val responseFields: Seq[RequestResponseField] = getResponseFields(raml)

      responseFields.length shouldBe 3

      verifyRequestResponseField(responseFields(0), name="totalNICableEarnings", description="Total NICable earnings",
        `type`="number", example="10.00")

      verifyRequestResponseField(responseFields(1), name="maxNICsReached", description="A flag",
        `type`="boolean",  example="false", required=true)

      verifyRequestResponseField(responseFields(2), name="nino", description="National Insurance number",
        `type`="string", pattern="^[A-Z]{2}[0-9]{6}[A-Z]$",  example="QQ123456A", required=true)
    }

    "return the pattern-based response fields defined in the JSON schema" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |/cust:
          |  get:
          |    responses:
          |      200:
          |        body:
          |          application/json:
          |            type: |
          |              {
          |                "$schema": "http://json-schema.org/draft-04/schema#",
          |                "description": "National Insurance annual summary",
          |                "type": "object",
          |                "patternProperties": {
          |                  "[0-9]{3}/[0-9a-zA-Z]{7}" : {
          |                    "title": "{empRef}",
          |                    "type": "number",
          |                    "example": "10.00"
          |                  },
          |                  "SS-[0-9]{4}":  {
          |                    "type": "boolean",
          |                    "example": "false"
          |                  }
          |                }
          |              }
          |""".stripMargin
      val responseFields = getResponseFields(raml)

      responseFields.length shouldBe 2

      verifyRequestResponseField(responseFields(0), name="{empRef}", description="",
        `type`="number", example="10.00")

      verifyRequestResponseField(responseFields(1), name="SS-[0-9]{4}", description="",
        `type`="boolean",  example="false")
    }

    "ignore the title for non-pattern properties" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |/cust:
          |  get:
          |    responses:
          |      200:
          |        body:
          |          application/json:
          |            type: |
          |              {
          |                "$schema": "http://json-schema.org/draft-04/schema#",
          |                "type": "object",
          |                "properties": {
          |                  "normal_prop": {
          |                    "title": "{empRef}",
          |                    "type": "number"
          |                  }
          |                }
          |              }
          |""".stripMargin
      val responseFields = getResponseFields(raml)

      responseFields.length shouldBe 1

      verifyRequestResponseField(responseFields(0), name="normal_prop", description="",
        `type`="number")
    }

    "use the ID defined on a type" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |/cust:
          |  get:
          |    responses:
          |      200:
          |        body:
          |          application/json:
          |            type: |
          |              {
          |                "$schema": "http://json-schema.org/draft-04/schema#",
          |                "type": "object",
          |                "properties": {
          |                  "activationDate": {
          |                    "description": "Date of activation",
          |                    "id": "full-date",
          |                    "type": "string"
          |                  }
          |                }
          |              }
          |""".stripMargin
      val responseFields = getResponseFields(raml)

      responseFields.length shouldBe 1

      verifyRequestResponseField(responseFields(0), name="activationDate", description="Date of activation",
        `type`="string", typeId="full-date")
    }

    "return no response fields when there are none defined" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |/cust:
          |  get:
          |    responses:
          |      200:
          |        body:
          |          application/json:
          |""".stripMargin
      val responseFields = getResponseFields(raml)

      responseFields.length shouldBe 0
    }

    "handle nested fields within the JSON schema" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |/cust:
          |  get:
          |    responses:
          |      200:
          |        body:
          |          application/json:
          |            type: |
          |              {
          |                "$schema": "http://json-schema.org/draft-04/schema#",
          |                "description": "National Insurance annual summary",
          |                "type": "object",
          |                "properties": {
          |                  "class1": {
          |                    "type": "object",
          |                    "properties": {
          |                      "totalNICableEarnings" : {
          |                        "description": "Total NICable earnings",
          |                        "type": "number",
          |                        "example": "10.00"
          |                      }
          |                    },
          |                    "required": [ "totalNICableEarnings" ]
          |                  }
          |                },
          |                "required": [ "class1" ]
          |              }
          |""".stripMargin

      val responseFields = getResponseFields(raml)

      responseFields.length shouldBe 2

      verifyRequestResponseField(responseFields(0), name="class1",
        `type`="object", required = true)

      verifyRequestResponseField(responseFields(1), name="totalNICableEarnings", description="Total NICable earnings",
        `type`="number", example="10.00", required = true, depth = 1)
    }

    "handle arrays of objects at the top level in the JSON schema" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |/cust:
          |  get:
          |    responses:
          |      200:
          |        body:
          |          application/json:
          |            type: |
          |              {
          |                "$schema": "http://json-schema.org/draft-04/schema#",
          |                "description": "National Insurance annual summary",
          |                "type": "array",
          |                "items": {
          |                  "type": "object",
          |                  "properties" : {
          |                    "totalNICableEarnings" : {
          |                      "description": "Total NICable earnings",
          |                      "type": "number",
          |                      "example": "10.00"
          |                    }
          |                  },
          |                  "required": [ "totalNICableEarnings" ]
          |                }
          |              }
          |""".stripMargin

      val responseFields = getResponseFields(raml)

      responseFields.length shouldBe 1

      verifyRequestResponseField(responseFields(0), name="totalNICableEarnings", description="Total NICable earnings",
        `type`="number", isArray = false, example="10.00", required = true)
    }

    "handle arrays of objects nested within the JSON schema" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |/cust:
          |  get:
          |    responses:
          |      200:
          |        body:
          |          application/json:
          |            type: |
          |              {
          |                "$schema": "http://json-schema.org/draft-04/schema#",
          |                "description": "National Insurance annual summary",
          |                "type": "object",
          |                "properties": {
          |                  "earnings": {
          |                    "description": "List of earnings",
          |                    "type": "array",
          |                    "items": {
          |                      "type": "object",
          |                      "properties" : {
          |                        "totalNICableEarnings" : {
          |                          "description": "Total NICable earnings",
          |                          "type": "number",
          |                          "example": "10.00"
          |                        }
          |                      },
          |                      "required": [ "totalNICableEarnings" ]
          |                    }
          |                  }
          |                },
          |                "required": []
          |              }
          |""".stripMargin

      val responseFields = getResponseFields(raml)

      responseFields.length shouldBe 2

      verifyRequestResponseField(responseFields(0), name="earnings", description="List of earnings",
        `type`="object", isArray = true)


      verifyRequestResponseField(responseFields(1), name="totalNICableEarnings", description="Total NICable earnings",
        `type`="number", example="10.00", required = true, depth = 1)
    }

    "handle arrays of simple types within the JSON schema" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |/cust:
          |  get:
          |    responses:
          |      200:
          |        body:
          |          application/json:
          |            type: |
          |              {
          |                "$schema": "http://json-schema.org/draft-04/schema#",
          |                "description": "National Insurance annual summary",
          |                "type": "object",
          |                "properties": {
          |                  "tags": {
          |                    "description": "Tags applicable",
          |                    "type": "array",
          |                    "items": {
          |                      "type": "string",
          |                      "example": "Red"
          |                    }
          |                  }
          |                },
          |                "required": [ "tags" ]
          |              }
          |""".stripMargin

      val responseFields = getResponseFields(raml)

      responseFields.length shouldBe 1

      verifyRequestResponseField(responseFields(0), name="tags", description="Tags applicable",
        `type`="string", isArray = true, example="Red", required = true)
    }

    "use the description defined on the simple type in an array if defined" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |/cust:
          |  get:
          |    responses:
          |      200:
          |        body:
          |          application/json:
          |            type: |
          |              {
          |                "$schema": "http://json-schema.org/draft-04/schema#",
          |                "description": "National Insurance annual summary",
          |                "type": "object",
          |                "properties": {
          |                  "tags": {
          |                    "type": "array",
          |                    "description": "Outer description",
          |                    "items": {
          |                      "description": "Inner description",
          |                      "type": "string",
          |                      "example": "Red"
          |                    }
          |                  }
          |                },
          |                "required": [ "tags" ]
          |              }
          |""".stripMargin

      val responseFields = getResponseFields(raml)

      responseFields.length shouldBe 1

      verifyRequestResponseField(responseFields(0), name="tags", description="Inner description",
        `type`="string", isArray = true, example="Red", required = true)
    }

    "handle enums defined directly in the schema" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |/cust:
          |  get:
          |    responses:
          |      200:
          |        body:
          |          application/json:
          |            type: |
          |              {
          |                "$schema": "http://json-schema.org/draft-04/schema#",
          |                "type": "object",
          |                "properties": {
          |                  "status" : {
          |                    "enum": ["DRAFT", "SUBMITTED"]
          |                  }
          |                }
          |              }
          |""".stripMargin
      val responseFields: Seq[RequestResponseField] = getResponseFields(raml)

      responseFields.length shouldBe 1

      verifyRequestResponseField(responseFields(0), name = "status",
        enumValues = Seq(EnumValue("DRAFT"), EnumValue("SUBMITTED"))
      )
    }

    "handle enums defined using oneOf in the schema" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |/cust:
          |  get:
          |    responses:
          |      200:
          |        body:
          |          application/json:
          |            type: |
          |              {
          |                "$schema": "http://json-schema.org/draft-04/schema#",
          |                "type": "object",
          |                "properties": {
          |                  "status" : {
          |                    "oneOf": [
          |                      { "enum": ["DRAFT"], "description": "Draft status" },
          |                      { "enum": ["SUBMITTED"], "description": "Final status" }
          |                    ]
          |                  }
          |                }
          |              }
          |""".stripMargin
      val responseFields: Seq[RequestResponseField] = getResponseFields(raml)

      responseFields.length shouldBe 1

      verifyRequestResponseField(responseFields(0), name = "status",
        enumValues = Seq(EnumValue("DRAFT", Some("Draft status")), EnumValue("SUBMITTED", Some("Final status")))
      )
    }
  }

  "RequestFields helper" should {

    "return the request fields defined in the JSON schema" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |/cust:
          |  get:
          |    body:
          |      application/json:
          |        type: |
          |          {
          |            "$schema": "http://json-schema.org/draft-04/schema#",
          |            "description": "National Insurance annual summary",
          |            "type": "object",
          |            "properties": {
          |              "totalNICableEarnings" : {
          |                "description": "Total NICable earnings",
          |                "type": "number",
          |                "example": "10.00"
          |              },
          |              "maxNICsReached":  {
          |                "description": "A flag",
          |                "type": "boolean",
          |                "example": "false"
          |              }
          |            },
          |            "required": [ "maxNICsReached" ]
          |          }
          |""".stripMargin
      val requestFields = getRequestFields(raml)

      requestFields.length shouldBe 2

      verifyRequestResponseField(requestFields(0), name= "totalNICableEarnings", description= "Total NICable earnings",
        `type`="number", example="10.00")

      verifyRequestResponseField(requestFields(1), name= "maxNICsReached", description="A flag",
        `type`="boolean", example="false", required =true)
    }
  }

}
