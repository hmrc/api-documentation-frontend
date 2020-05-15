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

import play.api.mvc._
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.partials.HtmlPartial
import uk.gov.hmrc.apidocumentation
import uk.gov.hmrc.apidocumentation.{controllers, ErrorHandler}
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.connectors.DeveloperFrontendConnector
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.apidocumentation.services.{PartialsService, RAML}
import uk.gov.hmrc.apidocumentation.views.html._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import org.mockito.Mockito.when
import org.mockito.Matchers.any
import play.twirl.api.Html

// TODO - Make this test work !!!
class DocumentationControllerSpec extends CommonControllerBaseSpec with PageRenderVerification {

  class Setup(ramlPreviewEnabled: Boolean = false) {
    implicit val appConfig = mock[ApplicationConfig]
    val developerFrontendConnector = mock[DeveloperFrontendConnector]
    val partialsService = new PartialsService(developerFrontendConnector)
    val errorHandler = app.injector.instanceOf[ErrorHandler]
    val mcc = app.injector.instanceOf[MessagesControllerComponents]
    val mockRamlAndSchemas = apidocumentation.models.RamlAndSchemas(mock[RAML], mock[Map[String, JsonSchema]])

    implicit lazy val materializer = app.materializer

    private lazy val indexView = app.injector.instanceOf[IndexView]
    private lazy val retiredVersionJumpView = app.injector.instanceOf[RetiredVersionJumpView]
    private lazy val tutorialsView = app.injector.instanceOf[TutorialsView]
    private lazy val credentialsView = app.injector.instanceOf[CredentialsView]
    private lazy val developmentPracticesView = app.injector.instanceOf[DevelopmentPracticesView]
    private lazy val fraudPreventionView = app.injector.instanceOf[FraudPreventionView]
    private lazy val mtdIntroductionView = app.injector.instanceOf[MtdIntroductionView]
    private lazy val namingGuidelinesView = app.injector.instanceOf[NamingGuidelinesView]
    private lazy val referenceView = app.injector.instanceOf[ReferenceView]
    private lazy val termsOfUseView = app.injector.instanceOf[TermsOfUseView]
    private lazy val usingTheHubView = app.injector.instanceOf[UsingTheHubView]

    lazy val apiDocsBreadcrumb = Crumb("API Documentation", controllers.routes.ApiDocumentationController.apiIndexPage(None, None, None).url)
    lazy val usingTheHubBreadcrumb = Crumb("Using the Developer Hub", controllers.routes.DocumentationController.usingTheHubPage().url)

    when(appConfig.ramlPreviewEnabled).thenReturn(ramlPreviewEnabled)
    when(appConfig.title).thenReturn("HMRC Developer Hub")
    when(documentationService.defaultExpiration).thenReturn(1.hour)

    val underTest: DocumentationController = new DocumentationController(navigationService,
                                        partialsService,
                                        mcc,
                                        indexView,
                                        retiredVersionJumpView,
                                        tutorialsView,
                                        credentialsView,
                                        developmentPracticesView,
                                        fraudPreventionView,
                                        mtdIntroductionView,
                                        namingGuidelinesView,
                                        referenceView,
                                        termsOfUseView,
                                        usingTheHubView)


    def verifyErrorPageRendered(actualPageFuture: Future[Result], expectedStatus: Int, expectedError: String) {
      val actualPage = await(actualPageFuture)
      status(actualPage) shouldBe expectedStatus
      bodyOf(actualPage) should include(expectedError)
    }

    def verifyNotFoundPageRendered(actualPageFuture: Future[Result]) {
      val actualPage = await(actualPageFuture)
      status(actualPage) shouldBe NOT_FOUND
    }

    def verifyLinkToStableDocumentationRendered(actualPage: Result, service: String, version: String) = {
      bodyOf(actualPage) should include(s"""<a href="/api-documentation/docs/api/service/$service/$version">""")
    }

    def verifyApiDocumentationPageRendered(actualPage: Result, version: String, apiStatus: String) = {
      verifyPageRendered(pageTitle("Hello World"), breadcrumbs = List(homeBreadcrumb, apiDocsBreadcrumb))(actualPage)
      verifyBreadcrumbEndpointRendered(actualPage, s"Hello World API v$version ($apiStatus)")
    }


