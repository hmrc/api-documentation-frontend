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
import uk.gov.hmrc.apidocumentation.models.apispecification.{ApiSpecification, DocumentationItem}
import uk.gov.hmrc.apidocumentation.models.{DocsVisibility, ExtendedAPIVersion, NavLink, SidebarLink}
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
    routes.ApiDocumentationController.apiIndexPage(None, None, None).url

  lazy val referenceGuideUrl =
    routes.DocumentationController.referenceGuidePage().url

  lazy val namingGuidelinesUrl =
    routes.DocumentationController.nameGuidelinesPage().url

  lazy val authorisationUri =
    routes.AuthorisationController.authorisationPage().url
  lazy val tutorialsUri     = routes.DocumentationController.tutorialsPage().url
  lazy val termsOfUseUri    = routes.DocumentationController.termsOfUsePage().url
  lazy val testingUri       = routes.TestingPagesController.testingPage().url

  lazy val mtdIntroductionPageUrl =
    routes.DocumentationController.mtdIntroductionPage().url
  lazy val fraudPreventionPageUrl = "/guides/fraud-prevention"

  lazy val developmentPracticesUrl =
    routes.DocumentationController.developmentPracticesPage().url

  lazy val sidebarNavigationLinks = Seq(
    SidebarLink(
      label = "Using the Developer Hub",
      href = gettingStartedUrl,
      subLinks = Seq(
        SidebarLink(
          label = "Application naming guidelines",
          href = namingGuidelinesUrl
        )
      )
    ),
    SidebarLink(
      label = "API documentation",
      href = apiDocumentationUrl
    )
  ) ++
    ramlPreviewLink() ++
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
      ),
      SidebarLink(
        label = "Making Tax Digital guides",
        href = mtdIntroductionPageUrl
      )
    )

  def sidebarNavigation() = sidebarNavigationLinks

  def apiSidebarNavigation2(service: String, version: ExtendedAPIVersion, apiSpecification: ApiSpecification): Seq[SidebarLink] = {
    val subLinks = apiSpecification.resourceGroups
      .map(group => group.name)
      .filter(_.nonEmpty)
      .flatten
      .map(name => SidebarLink(label = name, href = s"#${Slugify(name)}"))

    val sections = apiSpecification.documentationForVersionFilteredByVisibility(version).map { doc =>
      SidebarLink(label = doc.title, href = s"#${Slugify(doc.title)}")
    }

    val resources =
      if (VersionDocsVisible(version.visibility) == DocsVisibility.OVERVIEW_ONLY) {
        SidebarLink(
          label = "Read more",
          href = "#read-more"
        )
      } else {
        SidebarLink(
          label = "Endpoints",
          href = "#endpoints",
          subLinks = subLinks,
          showSubLinks = true
        )
      }

    sections :+ resources
  }

  def openApiSidebarNavigation(service: String, version: ExtendedAPIVersion, markdownBlocks: List[DocumentationItem]): Seq[SidebarLink] = {
    val variableSidebar = markdownBlocks.map(mb => SidebarLink(label = mb.title, href = s"#${Slugify(mb.title)}"))
    val fixedSidebar    = SidebarLink(label = "Endpoints", href = s"#endpoints-title")

    variableSidebar :+ fixedSidebar
  }

  private def ramlPreviewLink() =
    if (appConfig.ramlPreviewEnabled) {
      Seq(SidebarLink(
        "Preview RAML",
        routes.ApiDocumentationController.previewApiDocumentation(None).url
      ))
    } else {
      Seq.empty
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
