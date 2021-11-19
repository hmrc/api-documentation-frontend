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

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.models.{Breadcrumbs, Crumb}
import uk.gov.hmrc.apidocumentation.services._
import uk.gov.hmrc.apidocumentation.views.html._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.apidocumentation.util.ApplicationLogger

@Singleton
class AuthorisationController @Inject()(   
                                        mcc: MessagesControllerComponents,
                                        val navigationService: NavigationService,
                                        authorisationView: AuthorisationView,
                                        authorisation2SVView: Authorisation2SVView,
                                        authorisationAppRestrictedEndpointsView: AuthorisationAppRestrictedEndpointsView,
                                        authorisationOpenAccessEndpointsView: AuthorisationOpenAccessEndpointsView,
                                        authorisationUserRestrictedEndpointsView: AuthorisationUserRestrictedEndpointsView,
                                        credentialsView: CredentialsView
                                        )
                                       (implicit ec: ExecutionContext, applicationConfig: ApplicationConfig)
  extends FrontendController(mcc) with HeaderNavigation with PageAttributesHelper with HomeCrumb 
    with ApplicationLogger {

  private lazy val authCrumb = Crumb("Authorisation", routes.AuthorisationController.authorisationPage().url)

  def authorisationPage(): Action[AnyContent] = headerNavigation { implicit request =>
    navLinks =>
      Future.successful(Ok(authorisationView(pageAttributes("Authorisation", routes.AuthorisationController.authorisationPage().url, navLinks))))
  }

  def authorisation2SVPage(): Action[AnyContent] = headerNavigation { implicit request =>
    navLinks =>
      val breadcrumbs = Breadcrumbs(
        Crumb("2-step verification", routes.AuthorisationController.authorisation2SVPage().url),
        authCrumb,
        homeCrumb
      )
      Future.successful(Ok(authorisation2SVView(pageAttributes(
        "2-step verification",
        routes.AuthorisationController.authorisation2SVPage().url,
        navLinks,
        Some(breadcrumbs)))))
  }

  def authorisationCredentialsPage(): Action[AnyContent] = headerNavigation { implicit request =>
    navLinks =>
      val breadcrumbs = Breadcrumbs(
        Crumb("Credentials", routes.AuthorisationController.authorisationCredentialsPage().url),
        authCrumb,
        homeCrumb
      )
      Future.successful(Ok(credentialsView(pageAttributes(
        "Credentials",
        routes.AuthorisationController.authorisationCredentialsPage().url,
        navLinks,
        Some(breadcrumbs)))))
  }

  def authorisationOpenAccessEndpointsPage(): Action[AnyContent] = headerNavigation { implicit request =>
    navLinks =>
      val breadcrumbs = Breadcrumbs(
        Crumb("Open access endpoints", routes.AuthorisationController.authorisationOpenAccessEndpointsPage().url),
        authCrumb,
        homeCrumb
      )
      Future.successful(Ok(authorisationOpenAccessEndpointsView(pageAttributes(
        "Open access endpoints",
        routes.AuthorisationController.authorisationOpenAccessEndpointsPage().url,
        navLinks,
        Some(breadcrumbs)))))
  }

  def authorisationAppRestrictedEndpointsPage(): Action[AnyContent] = headerNavigation { implicit request =>
    navLinks =>
      val breadcrumbs = Breadcrumbs(
        Crumb("Application-restricted endpoints", routes.AuthorisationController.authorisationAppRestrictedEndpointsPage().url),
        authCrumb,
        homeCrumb
      )
      Future.successful(Ok(authorisationAppRestrictedEndpointsView(pageAttributes(
        "Application-restricted endpoints",
        routes.AuthorisationController.authorisationAppRestrictedEndpointsPage().url,
        navLinks,
        Some(breadcrumbs)))))
  }

  def authorisationUserRestrictedEndpointsPage(): Action[AnyContent] = headerNavigation { implicit request =>
    navLinks =>
      val breadcrumbs = Breadcrumbs(
        Crumb("User-restricted endpoints", routes.AuthorisationController.authorisationUserRestrictedEndpointsPage().url),
        authCrumb,
        homeCrumb
      )
      Future.successful(Ok(authorisationUserRestrictedEndpointsView(pageAttributes(
        "User-restricted endpoints",
        routes.AuthorisationController.authorisationUserRestrictedEndpointsPage().url,
        navLinks,
        Some(breadcrumbs)))))
  }
}