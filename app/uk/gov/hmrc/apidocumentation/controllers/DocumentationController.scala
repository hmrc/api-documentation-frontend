/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject.Inject

import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.apidocumentation
import uk.gov.hmrc.apidocumentation.config.{ApplicationConfig, ApplicationGlobal}
import uk.gov.hmrc.apidocumentation.models.JsonFormatters._
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.apidocumentation.services._
import uk.gov.hmrc.apidocumentation.views
import uk.gov.hmrc.apidocumentation.views.html._
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.ramltools.domain.{RamlNotFoundException, RamlParseException}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class DocumentationController @Inject()(documentationService: DocumentationService, navigationService: NavigationService,
                                        partialsService: PartialsService,
                                        loggedInUserProvider: LoggedInUserProvider, val messagesApi: MessagesApi,
                                        implicit val appConfig: ApplicationConfig)
    extends FrontendController with I18nSupport {

  private lazy val cacheControlHeaders = "cache-control" -> s"public, max-age=${documentationService.defaultExpiration.toSeconds}"
  private val homeCrumb = Crumb("Home", routes.DocumentationController.indexPage().url)
  private val apiDocCrumb = Crumb("API Documentation", routes.DocumentationController.apiIndexPage(None, None).url)
  private val usingTheHubCrumb = Crumb("Using the Developer Hub", routes.DocumentationController.usingTheHubPage().url)
  private val mtdCrumb = Crumb("The Making Tax Digital Programme", routes.DocumentationController.mtdIntroductionPage().url)
  private val authCrumb = Crumb("Authorisation", routes.DocumentationController.authorisationPage().url)

  def cookiesPage() = {
    Action.async { implicit request => Future.successful(Ok(views.html.cookies())) }
  }

  def privacyPage() = Action.async { implicit request =>
    Future.successful(Ok(views.html.privacy()))
  }

  def termsPage() = Action.async { implicit request =>
    Future.successful(Ok(views.html.termsAndConditions()))
  }

  def indexPage() = headerNavigation { implicit request => navLinks =>
    Future.successful(Ok(index("Home", navLinks)))
  }

  def testingPage() = headerNavigation { implicit request => navLinks =>
    Future.successful(Ok(testing(pageAttributes("Testing in the sandbox", routes.DocumentationController.testingPage().url, navLinks))))
  }

  def testingStatefulBehaviourPage() = headerNavigation { implicit request => navLinks =>
    val testingStatefulBehaviourUrl = routes.DocumentationController.testingStatefulBehaviourPage().url
    Future.successful(Ok(testingStatefulBehaviour(pageAttributes("Stateful Behaviour", testingStatefulBehaviourUrl, navLinks))))
  }

  def testingDataClearDownPage() = headerNavigation { implicit request => navLinks =>
    Future.successful(Ok(testingDataClearDown(pageAttributes("Data Clear Down", routes.DocumentationController.testingDataClearDownPage().url, navLinks))))
  }

  def tutorialsPage() = headerNavigation { implicit request => navLinks =>
    Future.successful(Ok(tutorials(pageAttributes("Tutorials", routes.DocumentationController.tutorialsPage().url, navLinks))))
  }

  def termsOfUsePage() = headerNavigation { implicit request => navLinks =>
    partialsService.termsOfUsePartial() map { termsOfUsePartial =>
      Ok(termsOfUse(pageAttributes("Terms Of Use", routes.DocumentationController.termsOfUsePage().url, navLinks), termsOfUsePartial))
    }
  }

  def authorisationPage() = headerNavigation { implicit request =>
    navLinks =>
      Future.successful(Ok(authorisation(pageAttributes("Authorisation", routes.DocumentationController.authorisationPage().url, navLinks))))
  }

  def authorisation2SVPage() = headerNavigation { implicit request => navLinks =>
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

  def authorisationOpenAccessEndpointsPage() = headerNavigation { implicit request => navLinks =>
    val breadcrumbs = Breadcrumbs(
      Crumb("Open Access Endpoints", routes.DocumentationController.authorisationOpenAccessEndpointsPage().url),
      authCrumb,
      homeCrumb
    )
    Future.successful(Ok(authorisationOpenAccessEndpoints(pageAttributes(
      "Authorisation Introduction",
      routes.DocumentationController.authorisationOpenAccessEndpointsPage().url,
      navLinks,
      Some(breadcrumbs)))))
  }

  def authorisationAppRestrictedEndpointsPage() = headerNavigation { implicit request => navLinks =>
    val breadcrumbs = Breadcrumbs(
      Crumb("Application Restricted Endpoints", routes.DocumentationController.authorisationAppRestrictedEndpointsPage().url),
      authCrumb,
      homeCrumb
    )
    Future.successful(Ok(authorisationAppRestrictedEndpoints(pageAttributes(
      "Application Restricted Endpoints",
      routes.DocumentationController.authorisationAppRestrictedEndpointsPage().url,
      navLinks,
      Some(breadcrumbs)))))
  }

  def authorisationUserRestrictedEndpointsPage() = headerNavigation { implicit request => navLinks =>
    val breadcrumbs = Breadcrumbs(
      Crumb("User Restricted Endpoints", routes.DocumentationController.authorisationUserRestrictedEndpointsPage().url),
      authCrumb,
      homeCrumb
    )
    Future.successful(Ok(authorisationUserRestrictedEndpoints(pageAttributes(
      "User Restricted Endpoints",
      routes.DocumentationController.authorisationUserRestrictedEndpointsPage().url,
      navLinks,
      Some(breadcrumbs)))))
  }

  def usingTheHubPage() = headerNavigation { implicit request => navLinks =>
    Future.successful(Ok(usingTheHub(pageAttributes(s"Using the Developer Hub", routes.DocumentationController.usingTheHubPage().url, navLinks))))
  }

  def mtdIntroductionPage() = headerNavigation { implicit request => navLinks =>
    val introPageUrl = routes.DocumentationController.mtdIntroductionPage().url
    Future.successful(Ok(mtdIntroduction(pageAttributes("The Making Tax Digital Programme", introPageUrl, navLinks))))
  }

  def referenceGuidePage() = headerNavigation { implicit request => navLinks =>
    Future.successful(Ok(reference(pageAttributes("Reference guide", routes.DocumentationController.referenceGuidePage().url, navLinks))))
  }

  def nameGuidelinesPage() = headerNavigation { implicit request => navLinks =>
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

  def apiIndexPage(service: Option[String], version: Option[String]) = headerNavigation { implicit request => navLinks =>

    val params = for (a <- service; b <- version) yield (a, b)

    params match {
      case Some((service, version)) => {
        val url = routes.DocumentationController.renderApiDocumentation(service, version, None).url
        Future.successful(Redirect(url))
      }
      case None =>
        (for {
          email <- extractEmail(loggedInUserProvider.fetchLoggedInUser())
          apis <- documentationService.fetchAPIs(email)
        } yield {
          Ok(apiIndex(
            apidocumentation.models.PageAttributes(title = "API Documentation",
              breadcrumbs = Breadcrumbs(apiDocCrumb, homeCrumb),
              headerLinks = navLinks,
              sidebarLinks = navigationService.sidebarNavigation()),
            exampleApis = apis.filter(isExampleApiDefinition),
            otherApis = apis.filterNot(isExampleApiDefinition).filterNot(isTestSupportApi),
            testSupportApis = apis.filter(isTestSupportApi)
          ))

        }) recover {
          case e: Throwable =>
            Logger.error("Could not load API Documentation service", e)
            InternalServerError(ApplicationGlobal.internalServerErrorTemplate)
        }
    }
  }

  private def makeBreadcrumbName(api: ExtendedAPIDefinition, selectedVersion: ExtendedAPIVersion) = {
    val suffix = if (api.name.endsWith("API")) "" else " API"
    s"${api.name}$suffix v${selectedVersion.version} (${selectedVersion.displayedStatus})"
  }

  def redirectToCurrentApiDocumentation(service: String, cacheBuster: Option[Boolean]) = Action.async { implicit request =>
    (for {
      email <- extractEmail(loggedInUserProvider.fetchLoggedInUser())
      extendedDefn <- documentationService.fetchExtendedApiDefinition(service, email)
    } yield {
      extendedDefn.flatMap(_.userAccessibleApiDefinition.defaultVersion).fold(NotFound(ApplicationGlobal.notFoundTemplate)) { version =>
        Redirect(routes.DocumentationController.renderApiDocumentation(service, version.version, cacheBuster))
      }
    }) recover {
      case e: NotFoundException => NotFound(ApplicationGlobal.notFoundTemplate)
      case e: Throwable =>
        Logger.error("Could not load API Documentation service", e)
        InternalServerError(ApplicationGlobal.internalServerErrorTemplate)
    }
  }

  def renderApiDocumentation(service: String, version: String, cacheBuster: Option[Boolean]): Action[AnyContent] =
    headerNavigation { implicit request =>navLinks =>
    (for {
      email <- extractEmail(loggedInUserProvider.fetchLoggedInUser())
      api <- documentationService.fetchExtendedApiDefinition(service, email)
      cacheBust = bustCache(appConfig.isStubMode, cacheBuster)
      apiDocumentation <- doRenderApiDocumentation(service, version, cacheBust, api, navLinks, email)
    } yield apiDocumentation) recover {
      case e: NotFoundException =>
        Logger.info(s"Upstream request not found: ${e.getMessage}")
        NotFound(ApplicationGlobal.notFoundTemplate)
      case e: RamlNotFoundException =>
        Logger.info(s"RAML document not found: ${e.getMessage}")
        NotFound(ApplicationGlobal.notFoundTemplate)
      case e: Throwable =>
        Logger.error("Could not load API Documentation service", e)
        InternalServerError(ApplicationGlobal.internalServerErrorTemplate)
    }
  }

  def bustCache(stubMode: Boolean, cacheBuster: Option[Boolean]) = stubMode || cacheBuster.getOrElse(false)

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

    def renderNotFoundPage = Future.successful(NotFound(ApplicationGlobal.notFoundTemplate))

    def redirectToLoginPage = {
      Logger.info(s"redirectToLogin - access_uri ${routes.DocumentationController.renderApiDocumentation(service, version, None).url}")
      Future.successful(Redirect("/developer/login").withSession(
        "access_uri" -> routes.DocumentationController.renderApiDocumentation(service, version, None).url,
        "ts"-> DateTime.now(DateTimeZone.UTC).getMillis.toString)
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

  def previewApiDocumentation(url: Option[String]) = headerNavigation { implicit request => navLinks =>
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
          documentationService.fetchRAML(url.get, true).map { ramlAndSchemas =>
            Ok(page(Success(Some(ramlAndSchemas))))
          } recover {
            case e: Throwable =>
              Logger.error("Could not load API Documentation service", e)
              InternalServerError(page(Failure(e)))
          }
      }
    } else {
      Future.successful(NotFound(ApplicationGlobal.notFoundTemplate))
    }
  }

  def fetchTestEndpointJson(service: String, version: String) = Action.async { implicit request =>
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

  private def extractEmail(fut: Future[Option[Developer]])(implicit ec: ExecutionContext): Future[Option[String]] = {
    fut.map(opt => opt.map(dev => dev.email))
  }

  private def isExampleApiDefinition(apiDef: APIDefinition): Boolean = apiDef.context.equals("hello")

  private def isTestSupportApi(apiDef: APIDefinition): Boolean = apiDef.isTestSupport.getOrElse(false)
}

