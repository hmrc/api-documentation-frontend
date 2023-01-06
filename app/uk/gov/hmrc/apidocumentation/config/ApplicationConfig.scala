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

package uk.gov.hmrc.apidocumentation.config

import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.{ConfigLoader, Configuration}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@ImplementedBy(classOf[ApplicationConfigImpl])
trait ApplicationConfig {
  def developerFrontendUrl: String

  def developerFrontendBaseUrl: String
  def thirdPartyDeveloperUrl: String
  def apiPlatformMicroserviceBaseUrl: String
  def ramlPreviewMicroserviceBaseUrl: String

  def securedCookie: Boolean
  def ramlPreviewEnabled: Boolean
  def openApiPreviewEnabled: Boolean

  def showProductionAvailability: Boolean
  def showSandboxAvailability: Boolean

  def productionWwwHost: String
  def productionApiBaseUrl: String

  def sandboxApiBaseUrl: String
  def sandboxWwwBaseUrl: String

  def documentationRenderVersion: String

  def nameOfPrincipalEnvironment: String
  def nameOfSubordinateEnvironment: String
  def principalBaseUrl: String
  def subordinateBaseUrl: String

  def title: String
  def xmlApiBaseUrl: String
  def feedbackSurveyUrl: String
}

@Singleton
class ApplicationConfigImpl @Inject() (config: Configuration)
    extends ServicesConfig(config)
    with ApplicationConfig {

  def getConfigDefaulted[A](key: String, default: A)(implicit loader: ConfigLoader[A]) = config.getOptional[A](key)(loader).getOrElse(default)

  val developerFrontendUrl = getString("developer-frontend-url")

  val developerFrontendBaseUrl = baseUrl("developer-frontend")
  val thirdPartyDeveloperUrl   = baseUrl("third-party-developer")

  /** This value needs to be lazy because it doesn't actually exist in all environments that we deploy to. Specifically, it doesn't exist in Development which really shouldn't need
    * this app deployed but does due to api-publisher needing it.
    *
    * DO NOT REMOVE
    */
  lazy val apiPlatformMicroserviceBaseUrl = baseUrl("api-platform-microservice")
  lazy val ramlPreviewMicroserviceBaseUrl = baseUrl("raml-preview-microservice")

  val securedCookie         = getBoolean("cookie.secure")
  val ramlPreviewEnabled    = getBoolean("features.ramlPreview")
  val openApiPreviewEnabled = getBoolean("features.openApiPreview")

  val showProductionAvailability = getBoolean("features.showProductionAvailability")
  val showSandboxAvailability    = getBoolean("features.showSandboxAvailability")
  val productionWwwHost          = getString("platform.production.www.host")
  val productionApiBaseUrl       = platformBaseUrl("platform.production.api")

  val sandboxApiBaseUrl = platformBaseUrl("platform.sandbox.api")
  val sandboxWwwBaseUrl = platformBaseUrl("platform.sandbox.www")

  val documentationRenderVersion = getString("features.documentationRenderVersion")

  val nameOfPrincipalEnvironment   = getString("features.nameOfPrincipalEnvironment")
  val nameOfSubordinateEnvironment = getString("features.nameOfSubordinateEnvironment")
  val principalBaseUrl             = getString("features.principalBaseUrl")
  val subordinateBaseUrl           = getString("features.subordinateBaseUrl")

  val title         = "HMRC Developer Hub"
  val xmlApiBaseUrl = getString("xml-api.base-url")

  val feedbackSurveyUrl: String = getString("feedbackBanner.generic.surveyUrl")

  private def platformBaseUrl(key: String) = {
    (getConfigDefaulted(s"$key.protocol", ""), getConfigDefaulted(s"$key.host", "")) match {
      case (p, h) if !p.isEmpty && !h.isEmpty => s"$p://$h"
      case (p, h) if p.isEmpty                => s"https://$h"
      case _                                  => ""
    }
  }

}
