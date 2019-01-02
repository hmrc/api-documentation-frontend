/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.apidocumentation

import javax.inject.Inject

import com.google.inject.{AbstractModule, Provides}
import play.api.mvc.EssentialFilter
import uk.gov.hmrc.apidocumentation.config._
import uk.gov.hmrc.ramltools.loaders.RamlLoader
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.http.metrics.{Metrics, PlayMetrics}

/**
  * This class is a Guice module that tells Guice how to bind several
  * different types. This Guice module is created when the Play
  * application starts.
  * Play will automatically use any class called `uk.gov.hmrc.apidocumentation.Module` that is in
  * the root package. You can create modules in other locations by
  * adding `play.modules.enabled` settings to the `application.conf`
  * configuration file.
  */
class Module extends AbstractModule {

  override def configure() = {
    bind(classOf[Metrics]).toInstance(PlayMetrics)
    bind(classOf[RamlLoader]).toInstance(DocumentationRamlLoader)
    bind(classOf[AuditConnector]).toInstance(ApiDocumentationFrontendAuditConnector)
  }

}
