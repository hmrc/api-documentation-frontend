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
import scala.concurrent.Future

import org.apache.pekko.stream.Materializer

import play.api.http.HeaderNames.LOCATION
import play.api.http.Status.MOVED_PERMANENTLY
import play.api.mvc._
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.play.partials.HtmlPartial

import uk.gov.hmrc.apidocumentation.connectors.DeveloperFrontendConnector
import uk.gov.hmrc.apidocumentation.controllers.utils._
import uk.gov.hmrc.apidocumentation.mocks.config._
import uk.gov.hmrc.apidocumentation.mocks.services._
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.apidocumentation.services.{LoggedInUserService, PartialsService}
import uk.gov.hmrc.apidocumentation.views.html.{TermsOfUseNotMeetingView, TermsOfUseWhatYouCanExpectView, _}
import uk.gov.hmrc.apidocumentation.{ErrorHandler, controllers}

class DocumentationControllerSpec
    extends CommonControllerBaseSpec
    with PageRenderVerification {

  trait Setup
      extends AppConfigMock
      with NavigationServiceMock {

    val developerFrontendConnector = mock[DeveloperFrontendConnector]
    val partialsService            = new PartialsService(developerFrontendConnector)
    val errorHandler               = app.injector.instanceOf[ErrorHandler]
    val mcc                        = app.injector.instanceOf[MessagesControllerComponents]

    implicit lazy val materializer: Materializer = app.materializer

    private lazy val indexView = app.injector.instanceOf[IndexView]

    private lazy val tutorialsView = app.injector.instanceOf[TutorialsView]

    private lazy val developmentPracticesView =
      app.injector.instanceOf[DevelopmentPracticesView]

    private lazy val namingGuidelinesView           =
      app.injector.instanceOf[NamingGuidelinesView]
    private lazy val referenceView                  = app.injector.instanceOf[ReferenceView]
    private lazy val apiStatusesView                = app.injector.instanceOf[ApiStatusesView]
    private lazy val termsOfUseView                 = app.injector.instanceOf[TermsOfUseView]
    private lazy val usingTheHubView                = app.injector.instanceOf[UsingTheHubView]
    private lazy val termsOfUseWhatYouCanExpectView = app.injector.instanceOf[TermsOfUseWhatYouCanExpectView]
    private lazy val termsOfUseNotMeetingView       = app.injector.instanceOf[TermsOfUseNotMeetingView]
    private lazy val loggedInUserService            = app.injector.instanceOf[LoggedInUserService]

    lazy val usingTheHubBreadcrumb = Crumb(
      "Getting started",
      controllers.routes.DocumentationController.usingTheHubPage().url
    )

    val underTest: DocumentationController = new DocumentationController(
      navigationService,
      loggedInUserService,
      partialsService,
      mcc,
      indexView,
      tutorialsView,
      developmentPracticesView,
      namingGuidelinesView,
      referenceView,
      termsOfUseView,
      termsOfUseWhatYouCanExpectView,
      termsOfUseNotMeetingView,
      usingTheHubView,
      apiStatusesView
    )

    def pageTitle(pagePurpose: String) = {
      s"$pagePurpose - HMRC Developer Hub - GOV.UK"
    }
  }

  "DocumentationController" should {
    "fetch the terms of use from third party developer and render them in the terms of use page" in new Setup {
      when(
        developerFrontendConnector.fetchTermsOfUsePartial()(*)
      ).thenReturn(
        Future.successful(
          HtmlPartial.Success(None, Html("<p>blah blah blah</p>"))
        )
      )

      verifyPageRendered(
        pageTitle("Terms Of Use"),
        bodyContains = Seq("blah blah blah")
      )(underTest.termsOfUsePage()(request))
    }

    "display the reference guide page" in new Setup {
      when(underTest.appConfig.productionApiBaseUrl)
        .thenReturn("https://api.service.hmrc.gov.uk")
      verifyPageRendered(
        pageTitle("Reference guide"),
        bodyContains = Seq(
          "The base URL for sandbox APIs is:",
          "https://api.service.hmrc.gov.uk"
        )
      )(underTest.referenceGuidePage()(request))
    }

    "display the API statuses page" in new Setup {
      when(underTest.appConfig.productionApiBaseUrl)
        .thenReturn("https://api.service.hmrc.gov.uk")
      verifyPageRendered(
        pageTitle("API statuses"),
        bodyContains = Seq(
          "We release different versions of APIs as they’re developed and updated.",
          "Alpha",
          "This version is being developed.",
          "Beta",
          "Stable",
          "This version is available for use.",
          "Deprecated",
          "This version has been replaced and will be removed."
        )
      )(underTest.apiStatusesPage()(request))
    }

    "display the What you can expect from us page" in new Setup {
      verifyPageRendered(pageTitle("What you can expect from us"))(
        underTest.termsOfUseWhatYouCanExpectPage()(request)
      )
    }

    "display the Not Meeting the Terms of Use page" in new Setup {
      verifyPageRendered(pageTitle("Not meeting the terms of use"))(
        underTest.termsOfUseNotMeetingPage()(request)
      )
    }

    "display the getting started page" in new Setup {
      verifyPageRendered(pageTitle("Getting started"))(
        underTest.usingTheHubPage()(request)
      )
    }

    "display the naming guidelines page" in new Setup {
      verifyPageRendered(pageTitle("Application naming guidelines"))(
        underTest.nameGuidelinesPage()(request)
      )
    }

    "display the Making Tax Digital guides page" in new Setup {
      val result = underTest.mtdIntroductionPage()(request)
      status(result) shouldBe SEE_OTHER
      headers(result).get(LOCATION) shouldBe Some("/api-documentation/docs/api")
    }

    "display the Income Tax (MTD) End-to-End Service Guide page" in new Setup {
      val result = underTest.mtdIncomeTaxServiceGuidePage()(request)
      status(result) shouldBe MOVED_PERMANENTLY
      headers(result).get(LOCATION) shouldBe Some(
        "/guides/income-tax-mtd-end-to-end-service-guide/"
      )
    }
  }
}
