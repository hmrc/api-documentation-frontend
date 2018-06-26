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

package unit.uk.gov.hmrc.apidocumentation.views.helpers

import org.raml.v2.api.model.v10.api.Api
import org.scalatest.{Matchers, WordSpec}
import uk.gov.hmrc.apidocumentation.views.helpers.{QueryParams, UriParams}
import unit.uk.gov.hmrc.apidocumentation.utils.StringRamlLoader

class MethodParametersHelpersSpec extends WordSpec with Matchers {

  "UriParams" should {
    val simpleRaml =
      """#%RAML 1.0
        |---
        |title: Trivial Doc
        |/cust:
        |  /{cust_id}:
        |    uriParameters:
        |      cust_id:
        |        displayName: Customer Id
        |        type: string
        |        description: The customer's id
        |        example: "ABC123"
        |
      """.stripMargin

    "Return the uri parameters of a simple resource" in {
      val ramlApi: Api = new StringRamlLoader().load(simpleRaml).get
      val resource = ramlApi.resources().get(0).resources().get(0)
      resource.resourcePath() shouldBe "/cust/{cust_id}"
      val params = UriParams(resource, ramlApi)
      params.length shouldBe 1
      params(0).name shouldBe "cust_id"
    }

    val complexRaml =
      """#%RAML 1.0
        |---
        |title: Trivial Doc
        |/cust:
        |  /{cust_id}:
        |    uriParameters:
        |      cust_id:
        |        displayName: Customer Id
        |        type: string
        |        description: The customer's id
        |        example: "ABC123"
        |    /order:
        |      /{order_id}:
        |        uriParameters:
        |          order_id:
        |            displayName: Order Id
        |            type: string
        |            description: The id of the order
        |            example: "123-456-7890"
        |        /other:
        |          /{other_id}:
        |            uriParameters:
        |              other_id:
        |                displayName: Other Id
        |                type: string
        |                description: The id of the other thing
        |                example: "XYZ 99 00 ss"
        |
      """.stripMargin

    "walk back up the resource tree and return a list of the uri parameters" in {
      val ramlApi: Api = new StringRamlLoader().load(complexRaml).get
      val resource = ramlApi.resources().get(0).resources().get(0).resources().get(0)
        .resources().get(0).resources().get(0).resources().get(0)
      resource.resourcePath() shouldBe "/cust/{cust_id}/order/{order_id}/other/{other_id}"
      val params = UriParams(resource, ramlApi)
      params.length shouldBe 3
      params(0).name shouldBe "cust_id"
      params(1).name shouldBe "order_id"
      params(2).name shouldBe "other_id"
    }

    "null resources should return an empty list" in {
      val params = UriParams(null, null)
      params.isEmpty shouldBe true
    }

    "look through a custom RAML type to the underlying type" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |types:
          |  customerId:
          |    type: integer
          |
          |/cust:
          |  /{cust_id}:
          |    uriParameters:
          |      cust_id:
          |        displayName: Customer Id
          |        type: customerId
          |        description: The customer's id
          |        example: "54321"
          |
        """.stripMargin

      val ramlApi: Api = new StringRamlLoader().load(raml).get
      val resource = ramlApi.resources().get(0).resources().get(0)
      val params = UriParams(resource, ramlApi)
      params.length shouldBe 1
      params(0).name shouldBe "cust_id"
      params(0).typeName shouldBe "customerId"
      params(0).baseTypeName shouldBe "integer"
      params(0).description.value shouldBe "The customer's id"
      params(0).example.value shouldBe "54321"
    }

    "look through a custom RAML type to the underlying type, defaulting to string when no explicit type declared" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |types:
          |  customerId:
          |
          |/cust:
          |  /{cust_id}:
          |    uriParameters:
          |      cust_id:
          |        displayName: Customer Id
          |        type: customerId
          |        description: The customer's id
          |        example: "54321"
          |
        """.stripMargin

      val ramlApi: Api = new StringRamlLoader().load(raml).get
      val resource = ramlApi.resources().get(0).resources().get(0)
      val params = UriParams(resource, ramlApi)
      params.length shouldBe 1
      params(0).name shouldBe "cust_id"
      params(0).typeName shouldBe "customerId"
      params(0).baseTypeName shouldBe "string"
      params(0).description.value shouldBe "The customer's id"
      params(0).example.value shouldBe "54321"
      params(0).pattern shouldBe None
      params(0).enumValues shouldBe Nil
    }

