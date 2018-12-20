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

import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{EssentialFilter, Request}
import play.api.{Application, Configuration, Play}
import play.twirl.api.Html
import uk.gov.hmrc.ramltools.loaders.{UrlRewriter, UrlRewritingRamlLoader}
import uk.gov.hmrc.apidocumentation.{LoginFilter, SessionRedirectFilter, views}
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{AppName, ControllerConfig, RunMode}
import uk.gov.hmrc.play.frontend.bootstrap.DefaultFrontendGlobal
import uk.gov.hmrc.play.frontend.config.LoadAuditingConfig
import uk.gov.hmrc.play.frontend.filters.{FrontendAuditFilter, FrontendLoggingFilter, MicroserviceFilterSupport}

object ApplicationGlobal extends DefaultFrontendGlobal with RunMode {

  override val auditConnector = ApiDocumentationFrontendAuditConnector
  override val loggingFilter = ApiDocumentationFrontendLoggingFilter
  override val frontendAuditFilter = ApiDocumentationAuditFilter
  lazy implicit val appConfig = new ApplicationConfig(Play.current.configuration)

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = app.configuration.getConfig(s"$env.microservice.metrics")

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html =
    views.html.errorTemplate(pageTitle, heading, message)

  override def onStart(app: Application) {
    super.onStart(app)
    ApplicationCrypto.verifyConfiguration()
  }

  override def frontendFilters: Seq[EssentialFilter] = defaultFrontendFilters :+ Play.current.injector.instanceOf[SessionRedirectFilter]
}

object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs = Play.current.configuration.underlying.getConfig("controllers")
}

object ApiDocumentationFrontendLoggingFilter extends FrontendLoggingFilter with MicroserviceFilterSupport {
  override def controllerNeedsLogging(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsLogging
}

object ApiDocumentationAuditFilter extends FrontendAuditFilter with RunMode with AppName with MicroserviceFilterSupport {
  override lazy val maskedFormFields = Seq("password")
  override lazy val applicationPort = None
  override lazy val auditConnector = ApiDocumentationFrontendAuditConnector

  override def controllerNeedsAuditing(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsAuditing
}

object ApiDocumentationFrontendAuditConnector extends AuditConnector with AppName with RunMode {
  override lazy val auditingConfig = LoadAuditingConfig(s"$env.auditing")
}

object DocumentationUrlRewriter extends UrlRewriter {
  lazy val rewrites = ApplicationGlobal.appConfig.ramlLoaderRewrites
}

object DocumentationRamlLoader extends UrlRewritingRamlLoader(DocumentationUrlRewriter)
