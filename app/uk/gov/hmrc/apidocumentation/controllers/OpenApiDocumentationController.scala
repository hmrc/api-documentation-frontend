/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.apidocumentation.views.html._
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import scala.concurrent.Future.successful
import uk.gov.hmrc.apidocumentation.connectors.DownloadConnector

@Singleton
class OpenApiDocumentationController @Inject()(
  openApiViewRedoc: OpenApiViewRedoc,
  downloadConnector:DownloadConnector,
  mcc: MessagesControllerComponents
)(implicit val ec: ExecutionContext)
    extends FrontendController(mcc) {

  def renderApiDocumentationUsingRedoc(service: String, version: String, cacheBuster: Option[Boolean]) = Action.async { _ =>
    successful(Ok(openApiViewRedoc(service, version)))
  }

  def fetchOas(service: String, version: String, cacheBuster: Option[Boolean]) = Action.async { _ =>
    downloadConnector.fetch(service, version, "application.yaml")
  }
}
