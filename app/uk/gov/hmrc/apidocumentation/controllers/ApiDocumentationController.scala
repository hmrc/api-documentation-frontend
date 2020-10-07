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
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logger
import play.api.i18n.MessagesProvider
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.apidocumentation
import uk.gov.hmrc.apidocumentation.ErrorHandler
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.models.ViewModel
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.apidocumentation.models.JsonFormatters._
import uk.gov.hmrc.apidocumentation.services._
import uk.gov.hmrc.apidocumentation.views.html._
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import uk.gov.hmrc.apidocumentation.connectors.RamlPreviewConnector
import scala.util.control.NonFatal
import uk.gov.hmrc.apidocumentation.controllers.ApiDocumentationController.RamlParseException

object ApiDocumentationController {
    case class RamlParseException(msg: String) extends RuntimeException(msg)
}
@Singleton
class ApiDocumentationController @Inject()(
                                            documentationService: DocumentationService,
                                            ramlPreviewConnector: RamlPreviewConnector,
                                            apiDefinitionService: ApiDefinitionService,
                                            val navigationService: NavigationService,
                                            loggedInUserService: LoggedInUserService,
                                            errorHandler: ErrorHandler,
                                            mcc: MessagesControllerComponents,
                                            apiIndexView: ApiIndexView,
                                            retiredVersionJumpView: RetiredVersionJumpView,
                                            apisFilteredView: ApisFilteredView,
                                            previewDocumentationView: PreviewDocumentationView2,
                                            serviceDocumentationView: ServiceDocumentationView2,
                                            xmlDocumentationView: XmlDocumentationView,
                                            appConfig: ApplicationConfig
                                          )
                                         (implicit val ec: ExecutionContext)
  extends FrontendController(mcc) with HeaderNavigation with PageAttributesHelper with HomeCrumb {

  private lazy val cacheControlHeaders = "cache-control" -> "no-cache,no-store,max-age=0"
  private lazy val apiDocCrumb = Crumb("API Documentation", routes.ApiDocumentationController.apiIndexPage(None, None, None).url)

  def apiIndexPage(service: Option[String], version: Option[String], filter: Option[String]): Action[AnyContent] = headerNavigation { implicit request =>
    navLinks =>
      def pageAttributes(title: String = "API Documentation") = apidocumentation.models.PageAttributes(title,
        breadcrumbs = Breadcrumbs(apiDocCrumb, homeCrumb),
        headerLinks = navLinks,
        sidebarLinks = navigationService.sidebarNavigation())

      val params = for (a <- service; b <- version) yield (a, b)

      params match {
        case Some((service, version)) =>
          val url = routes.ApiDocumentationController.renderApiDocumentation(service, version, None).url
          Future.successful(Redirect(url))
        case None =>
          (for {
            email <- extractEmail(loggedInUserService.fetchLoggedInUser())
            apis <- apiDefinitionService.fetchAllDefinitions(email)
          } yield {
            val apisByCategory = Documentation.groupedByCategory(apis, XmlApiDocumentation.xmlApiDefinitions, ServiceGuide.serviceGuides, RoadMap.roadMaps)

            filter match {
              case Some(f) => Ok(apisFilteredView(pageAttributes("Filtered API Documentation"), apisByCategory, APICategory.fromFilter(f)))
              case _ => Ok(apiIndexView(pageAttributes(), apisByCategory))
            }

          }) recover {
            case e: Throwable =>
              Logger.error("Could not load API Documentation service", e)
              InternalServerError(errorHandler.internalServerErrorTemplate)
          }
      }
  }

  private def makeBreadcrumbName(api: ExtendedAPIDefinition, selectedVersion: ExtendedAPIVersion) = {
    val suffix = if (api.name.endsWith("API")) "" else " API"
    s"${api.name}$suffix v${selectedVersion.version} (${selectedVersion.displayedStatus})"
  }

  def redirectToApiDocumentation(service: String, version: Option[String], cacheBuster: Option[Boolean]): Action[AnyContent] = version match {
    case Some(version) => Action.async {
      Future.successful(Redirect(routes.ApiDocumentationController.renderApiDocumentation(service, version, cacheBuster)))
    }
    case _ => redirectToCurrentApiDocumentation(service, cacheBuster)
  }

  private def redirectToCurrentApiDocumentation(service: String, cacheBuster: Option[Boolean]) = Action.async { implicit request =>
    (for {
      email <- extractEmail(loggedInUserService.fetchLoggedInUser())
      extendedDefn <- apiDefinitionService.fetchExtendedDefinition(service, email)
    } yield {
      extendedDefn.flatMap(_.userAccessibleApiDefinition.defaultVersion).fold(NotFound(errorHandler.notFoundTemplate)) { version =>
        Redirect(routes.ApiDocumentationController.renderApiDocumentation(service, version.version, cacheBuster))
      }
    }) recover {
      case _: NotFoundException => NotFound(errorHandler.notFoundTemplate)
      case e: Throwable =>
        Logger.error("Could not load API Documentation service", e)
        InternalServerError(errorHandler.internalServerErrorTemplate)
    }
  }

  def renderApiDocumentation(service: String, version: String, cacheBuster: Option[Boolean]): Action[AnyContent] =
    headerNavigation { implicit request =>
      navLinks =>
        (for {
          email <- extractEmail(loggedInUserService.fetchLoggedInUser())
          api <- apiDefinitionService.fetchExtendedDefinition(service, email)
          cacheBust = bustCache(appConfig.isStubMode, cacheBuster)
          apiDocumentation <- doRenderApiDocumentation(service, version, cacheBust, api, navLinks, email)
        } yield apiDocumentation) recover {
          case e: NotFoundException =>
            Logger.info(s"Upstream request not found: ${e.getMessage}")
            NotFound(errorHandler.notFoundTemplate)
          case e: Throwable =>
            Logger.error("Could not load API Documentation service", e)
            InternalServerError(errorHandler.internalServerErrorTemplate)
        }
    }

  def bustCache(stubMode: Boolean, cacheBuster: Option[Boolean]): Boolean = stubMode || cacheBuster.getOrElse(false)

  private def doRenderApiDocumentation(service: String, version: String, cacheBuster: Boolean, apiOption: Option[ExtendedAPIDefinition],
                                       navLinks: Seq[NavLink], email: Option[String])(implicit request: Request[AnyContent], messagesProvider: MessagesProvider): Future[Result] = {
    def makePageAttributes(apiDefinition: ExtendedAPIDefinition, selectedVersion: ExtendedAPIVersion, sidebarLinks: Seq[SidebarLink]): PageAttributes = {
      val breadcrumbs = Breadcrumbs(
        Crumb(
          makeBreadcrumbName(apiDefinition, selectedVersion),
          routes.ApiDocumentationController.renderApiDocumentation(service, selectedVersion.version, None).url),
        apiDocCrumb,
        homeCrumb)

      apidocumentation.models.PageAttributes(apiDefinition.name, breadcrumbs, navLinks, sidebarLinks)
    }

    def findVersion(apiOption: Option[ExtendedAPIDefinition]) =
      for {
        api <- apiOption
        apiVersion <- api.versions.find(v => v.version == version)
        visibility <- apiVersion.visibility
      } yield (api, apiVersion, visibility)

    def renderNotFoundPage = Future.successful(NotFound(errorHandler.notFoundTemplate))

    def redirectToLoginPage = {
      Logger.info(s"redirectToLogin - access_uri ${routes.ApiDocumentationController.renderApiDocumentation(service, version, None).url}")
      Future.successful(Redirect("/developer/login").withSession(
        "access_uri" -> routes.ApiDocumentationController.renderApiDocumentation(service, version, None).url,
        "ts" -> DateTime.now(DateTimeZone.UTC).getMillis.toString)
      )
    }

    def renderRetiredVersionJumpPage(api: ExtendedAPIDefinition, selectedVersion: ExtendedAPIVersion)(implicit request: Request[AnyContent], messagesProvider: MessagesProvider) = {
      val apiDefinition = api.userAccessibleApiDefinition

      Future.successful(Ok(retiredVersionJumpView(
        makePageAttributes(apiDefinition, selectedVersion, navigationService.sidebarNavigation()), apiDefinition)))
    }

    def renderDocumentationPage(api: ExtendedAPIDefinition, selectedVersion: ExtendedAPIVersion, overviewOnly: Boolean = false)(implicit request: Request[AnyContent], messagesProvider: MessagesProvider) = {
      documentationService.fetchApiSpecification(service, version, cacheBuster).map { apiSpecification =>
        val attrs = makePageAttributes(api, selectedVersion, navigationService.apiSidebarNavigation2(service, selectedVersion, apiSpecification))
        val viewModel = ViewModel(apiSpecification)
        Ok(serviceDocumentationView(attrs, api, selectedVersion, viewModel, email.isDefined)).withHeaders(cacheControlHeaders)
      }
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

  def previewApiDocumentation(url: Option[String]): Action[AnyContent] = headerNavigation { implicit request =>
    navLinks =>
      if (appConfig.ramlPreviewEnabled) {
        val pageAttributes = apidocumentation.models.PageAttributes(title = "API Documentation Preview",
          breadcrumbs = Breadcrumbs(
            Crumb("Preview RAML", routes.ApiDocumentationController.previewApiDocumentation(None).url),
            homeCrumb),
          headerLinks = navLinks,
          sidebarLinks = navigationService.sidebarNavigation())

        val page = (result: Try[Option[ViewModel]]) => previewDocumentationView(pageAttributes, url, result)

        url match {
          case Some("") => Future.successful(InternalServerError(page(Failure(RamlParseException("No URL supplied")))))
          case None => Future.successful(Ok(page(Success(None))))
          case Some(url) =>
            ramlPreviewConnector.fetchPreviewApiSpecification(url)
            .map { apiSpecification =>
              Ok(page(Success(Some(ViewModel(apiSpecification)))))
            }
            .recover {
              case NonFatal(e) =>
                Logger.error("Could not load API Documentation service", e)
                InternalServerError(page(Failure(e)))
            }
        }
      } else {
        Future.successful(NotFound(errorHandler.notFoundTemplate))
      }
  }

  def fetchTestEndpointJson(service: String, version: String): Action[AnyContent] = Action.async { implicit request =>
    if (appConfig.ramlPreviewEnabled) {
      documentationService.buildTestEndpoints(service, version) map { endpoints =>
        Ok(Json.toJson(endpoints.sortWith((x, y) => x.url < y.url)))
      } recover {
        case e: NotFoundException =>
          Logger.info(s"RAML document not found: ${e.getMessage}")
          NotFound(Json.toJson(s"RAML Doc not found: ${e.getMessage}"))
        case e: Throwable =>
          Logger.error("Could not build Endpoint Json for API Documentation service", e)
          InternalServerError(Json.toJson(s"Could not build Endpoint Json for API Documentation service: ${e.getMessage}"))
      }
    } else {
      Future.successful(NotFound(Json.toJson("Not Found")))
    }
  }

  def renderXmlApiDocumentation(name: String): Action[AnyContent] = headerNavigation { implicit request =>
    navLinks =>
      def makePageAttributes(apiDefinition: Documentation): PageAttributes = {
        val breadcrumbs = Breadcrumbs(
          Crumb(
            apiDefinition.name,
            routes.ApiDocumentationController.renderXmlApiDocumentation(apiDefinition.context).url),
          apiDocCrumb,
          homeCrumb)

        apidocumentation.models.PageAttributes(apiDefinition.name, breadcrumbs, navLinks, navigationService.sidebarNavigation())
      }

      XmlApiDocumentation.xmlApiDefinitions.find(_.name == name) match {
        case Some(xmlApiDefinition) => Future.successful(Ok(xmlDocumentationView(makePageAttributes(xmlApiDefinition), xmlApiDefinition)))
        case _ => Future.successful(NotFound(errorHandler.notFoundTemplate))
      }
  }


  private def extractEmail(fut: Future[Option[Developer]]): Future[Option[String]] = {
    fut.map(opt => opt.map(dev => dev.email))
  }
}
