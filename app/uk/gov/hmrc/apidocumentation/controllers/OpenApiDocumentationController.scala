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
import scala.concurrent.Future
import scala.concurrent.Future.successful
import play.api.i18n.MessagesProvider
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.apidocumentation.views.html._
import uk.gov.hmrc.apidocumentation.connectors.DownloadConnector
import uk.gov.hmrc.apidocumentation.util.ApplicationLogger
import uk.gov.hmrc.apidocumentation.services.NavigationService
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.ErrorHandler
import uk.gov.hmrc.apidocumentation.views.html.openapispec.ParentPageView
import uk.gov.hmrc.apidocumentation.models.apispecification.DocumentationItem
import uk.gov.hmrc.apidocumentation.services.LoggedInUserService
import uk.gov.hmrc.apidocumentation.services.ApiDefinitionService
import uk.gov.hmrc.http.NotFoundException
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
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

  private lazy val cacheControlHeaders = "cache-control" -> "no-cache,no-store,max-age=0"
  private lazy val apiDocCrumb = Crumb("API Documentation", routes.ApiDocumentationController.apiIndexPage(None, None, None).url)


  private def makeBreadcrumbName(api: ExtendedAPIDefinition, selectedVersion: ExtendedAPIVersion) = {
    val suffix = if (api.name.endsWith("API")) "" else " API"
    s"${api.name}$suffix v${selectedVersion.version} (${selectedVersion.displayedStatus})"
  }

  private def doRenderApiDocumentation(service: String, version: String, cacheBuster: Boolean, apiOption: Option[ExtendedAPIDefinition],
                                       navLinks: Seq[NavLink], developerId: Option[DeveloperIdentifier])(implicit request: Request[AnyContent], messagesProvider: MessagesProvider): Future[Result] = {
    def makePageAttributes(apiDefinition: ExtendedAPIDefinition, selectedVersion: ExtendedAPIVersion, sidebarLinks: Seq[SidebarLink]): PageAttributes = {
      val breadcrumbs = Breadcrumbs(
        Crumb(
          makeBreadcrumbName(apiDefinition, selectedVersion),
          routes.ApiDocumentationController.renderApiDocumentation(service, selectedVersion.version, None).url),
        apiDocCrumb,
        homeCrumb)

      PageAttributes(apiDefinition.name, breadcrumbs, navLinks, sidebarLinks)
    }

    def findVersion(apiOption: Option[ExtendedAPIDefinition]) =
      for {
        api <- apiOption
        apiVersion <- api.versions.find(v => v.version == version)
        visibility <- apiVersion.visibility
      } yield (api, apiVersion, visibility)

    def renderNotFoundPage = Future.successful(NotFound(errorHandler.notFoundTemplate))

    def redirectToLoginPage = {
      logger.info(s"redirectToLogin - access_uri ${routes.ApiDocumentationController.renderApiDocumentation(service, version, None).url}")
      Future.successful(Redirect("/developer/login").withSession(
        "access_uri" -> routes.OpenApiDocumentationController.renderParentPage(service).url,
        "ts" -> DateTime.now(DateTimeZone.UTC).getMillis.toString)
      )
    }

    def renderRetiredVersionJumpPage(api: ExtendedAPIDefinition, selectedVersion: ExtendedAPIVersion)(implicit request: Request[AnyContent], messagesProvider: MessagesProvider) = {
      val apiDefinition = api.userAccessibleApiDefinition

      Future.successful(Ok(retiredVersionJumpView(
        makePageAttributes(apiDefinition, selectedVersion, navigationService.sidebarNavigation()), apiDefinition)))
    }

    def renderDocumentationPage(api: ExtendedAPIDefinition, selectedVersion: ExtendedAPIVersion, overviewOnly: Boolean = false)(implicit request: Request[AnyContent], messagesProvider: MessagesProvider): Future[Result] = {
      val markdownBlocks = List(DocumentationItem("Overview", "Blah blah"), DocumentationItem("Errors", "Blah blah"))
      val attrs = makePageAttributes(api, selectedVersion, navigationService.openApiSidebarNavigation(service, selectedVersion, markdownBlocks))
      successful(
        Ok(
          parentPage(attrs, markdownBlocks, api.name, api, selectedVersion, developerId.isDefined)
        ).withHeaders(cacheControlHeaders)
      )
    }

    findVersion(apiOption) match {
      case Some((api, selectedVersion, VersionVisibility(_, _, true, _))) if selectedVersion.status == APIStatus.RETIRED =>
        renderRetiredVersionJumpPage(api, selectedVersion)
      case Some((api, selectedVersion, VersionVisibility(_, _, true, _))) => renderDocumentationPage(api, selectedVersion)
      case Some((api, selectedVersion, VersionVisibility(APIAccessType.PRIVATE, _, _, Some(true)))) => renderDocumentationPage(api, selectedVersion)
      case Some((_, _, VersionVisibility(APIAccessType.PRIVATE, false, _, _))) => redirectToLoginPage
      case _ => renderNotFoundPage
    }
  }

  def renderParentPage(service: String): Action[AnyContent] =
    headerNavigation { implicit request =>
      navLinks =>
        (for {
          userId <- extractDeveloperIdentifier(loggedInUserService.fetchLoggedInUser())
          api <- apiDefinitionService.fetchExtendedDefinition(service, userId)
          documentation <- doRenderApiDocumentation(service, "1.0", false, api, navLinks, userId)
        } yield documentation
        ) recover {
          case e: NotFoundException =>
            logger.info(s"Upstream request not found: ${e.getMessage}")
            NotFound(errorHandler.notFoundTemplate)
          case e: Throwable =>
            logger.error("Could not load API Documentation service", e)
            InternalServerError(errorHandler.internalServerErrorTemplate)
        }
      }

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

  private def extractDeveloperIdentifier(f: Future[Option[Developer]]): Future[Option[DeveloperIdentifier]] = {
    f.map( o =>
      o.map(d => UuidIdentifier(d.userId))
    )
  }
}
