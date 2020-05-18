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

package uk.gov.hmrc.apidocumentation.controllers

import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.apidocumentation.services.DocumentationService
import uk.gov.hmrc.apidocumentation.views.html._
import uk.gov.hmrc.apidocumentation.ErrorHandler
import play.api.http.Status.{INTERNAL_SERVER_ERROR, SEE_OTHER}
import uk.gov.hmrc.apidocumentation.utils.ApiDefinitionTestDataHelper
import org.mockito.Mockito.{when,verify}
import org.mockito.Matchers.any
import uk.gov.hmrc.apidocumentation.services.{PartialsService, RAML}

import uk.gov.hmrc.http.NotFoundException

import scala.concurrent.ExecutionContext.Implicits.global

class ApiDocumentationControllerSpec extends CommonControllerBaseSpec with PageRenderVerification with ApiDefinitionTestDataHelper {
  trait Setup extends ApiDocumentationServiceMock {
    val errorHandler = app.injector.instanceOf[ErrorHandler]
    val mcc = app.injector.instanceOf[MessagesControllerComponents]

    private lazy val apiIndexView = app.injector.instanceOf[ApiIndexView]
    private lazy val retiredVersionJumpView = app.injector.instanceOf[RetiredVersionJumpView]
    private lazy val apisFilteredView = app.injector.instanceOf[ApisFilteredView]
    private lazy val previewDocumentationView = app.injector.instanceOf[PreviewDocumentationView]
    private lazy val serviceDocumentationView = app.injector.instanceOf[ServiceDocumentationView]
    private lazy val xmlDocumentationView = app.injector.instanceOf[XmlDocumentationView]

    val underTest = new ApiDocumentationController(
      documentationService,
      apiDefinitionService,
      navigationService,
      loggedInUserProvider,
      errorHandler,
      mcc,
      apiIndexView,
      retiredVersionJumpView,
      apisFilteredView,
      previewDocumentationView,
      serviceDocumentationView,
      xmlDocumentationView
    )
  }

