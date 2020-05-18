/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.apidocumentation.ErrorHandler
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.models.{APIAccessType, Developer, ExtendedAPIDefinition, VersionVisibility}
import uk.gov.hmrc.apidocumentation.services.{ApiDefinitionService, DocumentationService, DownloadService, LoggedInUserProvider}
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DownloadController @Inject()(documentationService: DocumentationService,
                                   apiDefinitionService: ApiDefinitionService,
                                   downloadService: DownloadService,
                                   loggedInUserProvider: LoggedInUserProvider,
                                   errorHandler: ErrorHandler,
                                   val appConfig: ApplicationConfig,
                                   cc: MessagesControllerComponents)
                                  (implicit val ec: ExecutionContext)
                                  extends FrontendController(cc) {

  def downloadResource(service: String, version: String, resource: String) = Action.async { implicit request =>

    (for {
      email <- extractEmail(loggedInUserProvider.fetchLoggedInUser())
      api <- apiDefinitionService.fetchExtendedDefinition(service, email)
      validResource = validateResource(resource)
      result <- fetchResourceForApi(api, version, validResource)
    } yield {
      result
    }) recover {
      case e: NotFoundException =>
        Logger.info(s"Resource not found: ${e.getMessage}")
        NotFound(errorHandler.notFoundTemplate)
      case e: Throwable =>
        Logger.error("Could not load resource", e)
        InternalServerError(errorHandler.internalServerErrorTemplate)
    }
  }

  private def fetchResourceForApi(apiOption: Option[ExtendedAPIDefinition], version: String, validResource: String)
                                 (implicit request: Request[_]): Future[Result] = {
    def findVersion(apiOption: Option[ExtendedAPIDefinition]) =
      for {
        api <- apiOption
        apiVersion <- api.versions.find(v => v.version == version)
        visibility <- apiVersion.visibility
      } yield (api, apiVersion, visibility)

    def renderNotFoundPage =
      Future.successful(NotFound(errorHandler.notFoundTemplate))

    def redirectToLoginPage(service: String) =
      Future.successful(Redirect("/developer/login").withSession(
        "access_uri" -> routes.ApiDocumentationController.renderApiDocumentation(service, version, None).url))

    findVersion(apiOption) match {
      case Some((api, _, VersionVisibility(APIAccessType.PRIVATE, false, _, _))) =>
        redirectToLoginPage(api.serviceName)

      case Some((api, selectedVersion, VersionVisibility(_, _, true, _))) =>
        downloadService.fetchResource(api.serviceName, selectedVersion.version, validResource)

      case _ =>
        renderNotFoundPage
    }
  }

  private def validateResource(resourceName: String) = {
    if (resourceName.contains("..")) throw new IllegalArgumentException(s"Illegal resource name: $resourceName")
    resourceName
  }

  private def extractEmail(fut: Future[Option[Developer]]): Future[Option[String]] = {
    fut.map(opt => opt.map(dev => dev.email))
  }
}

