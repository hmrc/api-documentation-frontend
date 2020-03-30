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

package uk.gov.hmrc.apidocumentation.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.apidocumentation.connectors.ApiDefinitionConnector
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.metrics.{API, Metrics}

import scala.concurrent.ExecutionContext

//import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait BaseApiDefinitionService {
  def fetchExtendedDefinition(serviceName: String, email: Option[String])
                            (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ExtendedAPIDefinition]]

  def fetchAllDefinitions(email: Option[String])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[APIDefinition]]
}

@Singleton
class ApiDefinitionService @Inject()(val raw: ApiDefinitionConnector,
                                     val metrics: Metrics
                                    ) extends BaseApiDefinitionService {
  val api: API = API("api-definition")

  def fetchExtendedDefinition(serviceName: String, email: Option[String] = None)
                            (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ExtendedAPIDefinition]] =
    metrics.record(api) {
      raw.fetchApiDefinition(serviceName, email)
    }

  def fetchAllDefinitions(email: Option[String] = None)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[APIDefinition]] =
    metrics.record(api) {
      raw.fetchAllApiDefinitions(email)
    }
}
