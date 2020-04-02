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
import play.api.{Configuration, ConfigLoader}
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

class ApplicationConfigImpl @Inject()(config: Configuration, runMode: RunMode)
    extends ServicesConfig(config, runMode)
    with ApplicationConfig {

  val env = runMode.env

  def getConfigDefaulted[A](key: String, default: A)(implicit loader: ConfigLoader[A]) = config.getOptional[A](key)(loader).getOrElse(default)

  val contactFormServiceIdentifier = "API"
  val contactPath = getConfigDefaulted(s"$env.contactPath", "")

  val analyticsToken = config.getOptional[String](s"$env.google-analytics.token").filterNot(_ == "")

  val analyticsHost = getConfigDefaulted(s"$env.google-analytics.host", "auto")

  val developerFrontendUrl = getConfigDefaulted(s"$env.developer-frontend-url", "")

  val reportAProblemPartialUrl = s"$contactPath/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  val reportAProblemNonJSUrl = s"$contactPath/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"

  val developerFrontendBaseUrl = baseUrl("developer-frontend")
  val thirdPartyDeveloperUrl = baseUrl("third-party-developer")

  /**
   * This value needs to be lazy because it doesn't actually exist in all environments that we deploy to.
   * Specifically, it doesn't exist in Development which really shouldn't need this app deployed but does due
   * to api-publisher needing it.
   *
   * DO NOT REMOVE
   */
  lazy val apiDefinitionBaseUrl = baseUrl("api-definition")

  val securedCookie = getConfigDefaulted(s"$env.cookie.secure", true)
  val ramlPreviewEnabled = getConfigDefaulted(s"$env.features.ramlPreview", false)
  val ramlLoaderRewrites = buildRamlLoaderRewrites
  val showProductionAvailability = getConfigDefaulted(s"$env.features.showProductionAvailability", false)
  val showSandboxAvailability = getConfigDefaulted(s"$env.features.showSandboxAvailability", false)
  val productionApiHost = getString("platform.production.api.host")
  val productionWwwHost = getString("platform.production.www.host")
  val productionApiBaseUrl = platformBaseUrl("platform.production.api")

  val sandboxApiHost = getString("platform.sandbox.api.host")
  val sandboxWwwHost = getString("platform.sandbox.www.host")
  val sandboxApiBaseUrl = platformBaseUrl("platform.sandbox.api")
  val sandboxWwwBaseUrl = platformBaseUrl("platform.sandbox.www")

  val title = "HMRC Developer Hub"
  val isStubMode = env == "Stub"
  val xmlApiBaseUrl = getConfigDefaulted(s"$env.xml-api.base-url", "https://www.gov.uk")

  private def buildRamlLoaderRewrites: Map[String, String] = {
    Map(
      getConfigDefaulted(s"$env.ramlLoaderUrlRewrite.from", "") ->
      getConfigDefaulted(s"$env.ramlLoaderUrlRewrite.to", "")
    )
  }

  private def platformBaseUrl(key: String) = {
    (getConfigDefaulted(s"$key.protocol", ""), getConfigDefaulted(s"$key.host", "")) match {
      case (p, h) if !p.isEmpty && !h.isEmpty => s"$p://$h"
      case (p, h) if p.isEmpty => s"https://$h"
      case _ => ""
    }
  }
}
