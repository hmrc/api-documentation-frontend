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

package uk.gov.hmrc.apidocumentation.config

import javax.inject.Inject

import play.api.Configuration
import uk.gov.hmrc.play.config.ServicesConfig

class ApplicationConfig @Inject()(config: Configuration) extends ServicesConfig {

  private def loadConfig(key: String) = config.getString(key).getOrElse(throw new Exception(s"Missing key: $key"))

  val contactFormServiceIdentifier = "API"
  val contactPath = config.getString(s"$env.contactPath").getOrElse("")

  lazy val hotjarId = config.getInt(s"$env.hotjar.id")
  lazy val hotjarEnabled = config.getBoolean(s"$env.features.hotjar")
  lazy val analyticsToken = config.getString(s"$env.google-analytics.token")
  lazy val analyticsHost = config.getString(s"$env.google-analytics.host").getOrElse("auto")
  lazy val betaFeedbackUrl = "/contact/beta-feedback"
  lazy val betaFeedbackUnauthenticatedUrl: String = "/contact/beta-feedback-unauthenticated"
  lazy val developerFrontendUrl = config.getString(s"$env.developer-frontend-url").getOrElse("")
  lazy val reportAProblemPartialUrl = s"$contactPath/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  lazy val reportAProblemNonJSUrl = s"$contactPath/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  lazy val thirdPartyDeveloperUrl = baseUrl("third-party-developer")
  lazy val securedCookie = config.getBoolean(s"$env.cookie.secure").getOrElse(true)
  lazy val ramlPreviewEnabled = config.getBoolean(s"$env.features.ramlPreview").getOrElse(false)
  lazy val ramlLoaderRewrites = buildRamlLoaderRewrites(config)
  lazy val isExternalTestEnvironment = config.getBoolean("isExternalTestEnvironment").getOrElse(false)
  lazy val showProductionAvailability = config.getBoolean(s"$env.features.showProductionAvailability").getOrElse(false)
  lazy val showSandboxAvailability = config.getBoolean(s"$env.features.showSandboxAvailability").getOrElse(false)
  lazy val title = if (isExternalTestEnvironment) "Developer Sandbox" else "Developer Hub"
  lazy val isStubMode = env == "Stub"
  lazy val apiUrl = buildUrl("platform.api")

  private def buildRamlLoaderRewrites(config: Configuration): Map[String, String] = {
    Map(config.getString(s"$env.ramlLoaderUrlRewrite.from").getOrElse("") ->
      config.getString(s"$env.ramlLoaderUrlRewrite.to").getOrElse(""))
  }

  private def buildUrl(key: String) = {
    (config.getString(s"$env.$key.protocol"), config.getString(s"$env.$key.host")) match {
      case (Some(protocol), Some(host)) => s"$protocol://$host"
      case (None, Some(host)) => s"https://$host"
      case _ => ""
    }
  }
}