    // def versionOptionIsRendered(result: Result, service: String, version: String, displayedStatus: String) = {
    //   bodyOf(result).contains(s"""<option selected value="$version" aria-label="Select to view documentation for v$version ($displayedStatus)">""")
    // }

    // def theDocumentationServiceWillFetchRaml(ramlAndSchemas: RamlAndSchemas) = {
    //   when(documentationService.fetchRAML(any(), any(), any())(any[HeaderCarrier])).thenReturn(ramlAndSchemas)
    // }

    // def theDocumentationServiceWillFailWhenFetchingRaml(exception: Throwable) = {
    //   when(documentationService.fetchRAML(any(), any(), any())(any[HeaderCarrier])).thenReturn(failed(exception))
    // }

    def pageTitle(pagePurpose: String) = {
      s"$pagePurpose - HMRC Developer Hub - GOV.UK"
    }


  }

  "DocumentationController" should {

    "fetch the terms of use from third party developer and render them in the terms of use page" in new Setup {
      when(developerFrontendConnector.fetchTermsOfUsePartial()(any[HeaderCarrier]))
        .thenReturn(Future.successful(HtmlPartial.Success(None, Html("<p>blah blah blah</p>"))))

      verifyPageRendered(pageTitle("Terms Of Use"),
        bodyContains = Seq("blah blah blah"))(underTest.termsOfUsePage()(request))
    }

    "display the reference guide page" in new Setup {
      when(underTest.appConfig.productionApiBaseUrl).thenReturn("https://api.service.hmrc.gov.uk")
      verifyPageRendered(pageTitle("Reference guide"),
        bodyContains = Seq("The base URL for sandbox APIs is:", "https://api.service.hmrc.gov.uk"))(underTest.referenceGuidePage()(request))
    }

    "display the using the hub page" in new Setup {
      verifyPageRendered(pageTitle("Using the Developer Hub"))(underTest.usingTheHubPage()(request))
    }

    "display the naming guidelines page" in new Setup {
      verifyPageRendered(pageTitle("Application naming guidelines"), breadcrumbs = List(homeBreadcrumb, usingTheHubBreadcrumb))(underTest.nameGuidelinesPage()(request))
    }

    "display the fraud prevention page" in new Setup {
      verifyPageRendered(pageTitle("Fraud prevention"))(underTest.fraudPreventionPage()(request))
    }

//     "display the Making Tax Digital guides page" in new Setup {
//       verifyPageRendered(underTest.mtdIntroductionPage()(request), pageTitle("Making Tax Digital guides"))
//     }

//     "display the Income Tax (MTD) End-to-End Service Guide page" in new Setup {
//       val result = await(underTest.mtdIncomeTaxServiceGuidePage()(request))
//       status(result) shouldBe MOVED_PERMANENTLY
//       result.header.headers.get("Location") shouldBe Some("/guides/income-tax-mtd-end-to-end-service-guide/")
//     }

//   "apiIndexPage" must {

//     "render the API List" in new Setup {
//       theUserIsLoggedIn()
//       theDefinitionServiceWillReturnApiDefinitions(List(anApiDefinition("service1", "1.0"), anApiDefinition("service2", "1.0")))

//       val result = underTest.apiIndexPage(None, None, None)(request)
//       verifyPageRendered(result, pageTitle("API Documentation"), bodyContains = Seq("API documentation"))

//     }

//     "render the filtered API list" in new Setup {
//       theUserIsLoggedIn()
//       theDefinitionServiceWillReturnApiDefinitions(List(anApiDefinition("service1", "1.0"), anApiDefinition("service2", "1.0")))

//       val result = underTest.apiIndexPage(None, None, Some("vat"))(request)

//       verifyPageRendered(result, pageTitle("Filtered API Documentation"), bodyContains = Seq("Filtered API documentation", "1 document found in", "VAT"))
//     }

//     "display the error page when the documentationService throws an exception" in new Setup {
//       theUserIsLoggedIn()
//       theDefinitionServiceWillFail(new Exception("Expected unit test failure"))

//       val result = underTest.apiIndexPage(None, None, None)(request)

//       verifyErrorPageRendered(result, expectedStatus = INTERNAL_SERVER_ERROR, expectedError = "Sorry, we’re experiencing technical difficulties")
//     }
//   }

//   "redirectToApiDocumentation" must {
//     "when given a version" should {
//       val version = "2.0"

//       "redirect to the documentation page for the specified version" in new Setup {
//         theUserIsLoggedIn()
//         theDefinitionServiceWillReturnAnApiDefinition(extendedApiDefinition(serviceName, "1.0"))
//         val result = await(underTest.redirectToApiDocumentation(serviceName, Some(version), Option(true))(request))
//         status(result) shouldBe SEE_OTHER
//         result.header.headers.get("location") shouldBe Some(s"/api-documentation/docs/api/service/hello-world/${version}?cacheBuster=true")
//       }
//     }

//     "when not given a version" should {
//       val version = None

//       "redirect to the documentation page" in new Setup {
//         theUserIsLoggedIn()
//         theDefinitionServiceWillReturnAnApiDefinition(extendedApiDefinition(serviceName, "1.0"))
//         val result = await(underTest.redirectToApiDocumentation(serviceName, version, Option(true))(request))
//         status(result) shouldBe SEE_OTHER
//         result.header.headers.get("location") shouldBe Some(s"/api-documentation/docs/api/service/hello-world/1.0?cacheBuster=true")
//       }

//       "redirect to the documentation page for api in private trial for user without authorisation" in new Setup {
//         theUserIsLoggedIn()
//         val privateTrialAPIDefinition = extendedApiDefinition(serviceName, "1.0",
//           APIAccessType.PRIVATE, loggedIn = true, authorised = false, isTrial = Some(true))
//         theDefinitionServiceWillReturnAnApiDefinition(privateTrialAPIDefinition)

//         val result = await(underTest.redirectToApiDocumentation(serviceName, None, Option(true))(request))
//         status(result) shouldBe SEE_OTHER
//         result.header.headers.get("location") shouldBe Some(s"/api-documentation/docs/api/service/hello-world/1.0?cacheBuster=true")
//       }

//       "redirect to the documentation page for api in private trial for user with authorisation" in new Setup {
//         theUserIsLoggedIn()
//         val privateTrialAPIDefinition = extendedApiDefinition(serviceName, "1.0",
//           APIAccessType.PRIVATE, loggedIn = true, authorised = true, isTrial = Some(true))
//         theDefinitionServiceWillReturnAnApiDefinition(privateTrialAPIDefinition)
//         val result = await(underTest.redirectToApiDocumentation(serviceName, None, Option(true))(request))
//         status(result) shouldBe SEE_OTHER
//         result.header.headers.get("location") shouldBe Some(s"/api-documentation/docs/api/service/hello-world/1.0?cacheBuster=true")
//       }

//       "redirect to the documentation page for the latest accessible version" in new Setup {
//         theUserIsLoggedIn()

//         val apiDefinition =
//           ExtendedAPIDefinition(
//             serviceName,
//             "http://service",
//             "Hello World",
//             "Say Hello World",
//             "hello",
//             requiresTrust = false,
//             isTestSupport = false,
//             Seq(
//               ExtendedAPIVersion(
//                 "1.0", APIStatus.BETA, Seq(Endpoint(endpointName, "/world", HttpMethod.GET, None)),
//                 Some(APIAvailability(endpointsEnabled = true, APIAccess(APIAccessType.PUBLIC), loggedIn = false, authorised = true)),
//                 None),
//               ExtendedAPIVersion(
//                 "1.1", APIStatus.STABLE, Seq(Endpoint(endpointName, "/world", HttpMethod.GET, None)),
//                 Some(APIAvailability(endpointsEnabled = true, APIAccess(APIAccessType.PRIVATE), loggedIn = false, authorised = false)),
//                 None)
//             ))

//         theDefinitionServiceWillReturnAnApiDefinition(apiDefinition)
//         val result = await(underTest.redirectToApiDocumentation("hello-world", version, Option(true))(request))
//         status(result) shouldBe SEE_OTHER
//         result.header.headers.get("location") shouldBe Some(s"/api-documentation/docs/api/service/hello-world/1.0?cacheBuster=true")
//       }

//       "display the not found page when invalid service specified" in new Setup {
//         theUserIsLoggedIn()
//         theDefinitionServiceWillFail(new NotFoundException("Expected unit test failure"))

//         val result = underTest.redirectToApiDocumentation(serviceName, version, Option(true))(request)
//         verifyNotFoundPageRendered(result)
//       }
//     }
//   }

//   "renderApiDocumentation" should {

//     "display the documentation page" in new Setup {
//       theUserIsLoggedIn()
//       theDefinitionServiceWillReturnAnApiDefinition(extendedApiDefinition(serviceName, "1.0"))
//       theDocumentationServiceWillFetchRaml(mockRamlAndSchemas)

//       val result = underTest.renderApiDocumentation(serviceName, "1.0", Option(true))(request)

//       verifyApiDocumentationPageRendered(result, "1.0", "Stable")
//     }

//     "display the not found page when invalid service specified" in new Setup {
//       theUserIsLoggedIn()
//       theDefinitionServiceWillFail(new NotFoundException("Expected unit test failure"))

//       val result = underTest.renderApiDocumentation(serviceName, "1.0", Option(true))(request)

//       verifyNotFoundPageRendered(result)
//     }

//     "display the not found page when RAML file not found" in new Setup {
//       theUserIsLoggedIn()
//       theDefinitionServiceWillReturnAnApiDefinition(extendedApiDefinition(serviceName, "1.0"))
//       theDocumentationServiceWillFailWhenFetchingRaml(RamlNotFoundException("not found"))

//       val result = underTest.renderApiDocumentation(serviceName, "1.0", Option(true))(request)

//       verifyNotFoundPageRendered(result)
//     }

//     "display the retired version page when the API version is marked as retired" in new Setup {
//       theUserIsLoggedIn()
//       theDefinitionServiceWillReturnAnApiDefinition(
//         extendedApiDefinitionWithRetiredVersion(serviceName, "1.0", "1.1"))

//       val result = underTest.renderApiDocumentation(serviceName, "1.0", Option(true))(request)

//       verifyApiDocumentationPageRendered(result, "1.0", "Retired")
//       verifyLinkToStableDocumentationRendered(result, serviceName, "1.1")
//     }

//     "display the documentation when the API version is not marked as retired" in new Setup {
//       theUserIsLoggedIn()
//       theDefinitionServiceWillReturnAnApiDefinition(
//         extendedApiDefinitionWithRetiredVersion(serviceName, "1.0", "1.1"))
//       theDocumentationServiceWillFetchRaml(mockRamlAndSchemas)

//       val result = underTest.renderApiDocumentation(serviceName, "1.1", Option(true))(request)

//       verifyApiDocumentationPageRendered(result, "1.1", "Stable")
//     }

//     "display the not found page when invalid version specified" in new Setup {
//       theUserIsLoggedIn()
//       theDefinitionServiceWillReturnAnApiDefinition(
//         extendedApiDefinitionWithRetiredVersion(serviceName, "1.0", "1.1"))

//       val result = underTest.renderApiDocumentation(serviceName, "2.0", Option(true))(request)

//       verifyNotFoundPageRendered(result)
//     }

//     "display the not found page when no API definition is returned" in new Setup {
//       theUserIsLoggedIn()
//       theDefinitionServiceWillReturnNoApiDefinition()

//       val result = underTest.renderApiDocumentation(serviceName, "1.0", Option(true))(request)

//       verifyNotFoundPageRendered(result)
//     }

//     "display the documentation when the API is private but the logged in user has access to it" in new Setup {
//       theUserIsLoggedIn()
//       theDefinitionServiceWillReturnAnApiDefinition(
//         extendedApiDefinition(serviceName, "1.0", APIAccessType.PRIVATE, loggedIn = true, authorised = true))
//       theDocumentationServiceWillFetchRaml(mockRamlAndSchemas)

//       val result = underTest.renderApiDocumentation(serviceName, "1.0", Option(true))(request)

//       verifyApiDocumentationPageRendered(result, "1.0", "Private Stable")
//     }

//     "display the private API options when logged in and user has access to it" in new Setup {
//       theUserIsLoggedIn()

//       val apiDefinition = extendedApiDefinition(serviceName, "1.0", APIAccessType.PRIVATE, loggedIn = true, authorised = true)

//       theDefinitionServiceWillReturnAnApiDefinition(apiDefinition)
//       theDocumentationServiceWillFetchRaml(mockRamlAndSchemas)

//       val result = underTest.renderApiDocumentation(serviceName, "1.0", Option(true))(request)

//       versionOptionIsRendered(result, serviceName, "1.0", apiDefinition.versions.head.displayedStatus) shouldBe true
//     }

//     "display the private API options when not logged in and is in trial" in new Setup {
//       theUserIsNotLoggedIn()

//       val apiDefinition = extendedApiDefinition(serviceName, "1.0", APIAccessType.PRIVATE, loggedIn = false, authorised = false, isTrial = Some(true))

//       theDefinitionServiceWillReturnAnApiDefinition(apiDefinition)
//       theDocumentationServiceWillFetchRaml(mockRamlAndSchemas)

//       val result = underTest.renderApiDocumentation(serviceName, "1.0", Option(true))(request)

//       versionOptionIsRendered(result, serviceName, "1.0", apiDefinition.versions.head.displayedStatus) shouldBe true
//     }

//     "display the private API options when logged in and is in trial but the user is not authorised" in new Setup {
//       theUserIsLoggedIn()

//       val apiDefinition = extendedApiDefinition(serviceName, "1.0", APIAccessType.PRIVATE, loggedIn = true, authorised = false, isTrial = Some(true))

//       theDefinitionServiceWillReturnAnApiDefinition(apiDefinition)
//       theDocumentationServiceWillFetchRaml(mockRamlAndSchemas)

//       val result = underTest.renderApiDocumentation(serviceName, "1.0", Option(true))(request)

//       versionOptionIsRendered(result, serviceName, "1.0", apiDefinition.versions.head.displayedStatus) shouldBe true
//     }

//     "display the private API options when logged in and is in trial and the user is authorised" in new Setup {
//       theUserIsLoggedIn()

//       val apiDefinition = extendedApiDefinition(serviceName, "1.0", APIAccessType.PRIVATE, loggedIn = true, authorised = true, isTrial = Some(true))

//       theDefinitionServiceWillReturnAnApiDefinition(apiDefinition)

//       theDocumentationServiceWillFetchRaml(mockRamlAndSchemas)

//       val result = underTest.renderApiDocumentation(serviceName, "1.0", Option(true))(request)

//       versionOptionIsRendered(result, serviceName, "1.0", apiDefinition.versions.head.displayedStatus) shouldBe true
//     }

//     "not display the private API options when not in trial, not logged in and (therefore) is not authorised" in new Setup {
//       theUserIsNotLoggedIn()

//       val apiDefinition = extendedApiDefinition(serviceName, "1.0", APIAccessType.PRIVATE, loggedIn = false, authorised = false)

//       theDefinitionServiceWillReturnAnApiDefinition(apiDefinition)
//       theDocumentationServiceWillFetchRaml(mockRamlAndSchemas)

//       val result = underTest.renderApiDocumentation(serviceName, "1.0", Option(true))(request)

//       versionOptionIsRendered(result, serviceName, "1.0", apiDefinition.versions.head.displayedStatus) shouldBe false
//     }

//     "display the not found page when the API is private and the logged in user does not have access to it" in new Setup {
//       theUserIsLoggedIn()
//       theDefinitionServiceWillReturnAnApiDefinition(
//         extendedApiDefinition(serviceName, "1.0", APIAccessType.PRIVATE, loggedIn = true, authorised = false))
//       theDocumentationServiceWillFetchRaml(mockRamlAndSchemas)

//       val result = underTest.renderApiDocumentation(serviceName, "1.0", Option(true))(request)

//       verifyNotFoundPageRendered(result)
//     }

//     "redirect to the login page when the API is private and the user is not logged in" in new Setup {
//       theUserIsNotLoggedIn()
//       theDefinitionServiceWillReturnAnApiDefinition(
//         extendedApiDefinition(serviceName, "1.0", APIAccessType.PRIVATE, loggedIn = false, authorised = false))
//       theDocumentationServiceWillFetchRaml(mockRamlAndSchemas)

//       val result = underTest.renderApiDocumentation(serviceName, "1.0", Option(true))(request)

//       verifyRedirectToLoginPage(result, serviceName, "1.0")
//     }

//     "display the error page when any other exception occurs" in new Setup {
//       theUserIsLoggedIn()
//       theDefinitionServiceWillReturnAnApiDefinition(extendedApiDefinition(serviceName, "1.0"))
//       theDocumentationServiceWillFailWhenFetchingRaml(new Exception("expected unit test failure"))

//       val result = underTest.renderApiDocumentation(serviceName, "1.0", Option(true))(request)

//       verifyErrorPageRendered(result, expectedStatus = INTERNAL_SERVER_ERROR, expectedError = "Sorry, we’re experiencing technical difficulties")
//     }

//     "tell clients not to cache the page" in new Setup {
//       theUserIsLoggedIn()
//       theDefinitionServiceWillReturnAnApiDefinition(extendedApiDefinition(serviceName, "1.0"))
//       theDocumentationServiceWillFetchRaml(mockRamlAndSchemas)

//       val result = underTest.renderApiDocumentation(serviceName, "1.0", Option(true))(request)

//       result.header.headers.get("Cache-Control") shouldBe Some("no-cache,no-store,max-age=0")
//     }
//   }

//   "preview docs" should {

//     "render 404 page when feature switch off" in new Setup {
//       val result = underTest.previewApiDocumentation(None)(request)
//       verifyNotFoundPageRendered(result)
//     }

//     "render 200 page when feature switch on" in new Setup(ramlPreviewEnabled = true) {
//       val result = underTest.previewApiDocumentation(None)(request)
//       verifyPageRendered(result, pageTitle("API Documentation Preview"))
//     }

//     "render 500 page when no URL supplied" in new Setup(ramlPreviewEnabled = true) {
//       val result = underTest.previewApiDocumentation(Some(""))(request)
//       verifyErrorPageRendered(result, expectedStatus = INTERNAL_SERVER_ERROR, expectedError = "No URL supplied")
//     }

//     "render 500 page when service throws exception" in new Setup(ramlPreviewEnabled = true) {
//       val url = "http://host:port/some.path.to.a.raml.document"
//       when(documentationService.fetchRAML(any(), any())).thenReturn(failed(RamlParseException("Expected unit test failure")))
//       val result = underTest.previewApiDocumentation(Some(url))(request)
//       verifyErrorPageRendered(result, expectedStatus = INTERNAL_SERVER_ERROR, expectedError = "Expected unit test failure")
//     }

//   }

//   "bustCache" should {
//     "override value of the query parameter if in stub mode" in new Setup {
//       underTest.bustCache(stubMode = false, Some(true)) shouldBe true
//       underTest.bustCache(stubMode = false, Some(false)) shouldBe false
//       underTest.bustCache(stubMode = true, Some(true)) shouldBe true
//       underTest.bustCache(stubMode = true, Some(false)) shouldBe true
//     }

//     "return true if no query parameter was provided and the app is running in Stub mode" in new Setup {
//       underTest.bustCache(stubMode = false, None) shouldBe false
//       underTest.bustCache(stubMode = true, None) shouldBe true
//     }
//   }

//   "fetchTestEndpointJson" should {
//     "sort the results by URL" in new Setup(ramlPreviewEnabled = true) {
//       val endpoints = Seq(
//         TestEndpoint("{service-url}/employers-paye/www"),
//         TestEndpoint("{service-url}/employers-paye/aaa"),
//         TestEndpoint("{service-url}/employers-paye/zzz"),
//         TestEndpoint("{service-url}/employers-paye/ddd")
//       )
//       when(documentationService.buildTestEndpoints(any(), any())(any[HeaderCarrier])).thenReturn(endpoints)
//       val result = underTest.fetchTestEndpointJson("employers-paye", "1.0")(request)
//       val actualPage = await(result)
//       actualPage.header.status shouldBe OK
//       bodyOf(actualPage) should include regex s"aaa.*ddd.*www.*zzz"
//     }
//   }

//   "renderXmlApiDocumentation" must {

//     "render the XML API landing page when the XML API definition exists" in new Setup {
//       theUserIsLoggedIn()

//       val existingXmlApiName = "Charities Online"
//       val result = underTest.renderXmlApiDocumentation(existingXmlApiName)(request)

//       verifyPageRendered(result, pageTitle(existingXmlApiName), bodyContains = Seq(existingXmlApiName))
//     }

//     "return 404 not found when the XML API definition does not exist" in new Setup {
//       theUserIsLoggedIn()

//       val nonExistingXmlApiName = "Fake XML API name"
//       val result = underTest.renderXmlApiDocumentation(nonExistingXmlApiName)(request)

//       status(result) shouldBe NOT_FOUND
//     }

  }
}
