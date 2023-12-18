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
import scala.concurrent.Future.{failed, successful}

import akka.stream.Materializer
import controllers.Assets

import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK, SEE_OTHER}
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.http.NotFoundException

import uk.gov.hmrc.apidocumentation.ErrorHandler
import uk.gov.hmrc.apidocumentation.connectors.{DownloadConnector, RamlPreviewConnector}
import uk.gov.hmrc.apidocumentation.controllers.ApiDocumentationController.RamlParseException
import uk.gov.hmrc.apidocumentation.controllers.utils._
import uk.gov.hmrc.apidocumentation.mocks.config._
import uk.gov.hmrc.apidocumentation.mocks.services._
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.apidocumentation.models.apispecification.{ApiSpecification, DocumentationItem, ResourceGroup, TypeDeclaration}
import uk.gov.hmrc.apidocumentation.views.html._
import uk.gov.hmrc.apidocumentation.views.html.openapispec.ParentPageOuter

class ApiDocumentationControllerSpec extends CommonControllerBaseSpec with PageRenderVerification {

  private val versionOne = ApiVersionNbr("1.0")
  private val versionTwo = ApiVersionNbr("2.0")

  trait Setup
      extends ApiDocumentationServiceMock
      with AppConfigMock
      with ApiDefinitionServiceMock
      with LoggedInUserServiceMock
      with NavigationServiceMock
      with XmlServicesServiceMock {

    val errorHandler = app.injector.instanceOf[ErrorHandler]
    val mcc          = app.injector.instanceOf[MessagesControllerComponents]

    private lazy val apiIndexView                   = app.injector.instanceOf[ApiIndexView]
    lazy val ramlPreviewConnector                   = mock[RamlPreviewConnector]
    private lazy val retiredVersionJumpView         = app.injector.instanceOf[RetiredVersionJumpView]
    private lazy val apisFilteredView               = app.injector.instanceOf[ApisFilteredView]
    private lazy val previewDocumentationView       = app.injector.instanceOf[PreviewDocumentationView2]
    private lazy val xmlDocumentationView           = app.injector.instanceOf[XmlDocumentationView]
    private lazy val serviceDocumentationView       = app.injector.instanceOf[ServiceDocumentationView2]
    private lazy val parentPage                     = app.injector.instanceOf[ParentPageOuter]
    private lazy val assets                         = app.injector.instanceOf[Assets]
    val downloadConnector                           = mock[DownloadConnector]
    private implicit val materializer: Materializer = app.injector.instanceOf[Materializer]
    val definitionList: List[ApiDefinition]         = List(apiDefinition("service1"), apiDefinition("service2"))

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
      xmlDocumentationView,
      parentPage,
      xmlServicesService,
      downloadConnector,
      assets
    )
  }

  trait DocumentationRenderVersionSetup extends Setup {

    when(appConfig.documentationRenderVersion).thenReturn("specification")

    val mockApiSpecification =
      ApiSpecification(
        title = "mockTitle",
        version = versionOne.value,
        deprecationMessage = None,
        documentationItems = List.empty[DocumentationItem],
        resourceGroups = List.empty[ResourceGroup],
        types = List.empty[TypeDeclaration],
        isFieldOptionalityKnown = false
      )
  }

  "ApiDocumentationController" when {
    "routing to the apiIndexPage" should {
      "render the API List" in new Setup {
        theUserIsLoggedIn()
        theDefinitionServiceWillReturnApiDefinitions(definitionList)
        fetchAllXmlApisReturnsApis()

        val result = underTest.apiIndexPage(None, None, None)(request)
        verifyPageRendered(pageTitle("API Documentation"), bodyContains = Seq("API documentation"))(result)
      }

      "render the filtered API list" in new Setup {
        theUserIsLoggedIn()
        theDefinitionServiceWillReturnApiDefinitions(definitionList)
        fetchAllXmlApisReturnsVatApi()

        val result = underTest.apiIndexPage(None, None, Some("vat"))(request)

        verifyPageRendered(pageTitle("Filtered API Documentation"), bodyContains = Seq("Filtered API documentation", "1 document found in", "VAT"))(result)
      }

      "display the error page when the documentationService throws an exception" in new Setup {
        theUserIsLoggedIn()
        theDefinitionServiceWillFail(new Exception("Expected unit test failure"))

        val result = underTest.apiIndexPage(None, None, None)(request)

        verifyErrorPageRendered(INTERNAL_SERVER_ERROR, "Sorry, we’re experiencing technical difficulties")(result)
      }

      "display the error page when the xmlServicesService throws an exception" in new Setup {
        theUserIsLoggedIn()
        theDefinitionServiceWillReturnApiDefinitions(definitionList)
        fetchAllXmlApisFails(new Exception("Expected unit test failure"))

        val result = underTest.apiIndexPage(None, None, None)(request)

        verifyErrorPageRendered(INTERNAL_SERVER_ERROR, "Sorry, we’re experiencing technical difficulties")(result)
      }
    }

    "redirecting to Api Documentation" must {
      "when given a version" should {
        val version = ApiVersionNbr("2.0")

        "redirect to the documentation page for the specified version" in new Setup {
          theUserIsLoggedIn()
          theDefinitionServiceWillReturnAnApiDefinition(extendedApiDefinition(serviceName = serviceName.value))
          val result = underTest.redirectToApiDocumentation(serviceName, Some(version), Option(true))(request)
          status(result) shouldBe SEE_OTHER
          headers(result).get("location") shouldBe Some(s"/api-documentation/docs/api/service/hello-world/${version}?cacheBuster=true")
        }
      }

      "when not given a version" should {
        val version = None

        "redirect to the documentation page" in new Setup {
          theUserIsLoggedIn()
          theDefinitionServiceWillReturnAnApiDefinition(extendedApiDefinition(serviceName = serviceName.value))
          val result = underTest.redirectToApiDocumentation(serviceName, version, Option(true))(request)
          status(result) shouldBe SEE_OTHER
          headers(result).get("location") shouldBe Some(s"/api-documentation/docs/api/service/hello-world/1.0?cacheBuster=true")
        }

        "redirect to the documentation page for api in private trial for user without authorisation" in new Setup {
          theUserIsLoggedIn()

          val privateTrialAPIDefinition =
            extendedApiDefinition(serviceName = serviceName.value, access = ApiAccess.Private(true), loggedIn = true, authorised = false)

          theDefinitionServiceWillReturnAnApiDefinition(privateTrialAPIDefinition)

          val result = underTest.redirectToApiDocumentation(serviceName, None, Option(true))(request)
          status(result) shouldBe SEE_OTHER
          headers(result).get("location") shouldBe Some(s"/api-documentation/docs/api/service/hello-world/1.0?cacheBuster=true")
        }

        "redirect to the documentation page for api in private trial for user with authorisation" in new Setup {
          theUserIsLoggedIn()

          val privateTrialAPIDefinition =
            extendedApiDefinition(serviceName = serviceName.value, access = ApiAccess.Private(true), loggedIn = true, authorised = true)

          theDefinitionServiceWillReturnAnApiDefinition(privateTrialAPIDefinition)

          val result = underTest.redirectToApiDocumentation(serviceName, None, Option(true))(request)
          status(result) shouldBe SEE_OTHER
          headers(result).get("location") shouldBe Some(s"/api-documentation/docs/api/service/hello-world/1.0?cacheBuster=true")
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
          val result = underTest.redirectToApiDocumentation(ServiceName("hello-world"), version, Option(true))(request)
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
          val apiDefinition = extendedApiDefinition(serviceName = serviceName.value, access = ApiAccess.Private(), loggedIn = true, authorised = true)
          theDefinitionServiceWillReturnAnApiDefinition(apiDefinition)
          theDocumentationServiceWillFetchApiSpecification(mockApiSpecification)

          val result = underTest.renderApiDocumentation(serviceName, versionOne, Option(true))(request)

          verifyApiDocumentationPageRendered(result)
          versionOptionIsRendered(result, "1.0", apiDefinition.versions.head.displayedStatus) shouldBe true
        }

        "display the private API options when not logged in and is in trial" in new DocumentationRenderVersionSetup {
          theUserIsNotLoggedIn()

          val apiDefinition = extendedApiDefinition(serviceName = serviceName.value, access = ApiAccess.Private(true), loggedIn = false, authorised = false)

          theDefinitionServiceWillReturnAnApiDefinition(apiDefinition)
          theDocumentationServiceWillFetchApiSpecification(mockApiSpecification)

          val result = underTest.renderApiDocumentation(serviceName, versionOne, Option(true))(request)

          versionOptionIsRendered(result, "1.0", apiDefinition.versions.head.displayedStatus) shouldBe true
        }

        "display the private API options when logged in and is in trial but the user is not authorised" in new DocumentationRenderVersionSetup {
          theUserIsLoggedIn()

          val apiDefinition = extendedApiDefinition(serviceName = serviceName.value, access = ApiAccess.Private(true), loggedIn = true, authorised = false)

          theDefinitionServiceWillReturnAnApiDefinition(apiDefinition)
          theDocumentationServiceWillFetchApiSpecification(mockApiSpecification)

          val result = underTest.renderApiDocumentation(serviceName, versionOne, Option(true))(request)

          versionOptionIsRendered(result, "1.0", apiDefinition.versions.head.displayedStatus) shouldBe true
        }

        "display the private API options when logged in and is in trial and the user is authorised" in new DocumentationRenderVersionSetup {
          theUserIsLoggedIn()

          val apiDefinition = extendedApiDefinition(serviceName = serviceName.value, access = ApiAccess.Private(), loggedIn = true, authorised = true)

          theDefinitionServiceWillReturnAnApiDefinition(apiDefinition)
          theDocumentationServiceWillFetchApiSpecification(mockApiSpecification)

          val result = underTest.renderApiDocumentation(serviceName, versionOne, Option(true))(request)

          versionOptionIsRendered(result, "1.0", apiDefinition.versions.head.displayedStatus) shouldBe true
        }

        "not display the private API options when not in trial, not logged in and (therefore) is not authorised" in new DocumentationRenderVersionSetup {
          theUserIsNotLoggedIn()

          val apiDefinition = extendedApiDefinition(serviceName = serviceName.value, access = ApiAccess.Private(), loggedIn = false, authorised = false)

          theDefinitionServiceWillReturnAnApiDefinition(apiDefinition)
          theDocumentationServiceWillFetchApiSpecification(mockApiSpecification)

          val result = underTest.renderApiDocumentation(serviceName, versionOne, Option(true))(request)

          versionOptionIsRendered(result, "1.0", apiDefinition.versions.head.displayedStatus) shouldBe false
        }

        "display the not found page when the API is private and the logged in user does not have access to it" in new DocumentationRenderVersionSetup {
          theUserIsLoggedIn()
          theDefinitionServiceWillReturnAnApiDefinition(
            extendedApiDefinition(serviceName = serviceName.value, access = ApiAccess.Private(), loggedIn = true, authorised = false)
          )
          theDocumentationServiceWillFetchApiSpecification(mockApiSpecification)

          val result = underTest.renderApiDocumentation(serviceName, versionOne, Option(true))(request)

          verifyNotFoundPageRendered(result)
        }

        "redirect to the login page when the API is private and the user is not logged in" in new DocumentationRenderVersionSetup {
          theUserIsNotLoggedIn()
          theDefinitionServiceWillReturnAnApiDefinition(
            extendedApiDefinition(serviceName = serviceName.value, access = ApiAccess.Private(), loggedIn = false, authorised = false)
          )
          theDocumentationServiceWillFetchApiSpecification(mockApiSpecification)

          val result = underTest.renderApiDocumentation(serviceName, versionOne, Option(true))(request)

          verifyRedirectToLoginPage(result, serviceName, versionOne)
        }

        "display the not found page when Principal and Subordinate ApiAvailability's are None" in new DocumentationRenderVersionSetup {
          theUserIsLoggedIn()
          val apiDefinition = extendedApiDefinitionWithNoAPIAvailability(serviceName, versionOne)

          theDefinitionServiceWillReturnAnApiDefinition(apiDefinition)
          theDocumentationServiceWillFetchApiSpecification(mockApiSpecification)

          val result = underTest.renderApiDocumentation(serviceName, versionOne, Option(true))(request)

          verifyNotFoundPageRendered(result)
        }

        "display the private API options when Principal ApiAvailability is None but Subordinate ApiAvailability is set" in new DocumentationRenderVersionSetup {
          theUserIsLoggedIn()

          val subordinateApiAvailability = ApiAvailability(endpointsEnabled = true, ApiAccess.PUBLIC, loggedIn = false, authorised = true)
          val apiDefinition              = extendedApiDefinitionWithPrincipalAndSubordinateAPIAvailability(
            serviceName,
            versionOne,
            None,
            Some(subordinateApiAvailability)
          )

          theDefinitionServiceWillReturnAnApiDefinition(apiDefinition)
          theDocumentationServiceWillFetchApiSpecification(mockApiSpecification)

          val result = underTest.renderApiDocumentation(serviceName, versionOne, Option(true))(request)

          versionOptionIsRendered(result, "1.0", apiDefinition.versions.head.displayedStatus) shouldBe true
        }
        // APIS-4844
        "display the not found page when Principal access is PUBLIC and Subordinate access is PRIVATE loggedIn and not authorised" in new DocumentationRenderVersionSetup {
          theUserIsLoggedIn()

          val principalApiAvailability   = ApiAvailability(endpointsEnabled = true, ApiAccess.PUBLIC, loggedIn = false, authorised = true)
          val subordinateApiAvailability = ApiAvailability(endpointsEnabled = true, ApiAccess.Private(), loggedIn = true, authorised = false)

          val apiDefinition = extendedApiDefinitionWithPrincipalAndSubordinateAPIAvailability(
            serviceName,
            versionOne,
            Some(principalApiAvailability),
            Some(subordinateApiAvailability)
          )

          theDefinitionServiceWillReturnAnApiDefinition(apiDefinition)
          theDocumentationServiceWillFetchApiSpecification(mockApiSpecification)

          val result = underTest.renderApiDocumentation(serviceName, versionOne, Option(true))(request)

          verifyNotFoundPageRendered(result)
        }

        "display the private API options when Principal access is PUBLIC and Subordinate access is PUBLIC" in new DocumentationRenderVersionSetup {
          theUserIsLoggedIn()

          val principalApiAvailability   = ApiAvailability(endpointsEnabled = true, ApiAccess.PUBLIC, loggedIn = false, authorised = true)
          val subordinateApiAvailability = ApiAvailability(endpointsEnabled = true, ApiAccess.PUBLIC, loggedIn = false, authorised = true)

          val apiDefinition = extendedApiDefinitionWithPrincipalAndSubordinateAPIAvailability(
            serviceName,
            versionOne,
            Some(principalApiAvailability),
            Some(subordinateApiAvailability)
          )

          theDefinitionServiceWillReturnAnApiDefinition(apiDefinition)
          theDocumentationServiceWillFetchApiSpecification(mockApiSpecification)

          val result = underTest.renderApiDocumentation(serviceName, versionOne, Option(true))(request)

          versionOptionIsRendered(result, "1.0", apiDefinition.versions.head.displayedStatus) shouldBe true
        }

        "display the private API options when Principal access is PRIVATE and Subordinate access is PUBLIC" in new DocumentationRenderVersionSetup {
          theUserIsLoggedIn()

          val principalApiAvailability   = ApiAvailability(endpointsEnabled = true, ApiAccess.Private(), loggedIn = false, authorised = false)
          val subordinateApiAvailability = ApiAvailability(endpointsEnabled = true, ApiAccess.PUBLIC, loggedIn = false, authorised = true)

          val apiDefinition = extendedApiDefinitionWithPrincipalAndSubordinateAPIAvailability(
            serviceName,
            versionOne,
            Some(principalApiAvailability),
            Some(subordinateApiAvailability)
          )

          theDefinitionServiceWillReturnAnApiDefinition(apiDefinition)
          theDocumentationServiceWillFetchApiSpecification(mockApiSpecification)

          val result = underTest.renderApiDocumentation(serviceName, versionOne, Option(true))(request)

          versionOptionIsRendered(result, "1.0", apiDefinition.versions.head.displayedStatus) shouldBe true
        }

        "display the private API options when Principal access is PRIVATE and Subordinate access is PRIVATE, loggedIn and authorised" in new DocumentationRenderVersionSetup {
          theUserIsLoggedIn()

          val principalApiAvailability   = ApiAvailability(endpointsEnabled = true, ApiAccess.Private(), loggedIn = false, authorised = false)
          val subordinateApiAvailability = ApiAvailability(endpointsEnabled = true, ApiAccess.Private(), loggedIn = true, authorised = true)

          val apiDefinition = extendedApiDefinitionWithPrincipalAndSubordinateAPIAvailability(
            serviceName,
            versionOne,
            Some(principalApiAvailability),
            Some(subordinateApiAvailability)
          )

          theDefinitionServiceWillReturnAnApiDefinition(apiDefinition)
          theDocumentationServiceWillFetchApiSpecification(mockApiSpecification)

          val result = underTest.renderApiDocumentation(serviceName, versionOne, Option(true))(request)

          versionOptionIsRendered(result, "1.0", apiDefinition.versions.head.displayedStatus) shouldBe true
        }

        "display the not found page when Principal access is PRIVATE and Subordinate access is PRIVATE and not authorised" in new DocumentationRenderVersionSetup {
          theUserIsLoggedIn()

          val principalApiAvailability   = ApiAvailability(endpointsEnabled = true, ApiAccess.Private(), loggedIn = false, authorised = false)
          val subordinateApiAvailability = ApiAvailability(endpointsEnabled = true, ApiAccess.Private(), loggedIn = true, authorised = false)

          val apiDefinition = extendedApiDefinitionWithPrincipalAndSubordinateAPIAvailability(
            serviceName,
            versionOne,
            Some(principalApiAvailability),
            Some(subordinateApiAvailability)
          )

          theDefinitionServiceWillReturnAnApiDefinition(apiDefinition)
          theDocumentationServiceWillFetchApiSpecification(mockApiSpecification)

          val result = underTest.renderApiDocumentation(serviceName, versionOne, Option(true))(request)

          verifyNotFoundPageRendered(result)
        }

        "display the OAS when no RAML is found including fraud prevention information" in new Setup {
          theUserIsLoggedIn()

          theDefinitionServiceWillReturnAnApiDefinition(
            extendedApiDefinition(serviceName = serviceName.value, name = "VAT (MTD)")
          )

          theDocumentationServiceWillFetchNoSpecification()
          when(downloadConnector.fetch(*[ServiceName], *[ApiVersionNbr], *)).thenReturn(successful(None))

          val result = underTest.renderApiDocumentation(serviceName, versionOne, Option(true))(request)

          status(result) shouldBe OK
          contentAsString(result) should include("Endpoints")
          contentAsString(result) should include("Fraud Prevention")
        }

        "render OAS for a test support API without including fraud prevention information" in new Setup {
          theUserIsLoggedIn()

          theDefinitionServiceWillReturnAnApiDefinition(
            extendedApiDefinition(serviceName = serviceName.value, name = "Create Test User", isTestSupport = true)
          )

          theDocumentationServiceWillFetchNoSpecification()
          when(downloadConnector.fetch(*[ServiceName], *[ApiVersionNbr], *)).thenReturn(successful(None))

          val result = underTest.renderApiDocumentation(serviceName, versionOne, Option(true))(request)

          status(result) shouldBe OK
          println(contentAsString(result))
          contentAsString(result) should include("Endpoints")
          contentAsString(result) should not include ("Fraud Prevention")
        }
      }

      "display the not found page when invalid service specified" in new Setup {
        theUserIsLoggedIn()
        theDefinitionServiceWillFail(new NotFoundException("Expected unit test failure"))

        val result = underTest.renderApiDocumentation(serviceName, versionOne, Option(true))(request)

        verifyNotFoundPageRendered(result)
      }

      "display the retired version page when the API version is marked as retired" in new Setup {
        theUserIsLoggedIn()
        theDefinitionServiceWillReturnAnApiDefinition(
          extendedApiDefinitionWithRetiredVersion(serviceName, versionOne, ApiVersionNbr("1.1"))
        )

        val result = underTest.renderApiDocumentation(serviceName, versionOne, Option(true))(request)

        verifyApiDocumentationPageRendered(result)
        verifyLinkToStableDocumentationRendered(result, serviceName, ApiVersionNbr("1.1"))
      }

      "display the not found page when invalid version specified" in new Setup {
        theUserIsLoggedIn()
        theDefinitionServiceWillReturnAnApiDefinition(
          extendedApiDefinitionWithRetiredVersion(serviceName, versionOne, ApiVersionNbr("1.1"))
        )

        val result = underTest.renderApiDocumentation(serviceName, versionTwo, Option(true))(request)

        verifyNotFoundPageRendered(result)
      }

      "display the not found page when no API definition is returned" in new Setup {
        theUserIsLoggedIn()
        theDefinitionServiceWillReturnNoApiDefinition()

        val result = underTest.renderApiDocumentation(serviceName, versionOne, Option(true))(request)

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
        val url    = "http://host:port/some.path.to.a.raml.document"
        when(ramlPreviewConnector.fetchPreviewApiSpecification(*)(*)).thenReturn(failed(RamlParseException("Expected unit test failure")))
        val result = underTest.previewApiDocumentation(Some(url))(request)
        verifyErrorPageRendered(expectedStatus = INTERNAL_SERVER_ERROR, expectedError = "Expected unit test failure")(result)
      }
    }
  }

  "bustCache" should {
    "honour the passed in parameter" in new Setup {
      underTest.bustCache(Some(true)) shouldBe true
      underTest.bustCache(Some(false)) shouldBe false
      underTest.bustCache(None) shouldBe false
    }
  }

  "fetchTestEndpointJson" should {
    "sort the results by URL" in new Setup with RamlPreviewEnabled {
      val endpoints = List(
        TestEndpoint("{service-url}/employers-paye/www"),
        TestEndpoint("{service-url}/employers-paye/aaa"),
        TestEndpoint("{service-url}/employers-paye/zzz"),
        TestEndpoint("{service-url}/employers-paye/ddd")
      )

      when(documentationService.buildTestEndpoints(*[ServiceName], *[ApiVersionNbr])(*)).thenReturn(successful(endpoints))
      val result = underTest.fetchTestEndpointJson(ServiceName("employers-paye"), versionOne)(request)
      status(result) shouldBe OK
      contentAsString(result) should include regex s"aaa.*ddd.*www.*zzz"
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
