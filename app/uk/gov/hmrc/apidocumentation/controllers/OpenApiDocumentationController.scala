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

import java.io.FileNotFoundException
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future.successful
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, blocking}
import scala.jdk.CollectionConverters._

import io.swagger.v3.core.util.Yaml
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.core.extensions.SwaggerParserExtension
import io.swagger.v3.parser.core.models.ParseOptions
import io.swagger.v3.parser.exception.ReadContentException
import org.apache.pekko.actor.ActorSystem

import play.api.i18n.MessagesProvider
import play.api.mvc._
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import uk.gov.hmrc.apidocumentation.ErrorHandler
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.connectors.DownloadConnector
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.apidocumentation.services.{ApiDefinitionService, LoggedInUserService, NavigationService}
import uk.gov.hmrc.apidocumentation.util.ApplicationLogger
import uk.gov.hmrc.apidocumentation.views.html._

@Singleton
class OpenApiDocumentationController @Inject() (
    openApiViewRedoc: OpenApiViewRedoc,
    openApiPreviewRedoc: OpenApiPreviewRedoc,
    openApiPreviewView: OpenApiPreviewView,
    downloadConnector: DownloadConnector,
    mcc: MessagesControllerComponents,
    apiDefinitionService: ApiDefinitionService,
    loggedInUserService: LoggedInUserService,
    errorHandler: ErrorHandler,
    val navigationService: NavigationService,
    openAPIV3Parser: SwaggerParserExtension
  )(implicit val ec: ExecutionContext,
    appConfig: ApplicationConfig,
    system: ActorSystem
  ) extends FrontendController(mcc) with HeaderNavigation with HomeCrumb with ApplicationLogger {

  private val buildPageAttributes = (navLinks: Seq[NavLink]) =>
    PageAttributes(
      title = "OpenAPI Documentation Preview",
      breadcrumbs = Breadcrumbs(
        Crumb("Preview OpenAPI", routes.OpenApiDocumentationController.previewApiDocumentationPage().url),
        homeCrumb
      ),
      headerLinks = navLinks,
      sidebarLinks = navigationService.sidebarNavigation()
    )

  private def doRenderApiDocumentation(
      service: ServiceName,
      version: ApiVersionNbr,
      apiOption: Option[ExtendedApiDefinition]
    )(implicit request: Request[AnyContent],
      messagesProvider: MessagesProvider
    ): Future[Result] = {
    def renderDocumentationPage(apiName: String): Future[Result] = {
      successful(Ok(openApiViewRedoc(service, version, apiName)))
    }

    def renderNotFoundPage = errorHandler.notFoundTemplate.map(NotFound(_))
    def badRequestPage     = errorHandler.badRequestTemplate.map(BadRequest(_))

    def findVersion(apiOption: Option[ExtendedApiDefinition]) =
      for {
        api        <- apiOption
        apiVersion <- api.versions.find(v => v.version == version)
        visibility <- VersionVisibility(apiVersion)
      } yield (api, apiVersion, visibility)

    findVersion(apiOption) match {
      case Some((_, selectedVersion, VersionVisibility(_, _, true, _))) if selectedVersion.status == ApiStatus.RETIRED => badRequestPage
      case Some((api, _, VersionVisibility(_, _, true, _)))                                                            => renderDocumentationPage(api.name)
      case Some((api, _, VersionVisibility(ApiAccessType.PRIVATE, _, false, true)))                                    => renderDocumentationPage(api.name) // TODO - makes no sense for oas/page
      case Some((_, _, VersionVisibility(ApiAccessType.PRIVATE, false, _, _)))                                         => badRequestPage
      case _                                                                                                           => renderNotFoundPage
    }

  }

  private def extractDeveloperIdentifier(f: Future[Option[Developer]]): Future[Option[DeveloperIdentifier]] = {
    f.map(o =>
      o.map(d => UuidIdentifier(d.userId))
    )
  }

  def renderApiDocumentation(service: ServiceName, version: ApiVersionNbr) =
    headerNavigation { implicit request => navLinks =>
      (for {
        userId           <- extractDeveloperIdentifier(loggedInUserService.fetchLoggedInUser())
        api              <- apiDefinitionService.fetchExtendedDefinition(service, userId)
        apiDocumentation <- doRenderApiDocumentation(service, version, api)
      } yield apiDocumentation) recoverWith {
        case e: NotFoundException =>
          logger.info(s"Upstream request not found: ${e.getMessage}")
          errorHandler.notFoundTemplate.map(NotFound(_))
        case e: Throwable         =>
          logger.error("Could not load API Documentation service", e)
          errorHandler.internalServerErrorTemplate.map(InternalServerError(_))
      }
    }

  def fetchOas(service: ServiceName, version: ApiVersionNbr) = Action.async { implicit request =>
    downloadConnector.fetch(service, version, "application.yaml")
      .flatMap {
        case Some(result) => successful(result.withHeaders(HeaderNames.CONTENT_DISPOSITION -> "attachment; filename=\"application.yaml\""))
        case None         => errorHandler.notFoundTemplate.map(NotFound(_))
      }
  }

  def fetchOasResolved(service: ServiceName, version: ApiVersionNbr) = Action.async { implicit request =>
    def handleSuccess(openApi: OpenAPI): Result =
      Ok(Yaml.pretty.writeValueAsString(openApi)).withHeaders(HeaderNames.CONTENT_DISPOSITION -> "attachment; filename=\"application.yaml\"")
    val handleFailure: Result                   = NotFound

    val parseOptions = new ParseOptions()
    parseOptions.setResolve(true)
    parseOptions.setResolveFully(true)

    val emptyAuthList   = java.util.Collections.emptyList[io.swagger.v3.parser.core.models.AuthorizationValue]()
    val fetchUsingHttps = appConfig.oasFetchResolvedUsingHttps
    val oasFileLocation = routes.OpenApiDocumentationController.fetchOas(service, version).absoluteURL(fetchUsingHttps)

    val futureParsing = Future {
      blocking {
        try {
          val parserResult = openAPIV3Parser.readLocation(oasFileLocation, emptyAuthList, parseOptions)

          Option(parserResult.getOpenAPI()) match {
            // The OAS specification has been found and parsed by Swagger - return the fully resolved specification to the caller.
            case Some(openApi) => {
              logger.info("Successfully parsed the OAS specification.")
              logger.info(Option(parserResult.getMessages()).map(_.asScala.mkString).getOrElse(""))
              handleSuccess(openApi)
            }
            // The OAS specification has been found but there was a parsing problem - return an empty specification to the caller.
            case None          => {
              logger.info("There was a problem parsing the OAS specification.")
              logger.info(parserResult.getMessages().asScala.mkString)
              handleFailure
            }
          }
        } catch {
          // The OAS specification has not been found.
          case e: FileNotFoundException => {
            logger.info("The OAS specification could not be found.")
            handleFailure
          }
          case e: ReadContentException  => {
            logger.info("The OAS specification could not be found.")
            handleFailure
          }
        }
      }
    }

    val futureTimer: Future[Result] = org.apache.pekko.pattern.after(FiniteDuration(appConfig.oasFetchResolvedMaxDuration, TimeUnit.MILLISECONDS), using = system.scheduler)(
      Future.failed(new IllegalStateException("Exceeded OAS parse time"))
    )

    Future.firstCompletedOf(List(futureParsing, futureTimer))
  }

  def previewApiDocumentationPage(): Action[AnyContent] = headerNavigation { implicit request => navLinks =>
    if (appConfig.openApiPreviewEnabled) {
      val pageAttributes = buildPageAttributes(navLinks)

      successful(Ok(openApiPreviewView(pageAttributes)))
    } else {
      errorHandler.notFoundTemplate.map(NotFound(_))
    }
  }

  def previewApiDocumentationAction(url: Option[String]) = headerNavigation { implicit request => navLinks =>
    if (appConfig.openApiPreviewEnabled) {
      val pageAttributes = buildPageAttributes(navLinks)

      url match {
        case None           => successful(Ok(openApiPreviewView(pageAttributes)))
        case Some(location) => successful(Ok(openApiPreviewRedoc(location)))
      }
    } else {
      errorHandler.notFoundTemplate.map(NotFound(_))
    }
  }
}
