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

package uk.gov.hmrc.apidocumentation.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.connectors.{LocalRawApiDefinitionConnector, RawApiDefinitionConnector, RemoteRawApiDefinitionConnector}
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.metrics.{API, Metrics}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.successful


// TODO  - flatten to two methods
trait BaseApiDefinitionService {
  def fetchExtendedDefinitionByServiceName(serviceName: String)(implicit hc: HeaderCarrier): Future[Option[ExtendedAPIDefinition]]

  def fetchExtendedDefinitionByServiceNameAndEmail(serviceName: String, email: String)
                                                  (implicit hc: HeaderCarrier): Future[Option[ExtendedAPIDefinition]]

  def fetchAll()(implicit hc: HeaderCarrier): Future[Seq[APIDefinition]]

  def fetchByEmail(email: String)(implicit hc: HeaderCarrier): Future[Seq[APIDefinition]]
}

// TODO  - flatten to two methods
trait ConnectedApiDefinitionService extends BaseApiDefinitionService {
  def raw: RawApiDefinitionConnector
  def metrics: Metrics

  def api: API
  def enabled: Boolean

  def fetchExtendedDefinitionByServiceName(serviceName: String)(implicit hc: HeaderCarrier): Future[Option[ExtendedAPIDefinition]] =
    if (enabled) {
      metrics.record(api) {
        raw.fetchApiDefinition(serviceName, None)
      }
    } else {
      successful(None)
    }

  def fetchExtendedDefinitionByServiceNameAndEmail(serviceName: String, email: String)
                                                  (implicit hc: HeaderCarrier): Future[Option[ExtendedAPIDefinition]] =
    if (enabled) {
      metrics.record(api) {
        raw.fetchApiDefinition(serviceName, Some(email))
      }
    } else {
      successful(None)
    }

  def fetchAll()(implicit hc: HeaderCarrier): Future[Seq[APIDefinition]] =
    if (enabled) {
      metrics.record(api) {
        raw.fetchApiDefinitions(None)
      }
    } else {
      successful(Seq.empty)
    }

  def fetchByEmail(email: String)(implicit hc: HeaderCarrier): Future[Seq[APIDefinition]] =
    if (enabled) {
      metrics.record(api) {
        raw.fetchApiDefinitions(Some(email))
      }
    } else {
      successful(Seq.empty)
    }
}

@Singleton
class LocalApiDefinitionService @Inject()(
      val raw: LocalRawApiDefinitionConnector,
      val metrics: Metrics
   ) extends ConnectedApiDefinitionService {

  val api: API = API("local-api-definition")

  val enabled: Boolean = true
}

@Singleton
class RemoteApiDefinitionService @Inject()(
     val raw: RemoteRawApiDefinitionConnector,
     val appConfig: ApplicationConfig,
     val metrics: Metrics
   ) extends ConnectedApiDefinitionService {

  val api: API = API("remote-api-definition")

  val enabled: Boolean = appConfig.getConfBool("remote-api-definition.enabled", false)
}