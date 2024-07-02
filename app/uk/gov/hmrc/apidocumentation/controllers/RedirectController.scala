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
import scala.concurrent.Future

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ServiceName
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApiVersionNbr
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

@Singleton
class RedirectController @Inject() (cc: MessagesControllerComponents)
    extends FrontendController(cc) {

  def `redirectToDocumentationIndexPage`(useV2: Option[Boolean]): Action[AnyContent] = {

    val redirectTo = if (useV2.getOrElse(false)) {
      uk.gov.hmrc.apidocumentation.v2.controllers.routes.FilteredDocumentationIndexController.apiListIndexPage(List.empty, List.empty).url
    } else routes.ApiDocumentationController.apiIndexPage(None, None, None).url

    Action.async { _ =>
      Future.successful(MovedPermanently(redirectTo))
    }
  }

  def redirectToApiDocumentationPage(service: ServiceName, version: ApiVersionNbr, endpoint: String, useV2: Option[Boolean]): Action[AnyContent] = {
    val redirectTo = routes.ApiDocumentationController
      .renderApiDocumentation(service, version, None, useV2)
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
