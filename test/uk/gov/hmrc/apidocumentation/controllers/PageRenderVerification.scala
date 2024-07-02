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

package uk.gov.hmrc.apidocumentation.controllers.utils

import scala.concurrent.Future

import play.api.mvc._
import play.api.test.Helpers._
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ServiceName
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApiVersionNbr

import uk.gov.hmrc.apidocumentation.controllers.{CommonControllerBaseSpec, routes}
import uk.gov.hmrc.apidocumentation.mocks.services.NavigationServiceMock
import uk.gov.hmrc.apidocumentation.models._

trait PageRenderVerification {
  self: CommonControllerBaseSpec =>

  import NavigationServiceMock.sidebarLink

  lazy val homeBreadcrumb    = Crumb("Home", routes.DocumentationController.indexPage().url)
  lazy val apiDocsBreadcrumb = Crumb("API Documentation", routes.ApiDocumentationController.apiIndexPage(None, None, None).url)

  lazy val apiDocsV2Breadcrumb = Crumb(
    "API Documentation",
    uk.gov.hmrc.apidocumentation.v2.controllers.routes.FilteredDocumentationIndexController.apiListIndexPage(List.empty, List.empty).url
  )

  def titleOf(result: Future[Result]) = {
    val titleRegEx = """<title[^>]*>(.*)</title>""".r
    val title      = titleRegEx.findFirstMatchIn(contentAsString(result)).map(_.group(1))
    title.isDefined shouldBe true
    title.get
  }

  def subNavIsRendered(result: Future[Result]) = {
    contentAsString(result).contains("<ul class=\"side-nav side-nav--child\">")
  }

  def sideNavLinkIsRendered(result: Future[Result], sidebarLink: SidebarLink) = {
    contentAsString(result).contains(s"""<a href="${sidebarLink.href}" class="side-nav__link">${sidebarLink.label}</a>""")
  }

  def userNavLinkIsRendered(result: Future[Result], navLink: NavLink) = {
    contentAsString(result).contains(navLink.href) && contentAsString(result).contains(navLink.label)
  }

  def versionOptionIsRendered(result: Future[Result], version: String, displayedStatus: String): Boolean = {
    contentAsString(result).contains(s"""<option selected value="$version" aria-label="Select to view documentation for v$version ($displayedStatus)">""")
  }

  def verifyBreadcrumbRendered(actualPage: Future[Result], crumb: Crumb): Unit = {
    contentAsString(actualPage) should include(s"""
                                                  |                    <li class="govuk-breadcrumbs__list-item">
                                                  |                        <a class="govuk-breadcrumbs__link" href="${crumb.url}">${crumb.name}</a>
                                                  |                    </li>""".stripMargin)
  }

  def verifyPageRendered(
      expectedTitle: String,
      breadcrumbs: List[Crumb] = List(homeBreadcrumb),
      sideNavLinkRendered: Boolean = true,
      subNavRendered: Boolean = false,
      bodyContains: Seq[String] = Seq.empty
    )(
      actualPage: Future[Result]
    ): Unit = {
    status(actualPage) shouldBe 200
    titleOf(actualPage) shouldBe expectedTitle

    sideNavLinkIsRendered(actualPage, sidebarLink) shouldBe sideNavLinkRendered
    subNavIsRendered(actualPage) shouldBe subNavRendered
    breadcrumbs.foreach(verifyBreadcrumbRendered(actualPage, _))
    bodyContains.foreach { snippet => contentAsString(actualPage) should include(snippet) }
  }

  def verifyNotFoundPageRendered(actualPage: Future[Result]): Unit = {
    status(actualPage) shouldBe NOT_FOUND
  }

  def verifyErrorPageRendered(expectedStatus: Int, expectedError: String)(actualPage: Future[Result]): Unit = {
    status(actualPage) shouldBe expectedStatus
    contentAsString(actualPage) should include(expectedError)
  }

  def verifyApiDocumentationPageRendered(actualPage: Future[Result]): Unit = {
    verifyPageRendered(pageTitle("Hello World"), breadcrumbs = List(homeBreadcrumb, apiDocsBreadcrumb))(actualPage)
  }

  def verifyLinkToStableDocumentationRendered(actualPage: Future[Result], service: ServiceName, version: ApiVersionNbr) = {
    contentAsString(actualPage) should include(s"""<a href="/api-documentation/docs/api/service/$service/$version">""")
  }
}
