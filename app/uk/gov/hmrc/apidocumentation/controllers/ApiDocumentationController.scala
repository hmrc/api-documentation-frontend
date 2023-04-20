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
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

import akka.stream.Materializer
import controllers.Assets
import org.joda.time.{DateTime, DateTimeZone}

import play.api.i18n.MessagesProvider
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import uk.gov.hmrc.apidocumentation.ErrorHandler
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.connectors.{DownloadConnector, RamlPreviewConnector}
import uk.gov.hmrc.apidocumentation.controllers.ApiDocumentationController.RamlParseException
import uk.gov.hmrc.apidocumentation.models.APICategory.{APICategory, categoryMap}
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.apidocumentation.models.apispecification.{ApiSpecification, DocumentationItem}
import uk.gov.hmrc.apidocumentation.models.jsonFormatters._
import uk.gov.hmrc.apidocumentation.services._
import uk.gov.hmrc.apidocumentation.util.ApplicationLogger
import uk.gov.hmrc.apidocumentation.views.html._
import uk.gov.hmrc.apidocumentation.views.html.openapispec.ParentPageOuter

object ApiDocumentationController {
  case class RamlParseException(msg: String) extends RuntimeException(msg)
}

@Singleton
class ApiDocumentationController @Inject() (
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
    parentPage: ParentPageOuter,
    xmlServicesService: XmlServicesService,
    downloadConnector: DownloadConnector,
    assets: Assets
  )(implicit val ec: ExecutionContext,
    appConfig: ApplicationConfig,
    mat: Materializer
  ) extends FrontendController(mcc) with HeaderNavigation with PageAttributesHelper with HomeCrumb with ApplicationLogger {

  private lazy val cacheControlHeaders = "cache-control" -> "no-cache,no-store,max-age=0"
  private lazy val apiDocCrumb         = Crumb("API Documentation", routes.ApiDocumentationController.apiIndexPage(None, None, None).url)

  def apiIndexPage(service: Option[String], version: Option[String], filter: Option[String]): Action[AnyContent] = headerNavigation { implicit request => navLinks =>
    def pageAttributes(title: String = "API Documentation") =
      PageAttributes(title, breadcrumbs = Breadcrumbs(apiDocCrumb, homeCrumb), headerLinks = navLinks, sidebarLinks = navigationService.sidebarNavigation())

    val params = for (a <- service; b <- version) yield (a, b)

    params match {
      case Some((service, version)) =>
        val url = routes.ApiDocumentationController.renderApiDocumentation(service, version, None).url
        Future.successful(Redirect(url))
      case None                     =>
        (for {
          userId  <- extractDeveloperIdentifier(loggedInUserService.fetchLoggedInUser())
          apis    <- apiDefinitionService.fetchAllDefinitions(userId)
          xmlApis <- xmlServicesService.fetchAllXmlApis()
        } yield {
          val apisByCategory = Documentation.groupedByCategory(apis, xmlApis, ServiceGuide.serviceGuides, RoadMap.roadMaps)

          filter match {
            case Some(f) => Ok(apisFilteredView(pageAttributes("Filtered API Documentation"), apisByCategory, APICategory.fromFilter(f)))
            case _       => Ok(apiIndexView(pageAttributes(), apisByCategory))
          }

        }) recover {
          case e: Throwable =>
            logger.error("Could not load API Documentation service", e)
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
    case _             => redirectToCurrentApiDocumentation(service, cacheBuster)
  }

  private def redirectToCurrentApiDocumentation(service: String, cacheBuster: Option[Boolean]) = Action.async { implicit request =>
    (for {
      userId       <- extractDeveloperIdentifier(loggedInUserService.fetchLoggedInUser())
      extendedDefn <- apiDefinitionService.fetchExtendedDefinition(service, userId)
    } yield {
      extendedDefn.flatMap(_.userAccessibleApiDefinition.defaultVersion).fold(NotFound(errorHandler.notFoundTemplate)) { version =>
        Redirect(routes.ApiDocumentationController.renderApiDocumentation(service, version.version, cacheBuster))
      }
    }) recover {
      case _: NotFoundException => NotFound(errorHandler.notFoundTemplate)
      case e: Throwable         =>
        logger.error("Could not load API Documentation service", e)
        InternalServerError(errorHandler.internalServerErrorTemplate)
    }
  }

  def renderApiDocumentation(service: String, version: String, cacheBuster: Option[Boolean]): Action[AnyContent] =
    headerNavigation { implicit request => navLinks =>
      (for {
        userId           <- extractDeveloperIdentifier(loggedInUserService.fetchLoggedInUser())
        api              <- apiDefinitionService.fetchExtendedDefinition(service, userId)
        cacheBust         = bustCache(cacheBuster)
        apiDocumentation <- doRenderApiDocumentation(service, version, cacheBust, api, navLinks, userId)
      } yield apiDocumentation) recover {
        case e: NotFoundException =>
          logger.info(s"Upstream request not found: ${e.getMessage}")
          NotFound(errorHandler.notFoundTemplate)
        case e: Throwable         =>
          logger.error("Could not load API Documentation service", e)
          InternalServerError(errorHandler.internalServerErrorTemplate)
      }
    }

  def bustCache(cacheBuster: Option[Boolean]): Boolean = cacheBuster.getOrElse(false)

  // scalastyle:off method.length
  private def doRenderApiDocumentation(
      service: String,
      version: String,
      cacheBuster: Boolean,
      apiOption: Option[ExtendedAPIDefinition],
      navLinks: Seq[NavLink],
      developerId: Option[DeveloperIdentifier]
    )(implicit request: Request[AnyContent],
      messagesProvider: MessagesProvider
    ): Future[Result] = {
    def makePageAttributes(apiDefinition: ExtendedAPIDefinition, selectedVersion: ExtendedAPIVersion, sidebarLinks: Seq[SidebarLink]): PageAttributes = {
      val breadcrumbs = Breadcrumbs(
        Crumb(
          makeBreadcrumbName(apiDefinition, selectedVersion),
          routes.ApiDocumentationController.renderApiDocumentation(service, selectedVersion.version, None).url
        ),
        apiDocCrumb,
        homeCrumb
      )

      PageAttributes(apiDefinition.name, breadcrumbs, navLinks, sidebarLinks)
    }

    def renderNotFoundPage = Future.successful(NotFound(errorHandler.notFoundTemplate))

    def redirectToLoginPage = {
      logger.info(s"redirectToLogin - access_uri ${routes.ApiDocumentationController.renderApiDocumentation(service, version, None).url}")
      Future.successful(Redirect("/developer/login").withSession(
        "access_uri" -> routes.ApiDocumentationController.renderApiDocumentation(service, version, None).url,
        "ts"         -> DateTime.now(DateTimeZone.UTC).getMillis.toString
      ))
    }

    def renderRetiredVersionJumpPage(api: ExtendedAPIDefinition, selectedVersion: ExtendedAPIVersion)(implicit request: Request[AnyContent], messagesProvider: MessagesProvider) = {
      val apiDefinition = api.userAccessibleApiDefinition

      Future.successful(Ok(retiredVersionJumpView(
        makePageAttributes(apiDefinition, selectedVersion, navigationService.sidebarNavigation()),
        apiDefinition
      )))
    }

    def renderDocumentationPage(
        api: ExtendedAPIDefinition,
        selectedVersion: ExtendedAPIVersion
      )(implicit request: Request[AnyContent],
        messagesProvider: MessagesProvider
      ): Future[Result] = {
      def renderRamlSpec(apiSpecification: ApiSpecification): Future[Result] = {
        val attrs     = makePageAttributes(api, selectedVersion, navigationService.apiSidebarNavigation2(service, selectedVersion, apiSpecification))
        val viewModel = ViewModel(apiSpecification)
        successful(Ok(serviceDocumentationView(attrs, api, selectedVersion, viewModel, developerId.isDefined)).withHeaders(cacheControlHeaders))
      }

      def withDefault(service: String)(file: String, label: String): Future[DocumentationItem] = {
        def resultToDocumentationItem(result: Result): Future[DocumentationItem] = {
          result.body.consumeData
            .map(bs => bs.utf8String)
            .map(text => DocumentationItem(label, text))
        }

        val findLocally: Future[DocumentationItem] = {
          assets.at("/public/common/docs", file, false)(request).flatMap(resultToDocumentationItem)
        }

        downloadConnector.fetch(service, "common", file)
          .flatMap(_.fold(findLocally)(resultToDocumentationItem))
      }

      def renderOas(categories: Seq[APICategory]): Future[Result] = {
        val withDefaultForService = withDefault(service) _

        val requiredFraudPrevention = (categories.contains(APICategory.VAT_MTD) || categories.contains(APICategory.INCOME_TAX_MTD)) && !api.isTestSupport

        for {
          overview        <- withDefaultForService("overview.md", "Overview")
          errors          <- withDefaultForService("errors.md", "Errors")
          testing         <- withDefaultForService("testing.md", "Testing")
          fraudPrevention <- withDefaultForService("fraud-prevention.md", "Fraud Prevention")
          versioning      <- withDefaultForService("versioning.md", "Versioning")
          markdownBlocks   = List(overview, errors, testing) ++ (if (requiredFraudPrevention) List(fraudPrevention) else List()) ++ List(versioning)
          attrs            = makePageAttributes(api, selectedVersion, navigationService.openApiSidebarNavigation(service, selectedVersion, markdownBlocks))

        } yield Ok(parentPage(attrs, markdownBlocks, api.name, api, selectedVersion, developerId.isDefined)).withHeaders(cacheControlHeaders)
      }

      val categories = categoryMap.getOrElse(api.name, Seq.empty)
      documentationService.fetchApiSpecification(service, version, cacheBuster).flatMap(_.fold(renderOas(categories))(renderRamlSpec))
    }

    def findVersion(apiOption: Option[ExtendedAPIDefinition]) =
      for {
        api        <- apiOption
        apiVersion <- api.versions.find(v => v.version == version)
        visibility <- apiVersion.visibility
      } yield (api, apiVersion, visibility)

    findVersion(apiOption) match {
      case Some((api, selectedVersion, VersionVisibility(_, _, true, _))) if selectedVersion.status == APIStatus.RETIRED => renderRetiredVersionJumpPage(api, selectedVersion)
      case Some((api, selectedVersion, VersionVisibility(_, _, true, _)))                                                => renderDocumentationPage(api, selectedVersion)
      case Some((api, selectedVersion, VersionVisibility(APIAccessType.PRIVATE, _, false, Some(true))))                  => renderDocumentationPage(api, selectedVersion)
      case Some((_, _, VersionVisibility(APIAccessType.PRIVATE, false, _, _)))                                           => redirectToLoginPage
      case _                                                                                                             => renderNotFoundPage
    }
  }
  // scalastyle:on method.length

  def previewApiDocumentation(url: Option[String]): Action[AnyContent] = headerNavigation { implicit request => navLinks =>
    if (appConfig.ramlPreviewEnabled) {
      val pageAttributes = PageAttributes(
        title = "API Documentation Preview",
        breadcrumbs = Breadcrumbs(
          Crumb("Preview RAML", routes.ApiDocumentationController.previewApiDocumentation(None).url),
          homeCrumb
        ),
        headerLinks = navLinks,
        sidebarLinks = navigationService.sidebarNavigation()
      )

      val page = (result: Try[Option[ViewModel]]) => previewDocumentationView(pageAttributes, url, result)

      url match {
        case Some("")  => Future.successful(InternalServerError(page(Failure(RamlParseException("No URL supplied")))))
        case None      => Future.successful(Ok(page(Success(None))))
        case Some(url) =>
          ramlPreviewConnector.fetchPreviewApiSpecification(url)
            .map { apiSpecification =>
              Ok(page(Success(Some(ViewModel(apiSpecification)))))
            }
            .recover {
              case NonFatal(e) =>
                logger.error("Could not load API Documentation service", e)
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
          logger.info(s"RAML document not found: ${e.getMessage}")
          NotFound(Json.toJson(s"RAML Doc not found: ${e.getMessage}"))
        case e: Throwable         =>
          logger.error("Could not build Endpoint Json for API Documentation service", e)
          InternalServerError(Json.toJson(s"Could not build Endpoint Json for API Documentation service: ${e.getMessage}"))
      }
    } else {
      Future.successful(NotFound(Json.toJson("Not Found")))
    }
  }

  def renderXmlApiDocumentation(name: String): Action[AnyContent] = headerNavigation { implicit request => navLinks =>
    def makePageAttributes(apiDefinition: Documentation): PageAttributes = {
      val breadcrumbs = Breadcrumbs(
        Crumb(
          apiDefinition.name,
          routes.ApiDocumentationController.renderXmlApiDocumentation(apiDefinition.context).url
        ),
        apiDocCrumb,
        homeCrumb
      )

      PageAttributes(apiDefinition.name, breadcrumbs, navLinks, navigationService.sidebarNavigation())
    }

    xmlServicesService.fetchXmlApi(name) map {
      case Some(xmlApiDefinition) => Ok(xmlDocumentationView(makePageAttributes(xmlApiDefinition), xmlApiDefinition))
      case _                      => NotFound(errorHandler.notFoundTemplate)
    }
  }

  private def extractDeveloperIdentifier(f: Future[Option[Developer]]): Future[Option[DeveloperIdentifier]] = {
    f.map(o =>
      o.map(d => UuidIdentifier(d.userId))
    )
  }
}
