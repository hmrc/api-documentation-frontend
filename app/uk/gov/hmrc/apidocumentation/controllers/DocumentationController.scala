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

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import play.api.mvc._
import uk.gov.hmrc.apidocumentation
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.apidocumentation.services._
import uk.gov.hmrc.apidocumentation.util.ApplicationLogger
import uk.gov.hmrc.apidocumentation.views.html._

@Singleton
class DocumentationController @Inject() (
    val navigationService: NavigationService,
    partialsService: PartialsService,
    mcc: MessagesControllerComponents,
    indexView: IndexView,
    retiredVersionJumpView: RetiredVersionJumpView,
    tutorialsView: TutorialsView,
    credentialsView: CredentialsView,
    developmentPracticesView: DevelopmentPracticesView,
    mtdIntroductionView: MtdIntroductionView,
    namingGuidelinesView: NamingGuidelinesView,
    referenceView: ReferenceView,
    termsOfUseView: TermsOfUseView,
    termsOfUseWhatYouCanExpectView: TermsOfUseWhatYouCanExpectView,
    termsOfUseNotMeetingView: TermsOfUseNotMeetingView,
    usingTheHubView: UsingTheHubView
  )(implicit val appConfig: ApplicationConfig,
    val ec: ExecutionContext
  ) extends FrontendController(mcc)
    with HeaderNavigation
    with PageAttributesHelper
    with HomeCrumb
    with ApplicationLogger {

  def indexPage(): Action[AnyContent] = headerNavigation {
    implicit request => navLinks =>
      Future.successful(Ok(indexView("Home", navLinks)))
  }

  def tutorialsPage(): Action[AnyContent] = headerNavigation {
    implicit request => navLinks =>
      Future.successful(
        Ok(
          tutorialsView(
            pageAttributes(
              "Tutorials",
              routes.DocumentationController.tutorialsPage().url,
              navLinks
            )
          )
        )
      )
  }

  def termsOfUsePage(): Action[AnyContent] = headerNavigation {
    implicit request => navLinks =>
      partialsService.termsOfUsePartial() map { termsOfUsePartial =>
        Ok(
          termsOfUseView(
            pageAttributes(
              "Terms Of Use",
              routes.DocumentationController.termsOfUsePage().url,
              navLinks
            ),
            termsOfUsePartial
          )
        )
      }
  }

  def termsOfUseWhatYouCanExpectPage(): Action[AnyContent] = headerNavigation {
    implicit request => navLinks =>
      Future.successful(
        Ok(
          termsOfUseWhatYouCanExpectView(
            pageAttributes("What you can expect from us", routes.DocumentationController.termsOfUseWhatYouCanExpectPage().url, navLinks)
          )
        )
      )
  }

  def termsOfUseNotMeetingPage(): Action[AnyContent] = headerNavigation {
    implicit request => navLinks =>
      Future.successful(
        Ok(
          termsOfUseNotMeetingView(
            pageAttributes("Not meeting the terms of use", routes.DocumentationController.termsOfUseNotMeetingPage().url, navLinks)
          )
        )
      )
  }

  def usingTheHubPage(): Action[AnyContent] = headerNavigation {
    implicit request => navLinks =>
      Future.successful(
        Ok(
          usingTheHubView(
            pageAttributes(
              "Using the Developer Hub",
              routes.DocumentationController.usingTheHubPage().url,
              navLinks
            )
          )
        )
      )
  }

  def mtdIntroductionPage(): Action[AnyContent] = headerNavigation {
    implicit request => navLinks =>
      val introPageUrl =
        routes.DocumentationController.mtdIntroductionPage().url
      Future.successful(
        Ok(
          mtdIntroductionView(
            pageAttributes("Making Tax Digital guides", introPageUrl, navLinks)
          )
        )
      )
  }

  def mtdIncomeTaxServiceGuidePage(): Action[AnyContent] = headerNavigation {
    _ => navLinks =>
      Future.successful(
        MovedPermanently("/guides/income-tax-mtd-end-to-end-service-guide/")
      )
  }

  def referenceGuidePage(): Action[AnyContent] = headerNavigation {
    implicit request => navLinks =>
      Future.successful(
        Ok(
          referenceView(
            pageAttributes(
              "Reference guide",
              routes.DocumentationController.referenceGuidePage().url,
              navLinks
            )
          )
        )
      )
  }

  def developmentPracticesPage(): Action[AnyContent] = headerNavigation {
    implicit request => navLinks =>
      Future.successful(
        Ok(
          developmentPracticesView(
            pageAttributes(
              "Development practices",
              routes.DocumentationController.developmentPracticesPage().url,
              navLinks
            )
          )
        )
      )
  }

  def nameGuidelinesRedirect(): Action[AnyContent] = headerNavigation {
    implicit request => navLinks =>
      Future.successful(
        Redirect(
          routes.DocumentationController.nameGuidelinesPage().url
        )
      )
  }

  def nameGuidelinesPage(): Action[AnyContent] = headerNavigation {
    implicit request => navLinks =>
      Future.successful(
        Ok(
          namingGuidelinesView(
            pageAttributes(
              "Application naming guidelines",
              routes.DocumentationController.nameGuidelinesPage().url,
              navLinks
            )
          )
        )
      )
  }
}

trait HomeCrumb {

  lazy val homeCrumb =
    Crumb("Home", routes.DocumentationController.indexPage().url)
}

trait PageAttributesHelper {
  self: FrontendController with HomeCrumb =>

  def navigationService: NavigationService

  def pageAttributes(title: String, url: String, headerNavLinks: Seq[NavLink], customBreadcrumbs: Option[Breadcrumbs] = None) = {
    val breadcrumbs =
      customBreadcrumbs.getOrElse(Breadcrumbs(Crumb(title, url), homeCrumb))
    apidocumentation.models.PageAttributes(
      title,
      breadcrumbs,
      headerNavLinks,
      navigationService.sidebarNavigation()
    )
  }
}

trait HeaderNavigation {
  self: FrontendController with ApplicationLogger =>

  def navigationService: NavigationService

  def headerNavigation(
      f: MessagesRequest[AnyContent] => Seq[NavLink] => Future[Result]
    )(implicit ec: ExecutionContext
    ): Action[AnyContent] = {
    Action.async { implicit request =>
      // We use a non-standard cookie which doesn't get propagated in the header carrier
      val newHc = request.headers.get(COOKIE).fold(hc) { cookie =>
        hc.withExtraHeaders(COOKIE -> cookie)
      }
      navigationService.headerNavigation()(newHc) flatMap { navLinks =>
        f(request)(navLinks)
      } recoverWith {
        case ex =>
          logger.error(
            "User navigation links can not be rendered due to service call failure",
            ex
          )
          f(request)(Seq.empty)
      }
    }
  }
}
