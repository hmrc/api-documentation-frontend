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

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc._
import play.api.http.Status._
import play.api.test.FakeRequest
import uk.gov.hmrc.apidocumentation.models.APIAccessType.APIAccessType
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.apidocumentation.utils.ApiDefinitionTestDataHelper
import uk.gov.hmrc.apidocumentation.models._

import scala.concurrent.Future
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.apidocumentation.controllers.utils._
import scala.concurrent.Future.successful

class CommonControllerBaseSpec
  extends UnitSpec
    with ScalaFutures
    with MockitoSugar
    with ApiDefinitionTestDataHelper
    with GuiceOneAppPerSuite
    {

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(("metrics.jvm", false))
      .build()

  implicit lazy val request: Request[AnyContent] = FakeRequest()
  implicit lazy val materializer = app.materializer
  lazy val mcc = app.injector.instanceOf[MessagesControllerComponents]

  implicit val hc = HeaderCarrier()

  val serviceName = "hello-world"
  val endpointName = "Say Hello World!"

  def anApiDefinition(serviceName: String, version: String): APIDefinition = {
    APIDefinition(serviceName, "Hello World", "Say Hello World", "hello", None, None,
      Seq(APIVersion(version, None, APIStatus.STABLE, Seq(endpoint()))))
  }

  def anXmlApiDefinition(name: String) = XmlApiDocumentation(name, "description", "context")

  def extendedApiDefinition(serviceName: String, version: String): ExtendedAPIDefinition =
    extendedApiDefinition(serviceName, version, APIAccessType.PUBLIC, loggedIn = false, authorised = true)

  def extendedApiDefinition(serviceName: String,
                            version: String,
                            access: APIAccessType,
                            loggedIn: Boolean,
                            authorised: Boolean,
                            isTrial: Option[Boolean] = None): ExtendedAPIDefinition = {
    ExtendedAPIDefinition(serviceName, "http://service", "Hello World", "Say Hello World", "hello", requiresTrust = false, isTestSupport = false,
      Seq(
        ExtendedAPIVersion(version, APIStatus.STABLE, Seq(Endpoint(endpointName, "/world", HttpMethod.GET, None)),
          Some(APIAvailability(endpointsEnabled = true, APIAccess(access, whitelistedApplicationIds = Some(Seq.empty), isTrial = isTrial), loggedIn, authorised)), None)
      ))
  }

  def extendedApiDefinitionWithRetiredVersion(serviceName: String, retiredVersion: String, nonRetiredVersion: String) = {
    ExtendedAPIDefinition(serviceName, "http://service", "Hello World", "Say Hello World", "hello", requiresTrust = false, isTestSupport = false,
      Seq(
        ExtendedAPIVersion(retiredVersion, APIStatus.RETIRED, Seq(endpoint(endpointName)),
          Some(APIAvailability(endpointsEnabled = true, APIAccess(APIAccessType.PUBLIC), loggedIn = false, authorised = true)),
          None),
        ExtendedAPIVersion(nonRetiredVersion, APIStatus.STABLE, Seq(endpoint(endpointName)),
          Some(APIAvailability(endpointsEnabled = true, APIAccess(APIAccessType.PUBLIC, Some(Seq.empty)), loggedIn = false, authorised = true)),
          None)
      ))
  }

  def extendedApiDefinitionWithRetiredVersionAndInaccessibleLatest(serviceName: String): ExtendedAPIDefinition = {
    ExtendedAPIDefinition(serviceName, "http://service", "Hello World", "Say Hello World", "hello", requiresTrust = false, isTestSupport = false,
      Seq(
        ExtendedAPIVersion("1.0", APIStatus.RETIRED, Seq(endpoint(endpointName)),
          Some(APIAvailability(endpointsEnabled = true, APIAccess(APIAccessType.PUBLIC), loggedIn = false, authorised = true)),
          None),
        ExtendedAPIVersion("1.1", APIStatus.BETA, Seq(endpoint(endpointName)),
          Some(APIAvailability(endpointsEnabled = true, APIAccess(APIAccessType.PUBLIC), loggedIn = false, authorised = true)),
          None),
        ExtendedAPIVersion("1.2", APIStatus.STABLE, Seq(endpoint(endpointName)),
          Some(APIAvailability(endpointsEnabled = true, APIAccess(APIAccessType.PRIVATE), loggedIn = false, authorised = false)),
          None)
      ))
  }

  def aServiceGuide(name: String) = ServiceGuide(name, "context")

  def verifyRedirectToLoginPage(actualPageFuture: Future[Result], service: String, version: String) {
    val actualPage = await(actualPageFuture)
    status(actualPage) shouldBe 303

    actualPage.header.headers.get("Location") shouldBe Some("/developer/login")
    actualPage.session.get("access_uri") shouldBe Some(s"/api-documentation/docs/api/service/$service/$version")
  }

  def pageTitle(pagePurpose: String) = s"$pagePurpose - HMRC Developer Hub - GOV.UK"

  def isPresentAndCorrect(includesText: String, title: String)(fResult: Future[Result]): Unit = {
    val result = await(fResult)
    status(result) shouldBe OK
    bodyOf(result) should include(includesText)
    bodyOf(result) should include(pageTitle(title))
  }
}