    "extract the pattern regex for a custom RAML type" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |types:
          |  customerId:
          |    pattern: "^[A-Z]{3}\\d{3}[A-Z]{3}$"
          |/cust:
          |  /{cust_id}:
          |    uriParameters:
          |      cust_id:
          |        displayName: Customer Id
          |        type: customerId
          |        description: The customer's id
          |        example: "ABC123XYZ"
          |
        """.stripMargin

      val ramlApi: Api = new StringRamlLoader().load(raml).get
      val resource = ramlApi.resources().get(0).resources().get(0)
      val params = UriParams(resource, ramlApi)
      params.length shouldBe 1
      params(0).name shouldBe "cust_id"
      params(0).baseTypeName shouldBe "string"
      params(0).description.value shouldBe "The customer's id"
      params(0).example.value shouldBe "ABC123XYZ"
      params(0).pattern shouldBe Some("^[A-Z]{3}\\d{3}[A-Z]{3}$")
      params(0).enumValues shouldBe Nil
    }

    "extract the enum values for a custom RAML type" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |types:
          |  customerType:
          |    enum: [individual, business]
          |/cust:
          |  /{custType}:
          |    uriParameters:
          |      custType:
          |        displayName: Customer type
          |        type: customerType
          |        description: The customer type
          |        example: "individual"
          |
        """.stripMargin

      val ramlApi: Api = new StringRamlLoader().load(raml).get
      val resource = ramlApi.resources().get(0).resources().get(0)
      val params = UriParams(resource, ramlApi)
      params.length shouldBe 1
      params(0).name shouldBe "custType"
      params(0).baseTypeName shouldBe "string"
      params(0).description.value shouldBe "The customer type"
      params(0).example.value shouldBe "individual"
      params(0).pattern shouldBe None
      params(0).enumValues shouldBe Seq("individual", "business")
    }

    "allow types to be imported from a library" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |uses:
          |  types: unit/raml/types.raml
          |/cust:
          |  /{taxYear}:
          |    uriParameters:
          |      taxYear:
          |        displayName: Tax year
          |        type: types.taxYear
          |        description: The tax year
          |        example: 2016-17
          |
        """.stripMargin

      val ramlApi: Api = new StringRamlLoader().load(raml).get
      val resource = ramlApi.resources().get(0).resources().get(0)
      val params = UriParams(resource, ramlApi)
      params.length shouldBe 1
      params(0).name shouldBe "taxYear"
      params(0).typeName shouldBe "taxYear"
      params(0).baseTypeName shouldBe "string"
      params(0).example.value shouldBe "2016-17"
    }

    "use the example from a type imported from a library if no example is provided locally" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |uses:
          |  types: unit/raml/types.raml
          |/cust:
          |  /{taxYear}:
          |    uriParameters:
          |      taxYear:
          |        displayName: Tax year
          |        type: types.taxYear
          |        description: The tax year
          |
        """.stripMargin

      val ramlApi: Api = new StringRamlLoader().load(raml).get
      val resource = ramlApi.resources().get(0).resources().get(0)
      val params = UriParams(resource, ramlApi)
      params.length shouldBe 1
      params(0).name shouldBe "taxYear"
      params(0).typeName shouldBe "taxYear"
      params(0).baseTypeName shouldBe "string"
      params(0).example.value shouldBe "2014-15"
    }

    "map date-only to date" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |/cust:
          |  /{date}:
          |    uriParameters:
          |      date:
          |        type: date-only
          |
        """.stripMargin

      val ramlApi: Api = new StringRamlLoader().load(raml).get
      val resource = ramlApi.resources().get(0).resources().get(0)
      val params = UriParams(resource, ramlApi)
      params.length shouldBe 1
      params(0).baseTypeName shouldBe "date"
    }
  }

  "QueryParams" should {
    "return an empty list when the method has no query parameters" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |/cust:
          |  get:
        """.stripMargin
      val ramlApi: Api = new StringRamlLoader().load(raml).get
      val method = ramlApi.resources().get(0).methods().get(0)
      val params = QueryParams(method, ramlApi)
      params.isEmpty shouldBe true
    }

    "return the parameter list when the method has query parameters" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |/cust:
          |  get:
          |    queryParameters:
          |      utr:
          |        type: string
          |        description: The Unique Taxpayer Reference
          |        example: "ABC123"
          |        required: true
          |      taxYear:
          |        type: string
          |        description: The tax year
          |        example: 2014-15
          |        required: true
        """.stripMargin
      val ramlApi: Api = new StringRamlLoader().load(raml).get
      val method = ramlApi.resources().get(0).methods().get(0)
      val params = QueryParams(method, ramlApi)

      params.length shouldBe 2

      params(0).name shouldBe "utr"
      params(1).name shouldBe "taxYear"
    }

    "return an empty list when null method" in {
      val params = QueryParams(null, null)
      params.isEmpty shouldBe true
    }
  }

}