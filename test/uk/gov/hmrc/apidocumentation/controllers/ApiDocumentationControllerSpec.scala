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

package uk.gov.hmrc.apidocumentation.controllers

import scala.concurrent.ExecutionContext.Implicits.global

import controllers.Assets
import org.apache.pekko.stream.Materializer

import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.http.NotFoundException

import uk.gov.hmrc.apidocumentation.ErrorHandler
import uk.gov.hmrc.apidocumentation.controllers.utils._
import uk.gov.hmrc.apidocumentation.mocks.config._
import uk.gov.hmrc.apidocumentation.mocks.connectors.DownloadConnectorMockModule
import uk.gov.hmrc.apidocumentation.mocks.services._
import uk.gov.hmrc.apidocumentation.views.html._
import uk.gov.hmrc.apidocumentation.views.html.openapispec.ParentPageOuter

class ApiDocumentationControllerSpec extends CommonControllerBaseSpec with PageRenderVerification {

  private val versionOne = ApiVersionNbr("1.0")
  private val versionTwo = ApiVersionNbr("2.0")

  trait Setup
      extends AppConfigMock
      with ApiDefinitionServiceMock
      with LoggedInUserServiceMock
      with NavigationServiceMock
      with XmlServicesServiceMock
      with DownloadConnectorMockModule {

    val errorHandler = app.injector.instanceOf[ErrorHandler]
    val mcc          = app.injector.instanceOf[MessagesControllerComponents]

    private lazy val retiredVersionJumpView         = app.injector.instanceOf[RetiredVersionJumpView]
    private lazy val xmlDocumentationView           = app.injector.instanceOf[XmlDocumentationView]
    private lazy val parentPage                     = app.injector.instanceOf[ParentPageOuter]
    private lazy val assets                         = app.injector.instanceOf[Assets]
    private implicit val materializer: Materializer = app.injector.instanceOf[Materializer]
    val definitionList: List[ApiDefinition]         = List(apiDefinition("service1"), apiDefinition("service2"))

    val underTest = new ApiDocumentationController(
      apiDefinitionService,
      navigationService,
      loggedInUserService,
      errorHandler,
      mcc,
      retiredVersionJumpView,
      xmlDocumentationView,
      parentPage,
      xmlServicesService,
      DownloadConnectorMock.aMock,
      assets
    )
  }

