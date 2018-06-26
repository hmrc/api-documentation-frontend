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

package uk.gov.hmrc.apidocumentation.services

import javax.inject.Inject

import org.raml.v2.api.model.v10.resources.Resource
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.connectors.DeveloperFrontendConnector
import uk.gov.hmrc.apidocumentation.controllers.routes
import uk.gov.hmrc.apidocumentation.models.{NavLink, SidebarLink}
import uk.gov.hmrc.apidocumentation.views.helpers._
import uk.gov.hmrc.http.HeaderCarrier

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class NavigationService @Inject()(connector: DeveloperFrontendConnector, appConfig: ApplicationConfig) {

  val gettingStartedUrl = routes.DocumentationController.usingTheHubPage().url
  val usingTheSandboxUrl = routes.DocumentationController.sandboxIntroductionPage().url
  val apiDocumentationUrl = routes.DocumentationController.apiIndexPage(None, None).url
  val referenceGuideUrl = routes.DocumentationController.referenceGuidePage().url
  val namingGuidelinesUrl = routes.DocumentationController.nameGuidelinesPage().url
  val authorisationUri =routes.DocumentationController.authorisationPage().url
  val tutorialsUri = routes.DocumentationController.tutorialsPage().url
  val termsOfUseUri = routes.DocumentationController.termsOfUsePage().url
  val testingUri = routes.DocumentationController.testingPage().url
  val mtdIntroductionPageUrl = routes.DocumentationController.mtdIntroductionPage().url

  val sidebarNavigationLinks = Seq(
    SidebarLink(label = s"Using the ${appConfig.title}", href = gettingStartedUrl,
      subLinks = Seq(SidebarLink(label = "Application naming guidelines", href = namingGuidelinesUrl))),
    SidebarLink(label = "Authorisation", href = authorisationUri),
    SidebarLink(label = "Tutorials", href = tutorialsUri),
    SidebarLink(label = "API documentation", href = apiDocumentationUrl, subLinks = previewSublinks()),
    SidebarLink(label = "Testing in the sandbox", href = testingUri),
    SidebarLink(label = "Reference guide", href = referenceGuideUrl),
    SidebarLink(label = "Terms of use", href = termsOfUseUri),
    SidebarLink(label = "Making Tax Digital guides", href = mtdIntroductionPageUrl)
  )

  def sidebarNavigationLinksForET(url: String) = Seq(
    SidebarLink(label = "Using the Sandbox", href = url),
    SidebarLink(label = "API documentation", href = apiDocumentationUrl),
    SidebarLink(label = "Reference guide", href = referenceGuideUrl),
    SidebarLink(label = "Making Tax Digital guide", href = mtdIntroductionPageUrl)
  )

  def sidebarNavigation() =
    if (appConfig.isExternalTestEnvironment) sidebarNavigationLinksForET(usingTheSandboxUrl) else sidebarNavigationLinks


  private def traverse(resources: Seq[Resource], accum: Seq[SidebarLink] = Seq.empty): Seq[SidebarLink] = {
    if (resources.isEmpty)
      accum
    else {
      (for {
        res <- resources
        ann = Annotation.getAnnotation(res, "(group)", "name")
        link = ann.map(name => SidebarLink(label = name, href = s"#${Slugify(name)}"))
      } yield link ++: traverse(res.resources.asScala, accum)).flatten
    }
  }

  def apiSidebarNavigation(service: String, version: String, raml: RAML): Seq[SidebarLink] = {
    val sections = raml.documentation.asScala.map { doc =>
      SidebarLink(label = doc.title.value, href = s"#${Slugify(doc.title.value)}")
    }

    val resources = SidebarLink(
      label = "Resources",
      href = s"#resources",
      subLinks = traverse(raml.resources.asScala),
      showSubLinks = true)

    sections :+ resources
  }

  private def previewSublinks() = {
    if (appConfig.ramlPreviewEnabled) {
      Seq(SidebarLink("Preview RAML", routes.DocumentationController.previewApiDocumentation(None).url))
    } else {
      Seq.empty
    }
  }

  def headerNavigation()(implicit hc: HeaderCarrier): Future[Seq[NavLink]] =
    connector.fetchNavLinks() map (navLinks => addUrlPrefix(appConfig.developerFrontendUrl, navLinks))

  private def addUrlPrefix(urlPrefix: String, links: Seq[NavLink]) =
    links map (link => link.copy(href = urlPrefix.concat(link.href)))
}

