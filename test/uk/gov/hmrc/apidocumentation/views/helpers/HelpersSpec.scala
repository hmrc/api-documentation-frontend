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

package uk.gov.hmrc.apidocumentation.views.helpers

import org.raml.v2.api.model.v10.api.Api
import org.raml.v2.api.model.v10.system.types.AnnotableSimpleType
import org.scalatest.{Matchers, WordSpec}
import uk.gov.hmrc.apidocumentation.utils.StringRamlLoader

import scala.collection.JavaConverters._

case class Wrapper(data: String) {
  def value() = {
    data
  }
}

class HelpersSpec extends WordSpec with Matchers {
  "Slugify" should {
    "create an HTML/URI safe string from a simple string" in {
      Slugify("simple string") shouldEqual ("simple-string")
    }

    "create a safe string from a more complex string" in {
      Slugify("Spaces and cApS in HeRe") shouldEqual ("spaces-and-caps-in-here")
    }

    "create a safe string from a string with random characters" in {
      Slugify("Sp4ce$ and c%pS....in He&e") shouldEqual ("sp4ce-and-cpsin-hee")
    }
  }

  "Markdown" should {
    "Process markdown text into HTML fragments" in {
      Markdown("This should be **bold**").body.trim shouldEqual "<p>This should be <strong>bold</strong></p>"
      Markdown(
        """List:
          |* one
          |* two""".stripMargin).body.replaceAll("\\s+", "") shouldEqual
        """<p>List:</p>
          |<ul class="list list-bullet"><li>one</li>
          |<li>two</li>
          |</ul>""".stripMargin.replaceAll("\\s+", "")
    }
    "Process blocks of text into paragraphs" in {
      Markdown(
        """
          |Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nulla convallis
          |porta sapien ut fermentum. Mauris sed orci magna. Donec sagittis metus quam,
          |a placerat turpis vulputate in. Pellentesque nec mollis augue. Nullam rutrum
          |vel ligula sed pulvinar. Integer eget nisi diam. Nulla facilisi. Maecenas eget
          |ullamcorper magna. Nullam facilisis vitae felis quis egestas. Vestibulum interdum
          |quam sed risus feugiat faucibus. Cras a purus id metus mollis molestie. Aliquam
          |quis pellentesque orci. In eget turpis non lacus accumsan suscipit. Cras vitae
          |vestibulum ipsum.
          |
          |Duis eu nisl id dolor efficitur ornare. Fusce tincidunt ligula lacus, in congue
          |ex hendrerit in. Praesent eget eleifend lorem. In imperdiet fringilla orci, a
          |facilisis mauris lobortis vel. Quisque vitae blandit risus. Integer in velit
          |vulputate, suscipit leo quis, fringilla sapien. Etiam lectus odio, porttitor
          |at nunc id, lacinia porttitor purus. Integer in justo leo. Quisque turpis sapien,
          |sagittis non sagittis eget, cursus eu augue. Maecenas malesuada venenatis lectus
          |vitae semper. Fusce vehicula turpis scelerisque porttitor finibus. Nullam lacinia
          |eros nulla, in tristique elit laoreet nec. Nulla et elit in elit dapibus ultricies
          |eget sit amet nulla. Nam eleifend risus risus, sit amet feugiat magna viverra eget.
          | """.stripMargin).body.trim shouldEqual
        """<p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nulla convallis
          |porta sapien ut fermentum. Mauris sed orci magna. Donec sagittis metus quam,
          |a placerat turpis vulputate in. Pellentesque nec mollis augue. Nullam rutrum
          |vel ligula sed pulvinar. Integer eget nisi diam. Nulla facilisi. Maecenas eget
          |ullamcorper magna. Nullam facilisis vitae felis quis egestas. Vestibulum interdum
          |quam sed risus feugiat faucibus. Cras a purus id metus mollis molestie. Aliquam
          |quis pellentesque orci. In eget turpis non lacus accumsan suscipit. Cras vitae
          |vestibulum ipsum.</p>
          |<p>Duis eu nisl id dolor efficitur ornare. Fusce tincidunt ligula lacus, in congue
          |ex hendrerit in. Praesent eget eleifend lorem. In imperdiet fringilla orci, a
          |facilisis mauris lobortis vel. Quisque vitae blandit risus. Integer in velit
          |vulputate, suscipit leo quis, fringilla sapien. Etiam lectus odio, porttitor
          |at nunc id, lacinia porttitor purus. Integer in justo leo. Quisque turpis sapien,
          |sagittis non sagittis eget, cursus eu augue. Maecenas malesuada venenatis lectus
          |vitae semper. Fusce vehicula turpis scelerisque porttitor finibus. Nullam lacinia
          |eros nulla, in tristique elit laoreet nec. Nulla et elit in elit dapibus ultricies
          |eget sit amet nulla. Nam eleifend risus risus, sit amet feugiat magna viverra eget.</p>
          | """.stripMargin.trim
    }

    "Process null values into empty strings" in {
      Markdown(null: AnnotableSimpleType[String]).body.trim shouldEqual ""
    }
  }