  "ApiDocumentationController" when {
    "redirecting to Api Documentation" must {
      "when given a version" should {
        val version = ApiVersionNbr("2.0")

        "redirect to the documentation page for the specified version" in new Setup {
          theUserIsLoggedIn()
          theDefinitionServiceWillReturnAnApiDefinition(extendedApiDefinition(serviceName = serviceName.value))
          val result = underTest.redirectToApiDocumentation(serviceName, Some(version))(request)
          status(result) shouldBe SEE_OTHER
          headers(result).get("location") shouldBe Some(s"/api-documentation/docs/api/service/hello-world/${version}")
        }
      }

      "when not given a version" should {
        val version = None

        "redirect to the documentation page" in new Setup {
          theUserIsLoggedIn()
          theDefinitionServiceWillReturnAnApiDefinition(extendedApiDefinition(serviceName = serviceName.value))
          val result = underTest.redirectToApiDocumentation(serviceName, version)(request)
          status(result) shouldBe SEE_OTHER
          headers(result).get("location") shouldBe Some(s"/api-documentation/docs/api/service/hello-world/1.0")
        }

        "redirect to the documentation page for api in private trial for user without authorisation" in new Setup {
          theUserIsLoggedIn()

          val privateTrialAPIDefinition =
            extendedApiDefinition(serviceName = serviceName.value, access = ApiAccess.Private(true), loggedIn = true, authorised = false)

          theDefinitionServiceWillReturnAnApiDefinition(privateTrialAPIDefinition)

          val result = underTest.redirectToApiDocumentation(serviceName, None)(request)
          status(result) shouldBe SEE_OTHER
          headers(result).get("location") shouldBe Some(s"/api-documentation/docs/api/service/hello-world/1.0")
        }

        "redirect to the documentation page for api in private trial for user with authorisation" in new Setup {
          theUserIsLoggedIn()

          val privateTrialAPIDefinition =
            extendedApiDefinition(serviceName = serviceName.value, access = ApiAccess.Private(true), loggedIn = true, authorised = true)

          theDefinitionServiceWillReturnAnApiDefinition(privateTrialAPIDefinition)

          val result = underTest.redirectToApiDocumentation(serviceName, None)(request)
          status(result) shouldBe SEE_OTHER
          headers(result).get("location") shouldBe Some(s"/api-documentation/docs/api/service/hello-world/1.0")
        }

        "redirect to the documentation page for the latest accessible version" in new Setup {
          theUserIsLoggedIn()

          val apiDefinition =
            ExtendedApiDefinition(
              serviceName,
              serviceBaseUrl = "/world",
              name = "Hello World",
              description = "Say Hello World",
              context = ApiContext("hello"),
              List(
                ExtendedApiVersion(
                  versionOne,
                  ApiStatus.BETA,
                  List(endpoint(endpointName, "/world")),
                  Some(apiAvailability().asAuthorised),
                  None
                ),
                ExtendedApiVersion(
                  ApiVersionNbr("1.1"),
                  ApiStatus.STABLE,
                  List(endpoint(endpointName, "/world")),
                  Some(apiAvailability().asPrivate),
                  None
                )
              ),
              isTestSupport = false,
              lastPublishedAt = None,
              categories = List(ApiCategory.OTHER)
            )

          theDefinitionServiceWillReturnAnApiDefinition(apiDefinition)
          val result = underTest.redirectToApiDocumentation(ServiceName("hello-world"), version)(request)
          status(result) shouldBe SEE_OTHER
          headers(result).get("location") shouldBe Some("/api-documentation/docs/api/service/hello-world/1.0")
        }

        "display the not found page when invalid service specified" in new Setup {
          theUserIsLoggedIn()
          theDefinitionServiceWillFail(new NotFoundException("Expected unit test failure"))

          val result = underTest.redirectToApiDocumentation(serviceName, version)(request)
          verifyNotFoundPageRendered(result)
        }

      }
    }

    "routing to renderApiDocumentation" should {

      "display the not found page when invalid service specified" in new Setup {
        theUserIsLoggedIn()
        theDefinitionServiceWillFail(new NotFoundException("Expected unit test failure"))

        val result = underTest.renderApiDocumentation(serviceName, versionOne)(request)

        verifyNotFoundPageRendered(result)
      }

      "display the retired version page when the API version is marked as retired" in new Setup {
        theUserIsLoggedIn()
        theDefinitionServiceWillReturnAnApiDefinition(
          extendedApiDefinitionWithRetiredVersion(serviceName, versionOne, ApiVersionNbr("1.1"))
        )

        val result = underTest.renderApiDocumentation(serviceName, versionOne)(request)

        verifyApiDocumentationPageRendered(result)
        verifyLinkToStableDocumentationRendered(result, serviceName, ApiVersionNbr("1.1"))
      }

      "display the API landing page private trial and private" in new Setup {
        theUserIsLoggedIn()
        theDefinitionServiceWillReturnAnApiDefinition(
          extendedApiDefinitionWithPrincipalAndSubordinateAPIAvailability(
            serviceName,
            versionOne,
            Some(ApiAvailability(true, ApiAccess.Private(true), true, true)),
            Some(ApiAvailability(true, ApiAccess.Private(false), true, true))
          )
        )
        DownloadConnectorMock.Fetch.returnsNoneIfNotFound()

        val result = underTest.renderApiDocumentation(serviceName, versionOne)(request)

        verifyApiDocumentationPageRendered(result)
        verifyPageRendered(pageTitle("Hello World"), bodyContains = Seq("Yes - private trial"))(result)
      }

      "display the not found page when invalid version specified" in new Setup {
        theUserIsLoggedIn()
        theDefinitionServiceWillReturnAnApiDefinition(
          extendedApiDefinitionWithRetiredVersion(serviceName, versionOne, ApiVersionNbr("1.1"))
        )

        val result = underTest.renderApiDocumentation(serviceName, versionTwo)(request)

        verifyNotFoundPageRendered(result)
      }

      "display the not found page when no API definition is returned" in new Setup {
        theUserIsLoggedIn()
        theDefinitionServiceWillReturnNoApiDefinition()

        val result = underTest.renderApiDocumentation(serviceName, versionOne)(request)

        verifyNotFoundPageRendered(result)
      }
    }
  }

  "renderXmlApiDocumentation" must {

    "render the XML API landing page when the XML API definition exists" in new Setup {
      theUserIsLoggedIn()
      fetchXmlApiReturnsApi()

      val existingXmlApiName = xmlApi1.name
      val result             = underTest.renderXmlApiDocumentation(existingXmlApiName)(request)

      verifyPageRendered(pageTitle(existingXmlApiName), bodyContains = Seq(existingXmlApiName))(result)
    }

    "return 404 not found when the XML API definition does not exist" in new Setup {
      theUserIsLoggedIn()
      fetchXmlApiReturnsNone()

      val nonExistingXmlApiName = "Fake XML API name"
      val result                = underTest.renderXmlApiDocumentation(nonExistingXmlApiName)(request)

      status(result) shouldBe NOT_FOUND
    }
  }
}
