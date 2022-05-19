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
import scala.concurrent.ExecutionContext
import scala.concurrent.Future.successful
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.apidocumentation.views.html._
import uk.gov.hmrc.apidocumentation.connectors.DownloadConnector
import uk.gov.hmrc.apidocumentation.util.ApplicationLogger
import uk.gov.hmrc.apidocumentation.services.NavigationService
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.ErrorHandler
import uk.gov.hmrc.apidocumentation.services.LoggedInUserService
import uk.gov.hmrc.apidocumentation.services.ApiDefinitionService
import uk.gov.hmrc.apidocumentation.views.html.openapispec.ParentPageOuter

@Singleton
class OpenApiDocumentationController @Inject()(
  openApiViewRedoc: OpenApiViewRedoc,
  openApiViewRapidoc: OpenApiViewRapiDoc,
  openApiPreviewRedoc: OpenApiPreviewRedoc,
  openApiPreviewRapidoc: OpenApiPreviewRapiDoc,
  openApiViewRapidocRead: OpenApiViewRapiDocRead,
  openApiViewSwagger: OpenApiViewSwagger,
  openApiPreviewView: OpenApiPreviewView,
  parentPage: ParentPageOuter,
  retiredVersionJumpView: RetiredVersionJumpView,
  downloadConnector:DownloadConnector,
  mcc: MessagesControllerComponents,
  apiDefinitionService: ApiDefinitionService,
  loggedInUserService: LoggedInUserService,
  errorHandler: ErrorHandler,
  val navigationService: NavigationService
)(implicit val ec: ExecutionContext, appConfig: ApplicationConfig)
    extends FrontendController(mcc) with HeaderNavigation with HomeCrumb with ApplicationLogger {

  def renderApiDocumentationUsingRedoc(service: String, version: String) = Action.async { _ =>
    successful(Ok(openApiViewRedoc(service, version)))
  }

  def renderApiDocumentationUsingRapidoc(service: String, version: String) = Action.async { _ =>
    successful(Ok(openApiViewRapidoc(service, version)))
  }

  def renderApiDocumentationUsingRapidocRead(service: String, version: String) = Action.async { _ =>
    successful(Ok(openApiViewRapidocRead(service, version)))
  }

  def renderApiDocumentationUsingSwagger(service: String, version: String) = Action.async { _ =>
    successful(Ok(openApiViewSwagger(service, version)))
  }

  def fetchOas(service: String, version: String) = Action.async { _ =>
    downloadConnector.fetch(service, version, "application.yaml")
  }
  
  def previewApiDocumentationPage(): Action[AnyContent] = headerNavigation { implicit request =>
    navLinks =>
      if (appConfig.openApiPreviewEnabled) {
        val pageAttributes = PageAttributes(title = "OpenAPI Documentation Preview",
          breadcrumbs = Breadcrumbs(
            Crumb("Preview OpenAPI", routes.OpenApiDocumentationController.previewApiDocumentationPage.url),
            homeCrumb),
          headerLinks = navLinks,
          sidebarLinks = navigationService.sidebarNavigation())

        successful(Ok(openApiPreviewView(pageAttributes)))
      } else {
        successful(NotFound(errorHandler.notFoundTemplate))
      }
  }

  def previewApiDocumentationAction(url: Option[String]) = headerNavigation { implicit request =>
    navLinks =>
      if (appConfig.openApiPreviewEnabled) {
        val pageAttributes = PageAttributes(title = "OpenAPI Documentation Preview",
          breadcrumbs = Breadcrumbs(
            Crumb("Preview OpenAPI", routes.OpenApiDocumentationController.previewApiDocumentationPage.url),
            homeCrumb),
          headerLinks = navLinks,
          sidebarLinks = navigationService.sidebarNavigation())

        url match {
          case None           => successful(Ok(openApiPreviewView(pageAttributes)))
          case Some(location) => successful(Ok(openApiPreviewRedoc(location)))
        }
      } else {
        successful(NotFound(errorHandler.notFoundTemplate))
      }
  }
}
