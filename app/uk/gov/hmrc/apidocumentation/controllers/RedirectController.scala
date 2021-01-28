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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

@Singleton
class RedirectController @Inject()(cc: MessagesControllerComponents)
    extends FrontendController(cc) {
  def redirectToDocumentationIndexPage(): Action[AnyContent] = {
    val redirectTo =
      routes.ApiDocumentationController.apiIndexPage(None, None, None).url
    Action.async { _ =>
      Future.successful(MovedPermanently(redirectTo))
    }
  }

  def redirectToApiDocumentationPage(service: String,
                                     version: String,
                                     endpoint: String): Action[AnyContent] = {
    val redirectTo = routes.ApiDocumentationController
      .renderApiDocumentation(service, version, None)
      .url
    Action.async { _ =>
      Future.successful(MovedPermanently(redirectTo))
    }
  }

  def redirectToFraudPreventionGuide(): Action[AnyContent] = {
    val redirectTo = "/guides/fraud-prevention"
    Action.async { _ =>
      Future.successful(MovedPermanently(redirectTo))
    }
  }
}
