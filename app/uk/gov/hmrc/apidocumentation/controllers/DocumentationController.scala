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
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.apidocumentation
import uk.gov.hmrc.apidocumentation.ErrorHandler
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.models.JsonFormatters._
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.apidocumentation.services._
import uk.gov.hmrc.apidocumentation.views
import uk.gov.hmrc.apidocumentation.views.html._
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import uk.gov.hmrc.ramltools.domain.{RamlNotFoundException, RamlParseException}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


class DocumentationController @Inject()(documentationService: DocumentationService,
                                        apiDefinitionService: ApiDefinitionService,
                                        navigationService: NavigationService,
                                        partialsService: PartialsService,
                                        loggedInUserProvider: LoggedInUserProvider,
                                        errorHandler: ErrorHandler,
                                        mcc: MessagesControllerComponents)
                                       (implicit val appConfig: ApplicationConfig, val ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport {

  private lazy val cacheControlHeaders = "cache-control" -> "no-cache,no-store,max-age=0"
  private val homeCrumb = Crumb("Home", routes.DocumentationController.indexPage().url)
  private val apiDocCrumb = Crumb("API Documentation", routes.DocumentationController.apiIndexPage(None, None, None).url)
  private val usingTheHubCrumb = Crumb("Using the Developer Hub", routes.DocumentationController.usingTheHubPage().url)
  private val mtdCrumb = Crumb("The Making Tax Digital Programme", routes.DocumentationController.mtdIntroductionPage().url)
  private val authCrumb = Crumb("Authorisation", routes.DocumentationController.authorisationPage().url)

  def cookiesPage(): Action[AnyContent] = {
    Action.async { implicit request => Future.successful(Ok(views.html.cookies())) }
  }

  def privacyPage(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(views.html.privacy()))
  }

  def termsPage(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(views.html.termsAndConditions()))
  }

  def indexPage(): Action[AnyContent] = headerNavigation { implicit request =>
    navLinks =>
      Future.successful(Ok(index("Home", navLinks)))
  }

  def testingPage(): Action[AnyContent] = headerNavigation { implicit request =>
    navLinks =>
      Future.successful(Ok(testing(pageAttributes("Testing in the sandbox", routes.DocumentationController.testingPage().url, navLinks))))
  }

  def testingStatefulBehaviourPage(): Action[AnyContent] = headerNavigation { implicit request =>
    _ => Future.successful(MovedPermanently(routes.DocumentationController.testUsersDataStatefulBehaviourPage().url))
  }

  def testUsersDataStatefulBehaviourPage(): Action[AnyContent] = headerNavigation { implicit request =>
    navLinks =>
      val testUsersDataStatefulBehaviourUrl = routes.DocumentationController.testUsersDataStatefulBehaviourPage().url
      Future.successful(Ok(testUsersDataStatefulBehaviour(pageAttributes("Test users, test data and stateful behaviour", testUsersDataStatefulBehaviourUrl, navLinks))))
  }

  def tutorialsPage(): Action[AnyContent] = headerNavigation { implicit request =>
    navLinks =>
      Future.successful(Ok(tutorials(pageAttributes("Tutorials", routes.DocumentationController.tutorialsPage().url, navLinks))))
  }

  def termsOfUsePage(): Action[AnyContent] = headerNavigation { implicit request =>
    navLinks =>
      partialsService.termsOfUsePartial() map { termsOfUsePartial =>
        Ok(termsOfUse(pageAttributes("Terms Of Use", routes.DocumentationController.termsOfUsePage().url, navLinks), termsOfUsePartial))
      }
  }

  def authorisationPage(): Action[AnyContent] = headerNavigation { implicit request =>
    navLinks =>
      Future.successful(Ok(authorisation(pageAttributes("Authorisation", routes.DocumentationController.authorisationPage().url, navLinks))))
  }

  def authorisation2SVPage(): Action[AnyContent] = headerNavigation { implicit request =>
    navLinks =>
      val breadcrumbs = Breadcrumbs(
        Crumb("2-step verification", routes.DocumentationController.authorisation2SVPage().url),
        authCrumb,
        homeCrumb
      )
      Future.successful(Ok(authorisation2SV(pageAttributes(
        "2-step verification",
        routes.DocumentationController.authorisation2SVPage().url,
        navLinks,
        Some(breadcrumbs)))))
  }

  def authorisationCredentialsPage(): Action[AnyContent] = headerNavigation { implicit request =>
    navLinks =>
      val breadcrumbs = Breadcrumbs(
        Crumb("Credentials", routes.DocumentationController.authorisationCredentialsPage().url),
        authCrumb,
        homeCrumb
      )
      Future.successful(Ok(credentials(pageAttributes(
        "Credentials",
        routes.DocumentationController.authorisationCredentialsPage().url,
        navLinks,
        Some(breadcrumbs)))))
  }

  def authorisationOpenAccessEndpointsPage(): Action[AnyContent] = headerNavigation { implicit request =>
    navLinks =>
      val breadcrumbs = Breadcrumbs(
        Crumb("Open access endpoints", routes.DocumentationController.authorisationOpenAccessEndpointsPage().url),
        authCrumb,
        homeCrumb
      )
      Future.successful(Ok(authorisationOpenAccessEndpoints(pageAttributes(
        "Open access endpoints",
        routes.DocumentationController.authorisationOpenAccessEndpointsPage().url,
        navLinks,
        Some(breadcrumbs)))))
  }

  def authorisationAppRestrictedEndpointsPage(): Action[AnyContent] = headerNavigation { implicit request =>
    navLinks =>
      val breadcrumbs = Breadcrumbs(
        Crumb("Application-restricted endpoints", routes.DocumentationController.authorisationAppRestrictedEndpointsPage().url),
        authCrumb,
        homeCrumb
      )
      Future.successful(Ok(authorisationAppRestrictedEndpoints(pageAttributes(
        "Application-restricted endpoints",
        routes.DocumentationController.authorisationAppRestrictedEndpointsPage().url,
        navLinks,
        Some(breadcrumbs)))))
  }

  def authorisationUserRestrictedEndpointsPage(): Action[AnyContent] = headerNavigation { implicit request =>
    navLinks =>
      val breadcrumbs = Breadcrumbs(
        Crumb("User-restricted endpoints", routes.DocumentationController.authorisationUserRestrictedEndpointsPage().url),
        authCrumb,
        homeCrumb
      )
      Future.successful(Ok(authorisationUserRestrictedEndpoints(pageAttributes(
        "User-restricted endpoints",
        routes.DocumentationController.authorisationUserRestrictedEndpointsPage().url,
        navLinks,
        Some(breadcrumbs)))))
  }

  def usingTheHubPage(): Action[AnyContent] = headerNavigation { implicit request =>
    navLinks =>
      Future.successful(Ok(usingTheHub(pageAttributes(s"Using the Developer Hub", routes.DocumentationController.usingTheHubPage().url, navLinks))))
  }

  def mtdIntroductionPage(): Action[AnyContent] = headerNavigation { implicit request =>
    navLinks =>
      val introPageUrl = routes.DocumentationController.mtdIntroductionPage().url
      Future.successful(Ok(mtdIntroduction(pageAttributes("Making Tax Digital guides", introPageUrl, navLinks))))
  }

  def mtdIncomeTaxServiceGuidePage(): Action[AnyContent] = headerNavigation { implicit request =>
    navLinks =>
      Future.successful(MovedPermanently("/guides/income-tax-mtd-end-to-end-service-guide/"))
  }

  def referenceGuidePage(): Action[AnyContent] = headerNavigation { implicit request =>
    navLinks =>
      Future.successful(Ok(reference(pageAttributes("Reference guide", routes.DocumentationController.referenceGuidePage().url, navLinks))))
  }

  def developmentPracticesPage(): Action[AnyContent] = headerNavigation { implicit request =>
    navLinks =>
      Future.successful(Ok(developmentPractices(pageAttributes("Development practices", routes.DocumentationController.developmentPracticesPage().url, navLinks))))
  }

  def nameGuidelinesPage(): Action[AnyContent] = headerNavigation { implicit request =>
    navLinks =>
      val breadcrumbs = Breadcrumbs(
        Crumb("Application naming guidelines", routes.DocumentationController.nameGuidelinesPage().url),
        usingTheHubCrumb,
        homeCrumb
      )
      Future.successful(Ok(namingGuidelines(pageAttributes(
        "Application naming guidelines",
        routes.DocumentationController.nameGuidelinesPage().url,
        navLinks,
        Some(breadcrumbs)))))
  }

  def fraudPreventionPage(): Action[AnyContent] = headerNavigation { implicit request =>
    navLinks =>
      Future.successful(Ok(fraudPrevention(pageAttributes("Fraud prevention", routes.DocumentationController.fraudPreventionPage().url, navLinks))))
  }

  def apiIndexPage(service: Option[String], version: Option[String], filter: Option[String]): Action[AnyContent] = headerNavigation { implicit request =>
    navLinks =>
      def pageAttributes(title: String = "API Documentation") = apidocumentation.models.PageAttributes(title,
        breadcrumbs = Breadcrumbs(apiDocCrumb, homeCrumb),
        headerLinks = navLinks,
        sidebarLinks = navigationService.sidebarNavigation())

      val params = for (a <- service; b <- version) yield (a, b)

      params match {
        case Some((service, version)) =>
          val url = routes.DocumentationController.renderApiDocumentation(service, version, None).url
          Future.successful(Redirect(url))
        case None =>
          (for {
            email <- extractEmail(loggedInUserProvider.fetchLoggedInUser())
            apis <- apiDefinitionService.fetchAllDefinitions(email)
          } yield {
            val apisByCategory = Documentation.groupedByCategory(apis, XmlApiDocumentation.xmlApiDefinitions, ServiceGuide.serviceGuides)

            filter match {
              case Some(f) => Ok(apisFiltered(pageAttributes("Filtered API Documentation"), apisByCategory, APICategory.fromFilter(f)))
              case _ => Ok(apiIndex(pageAttributes(), apisByCategory))
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
      Future.successful(Redirect(routes.DocumentationController.renderApiDocumentation(service, version, cacheBuster)))
    }
    case _ => redirectToCurrentApiDocumentation(service, cacheBuster)
  }

  private def redirectToCurrentApiDocumentation(service: String, cacheBuster: Option[Boolean]) = Action.async { implicit request =>
    (for {
      email <- extractEmail(loggedInUserProvider.fetchLoggedInUser())
      extendedDefn <- apiDefinitionService.fetchExtendedDefinition(service, email)
    } yield {
      extendedDefn.flatMap(_.userAccessibleApiDefinition.defaultVersion).fold(NotFound(errorHandler.notFoundTemplate)) { version =>
        Redirect(routes.DocumentationController.renderApiDocumentation(service, version.version, cacheBuster))
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
          email <- extractEmail(loggedInUserProvider.fetchLoggedInUser())
          api <- apiDefinitionService.fetchExtendedDefinition(service, email)
          cacheBust = bustCache(appConfig.isStubMode, cacheBuster)
          apiDocumentation <- doRenderApiDocumentation(service, version, cacheBust, api, navLinks, email)
        } yield apiDocumentation) recover {
          case e: NotFoundException =>
            Logger.info(s"Upstream request not found: ${e.getMessage}")
            NotFound(errorHandler.notFoundTemplate)
          case e: RamlNotFoundException =>
            Logger.info(s"RAML document not found: ${e.getMessage}")
            NotFound(errorHandler.notFoundTemplate)
          case e: Throwable =>
            Logger.error("Could not load API Documentation service", e)
            InternalServerError(errorHandler.internalServerErrorTemplate)
        }
    }

  def bustCache(stubMode: Boolean, cacheBuster: Option[Boolean]): Boolean = stubMode || cacheBuster.getOrElse(false)

  private def doRenderApiDocumentation(service: String, version: String, cacheBuster: Boolean, apiOption: Option[ExtendedAPIDefinition],
                                       navLinks: Seq[NavLink], email: Option[String])(implicit hc: HeaderCarrier, request: Request[_]): Future[Result] = {
    def makePageAttributes(apiDefinition: ExtendedAPIDefinition, selectedVersion: ExtendedAPIVersion, sidebarLinks: Seq[SidebarLink]): PageAttributes = {
      val breadcrumbs = Breadcrumbs(
        Crumb(
          makeBreadcrumbName(apiDefinition, selectedVersion),
          routes.DocumentationController.renderApiDocumentation(service, selectedVersion.version, None).url),
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
      Logger.info(s"redirectToLogin - access_uri ${routes.DocumentationController.renderApiDocumentation(service, version, None).url}")
      Future.successful(Redirect("/developer/login").withSession(
        "access_uri" -> routes.DocumentationController.renderApiDocumentation(service, version, None).url,
        "ts" -> DateTime.now(DateTimeZone.UTC).getMillis.toString)
      )
    }

    def renderRetiredVersionJumpPage(api: ExtendedAPIDefinition, selectedVersion: ExtendedAPIVersion) = {
      val apiDefinition = api.userAccessibleApiDefinition

      Future.successful(Ok(retiredVersionJump(
        makePageAttributes(apiDefinition, selectedVersion, navigationService.sidebarNavigation()), apiDefinition)))
    }

    def renderDocumentationPage(api: ExtendedAPIDefinition, selectedVersion: ExtendedAPIVersion, overviewOnly: Boolean = false) =
      documentationService.fetchRAML(service, version, cacheBuster).map { ramlAndSchemas =>
        val attrs = makePageAttributes(api, selectedVersion, navigationService.apiSidebarNavigation(service, selectedVersion, ramlAndSchemas.raml))
        Ok(serviceDocumentation(attrs, api, selectedVersion, ramlAndSchemas, email.isDefined)).withHeaders(cacheControlHeaders)
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
            Crumb("Preview RAML", routes.DocumentationController.previewApiDocumentation(None).url),
            apiDocCrumb,
            homeCrumb),
          headerLinks = navLinks,
          sidebarLinks = navigationService.sidebarNavigation())

        val page = (result: Try[Option[RamlAndSchemas]]) => previewDocumentation(pageAttributes, url, result)

        url match {
          case Some("") => Future.successful(InternalServerError(page(Failure(RamlParseException("No URL supplied")))))
          case None => Future.successful(Ok(page(Success(None))))
          case _ =>
            documentationService.fetchRAML(url.get, cacheBuster = true).map { ramlAndSchemas =>
              Ok(page(Success(Some(ramlAndSchemas))))
            } recover {
              case e: Throwable =>
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
        case e: RamlNotFoundException =>
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
            routes.DocumentationController.renderXmlApiDocumentation(apiDefinition.context).url),
          apiDocCrumb,
          homeCrumb)

        apidocumentation.models.PageAttributes(apiDefinition.name, breadcrumbs, navLinks, navigationService.sidebarNavigation())
      }

      XmlApiDocumentation.xmlApiDefinitions.find(_.name == name) match {
        case Some(xmlApiDefinition) => Future.successful(Ok(xmlDocumentation(makePageAttributes(xmlApiDefinition), xmlApiDefinition)))
        case _ => Future.successful(NotFound(errorHandler.notFoundTemplate))
      }
  }

  private def headerNavigation(f: Request[AnyContent] => Seq[NavLink] => Future[Result]): Action[AnyContent] = {
    Action.async { implicit request =>
      // We use a non-standard cookie which doesn't get propagated in the header carrier
      val newHc = request.headers.get(COOKIE).fold(hc) { cookie => hc.withExtraHeaders(COOKIE -> cookie) }
      navigationService.headerNavigation()(newHc) flatMap { navLinks =>
        f(request)(navLinks)
      } recoverWith {
        case ex =>
          Logger.error("User navigation links can not be rendered due to service call failure", ex)
          f(request)(Seq.empty)
      }
    }
  }

  private def pageAttributes(title: String, url: String, headerNavLinks: Seq[NavLink], customBreadcrumbs: Option[Breadcrumbs] = None) = {
    val breadcrumbs = customBreadcrumbs.getOrElse(Breadcrumbs(Crumb(title, url), homeCrumb))
    apidocumentation.models.PageAttributes(title, breadcrumbs, headerNavLinks, navigationService.sidebarNavigation())
  }

  private def extractEmail(fut: Future[Option[Developer]]): Future[Option[String]] = {
    fut.map(opt => opt.map(dev => dev.email))
  }
}