trait PageRenderVerification {
  self: CommonControllerBaseSpec =>
  import uk.gov.hmrc.apidocumentation.services.NavigationService

  lazy val homeBreadcrumb = Crumb("Home", routes.DocumentationController.indexPage().url)
  lazy val apiDocsBreadcrumb = Crumb("API Documentation", routes.ApiDocumentationController.apiIndexPage(None, None, None).url)
  lazy val navLink = NavLink("Header Link", "/api-documentation/headerlink")
  lazy val sidebarLink = SidebarLink("API Documentation", "/api-documentation/docs/api")

  val navigationService = mock[NavigationService]
  when(navigationService.headerNavigation()(any[HeaderCarrier])).thenReturn(successful(Seq(navLink)))
  when(navigationService.sidebarNavigation()).thenReturn(successful(Seq(sidebarLink)))
  when(navigationService.apiSidebarNavigation(any(), any(), any())).thenReturn(Seq(sidebarLink))


  def titleOf(result: Result) = {
    val titleRegEx = """<title[^>]*>(.*)</title>""".r
    val title = titleRegEx.findFirstMatchIn(bodyOf(result)).map(_.group(1))
    title.isDefined shouldBe true
    title.get
  }

  def subNavIsRendered(result: Result) = {
    bodyOf(result).contains("<ul class=\"side-nav side-nav--child\">")
  }

  def sideNavLinkIsRendered(result: Result, sidebarLink: SidebarLink) = {
    bodyOf(result).contains(s"""<a href="${sidebarLink.href}" class="side-nav__link">${sidebarLink.label}</a>""")
  }

  def userNavLinkIsRendered(result: Result, navLink: NavLink) = {
    bodyOf(result).contains(navLink.href) && bodyOf(result).contains(navLink.label)
  }

  def versionOptionIsRendered(result: Result, service: String, version: String, displayedStatus: String) = {
    bodyOf(result).contains(s"""<option selected value="$version" aria-label="Select to view documentation for v$version ($displayedStatus)">""")
  }

  def verifyBreadcrumbRendered(actualPage: Result, crumb: Crumb) {
    bodyOf(actualPage) should include(s"""<li><a href="${crumb.url}">${crumb.name}</a></li>""")
  }

  def verifyBreadcrumbEndpointRendered(actualPage: Result, crumbText: String) = {
    bodyOf(actualPage) should include(s"""<li>${crumbText}</li>""")
  }

  def verifyPageRendered(expectedTitle: String,
                          breadcrumbs: List[Crumb] = List(homeBreadcrumb),
                          sideNavLinkRendered: Boolean = true,
                          subNavRendered: Boolean = false,
                          bodyContains: Seq[String] = Seq.empty
                          )(
                            actualPageFuture: Future[Result]
                          ): Unit = {
    val actualPage = await(actualPageFuture)
    status(actualPage) shouldBe 200
    titleOf(actualPage) shouldBe expectedTitle

    userNavLinkIsRendered(actualPage, navLink) shouldBe true
    sideNavLinkIsRendered(actualPage, sidebarLink) shouldBe sideNavLinkRendered
    subNavIsRendered(actualPage) shouldBe subNavRendered
    breadcrumbs.foreach(verifyBreadcrumbRendered(actualPage, _))
    bodyContains.foreach { snippet => bodyOf(actualPage) should include(snippet) }
  }

  def verifyNotFoundPageRendered(actualPageFuture: Future[Result]) {
    val actualPage = await(actualPageFuture)
    status(actualPage) shouldBe NOT_FOUND
  }


  def verifyErrorPageRendered(expectedStatus: Int, expectedError: String)(actualPageFuture: Future[Result]) {
    val actualPage = await(actualPageFuture)
    status(actualPage) shouldBe expectedStatus
    bodyOf(actualPage) should include(expectedError)
  }

  def verifyApiDocumentationPageRendered(actualPage: Result, version: String, apiStatus: String) = {
    verifyPageRendered(pageTitle("Hello World"), breadcrumbs = List(homeBreadcrumb, apiDocsBreadcrumb))(actualPage)
    verifyBreadcrumbEndpointRendered(actualPage, s"Hello World API v$version ($apiStatus)")
  }

  def verifyLinkToStableDocumentationRendered(actualPage: Result, service: String, version: String) = {
    bodyOf(actualPage) should include(s"""<a href="/api-documentation/docs/api/service/$service/$version">""")
  }
}
