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

import javax.inject.Inject
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.config.ServicesConfig

class ApplicationConfig @Inject()(override val runModeConfiguration: Configuration, environment: Environment) extends ServicesConfig {

  override protected def mode = environment.mode

  private def loadConfig(key: String) = runModeConfiguration.getString(key).getOrElse(throw new Exception(s"Missing key: $key"))

  val contactFormServiceIdentifier = "API"
  val contactPath = runModeConfiguration.getString(s"$env.contactPath").getOrElse("")

  lazy val analyticsToken = runModeConfiguration.getString(s"$env.google-analytics.token")
  lazy val analyticsHost = runModeConfiguration.getString(s"$env.google-analytics.host").getOrElse("auto")

  lazy val developerFrontendUrl = runModeConfiguration.getString(s"$env.developer-frontend-url").getOrElse("")

  lazy val reportAProblemPartialUrl = s"$contactPath/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  lazy val reportAProblemNonJSUrl = s"$contactPath/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"

  lazy val developerFrontendBaseUrl = baseUrl("developer-frontend")
  lazy val thirdPartyDeveloperUrl = baseUrl("third-party-developer")
  lazy val securedCookie = runModeConfiguration.getBoolean(s"$env.cookie.secure").getOrElse(true)
  lazy val ramlPreviewEnabled = runModeConfiguration.getBoolean(s"$env.features.ramlPreview").getOrElse(false)
  lazy val ramlLoaderRewrites = buildRamlLoaderRewrites
  lazy val showProductionAvailability = runModeConfiguration.getBoolean(s"$env.features.showProductionAvailability").getOrElse(false)
  lazy val showSandboxAvailability = runModeConfiguration.getBoolean(s"$env.features.showSandboxAvailability").getOrElse(false)
  lazy val productionApiHost = runModeConfiguration.getString("platform.production.api.host")
  lazy val productionWwwHost = runModeConfiguration.getString("platform.production.www.host")
  lazy val productionApiBaseUrl = platformBaseUrl("platform.production.api")

  lazy val sandboxApiHost = runModeConfiguration.getString("platform.sandbox.api.host")
  lazy val sandboxWwwHost = runModeConfiguration.getString("platform.sandbox.www.host")
  lazy val sandboxApiBaseUrl = platformBaseUrl("platform.sandbox.api")
  lazy val sandboxWwwBaseUrl = platformBaseUrl("platform.sandbox.www")

  lazy val title = "HMRC Developer Hub"
  lazy val isStubMode = env == "Stub"
  lazy val xmlApiBaseUrl = runModeConfiguration.getString(s"$env.xml-api.base-url").getOrElse("https://www.gov.uk")

  lazy val apiDefinitionBaseUrl = baseUrl("api-definition")

  private def buildRamlLoaderRewrites: Map[String, String] = {
    Map(runModeConfiguration.getString(s"$env.ramlLoaderUrlRewrite.from").getOrElse("") ->
      runModeConfiguration.getString(s"$env.ramlLoaderUrlRewrite.to").getOrElse(""))
  }

  private def platformBaseUrl(key: String) = {
    (runModeConfiguration.getString(s"$key.protocol"), runModeConfiguration.getString(s"$key.host")) match {
      case (Some(protocol), Some(host)) => s"$protocol://$host"
      case (None, Some(host)) => s"https://$host"
      case _ => ""
    }
  }
}
