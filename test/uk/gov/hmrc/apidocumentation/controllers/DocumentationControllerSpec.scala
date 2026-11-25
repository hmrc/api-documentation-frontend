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
import play.api.mvc.*
import play.api.test.Helpers.*
import play.twirl.api.Html
import uk.gov.hmrc.play.partials.HtmlPartial

import uk.gov.hmrc.apidocumentation.connectors.DeveloperFrontendConnector
import uk.gov.hmrc.apidocumentation.mocks.config.*
import uk.gov.hmrc.apidocumentation.mocks.services.*
import uk.gov.hmrc.apidocumentation.models.*
import uk.gov.hmrc.apidocumentation.services.{LoggedInUserService, PartialsService}
import uk.gov.hmrc.apidocumentation.views.html.{TermsOfUseNotMeetingView, TermsOfUseWhatYouCanExpectView, *}
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

    private lazy val guidesAndTutorialsView = app.injector.instanceOf[GuidesAndTutorialsView]

    private lazy val tutorialsView = app.injector.instanceOf[TutorialsView]

    private lazy val technologiesView = app.injector.instanceOf[TechnologiesView]

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
      guidesAndTutorialsView,
      tutorialsView,
      technologiesView,
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
        developerFrontendConnector.fetchTermsOfUsePartial()(using *)
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
      verifyPageRendered(
        pageTitle("Reference guide"),
        breadcrumbs = List(homeBreadcrumb, Crumb("Guides and tutorials", controllers.routes.DocumentationController.guidesAndTutorialsPage().url)),
        sideNavLinkRendered = false,
        bodyContains = Seq(
          "<h2 class=\"govuk-heading-l\">Authentication and authorisation</h2>",
          "<h2 class=\"govuk-heading-l\">Behaviour and standards</h2>",
          "<h2 class=\"govuk-heading-l\">Platform constraints</h2>",
          "<h2 class=\"govuk-heading-l\">Data standards</h2>",
          "href=\"#redirect-uris\">Redirect URIs</a>",
          "href=\"#versioning\">Versioning</a>"
        )
      )(underTest.referenceGuidePage()(request))
    }

    "display the guides and tutorials page" in new Setup {
      verifyPageRendered(
        pageTitle("Guides and tutorials"),
        sideNavLinkRendered = false,
        bodyContains = Seq(
          "Guidance you need to build and support software using our APIs, from getting started to production use.",
          "Reference guide",
          "Use this guide to find technical reference information for our REST APIs.",
          "Tutorials",
          "Step-by-step guides to help you integrate and test your software.",
          "Our API technologies",
          "Learn about HMRC's API architecture, authorisation and supported standards.",
          "Terms of use",
          "Read the terms you must agree to before accessing HMRC's production environment."
        )
      )(underTest.guidesAndTutorialsPage()(request))
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

    "display the tutorials page" in new Setup {
      when(underTest.appConfig.sandboxApiBaseUrl)
        .thenReturn("https://sandbox.example.com")
      verifyPageRendered(
        pageTitle("Tutorials"),
        breadcrumbs = List(homeBreadcrumb, Crumb("Guides and tutorials", controllers.routes.DocumentationController.guidesAndTutorialsPage().url)),
        sideNavLinkRendered = false,
        bodyContains = Seq(
          "<h2 class=\"govuk-heading-m\" id=\"before-you-start\">Before you start</h2>",
          "<h2 class=\"govuk-heading-m\" id=\"open-endpoint\">Accessing an open endpoint</h2>",
          "<h2 class=\"govuk-heading-m\" id=\"app-restricted\">Accessing an application-restricted endpoint</h2>",
          "<h2 class=\"govuk-heading-m\" id=\"user-restricted\">Accessing a user-restricted endpoint</h2>",
          "/hello/world",
          "https://sandbox.example.com"
        )
      )(underTest.tutorialsPage()(request))
    }

    "display the Our API technologies page" in new Setup {
      when(underTest.appConfig.sandboxApiBaseUrl)
        .thenReturn("https://sandbox.example.com")
      verifyPageRendered(
        pageTitle("Our API technologies"),
        breadcrumbs = List(homeBreadcrumb, Crumb("Guides and tutorials", controllers.routes.DocumentationController.guidesAndTutorialsPage().url)),
        sideNavLinkRendered = false,
        bodyContains = Seq(
          "<h2 class=\"govuk-heading-m\" id=\"introduction\">Introduction</h2>",
          "<h2 class=\"govuk-heading-m\">Endpoint types and tokens</h2>",
          "<h2 class=\"govuk-heading-m\" id=\"credentials\">Credentials</h2>",
          "<h2 class=\"govuk-heading-m\" id=\"open-access\">Open access endpoints</h2>",
          "<h2 class=\"govuk-heading-m\" id=\"application-restricted\">Application-restricted endpoints</h2>",
          "<h2 class=\"govuk-heading-m\" id=\"user-restricted\">User-restricted endpoints</h2>",
          "<h2 class=\"govuk-heading-m\" id=\"two-step-verification\">2-step verification</h2>",
          "https://sandbox.example.com"
        )
      )(underTest.technologiesPage()(request))
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
