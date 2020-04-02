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

package uk.gov.hmrc.apidocumentation.config

import com.google.inject.ImplementedBy
import javax.inject.Inject
import uk.gov.hmrc.play.bootstrap.config.{RunMode, ServicesConfig}

@ImplementedBy(classOf[ApplicationConfigImpl])
trait ApplicationConfig {
  def contactFormServiceIdentifier: String
  def contactPath: String

  def analyticsToken: Option[String]
  def analyticsHost: String

  def developerFrontendUrl: String

  def reportAProblemPartialUrl: String
  def reportAProblemNonJSUrl: String

  def developerFrontendBaseUrl: String
  def thirdPartyDeveloperUrl: String
  def apiDefinitionBaseUrl: String

  def securedCookie: Boolean
  def ramlPreviewEnabled: Boolean

  def ramlLoaderRewrites: Map[String, String]

  def showProductionAvailability: Boolean
  def showSandboxAvailability: Boolean

  def productionApiHost: String
  def productionWwwHost: String
  def productionApiBaseUrl: String

  def sandboxApiHost: String
  def sandboxWwwHost: String
  def sandboxApiBaseUrl: String
  def sandboxWwwBaseUrl: String

  def title: String
  def isStubMode: Boolean
  def xmlApiBaseUrl: String
}

class ApplicationConfigImpl @Inject()(config: ServicesConfig, runMode: RunMode) extends ApplicationConfig {

  val contactFormServiceIdentifier = "API"
  val contactPath = config.getConfString("contactPath", "")

  val analyticsToken = config.getConfString("google-analytics.token", "") match {
    case s if !s.isEmpty => Some(s)
    case _ => None
  }
  val analyticsHost = config.getConfString("google-analytics.host", "auto")

  val developerFrontendUrl = config.getConfString("developer-frontend-url", "")

  val reportAProblemPartialUrl = s"$contactPath/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  val reportAProblemNonJSUrl = s"$contactPath/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"

  val developerFrontendBaseUrl = config.baseUrl("developer-frontend")
  val thirdPartyDeveloperUrl = config.baseUrl("third-party-developer")
  val securedCookie = config.getConfBool("cookie.secure", true)
  val ramlPreviewEnabled = config.getConfBool("features.ramlPreview", false)
  val ramlLoaderRewrites = buildRamlLoaderRewrites
  val showProductionAvailability = config.getConfBool("features.showProductionAvailability", false)
  val showSandboxAvailability = config.getConfBool("features.showSandboxAvailability", false)
  val productionApiHost = config.getString("platform.production.api.host")
  val productionWwwHost = config.getString("platform.production.www.host")
  val productionApiBaseUrl = platformBaseUrl("platform.production.api")

  val sandboxApiHost = config.getString("platform.sandbox.api.host")
  val sandboxWwwHost = config.getString("platform.sandbox.www.host")
  val sandboxApiBaseUrl = platformBaseUrl("platform.sandbox.api")
  val sandboxWwwBaseUrl = platformBaseUrl("platform.sandbox.www")

  val title = "HMRC Developer Hub"
  val isStubMode = runMode.env == "Stub"
  val xmlApiBaseUrl = config.getConfString("xml-api.base-url", "https://www.gov.uk")

  val apiDefinitionBaseUrl = config.baseUrl("api-definition")

  private def buildRamlLoaderRewrites: Map[String, String] = {
    Map(config.getConfString("ramlLoaderUrlRewrite.from", "") ->
      config.getConfString("ramlLoaderUrlRewrite.to", ""))
  }

  private def platformBaseUrl(key: String) = {
    (config.getConfString(s"$key.protocol", ""), config.getConfString(s"$key.host", "")) match {
      case (p, h) if !p.isEmpty && !h.isEmpty => s"$p://$h"
      case (p, h) if p.isEmpty => s"https://$h"
      case _ => ""
    }
  }
}
