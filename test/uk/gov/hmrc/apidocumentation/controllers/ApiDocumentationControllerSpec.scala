/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.test.Helpers._
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK, SEE_OTHER}
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.apidocumentation.views.html._
import uk.gov.hmrc.apidocumentation.ErrorHandler
import uk.gov.hmrc.apidocumentation.mocks.services._
import uk.gov.hmrc.apidocumentation.controllers.utils._
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.apidocumentation.mocks.config._
import scala.concurrent.Future.successful

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.failed
import uk.gov.hmrc.apidocumentation.models.apispecification.{ApiSpecification, DocumentationItem, ResourceGroup, TypeDeclaration}
import uk.gov.hmrc.apidocumentation.connectors.RamlPreviewConnector
import uk.gov.hmrc.apidocumentation.controllers.ApiDocumentationController.RamlParseException

class ApiDocumentationControllerSpec extends CommonControllerBaseSpec with PageRenderVerification {
  trait Setup
      extends ApiDocumentationServiceMock
      with AppConfigMock
      with ApiDefinitionServiceMock
    with LoggedInUserServiceMock
      with NavigationServiceMock {

    val errorHandler = app.injector.instanceOf[ErrorHandler]
    val mcc = app.injector.instanceOf[MessagesControllerComponents]

    private lazy val apiIndexView = app.injector.instanceOf[ApiIndexView]
    lazy val ramlPreviewConnector = mock[RamlPreviewConnector]
    private lazy val retiredVersionJumpView = app.injector.instanceOf[RetiredVersionJumpView]
    private lazy val apisFilteredView = app.injector.instanceOf[ApisFilteredView]
    private lazy val previewDocumentationView = app.injector.instanceOf[PreviewDocumentationView2]
    private lazy val xmlDocumentationView = app.injector.instanceOf[XmlDocumentationView]
    private lazy val serviceDocumentationView = app.injector.instanceOf[ServiceDocumentationView2]

    val underTest = new ApiDocumentationController(
      documentationService,
      ramlPreviewConnector,
      apiDefinitionService,
      navigationService,
      loggedInUserService,
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

  trait DocumentationRenderVersionSetup extends Setup {
    when(appConfig.documentationRenderVersion).thenReturn("specification")
    val mockApiSpecification =
      ApiSpecification(
        title = "mockTitle",
        version = "1.0",
        deprecationMessage = None,
        documentationItems = List.empty[DocumentationItem],
        resourceGroups = List.empty[ResourceGroup],
        types = List.empty[TypeDeclaration],
        isFieldOptionalityKnown = false)
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
          val result = underTest.redirectToApiDocumentation(serviceName, Some(version), Option(true))(request)
          status(result) shouldBe SEE_OTHER
          headers(result).get("location") shouldBe Some(s"/api-documentation/docs/api/service/hello-world/${version}?cacheBuster=true")
        }
      }

      "when not given a version" should {
        val version = None

        "redirect to the documentation page" in new Setup {
          theUserIsLoggedIn()
          theDefinitionServiceWillReturnAnApiDefinition(extendedApiDefinition(serviceName, "1.0"))
          val result = underTest.redirectToApiDocumentation(serviceName, version, Option(true))(request)
          status(result) shouldBe SEE_OTHER
          headers(result).get("location") shouldBe Some(s"/api-documentation/docs/api/service/hello-world/1.0?cacheBuster=true")
        }

        "redirect to the documentation page for api in private trial for user without authorisation" in new Setup {
          theUserIsLoggedIn()
          val privateTrialAPIDefinition = extendedApiDefinition(serviceName, "1.0",
          APIAccessType.PRIVATE, loggedIn = true, authorised = false, isTrial = Some(true))
          theDefinitionServiceWillReturnAnApiDefinition(privateTrialAPIDefinition)

          val result = underTest.redirectToApiDocumentation(serviceName, None, Option(true))(request)
          status(result) shouldBe SEE_OTHER
          headers(result).get("location") shouldBe Some(s"/api-documentation/docs/api/service/hello-world/1.0?cacheBuster=true")
        }

        "redirect to the documentation page for api in private trial for user with authorisation" in new Setup {
          theUserIsLoggedIn()
          val privateTrialAPIDefinition = extendedApiDefinition(serviceName, "1.0",
          APIAccessType.PRIVATE, loggedIn = true, authorised = true, isTrial = Some(true))
          theDefinitionServiceWillReturnAnApiDefinition(privateTrialAPIDefinition)

          val result = underTest.redirectToApiDocumentation(serviceName, None, Option(true))(request)
          status(result) shouldBe SEE_OTHER
          headers(result).get("location") shouldBe Some(s"/api-documentation/docs/api/service/hello-world/1.0?cacheBuster=true")
        }

        "redirect to the documentation page for the latest accessible version" in new Setup {
          theUserIsLoggedIn()

          val apiDefinition =
            ExtendedAPIDefinition(
              serviceName,
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
          val result = underTest.redirectToApiDocumentation("hello-world", version, Option(true))(request)
          status(result) shouldBe SEE_OTHER
          headers(result).get("location") shouldBe Some("/api-documentation/docs/api/service/hello-world/1.0?cacheBuster=true")
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

      "when documentationRenderVersion is specification" should {
        "display the documentation and private API options when the API is private but the logged in user has access to it" in new DocumentationRenderVersionSetup {
          theUserIsLoggedIn()
          val apiDefinition = extendedApiDefinition(serviceName, "1.0", APIAccessType.PRIVATE, loggedIn = true, authorised = true)
          theDefinitionServiceWillReturnAnApiDefinition(apiDefinition)
          theDocumentationServiceWillFetchApiSpecification(mockApiSpecification)

          val result = underTest.renderApiDocumentation(serviceName, "1.0", Option(true))(request)

          verifyApiDocumentationPageRendered(result, "1.0", "Private Stable")
          versionOptionIsRendered(result, serviceName, "1.0", apiDefinition.versions.head.displayedStatus) shouldBe true
        }

        "display the private API options when not logged in and is in trial" in new DocumentationRenderVersionSetup {
          theUserIsNotLoggedIn()

          val apiDefinition = extendedApiDefinition(serviceName, "1.0", APIAccessType.PRIVATE, loggedIn = false, authorised = false, isTrial = Some(true))

          theDefinitionServiceWillReturnAnApiDefinition(apiDefinition)
          theDocumentationServiceWillFetchApiSpecification(mockApiSpecification)

          val result = underTest.renderApiDocumentation(serviceName, "1.0", Option(true))(request)

          versionOptionIsRendered(result, serviceName, "1.0", apiDefinition.versions.head.displayedStatus) shouldBe true
        }

        "display the private API options when logged in and is in trial but the user is not authorised" in new DocumentationRenderVersionSetup {
          theUserIsLoggedIn()

          val apiDefinition = extendedApiDefinition(serviceName, "1.0", APIAccessType.PRIVATE, loggedIn = true, authorised = false, isTrial = Some(true))

          theDefinitionServiceWillReturnAnApiDefinition(apiDefinition)
          theDocumentationServiceWillFetchApiSpecification(mockApiSpecification)

          val result = underTest.renderApiDocumentation(serviceName, "1.0", Option(true))(request)

          versionOptionIsRendered(result, serviceName, "1.0", apiDefinition.versions.head.displayedStatus) shouldBe true
        }

        "display the private API options when logged in and is in trial and the user is authorised" in new DocumentationRenderVersionSetup {
          theUserIsLoggedIn()

          val apiDefinition = extendedApiDefinition(serviceName, "1.0", APIAccessType.PRIVATE, loggedIn = true, authorised = true, isTrial = Some(true))

          theDefinitionServiceWillReturnAnApiDefinition(apiDefinition)
          theDocumentationServiceWillFetchApiSpecification(mockApiSpecification)

          val result = underTest.renderApiDocumentation(serviceName, "1.0", Option(true))(request)

          versionOptionIsRendered(result, serviceName, "1.0", apiDefinition.versions.head.displayedStatus) shouldBe true
        }

        "not display the private API options when not in trial, not logged in and (therefore) is not authorised" in new DocumentationRenderVersionSetup {
          theUserIsNotLoggedIn()

          val apiDefinition = extendedApiDefinition(serviceName, "1.0", APIAccessType.PRIVATE, loggedIn = false, authorised = false)

          theDefinitionServiceWillReturnAnApiDefinition(apiDefinition)
          theDocumentationServiceWillFetchApiSpecification(mockApiSpecification)

          val result = underTest.renderApiDocumentation(serviceName, "1.0", Option(true))(request)

          versionOptionIsRendered(result, serviceName, "1.0", apiDefinition.versions.head.displayedStatus) shouldBe false
        }

        "display the not found page when the API is private and the logged in user does not have access to it" in new DocumentationRenderVersionSetup {
          theUserIsLoggedIn()
          theDefinitionServiceWillReturnAnApiDefinition(
            extendedApiDefinition(serviceName, "1.0", APIAccessType.PRIVATE, loggedIn = true, authorised = false))
          theDocumentationServiceWillFetchApiSpecification(mockApiSpecification)

          val result = underTest.renderApiDocumentation(serviceName, "1.0", Option(true))(request)

          verifyNotFoundPageRendered(result)
        }

        "redirect to the login page when the API is private and the user is not logged in" in new DocumentationRenderVersionSetup {
          theUserIsNotLoggedIn()
          theDefinitionServiceWillReturnAnApiDefinition(
            extendedApiDefinition(serviceName, "1.0", APIAccessType.PRIVATE, loggedIn = false, authorised = false))
          theDocumentationServiceWillFetchApiSpecification(mockApiSpecification)

          val result = underTest.renderApiDocumentation(serviceName, "1.0", Option(true))(request)

          verifyRedirectToLoginPage(result, serviceName, "1.0")
        }

        "display the not found page when Principal and Subordinate APIAvailability's are None" in new DocumentationRenderVersionSetup {
          theUserIsLoggedIn()
          val apiDefinition = extendedApiDefinitionWithNoAPIAvailability(serviceName, "1.0")

          theDefinitionServiceWillReturnAnApiDefinition(apiDefinition)
          theDocumentationServiceWillFetchApiSpecification(mockApiSpecification)

          val result = underTest.renderApiDocumentation(serviceName, "1.0", Option(true))(request)

          verifyNotFoundPageRendered(result)
        }

        "display the private API options when Principal APIAvailability is None but Subordinate APIAvailability is set" in new DocumentationRenderVersionSetup {
          theUserIsLoggedIn()

          val subordinateApiAvailability = APIAvailability(endpointsEnabled = true, APIAccess(APIAccessType.PUBLIC), loggedIn = false, authorised = true)
          val apiDefinition = extendedApiDefinitionWithPrincipalAndSubordinateAPIAvailability(
            serviceName, "1.0", None, Some(subordinateApiAvailability))

          theDefinitionServiceWillReturnAnApiDefinition(apiDefinition)
          theDocumentationServiceWillFetchApiSpecification(mockApiSpecification)

          val result = underTest.renderApiDocumentation(serviceName, "1.0", Option(true))(request)

          versionOptionIsRendered(result, serviceName, "1.0", apiDefinition.versions.head.displayedStatus) shouldBe true
        }
        // APIS-4844
        "display the not found page when Principal access is PUBLIC and Subordinate access is PRIVATE loggedIn and not authorised" in new DocumentationRenderVersionSetup {
          theUserIsLoggedIn()

          val principalApiAvailability = APIAvailability(endpointsEnabled = true, APIAccess(APIAccessType.PUBLIC), loggedIn = false, authorised = true)
          val subordinateApiAvailability = APIAvailability(endpointsEnabled = true, APIAccess(APIAccessType.PRIVATE), loggedIn = true, authorised = false)

          val apiDefinition = extendedApiDefinitionWithPrincipalAndSubordinateAPIAvailability(
            serviceName, "1.0", Some(principalApiAvailability), Some(subordinateApiAvailability))

          theDefinitionServiceWillReturnAnApiDefinition(apiDefinition)
          theDocumentationServiceWillFetchApiSpecification(mockApiSpecification)

          val result = underTest.renderApiDocumentation(serviceName, "1.0", Option(true))(request)

          verifyNotFoundPageRendered(result)
        }

        "display the private API options when Principal access is PUBLIC and Subordinate access is PUBLIC" in new DocumentationRenderVersionSetup {
          theUserIsLoggedIn()

          val principalApiAvailability = APIAvailability(endpointsEnabled = true, APIAccess(APIAccessType.PUBLIC), loggedIn = false, authorised = true)
          val subordinateApiAvailability = APIAvailability(endpointsEnabled = true, APIAccess(APIAccessType.PUBLIC), loggedIn = false, authorised = true)

          val apiDefinition = extendedApiDefinitionWithPrincipalAndSubordinateAPIAvailability(
            serviceName, "1.0", Some(principalApiAvailability), Some(subordinateApiAvailability))

          theDefinitionServiceWillReturnAnApiDefinition(apiDefinition)
          theDocumentationServiceWillFetchApiSpecification(mockApiSpecification)

          val result = underTest.renderApiDocumentation(serviceName, "1.0", Option(true))(request)

          versionOptionIsRendered(result, serviceName, "1.0", apiDefinition.versions.head.displayedStatus) shouldBe true
        }

        "display the private API options when Principal access is PRIVATE and Subordinate access is PUBLIC" in new DocumentationRenderVersionSetup {
          theUserIsLoggedIn()

          val principalApiAvailability = APIAvailability(endpointsEnabled = true, APIAccess(APIAccessType.PRIVATE), loggedIn = false, authorised = false)
          val subordinateApiAvailability = APIAvailability(endpointsEnabled = true, APIAccess(APIAccessType.PUBLIC), loggedIn = false, authorised = true)

          val apiDefinition = extendedApiDefinitionWithPrincipalAndSubordinateAPIAvailability(
            serviceName, "1.0", Some(principalApiAvailability), Some(subordinateApiAvailability))

          theDefinitionServiceWillReturnAnApiDefinition(apiDefinition)
          theDocumentationServiceWillFetchApiSpecification(mockApiSpecification)

          val result = underTest.renderApiDocumentation(serviceName, "1.0", Option(true))(request)

          versionOptionIsRendered(result, serviceName, "1.0", apiDefinition.versions.head.displayedStatus) shouldBe true
        }

        "display the private API options when Principal access is PRIVATE and Subordinate access is PRIVATE, loggedIn and authorised" in new DocumentationRenderVersionSetup {
          theUserIsLoggedIn()

          val principalApiAvailability = APIAvailability(endpointsEnabled = true, APIAccess(APIAccessType.PRIVATE), loggedIn = false, authorised = false)
          val subordinateApiAvailability = APIAvailability(endpointsEnabled = true, APIAccess(APIAccessType.PRIVATE), loggedIn = true, authorised = true)

          val apiDefinition = extendedApiDefinitionWithPrincipalAndSubordinateAPIAvailability(
            serviceName, "1.0", Some(principalApiAvailability), Some(subordinateApiAvailability))

          theDefinitionServiceWillReturnAnApiDefinition(apiDefinition)
          theDocumentationServiceWillFetchApiSpecification(mockApiSpecification)

          val result = underTest.renderApiDocumentation(serviceName, "1.0", Option(true))(request)

          versionOptionIsRendered(result, serviceName, "1.0", apiDefinition.versions.head.displayedStatus) shouldBe true
        }

        "display the not found page when Principal access is PRIVATE and Subordinate access is PRIVATE and not authorised" in new DocumentationRenderVersionSetup {
          theUserIsLoggedIn()

          val principalApiAvailability = APIAvailability(endpointsEnabled = true, APIAccess(APIAccessType.PRIVATE), loggedIn = false, authorised = false)
          val subordinateApiAvailability = APIAvailability(endpointsEnabled = true, APIAccess(APIAccessType.PRIVATE), loggedIn = true, authorised = false)

          val apiDefinition = extendedApiDefinitionWithPrincipalAndSubordinateAPIAvailability(
            serviceName, "1.0", Some(principalApiAvailability), Some(subordinateApiAvailability))

          theDefinitionServiceWillReturnAnApiDefinition(apiDefinition)
          theDocumentationServiceWillFetchApiSpecification(mockApiSpecification)

          val result = underTest.renderApiDocumentation(serviceName, "1.0", Option(true))(request)

          verifyNotFoundPageRendered(result)
        }
      }

      "display the not found page when invalid service specified" in new Setup {
        theUserIsLoggedIn()
        theDefinitionServiceWillFail(new NotFoundException("Expected unit test failure"))

        val result = underTest.renderApiDocumentation(serviceName, "1.0", Option(true))(request)

        verifyNotFoundPageRendered(result)
      }

      "display the retired version page when the API version is marked as retired" in new Setup {
        theUserIsLoggedIn()
        theDefinitionServiceWillReturnAnApiDefinition(
        extendedApiDefinitionWithRetiredVersion(serviceName, "1.0", "1.1"))

        val result = underTest.renderApiDocumentation(serviceName, "1.0", Option(true))(request)

        verifyApiDocumentationPageRendered(result, "1.0", "Retired")
        verifyLinkToStableDocumentationRendered(result, serviceName, "1.1")
      }

      "display the not found page when invalid version specified" in new Setup {
        theUserIsLoggedIn()
        theDefinitionServiceWillReturnAnApiDefinition(
        extendedApiDefinitionWithRetiredVersion(serviceName, "1.0", "1.1"))

        val result = underTest.renderApiDocumentation(serviceName, "2.0", Option(true))(request)

        verifyNotFoundPageRendered(result)
      }

      "display the not found page when no API definition is returned" in new Setup {
        theUserIsLoggedIn()
        theDefinitionServiceWillReturnNoApiDefinition()

        val result = underTest.renderApiDocumentation(serviceName, "1.0", Option(true))(request)

        verifyNotFoundPageRendered(result)
      }
    }

    "preview docs" should {

      "render 404 page when feature switch off" in new Setup {
        val result = underTest.previewApiDocumentation(None)(request)
        verifyNotFoundPageRendered(result)
      }

      "render 200 page when feature switch on" in new Setup with RamlPreviewEnabled {
        val result = underTest.previewApiDocumentation(None)(request)
        verifyPageRendered(pageTitle("API Documentation Preview"))(result)
      }

      "render 500 page when no URL supplied" in new Setup with RamlPreviewEnabled {
        val result = underTest.previewApiDocumentation(Some(""))(request)
        verifyErrorPageRendered(expectedStatus = INTERNAL_SERVER_ERROR, expectedError = "No URL supplied")(result)
      }

      "render 500 page when service throws exception" in new Setup with RamlPreviewEnabled {
        val url = "http://host:port/some.path.to.a.raml.document"
        when(ramlPreviewConnector.fetchPreviewApiSpecification(*)(*)).thenReturn(failed(RamlParseException("Expected unit test failure")))
        val result = underTest.previewApiDocumentation(Some(url))(request)
        verifyErrorPageRendered(expectedStatus = INTERNAL_SERVER_ERROR, expectedError = "Expected unit test failure")(result)
      }
    }
  }

  "bustCache" should {
    "override value of the query parameter if in stub mode" in new Setup {
      underTest.bustCache(stubMode = false, Some(true)) shouldBe true
      underTest.bustCache(stubMode = false, Some(false)) shouldBe false
      underTest.bustCache(stubMode = true, Some(true)) shouldBe true
      underTest.bustCache(stubMode = true, Some(false)) shouldBe true
    }

    "return true if no query parameter was provided and the app is running in Stub mode" in new Setup {
      underTest.bustCache(stubMode = false, None) shouldBe false
      underTest.bustCache(stubMode = true, None) shouldBe true
    }
  }

  "fetchTestEndpointJson" should {
    "sort the results by URL" in new Setup with RamlPreviewEnabled {
      val endpoints = Seq(
        TestEndpoint("{service-url}/employers-paye/www"),
        TestEndpoint("{service-url}/employers-paye/aaa"),
        TestEndpoint("{service-url}/employers-paye/zzz"),
        TestEndpoint("{service-url}/employers-paye/ddd")
      )

      when(documentationService.buildTestEndpoints(*, *)(*)).thenReturn(successful(endpoints))
      val result = underTest.fetchTestEndpointJson("employers-paye", "1.0")(request)
      status(result) shouldBe OK
      contentAsString(result) should include regex s"aaa.*ddd.*www.*zzz"
    }
  }

  "renderXmlApiDocumentation" must {

    "render the XML API landing page when the XML API definition exists" in new Setup {
      theUserIsLoggedIn()

      val existingXmlApiName = "Charities Online"
      val result = underTest.renderXmlApiDocumentation(existingXmlApiName)(request)

      verifyPageRendered(pageTitle(existingXmlApiName), bodyContains = Seq(existingXmlApiName))(result)
    }

    "return 404 not found when the XML API definition does not exist" in new Setup {
      theUserIsLoggedIn()

      val nonExistingXmlApiName = "Fake XML API name"
      val result = underTest.renderXmlApiDocumentation(nonExistingXmlApiName)(request)

      status(result) shouldBe NOT_FOUND
    }
  }
}
