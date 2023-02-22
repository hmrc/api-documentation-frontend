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

import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.views.html._

@Singleton
class HelpPagesController @Inject() (
    appConfig: ApplicationConfig,
    mcc: MessagesControllerComponents,
    cookiesView: CookiesView,
    privacyView: PrivacyView,
    termsAndConditionsView: TermsAndConditionsView
  )(implicit applicationConfig: ApplicationConfig
  ) extends FrontendController(mcc) {

  def cookiesPage(): Action[AnyContent] = Action { implicit request =>
    Redirect(appConfig.cookieSettingsUrl)
  }

  def cookiesDetailsPage(): Action[AnyContent] = Action { implicit request =>
    Ok(cookiesView())
  }

  def privacyPolicyPage(): Action[AnyContent] = Action { implicit request =>
    Ok(privacyView())
  }

  def termsAndConditionsPage(): Action[AnyContent] = Action { implicit request =>
    Ok(termsAndConditionsView())
  }

}
