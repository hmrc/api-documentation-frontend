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

package uk.gov.hmrc.apidocumentation.services

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.*
import scala.concurrent.Future

import play.api.cache.*
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.{ApiDefinition, ExtendedApiDefinition, ServiceName}
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apidocumentation.connectors.ApiPlatformMicroserviceConnector
import uk.gov.hmrc.apidocumentation.models.*
import uk.gov.hmrc.apidocumentation.util.ApplicationLogger

trait BaseApiDefinitionService {
  def fetchExtendedDefinition(serviceName: ServiceName, developerId: Option[DeveloperIdentifier])(implicit hc: HeaderCarrier): Future[Option[ExtendedApiDefinition]]

  def fetchAllDefinitions(developerId: Option[DeveloperIdentifier])(implicit hc: HeaderCarrier): Future[Seq[ApiDefinition]]
}

@Singleton
class ApiDefinitionService @Inject() (
    cache: AsyncCacheApi,
    apiPlatformMicroserviceConnector: ApiPlatformMicroserviceConnector
  ) extends BaseApiDefinitionService with ApplicationLogger {
  val cacheExpiry: FiniteDuration = 5 seconds

  def fetchExtendedDefinition(serviceName: ServiceName, developerId: Option[DeveloperIdentifier] = None)(implicit hc: HeaderCarrier): Future[Option[ExtendedApiDefinition]] = {
    val key = s"${serviceName}---${developerId.map(_.asText).getOrElse("NONE")}"

    cache.getOrElseUpdate(key, cacheExpiry) {
      logger.info(s"Extended definition for $serviceName for $developerId not found in cache")
      apiPlatformMicroserviceConnector.fetchExtendedApiDefinition(serviceName, developerId)(using hc)
    }
  }

  def fetchAllDefinitions(developerId: Option[DeveloperIdentifier] = None)(implicit hc: HeaderCarrier): Future[Seq[ApiDefinition]] =
    apiPlatformMicroserviceConnector.fetchApiDefinitionsByCollaborator(developerId)
}
