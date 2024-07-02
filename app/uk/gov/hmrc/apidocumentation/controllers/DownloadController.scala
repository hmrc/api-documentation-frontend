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
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.{ApiAccessType, ExtendedApiDefinition, ServiceName}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApiVersionNbr
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import uk.gov.hmrc.apidocumentation.ErrorHandler
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.connectors.DownloadConnector
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.apidocumentation.services.{ApiDefinitionService, LoggedInUserService}
import uk.gov.hmrc.apidocumentation.util.ApplicationLogger

@Singleton
class DownloadController @Inject() (
    apiDefinitionService: ApiDefinitionService,
    downloadConnector: DownloadConnector,
    loggedInUserService: LoggedInUserService,
    errorHandler: ErrorHandler,
    val appConfig: ApplicationConfig,
    cc: MessagesControllerComponents
  )(implicit val ec: ExecutionContext
  ) extends FrontendController(cc) with ApplicationLogger {

  def downloadResource(service: ServiceName, version: ApiVersionNbr, resource: String, useV2: Option[Boolean]) = Action.async { implicit request =>
    (for {
      userId       <- extractDeveloperIdentifier(loggedInUserService.fetchLoggedInUser())
      api          <- apiDefinitionService.fetchExtendedDefinition(service, userId)
      validResource = validateResource(resource)
      result       <- fetchResourceForApi(api, version, validResource, useV2)
    } yield {
      result
    }) recover {
      case e: NotFoundException =>
        logger.info(s"Resource not found: ${e.getMessage}")
        NotFound(errorHandler.notFoundTemplate)
      case e: Throwable         =>
        logger.error("Could not load resource", e)
        InternalServerError(errorHandler.internalServerErrorTemplate)
    }
  }

  private def fetchResourceForApi(apiOption: Option[ExtendedApiDefinition], version: ApiVersionNbr, validResource: String, useV2: Option[Boolean])(implicit request: Request[_])
      : Future[Result] = {
    def findVersion(apiOption: Option[ExtendedApiDefinition]) =
      for {
        api        <- apiOption
        apiVersion <- api.versions.find(v => version == v.version)
        visibility <- VersionVisibility(apiVersion)
      } yield (api, apiVersion, visibility)

    def renderNotFoundPage =
      NotFound(errorHandler.notFoundTemplate)

    def redirectToLoginPage(service: ServiceName) =
      Future.successful(Redirect("/developer/login").withSession(
        "access_uri" -> routes.ApiDocumentationController.renderApiDocumentation(service, version, None, useV2).url
      ))

    findVersion(apiOption) match {
      case Some((api, _, VersionVisibility(ApiAccessType.PRIVATE, false, _, _))) =>
        redirectToLoginPage(api.serviceName)

      case Some((api, selectedVersion, VersionVisibility(_, _, true, _))) =>
        downloadConnector.fetch(api.serviceName, selectedVersion.version, validResource)
          .map(_.getOrElse(renderNotFoundPage))

      case _ =>
        Future.successful(renderNotFoundPage)
    }
  }

  private def validateResource(resourceName: String) = {
    if (resourceName.contains("..")) throw new IllegalArgumentException(s"Illegal resource name: $resourceName")
    resourceName
  }

  private def extractDeveloperIdentifier(f: Future[Option[Developer]]): Future[Option[DeveloperIdentifier]] = {
    f.map(o =>
      o.map(d => UuidIdentifier(d.userId))
    )
  }
}
