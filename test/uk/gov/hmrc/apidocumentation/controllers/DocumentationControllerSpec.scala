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

import org.mockito.Matchers.any
import org.mockito.Mockito.when
import play.api.http.Status.MOVED_PERMANENTLY
import play.api.mvc._
import play.twirl.api.Html
import uk.gov.hmrc.apidocumentation.{controllers, ErrorHandler}
import uk.gov.hmrc.apidocumentation.connectors.DeveloperFrontendConnector
import uk.gov.hmrc.apidocumentation.controllers.utils._
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.apidocumentation.services.PartialsService
import uk.gov.hmrc.apidocumentation.views.html._
import uk.gov.hmrc.apidocumentation.mocks.services._
import uk.gov.hmrc.apidocumentation.mocks.config._

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.partials.HtmlPartial

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class DocumentationControllerSpec extends CommonControllerBaseSpec with PageRenderVerification {

  trait Setup extends ApiDocumentationServiceMock with AppConfigMock with NavigationServiceMock {

    val developerFrontendConnector = mock[DeveloperFrontendConnector]
    val partialsService = new PartialsService(developerFrontendConnector)
    val errorHandler = app.injector.instanceOf[ErrorHandler]
    val mcc = app.injector.instanceOf[MessagesControllerComponents]

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

    lazy val usingTheHubBreadcrumb = Crumb("Using the Developer Hub", controllers.routes.DocumentationController.usingTheHubPage().url)

    when(documentationService.defaultExpiration).thenReturn(1.hour)

    val underTest: DocumentationController = new DocumentationController(
      navigationService,
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
      usingTheHubView
    )

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

    "display the Making Tax Digital guides page" in new Setup {
      verifyPageRendered(pageTitle("Making Tax Digital guides"))(underTest.mtdIntroductionPage()(request))
    }

    "display the Income Tax (MTD) End-to-End Service Guide page" in new Setup {
      val result = await(underTest.mtdIncomeTaxServiceGuidePage()(request))
      status(result) shouldBe MOVED_PERMANENTLY
      result.header.headers.get("Location") shouldBe Some("/guides/income-tax-mtd-end-to-end-service-guide/")
    }
  }
}
