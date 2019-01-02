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

package uk.gov.hmrc.apidocumentation.connectors

import javax.inject.Inject

import uk.gov.hmrc.apidocumentation.config.WSHttp
import uk.gov.hmrc.apidocumentation.models.JsonFormatters._
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.metrics.{API, Metrics}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class APIDocumentationConnector @Inject()(http: WSHttp, metrics: Metrics) extends ServicesConfig {

  val api = API("api-documentation")
  val serviceBaseUrl = baseUrl("api-documentation")

  def fetchExtendedDefinitionByServiceName(serviceName: String)(implicit hc: HeaderCarrier): Future[ExtendedAPIDefinition] = metrics.record(api) {
    http.GET[ExtendedAPIDefinition](s"$serviceBaseUrl/apis/$serviceName/definition")
  }

  def fetchExtendedDefinitionByServiceNameAndEmail(serviceName: String, email: String)
                                                  (implicit hc: HeaderCarrier): Future[ExtendedAPIDefinition] = metrics.record(api) {
    http.GET[ExtendedAPIDefinition](s"$serviceBaseUrl/apis/$serviceName/definition", Seq("email" -> email))
  }

  def fetchAll()(implicit hc: HeaderCarrier): Future[Seq[APIDefinition]] = metrics.record(api) {
    http.GET[Seq[APIDefinition]](s"$serviceBaseUrl/apis/definition").map(list => list.sortBy(_.name))
  }

  def fetchByEmail(email: String)(implicit hc: HeaderCarrier): Future[Seq[APIDefinition]] = metrics.record(api) {
    http.GET[Seq[APIDefinition]](s"$serviceBaseUrl/apis/definition", Seq("email" -> email))
      .map(list => list.sortBy(_.name))
  }
}

