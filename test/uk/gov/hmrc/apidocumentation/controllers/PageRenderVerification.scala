/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.apidocumentation.controllers.utils

import play.api.http.Status._
import play.api.mvc._
import uk.gov.hmrc.apidocumentation.controllers.{routes, CommonControllerBaseSpec}
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.apidocumentation.mocks.services.NavigationServiceMock

import scala.concurrent.Future

trait PageRenderVerification {
  self: CommonControllerBaseSpec =>

  import NavigationServiceMock.{navLink, sidebarLink}

  lazy val homeBreadcrumb = Crumb("Home", routes.DocumentationController.indexPage().url)
  lazy val apiDocsBreadcrumb = Crumb("API Documentation", routes.ApiDocumentationController.apiIndexPage(None, None, None).url)

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

  //TODO: Uncomment the below once all the bread crumbs are replaced with the new styles (breadcrumbs2 - > breadcrumbs)
//  def verifyBreadcrumbRendered(actualPage: Result, crumb: Crumb) {
//    bodyOf(actualPage) should include(s""" <li class="govuk-breadcrumbs__list-item"><a class="govuk-breadcrumbs__link" href="@crumb.url">@crumb.name</a></li>""")
//  }

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

    sideNavLinkIsRendered(actualPage, sidebarLink) shouldBe sideNavLinkRendered
    subNavIsRendered(actualPage) shouldBe subNavRendered
//TODO: Uncomment this once breadcrumbs2 has completely replaced breadcrumbs view
//  breadcrumbs.foreach(verifyBreadcrumbRendered(actualPage, _))
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
