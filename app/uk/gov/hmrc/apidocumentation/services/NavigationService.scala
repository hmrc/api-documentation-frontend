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

package uk.gov.hmrc.apidocumentation.services

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.connectors.DeveloperFrontendConnector
import uk.gov.hmrc.apidocumentation.controllers.routes
import uk.gov.hmrc.apidocumentation.models.{DocumentationItem, NavLink, SidebarLink}
import uk.gov.hmrc.apidocumentation.views.helpers._

@Singleton
class NavigationService @Inject() (
    connector: DeveloperFrontendConnector,
    appConfig: ApplicationConfig
  )(implicit ec: ExecutionContext
  ) {

  lazy val gettingStartedUrl =
    routes.DocumentationController.usingTheHubPage().url

  lazy val apiDocumentationUrl =
    routes.FilteredDocumentationIndexController.apiListIndexPage(List.empty, List.empty).url

  lazy val referenceGuideUrl =
    routes.DocumentationController.referenceGuidePage().url

  lazy val namingGuidelinesUrl =
    routes.DocumentationController.nameGuidelinesPage().url

  lazy val authorisationUri =
    routes.AuthorisationController.authorisationPage().url
  lazy val tutorialsUri     = routes.DocumentationController.tutorialsPage().url
  lazy val termsOfUseUri    = routes.DocumentationController.termsOfUsePage().url
  lazy val testingUri       = routes.TestingPagesController.testingPage().url

  lazy val fraudPreventionPageUrl = "/guides/fraud-prevention"

  lazy val developmentPracticesUrl =
    routes.DocumentationController.developmentPracticesPage().url

  lazy val apiStatusesUrl =
    routes.DocumentationController.apiStatusesPage().url

  lazy val sidebarNavigationLinks = Seq(
    SidebarLink(
      label = "Getting started",
      href = gettingStartedUrl
    ),
    SidebarLink(
      label = "Application naming guidelines",
      href = namingGuidelinesUrl
    ),
    SidebarLink(
      label = "API statuses",
      href = apiStatusesUrl
    )
  ) ++
    openApiPreviewLink() ++
    Seq(
      SidebarLink(
        label = "Reference guide",
        href = referenceGuideUrl
      ),
      SidebarLink(
        label = "Development practices",
        href = developmentPracticesUrl
      ),
      SidebarLink(
        label = "Send fraud prevention data",
        href = fraudPreventionPageUrl
      ),
      SidebarLink(
        label = "Authorisation",
        href = authorisationUri
      ),
      SidebarLink(
        label = "Tutorials",
        href = tutorialsUri
      ),
      SidebarLink(
        label = "Testing in the sandbox",
        href = testingUri
      ),
      SidebarLink(
        label = "Terms of use",
        href = termsOfUseUri
      )
    )

  def sidebarNavigation() = sidebarNavigationLinks

  def openApiSidebarNavigation(markdownBlocks: List[DocumentationItem]): Seq[SidebarLink] = {
    val variableSidebar = markdownBlocks.map(mb => SidebarLink(label = mb.title, href = s"#${Slugify(mb.title)}"))
    val fixedSidebar    = SidebarLink(label = "Endpoints", href = s"#endpoints-title")

    variableSidebar :+ fixedSidebar
  }

  private def openApiPreviewLink() =
    if (appConfig.openApiPreviewEnabled) {
      Seq(
        SidebarLink(
          "Preview OpenAPI",
          routes.OpenApiDocumentationController.previewApiDocumentationPage().url
        )
      )
    } else {
      Seq.empty
    }

  def headerNavigation()(implicit hc: HeaderCarrier): Future[Seq[NavLink]] =
    connector.fetchNavLinks() map (navLinks => addUrlPrefix(appConfig.developerFrontendUrl, navLinks))

  private def addUrlPrefix(urlPrefix: String, links: Seq[NavLink]) =
    links map (link => link.copy(href = urlPrefix.concat(link.href)))
}
