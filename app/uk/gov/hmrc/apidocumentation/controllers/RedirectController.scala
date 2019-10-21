/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

class RedirectController extends FrontendController {
  def redirectToDocumentationIndexPage(): Action[AnyContent] = {
    val redirectTo = routes.DocumentationController.apiIndexPage(None, None, None).url
    Action.async { implicit request => Future.successful(MovedPermanently(redirectTo)) }
  }

  def redirectToApiDocumentationPage(service: String, version: String, endpoint: String): Action[AnyContent] = {
    val redirectTo = routes.DocumentationController.renderApiDocumentation(service, version, None).url
    Action.async { implicit request => Future.successful(MovedPermanently(redirectTo)) }
  }
}
