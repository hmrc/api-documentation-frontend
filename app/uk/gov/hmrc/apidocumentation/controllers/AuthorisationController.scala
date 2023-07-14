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
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.models.Crumb
import uk.gov.hmrc.apidocumentation.services._
import uk.gov.hmrc.apidocumentation.util.ApplicationLogger
import uk.gov.hmrc.apidocumentation.views.html._

@Singleton
class AuthorisationController @Inject() (
    mcc: MessagesControllerComponents,
    val navigationService: NavigationService,
    authorisationView: AuthorisationView,
    authorisation2SVView: Authorisation2SVView,
    authorisationAppRestrictedEndpointsView: AuthorisationAppRestrictedEndpointsView,
    authorisationOpenAccessEndpointsView: AuthorisationOpenAccessEndpointsView,
    authorisationUserRestrictedEndpointsView: AuthorisationUserRestrictedEndpointsView,
    credentialsView: CredentialsView
  )(implicit ec: ExecutionContext,
    applicationConfig: ApplicationConfig
  ) extends FrontendController(mcc) with HeaderNavigation with PageAttributesHelper with BaseCrumbs
    with ApplicationLogger {

  private lazy val authCrumb = Crumb("Authorisation", routes.AuthorisationController.authorisationPage().url)

  def authorisationPage(): Action[AnyContent] = headerNavigation { implicit request => navLinks =>
    Future.successful(Ok(authorisationView(pageAttributes("Authorisation", navLinks, baseCrumbs))))
  }

  def authorisation2SVPage(): Action[AnyContent] = headerNavigation { implicit request => navLinks =>
    Future.successful(Ok(authorisation2SVView(pageAttributes("2-step verification", navLinks, basePlus(authCrumb)))))
  }

  def authorisationCredentialsPage(): Action[AnyContent] = headerNavigation { implicit request => navLinks =>
    Future.successful(Ok(credentialsView(pageAttributes("Credentials", navLinks, basePlus(authCrumb)))))
  }

  def authorisationOpenAccessEndpointsPage(): Action[AnyContent] = headerNavigation { implicit request => navLinks =>
    Future.successful(Ok(authorisationOpenAccessEndpointsView(pageAttributes("Open access endpoints", navLinks, basePlus(authCrumb)))))
  }

  def authorisationAppRestrictedEndpointsPage(): Action[AnyContent] = headerNavigation { implicit request => navLinks =>
    Future.successful(Ok(authorisationAppRestrictedEndpointsView(pageAttributes("Application-restricted endpoints", navLinks, basePlus(authCrumb)))))
  }

  def authorisationUserRestrictedEndpointsPage(): Action[AnyContent] = headerNavigation { implicit request => navLinks =>
    Future.successful(Ok(authorisationUserRestrictedEndpointsView(pageAttributes("User-restricted endpoints", navLinks, basePlus(authCrumb)))))
  }
}
