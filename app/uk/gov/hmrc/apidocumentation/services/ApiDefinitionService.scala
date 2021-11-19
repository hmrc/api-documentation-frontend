/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.apidocumentation.connectors.ApiPlatformMicroserviceConnector
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.metrics.common._

import scala.concurrent.{ExecutionContext, Future}

trait BaseApiDefinitionService {
  def fetchExtendedDefinition(serviceName: String, developerId: Option[DeveloperIdentifier])
                             (implicit hc: HeaderCarrier): Future[Option[ExtendedAPIDefinition]]

  def fetchAllDefinitions(developerId: Option[DeveloperIdentifier])(implicit hc: HeaderCarrier): Future[Seq[APIDefinition]]
}

@Singleton
class ApiDefinitionService @Inject()(val apiPlatformMicroserviceConnector: ApiPlatformMicroserviceConnector, val apiMetrics: ApiMetrics)
                                    (implicit ec: ExecutionContext) extends BaseApiDefinitionService with RecordMetrics {
  val api: API = API("api-definition")

  def fetchExtendedDefinition(serviceName: String, developerId: Option[DeveloperIdentifier] = None)
                            (implicit hc: HeaderCarrier): Future[Option[ExtendedAPIDefinition]] =
    record {
      apiPlatformMicroserviceConnector.fetchApiDefinition(serviceName, developerId)
    }

  def fetchAllDefinitions(developerId: Option[DeveloperIdentifier] = None)(implicit hc: HeaderCarrier): Future[Seq[APIDefinition]] =
    record {
        apiPlatformMicroserviceConnector.fetchApiDefinitionsByCollaborator(developerId)
    }
}