  "ApiDocumentationController" when {
    "routing to the apiIndexPage" should {
      "render the API List" in new Setup {
        theUserIsLoggedIn()
        theDefinitionServiceWillReturnApiDefinitions(List(anApiDefinition("service1", "1.0"), anApiDefinition("service2", "1.0")))

        val result = underTest.apiIndexPage(None, None, None)(request)
        verifyPageRendered(pageTitle("API Documentation"), bodyContains = Seq("API documentation"))(result)
      }

      "render the filtered API list" in new Setup {
        theUserIsLoggedIn()
        theDefinitionServiceWillReturnApiDefinitions(List(anApiDefinition("service1", "1.0"), anApiDefinition("service2", "1.0")))

        val result = underTest.apiIndexPage(None, None, Some("vat"))(request)

        verifyPageRendered(pageTitle("Filtered API Documentation"), bodyContains = Seq("Filtered API documentation", "1 document found in", "VAT"))(result)
      }

      "display the error page when the documentationService throws an exception" in new Setup {
        theUserIsLoggedIn()
        theDefinitionServiceWillFail(new Exception("Expected unit test failure"))

        val result = underTest.apiIndexPage(None, None, None)(request)

        verifyErrorPageRendered(INTERNAL_SERVER_ERROR, "Sorry, we’re experiencing technical difficulties")(result)
      }
    }

    "redirecting to Api Documentation" must {
      "when given a version" should {
        val version = "2.0"

        "redirect to the documentation page for the specified version" in new Setup {
          theUserIsLoggedIn()
          theDefinitionServiceWillReturnAnApiDefinition(extendedApiDefinition(serviceName, "1.0"))
          val result = await(underTest.redirectToApiDocumentation(serviceName, Some(version), Option(true))(request))
          status(result) shouldBe SEE_OTHER
          result.header.headers.get("location") shouldBe Some(s"/api-documentation/docs/api/service/hello-world/${version}?cacheBuster=true")
        }
      }

      "when not given a version" should {
        val version = None

        "redirect to the documentation page" in new Setup {
          theUserIsLoggedIn()
          theDefinitionServiceWillReturnAnApiDefinition(extendedApiDefinition(serviceName, "1.0"))
          val result = await(underTest.redirectToApiDocumentation(serviceName, version, Option(true))(request))
          status(result) shouldBe SEE_OTHER
          result.header.headers.get("location") shouldBe Some(s"/api-documentation/docs/api/service/hello-world/1.0?cacheBuster=true")
        }

        "redirect to the documentation page for api in private trial for user without authorisation" in new Setup {
          theUserIsLoggedIn()
          val privateTrialAPIDefinition = extendedApiDefinition(serviceName, "1.0",
          APIAccessType.PRIVATE, loggedIn = true, authorised = false, isTrial = Some(true))
          theDefinitionServiceWillReturnAnApiDefinition(privateTrialAPIDefinition)

          val result = await(underTest.redirectToApiDocumentation(serviceName, None, Option(true))(request))
          status(result) shouldBe SEE_OTHER
          result.header.headers.get("location") shouldBe Some(s"/api-documentation/docs/api/service/hello-world/1.0?cacheBuster=true")
        }

        "redirect to the documentation page for api in private trial for user with authorisation" in new Setup {
          theUserIsLoggedIn()
          val privateTrialAPIDefinition = extendedApiDefinition(serviceName, "1.0",
          APIAccessType.PRIVATE, loggedIn = true, authorised = true, isTrial = Some(true))
          theDefinitionServiceWillReturnAnApiDefinition(privateTrialAPIDefinition)

          val result = await(underTest.redirectToApiDocumentation(serviceName, None, Option(true))(request))
          status(result) shouldBe SEE_OTHER
          result.header.headers.get("location") shouldBe Some(s"/api-documentation/docs/api/service/hello-world/1.0?cacheBuster=true")
        }

        "redirect to the documentation page for the latest accessible version" in new Setup {
          theUserIsLoggedIn()

          val apiDefinition =
            ExtendedAPIDefinition(
              serviceName,
              "http://service",
              "Hello World",
              "Say Hello World",
              "hello",
              requiresTrust = false,
              isTestSupport = false,
              Seq(
                ExtendedAPIVersion(
                  "1.0", APIStatus.BETA, Seq(endpoint(endpointName, "/world")),
                  Some(apiAvailability().asAuthorised),
                  None
                ),
                ExtendedAPIVersion(
                  "1.1", APIStatus.STABLE, Seq(endpoint(endpointName, "/world")),
                  Some(apiAvailability().asPrivate),
                  None
                )
              )
            )

          theDefinitionServiceWillReturnAnApiDefinition(apiDefinition)
          val result = await(underTest.redirectToApiDocumentation("hello-world", version, Option(true))(request))
          status(result) shouldBe SEE_OTHER
          result.header.headers.get("location") shouldBe Some("/api-documentation/docs/api/service/hello-world/1.0?cacheBuster=true")
        }

        "display the not found page when invalid service specified" in new Setup {
          theUserIsLoggedIn()
          theDefinitionServiceWillFail(new NotFoundException("Expected unit test failure"))

          val result = underTest.redirectToApiDocumentation(serviceName, version, Option(true))(request)
          verifyNotFoundPageRendered(result)
        }
      }
    }

    "routing to renderApiDocumentation" should {
      "display the documentation page" in new Setup {
        val mockRamlAndSchemas = RamlAndSchemas(mock[RAML], mock[Map[String, JsonSchema]])

        theUserIsLoggedIn()
        theDefinitionServiceWillReturnAnApiDefinition(extendedApiDefinition(serviceName, "1.0"))
        theDocumentationServiceWillFetchRaml(mockRamlAndSchemas)

        val result = underTest.renderApiDocumentation(serviceName, "1.0", Option(true))(request)

        verifyApiDocumentationPageRendered(result, "1.0", "Stable")
      }
    }
  }
}