  "Val" should {
    "Pass through a simple String" in {
      Val("This String") shouldBe "This String"
    }

    "Print the value of a wrapped String" in {
      Val(Wrapper("This String")) shouldBe "This String"
    }

    "Return an empty String if null is passed" in {
      Val(null: String) shouldBe ""
    }

    "Return an empty String if null is passed as a Wrapper" in {
      Val(null: Wrapper) shouldBe ""
    }
  }

  "Annotation" should {
    "Get a simple annotation" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |annotationTypes:
          |  thing: string
          |  other: integer
          |(thing): Hello
          |(other): 77
        """.stripMargin

      val ramlApi = new StringRamlLoader().load(raml).get

      Annotation.exists(ramlApi, "(thing)") shouldBe true
      Annotation(ramlApi, "(thing)") shouldBe "Hello"
      Annotation(ramlApi, "(other)") shouldBe "77"
    }

    val complexRaml =
      """#%RAML 1.0
        |---
        |title: Trivial Doc
        |annotationTypes:
        |  thing:
        |    type: object
        |    properties:
        |      name: string
        |      desc: string
        |      nested:
        |        type: object
        |        properties:
        |          deepname: string
        |          reallydeep:
        |            type: object
        |            properties:
        |              name: string
        |      count?: integer
        |      optional?: string
        |
        |(thing):
        |  name: Brian
        |  desc: Complex annotation test
        |  nested:
        |    deepname: Gertrude
        |    reallydeep:
        |      name: RealDeepMan
        |  count: 7
        |
      """.stripMargin
    val ramlApi = new StringRamlLoader().load(complexRaml).get

    "Get a complex annotation and it's parameters" in {
      Annotation.exists(ramlApi, "(thing)") shouldBe true
      Annotation(ramlApi, "(thing)") should include("model.TypeInstance")
      Annotation(ramlApi, "(thing)", "name") shouldBe "Brian"
      Annotation.exists(ramlApi, "(thing)", "count") shouldBe true
      Annotation(ramlApi, "(thing)", "count") shouldBe "7"
    }

    "non existent annotation parameters should come back as false" in {
      Annotation.exists(ramlApi, "(thing)", "not_here") shouldBe false
    }

    "deep paths in the annotation can be traversed" in {
      Annotation.exists(ramlApi, "(thing)", "nested", "deepname") shouldBe true
      Annotation(ramlApi, "(thing)", "nested", "deepname") shouldBe "Gertrude"
    }

    "really rather deep paths in the annotation can be traversed" in {
      Annotation.exists(ramlApi, "(thing)", "nested", "reallydeep", "name") shouldBe true
      Annotation(ramlApi, "(thing)", "nested", "reallydeep", "name") shouldBe "RealDeepMan"
    }

    "Get a namespace scoped annotation" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |uses:
          |  annotations: raml/annotations.raml
          |
          |(annotations.thing): Hello
        """.stripMargin

      val ramlApi = new StringRamlLoader().load(raml).get

      Annotation.exists(ramlApi, "(thing)") shouldBe true
      Annotation(ramlApi, "(thing)") shouldBe "Hello"
    }
  }

  "GroupResources helper" should {

    def verifyGroup(group: ResourceGroup, name: Option[String] = None, description: Option[String] = None, resourcePaths: Seq[String]) = {
      group.name shouldBe name
      group.description shouldBe description
      group.resources.map(_.resourcePath) shouldBe resourcePaths
    }

    "Return the endpoints in one unnamed group when no grouping applied" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |/root:
          |  /first:
          |    /inner:
          |  /second:
        """.stripMargin

      val resources = new StringRamlLoader().load(raml).get.resources()

      val groupedResources = GroupedResources(resources.asScala)

      groupedResources.length shouldBe 1

      verifyGroup(groupedResources(0), resourcePaths = Seq("/root", "/root/first", "/root/first/inner", "/root/second"))
    }

    "Return the endpoints in one group when a single group exists" in {
      val raml =
        """#%RAML 1.0
          |---
          |annotationTypes:
          |  group:
          |    type: object
          |    properties:
          |      name: string
          |      description: string
          |
          |title: Trivial Doc
          |/root:
          |  (group):
          |    name: The Group
          |    description: The One and Only
          |  /first:
          |    /inner:
          |  /second:
        """.stripMargin

      val resources = new StringRamlLoader().load(raml).get.resources()

      val groupedResources = GroupedResources(resources.asScala)

      groupedResources.length shouldBe 1

      verifyGroup(groupedResources(0), Some("The Group"), Some("The One and Only"), resourcePaths = Seq("/root", "/root/first", "/root/first/inner", "/root/second"))
    }

    "Return the endpoints in their groups" in {
      val raml =
        """#%RAML 1.0
          |---
          |annotationTypes:
          |  group:
          |    type: object
          |    properties:
          |      name: string
          |      description: string
          |
          |title: Trivial Doc
          |/root:
          |  (group):
          |    name: Root Group
          |    description: The root of it all
          |  /first:
          |    (group):
          |      name: First Child Group
          |      description: First among equals
          |    /inner:
          |  /second:
          |    (group):
          |      name: Second Child Group
          |      description: Last to the party again
        """.stripMargin

      val resources = new StringRamlLoader().load(raml).get.resources()

      val groupedResources = GroupedResources(resources.asScala)

      groupedResources.length shouldBe 3

      verifyGroup(groupedResources(0), Some("Root Group"), Some("The root of it all"), resourcePaths = Seq("/root"))
      verifyGroup(groupedResources(1), Some("First Child Group"), Some("First among equals"), resourcePaths = Seq("/root/first", "/root/first/inner"))
      verifyGroup(groupedResources(2), Some("Second Child Group"), Some("Last to the party again"), resourcePaths = Seq("/root/second"))
    }
  }

  "Methods helper" should {
    "Return methods ordered GET, POST, PUT, DELETE" in {
      val small_raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |/root:
          |  delete:
          |  get:
        """.stripMargin

      val ordered_raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |/root:
          |  get:
          |  post:
          |  put:
          |  delete:
        """.stripMargin

      val unordered_raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |/root:
          |  put:
          |  delete:
          |  post:
          |  get:
        """.stripMargin

      val ramlApi: Api = new StringRamlLoader().load(ordered_raml).get
      val resource = ramlApi.resources().get(0)
      val methods = Methods(resource)

      methods.map(_.method()) shouldBe List("get", "post", "put", "delete")

      val ramlApi2: Api = new StringRamlLoader().load(unordered_raml).get
      val resource2 = ramlApi2.resources().get(0)
      val methods2 = Methods(resource2)

      methods2.map(_.method()) shouldBe List("get", "post", "put", "delete")

      val ramlApi3: Api = new StringRamlLoader().load(small_raml).get
      val resource3 = ramlApi3.resources().get(0)
      val methods3 = Methods(resource3)

      methods3.map(_.method()) shouldBe List("get", "delete")
    }

    "All methods should be ordered" in {
      val unordered_raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |/root:
          |  put:
          |  options:
          |  delete:
          |  post:
          |  head:
          |  get:
          |  patch:
        """.stripMargin

      val ramlApi2: Api = new StringRamlLoader().load(unordered_raml).get
      val resource2 = ramlApi2.resources().get(0)
      val methods2 = Methods(resource2)

      methods2.map(_.method()) shouldBe List("get", "post", "put", "delete", "head", "patch", "options")

    }
  }

  "Authorisation helper" should {
    "return authorisation type as 'user' along with the scope when using OAuth 2.0 security on the method" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |annotationTypes:
          |  scope:
          |securitySchemes:
          |  oauth_2_0:
          |    type: OAuth 2.0
          |    settings:
          |      authorizationUri: https://api.service.hmrc.gov.uk/oauth/authorize
          |      accessTokenUri: https://api.service.hmrc.gov.uk/oauth/token
          |      authorizationGrants: [ authorization_code, client_credentials ]
          |  x-application:
          |    type: x-application
          |/cust:
          |  get:
          |    (scope): "read:marriage-allowance"
          |    securedBy: [ oauth_2_0 ]
        """.stripMargin
      val ramlApi: Api = new StringRamlLoader().load(raml).get
      val method = ramlApi.resources().get(0).methods().get(0)

      Authorisation(method) shouldBe ("user" -> Some("read:marriage-allowance"))
    }

    "return authorisation type as 'application' with no scope when using x-application security on the method" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |annotationTypes:
          |  scope:
          |securitySchemes:
          |  oauth_2_0:
          |    type: OAuth 2.0
          |    settings:
          |      authorizationUri: https://api.service.hmrc.gov.uk/oauth/authorize
          |      accessTokenUri: https://api.service.hmrc.gov.uk/oauth/token
          |      authorizationGrants: [ authorization_code, client_credentials ]
          |  x-application:
          |    type: x-application
          |/cust:
          |  get:
          |    securedBy: [ x-application ]
        """.stripMargin
      val ramlApi: Api = new StringRamlLoader().load(raml).get
      val method = ramlApi.resources().get(0).methods().get(0)

      Authorisation(method) shouldBe ("application" -> None)
    }

    "return authorisation type as 'application' with scope when using x-application security and scope annotation on the method" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |annotationTypes:
          |  scope:
          |securitySchemes:
          |  oauth_2_0:
          |    type: OAuth 2.0
          |    settings:
          |      authorizationUri: https://api.service.hmrc.gov.uk/oauth/authorize
          |      accessTokenUri: https://api.service.hmrc.gov.uk/oauth/token
          |      authorizationGrants: [ authorization_code, client_credentials ]
          |  x-application:
          |    type: x-application
          |/cust:
          |  get:
          |    securedBy: [ x-application ]
          |    (scope): "read:test-scope"
        """.stripMargin
      val ramlApi: Api = new StringRamlLoader().load(raml).get
      val method = ramlApi.resources().get(0).methods().get(0)

      Authorisation(method) shouldBe ("application" -> Some("read:test-scope"))
    }

    "return authorisation type as 'none' with no scope when not secured" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |/cust:
          |  get:
        """.stripMargin
      val ramlApi: Api = new StringRamlLoader().load(raml).get
      val method = ramlApi.resources().get(0).methods().get(0)

      Authorisation(method) shouldBe ("none" -> None)
    }
  }

  "ExampleResponses helper" should {

    "return the single example wrapped as a sequence" in {
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
          |            example: |
          |              {
          |                "message": "Hello User"
          |              }""".stripMargin
      val ramlApi: Api = new StringRamlLoader().load(raml).get
      val method = ramlApi.resources().get(0).methods().get(0)

      val responses = BodyExamples(method.responses.get(0).body().get(0))

      responses.length shouldBe 1
      responses(0).value.get shouldBe
        """|{
          |  "message": "Hello User"
          |}""".stripMargin
    }

    "return multiple examples" in {
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
          |            examples:
          |              example-1: |
          |                {
          |                  "message": "First"
          |                }
          |              example-2:
          |                description: First Example
          |                value: |
          |                  {
          |                    "message": "Second"
          |                  }""".stripMargin
      val ramlApi: Api = new StringRamlLoader().load(raml).get
      val method = ramlApi.resources().get(0).methods().get(0)

      val responses = BodyExamples(method.responses.get(0).body().get(0))

      responses.length shouldBe 2
      responses(0).value.get.trim shouldBe
        """|{
          |  "message": "First"
          |}""".stripMargin

      responses(0).description() shouldBe None

      responses(1).value.get.trim shouldBe
        """|{
          |  "message": "Second"
          |}""".stripMargin

      responses(1).description shouldBe Some("First Example")
    }
  }

  "Responses helper" should {
    "return success responses" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |types:
          |  errorResponse:
          |    properties:
          |      scenario:
          |        type: string
          |      code:
          |        type: string
          |/cust:
          |  get:
          |    responses:
          |      100:
          |        body:
          |          application/json:
          |      200:
          |        body:
          |          application/json:
          |      201:
          |        body:
          |          application/json:
          |      303:
          |        body:
          |          application/json:
          |      400:
          |        body:
          |          application/json:
          |      500:
          |        body:
          |          application/json:
          | """.stripMargin
      val ramlApi = new StringRamlLoader().load(raml).get
      val method = ramlApi.resources().get(0).methods().get(0)

      val responses = Responses.success(method)

      responses.length shouldBe 3
    }

    "return error responses" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |types:
          |  errorResponse:
          |    properties:
          |      scenario:
          |        type: string
          |      code:
          |        type: string
          |/cust:
          |  get:
          |    responses:
          |      100:
          |        body:
          |          application/json:
          |      200:
          |        body:
          |          application/json:
          |      303:
          |        body:
          |          application/json:
          |      400:
          |        body:
          |          application/json:
          |      500:
          |        body:
          |          application/json:
          | """.stripMargin
      val ramlApi = new StringRamlLoader().load(raml).get
      val method = ramlApi.resources().get(0).methods().get(0)

      val responses = Responses.error(method)

      responses.length shouldBe 2
    }
  }

  "ErrorScenarios helper" should {
    "return the error scenario" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |types:
          |  errorResponse:
          |    properties:
          |      code:
          |        type: string
          |/cust:
          |  get:
          |    responses:
          |      400:
          |        body:
          |          application/json:
          |            description: Invalid UTR
          |            type: errorResponse
          |            example:
          |              code: SA_UTR_INVALID
          | """.stripMargin
      val ramlApi = new StringRamlLoader().load(raml).get
      val method = ramlApi.resources().get(0).methods().get(0)

      val scenarios = ErrorScenarios(method)

      scenarios.length shouldBe 1

      scenarios(0)("scenario") shouldBe "Invalid UTR"
      scenarios(0)("code") shouldBe "SA_UTR_INVALID"
      scenarios(0)("httpStatus") shouldBe "400"
    }

    "return the error scenario when structured as examples" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |types:
          |  errorResponse:
          |    properties:
          |      code:
          |        type: string
          |/cust:
          |  get:
          |    responses:
          |      400:
          |        body:
          |          application/json:
          |            type: errorResponse
          |            examples:
          |              invalidUtr:
          |                description: Invalid UTR
          |                value:
          |                  code: SA_UTR_INVALID
          | """.stripMargin
      val ramlApi = new StringRamlLoader().load(raml).get
      val method = ramlApi.resources().get(0).methods().get(0)

      val scenarios = ErrorScenarios(method)

      scenarios.length shouldBe 1

      scenarios(0)("scenario") shouldBe "Invalid UTR"
      scenarios(0)("code") shouldBe "SA_UTR_INVALID"
      scenarios(0)("httpStatus") shouldBe "400"
    }

    "return a list of error scenarios for the same status code" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |types:
          |  errorResponse:
          |    properties:
          |      code:
          |        type: string
          |/cust:
          |  get:
          |    responses:
          |      400:
          |        body:
          |          application/json:
          |            type: errorResponse
          |            examples:
          |              invalidUtr:
          |                description: Invalid UTR
          |                value:
          |                  code: SA_UTR_INVALID
          |              invalidTaxYear:
          |                description: Invalid tax year
          |                value:
          |                  code: TAX_YEAR_INVALID
          | """.stripMargin
      val ramlApi = new StringRamlLoader().load(raml).get
      val method = ramlApi.resources().get(0).methods().get(0)

      val scenarios = ErrorScenarios(method)

      scenarios.length shouldBe 2

      scenarios(0)("scenario") shouldBe "Invalid UTR"
      scenarios(1)("scenario") shouldBe "Invalid tax year"
    }

    "return a list of error scenarios for the different status codes" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |types:
          |  errorResponse:
          |    properties:
          |      code:
          |        type: string
          |/cust:
          |  get:
          |    responses:
          |      400:
          |        body:
          |          application/json:
          |            type: errorResponse
          |            examples:
          |              invalidUtr:
          |                description: Invalid UTR
          |                value:
          |                  code: SA_UTR_INVALID
          |      404:
          |        body:
          |          application/json:
          |            type: errorResponse
          |            examples:
          |              notFound:
          |                description: Not found
          |                value:
          |                  code: NOT_FOUND
          | """.stripMargin
      val ramlApi = new StringRamlLoader().load(raml).get
      val method = ramlApi.resources().get(0).methods().get(0)

      val scenarios = ErrorScenarios(method)

      scenarios.length shouldBe 2

      scenarios(0)("scenario") shouldBe "Invalid UTR"
      scenarios(0)("httpStatus") shouldBe "400"
      scenarios(1)("scenario") shouldBe "Not found"
      scenarios(1)("httpStatus") shouldBe "404"
    }

    "support providing the error response example as JSON" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |
          |/cust:
          |  get:
          |    responses:
          |      400:
          |        body:
          |          application/json:
          |            description: Invalid UTR
          |            example: |
          |              {
          |                "code": "SA_UTR_INVALID",
          |                "message": "The UTR is not valid"
          |              }
          | """.stripMargin
      val ramlApi = new StringRamlLoader().load(raml).get
      val method = ramlApi.resources().get(0).methods().get(0)

      val scenarios = ErrorScenarios(method)

      scenarios.length shouldBe 1

      scenarios(0)("scenario") shouldBe "Invalid UTR"
      scenarios(0)("code") shouldBe "SA_UTR_INVALID"
      scenarios(0)("httpStatus") shouldBe "400"
    }

    "support providing the error response example as XML" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |
          |/cust:
          |  get:
          |    responses:
          |      400:
          |        body:
          |          application/json:
          |            description: Invalid XML Payload
          |            example: |
          |              <?xml version="1.0" encoding="UTF-8"?>
          |              <error_response>
          |                 <code>BAD_REQUEST</code>
          |                 <errors>
          |                   <error>
          |                     <type>xml_validation_error</type>
          |                     <description>Error at line 13, column 13: no declaration found for element 'unknown'</description>
          |                   </error>
          |                 </errors>
          |              </error_response>
          | """.stripMargin
      val ramlApi = new StringRamlLoader().load(raml).get
      val method = ramlApi.resources().get(0).methods().get(0)

      val scenarios = ErrorScenarios(method)

      scenarios.length shouldBe 1

      scenarios(0)("scenario") shouldBe "Invalid XML Payload"
      scenarios(0)("code") shouldBe "BAD_REQUEST"
      scenarios(0)("httpStatus") shouldBe "400"
    }

    "support providing the error response example as inline JSON" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |
          |/cust:
          |  get:
          |    responses:
          |      400:
          |        body:
          |          application/json:
          |            description: Invalid UTR
          |            example: { "code": "SA_UTR_INVALID", "message": "The UTR is not valid" }
          | """.stripMargin
      val ramlApi = new StringRamlLoader().load(raml).get
      val method = ramlApi.resources().get(0).methods().get(0)

      val scenarios = ErrorScenarios(method)

      scenarios.length shouldBe 1

      scenarios(0)("scenario") shouldBe "Invalid UTR"
      scenarios(0)("code") shouldBe "SA_UTR_INVALID"
      scenarios(0)("httpStatus") shouldBe "400"
    }

    "support providing error response examples as JSON" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |
          |/cust:
          |  get:
          |    responses:
          |      400:
          |        body:
          |          application/json:
          |            examples:
          |              invalidUtr:
          |                description: Invalid UTR
          |                value: |
          |                  {
          |                    "code": "SA_UTR_INVALID",
          |                    "message": "The UTR is not valid"
          |                  }
          | """.stripMargin
      val ramlApi = new StringRamlLoader().load(raml).get
      val method = ramlApi.resources().get(0).methods().get(0)

      val scenarios = ErrorScenarios(method)

      scenarios.length shouldBe 1

      scenarios(0)("scenario") shouldBe "Invalid UTR"
      scenarios(0)("code") shouldBe "SA_UTR_INVALID"
      scenarios(0)("httpStatus") shouldBe "400"
    }

    "support providing error response examples as XML" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |
          |/cust:
          |  get:
          |    responses:
          |      400:
          |        body:
          |          application/json:
          |            examples:
          |              invalidXml:
          |                description: Invalid XML Payload
          |                value: |
          |                  <?xml version="1.0" encoding="UTF-8"?>
          |                   <error_response>
          |                    <code>BAD_REQUEST</code>
          |                    <errors>
          |                      <error>
          |                        <type>xml_validation_error</type>
          |                        <description>Error at line 13, column 13: no declaration found for element 'unknown'</description>
          |                      </error>
          |                    </errors>
          |                   </error_response>
          | """.stripMargin
      val ramlApi = new StringRamlLoader().load(raml).get
      val method = ramlApi.resources().get(0).methods().get(0)

      val scenarios = ErrorScenarios(method)

      scenarios.length shouldBe 1

      scenarios(0)("scenario") shouldBe "Invalid XML Payload"
      scenarios(0)("code") shouldBe "BAD_REQUEST"
      scenarios(0)("httpStatus") shouldBe "400"
    }

    "ignore XML error response examples which don't include a code element" in {
      val raml =
        """#%RAML 1.0
          |---
          |title: Trivial Doc
          |
          |/cust:
          |  get:
          |    responses:
          |      400:
          |        body:
          |          application/json:
          |            examples:
          |              invalidXml:
          |                description: Invalid XML Payload
          |                value: |
          |                  <?xml version="1.0" encoding="UTF-8"?>
          |                   <error_response>
          |                    <status>BAD_REQUEST</status>
          |                    <errors>
          |                      <error>
          |                        <type>xml_validation_error</type>
          |                        <description>Error at line 13, column 13: no declaration found for element 'unknown'</description>
          |                      </error>
          |                    </errors>
          |                   </error_response>
          | """.stripMargin
      val ramlApi = new StringRamlLoader().load(raml).get
      val method = ramlApi.resources().get(0).methods().get(0)

      val scenarios = ErrorScenarios(method)

      scenarios.length shouldBe 0
    }
  }

  "HttpStatus helper" should {
    "provide an enriched string for the status code" in {
      HttpStatus(200) shouldBe "200 (OK)"
    }

    "parse invalid status code like 498" in {
      HttpStatus(498) shouldBe "498 (non-standard)"
    }
  }
}
