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

package uk.gov.hmrc.apidocumentation.connectors

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.connectors.ApiPlatformMicroserviceConnector.{definitionUrl, definitionsUrl, queryParams}
import uk.gov.hmrc.apidocumentation.models.DeveloperIdentifier
import uk.gov.hmrc.apidocumentation.util.ApplicationLogger

@Singleton
class ApiPlatformMicroserviceConnector @Inject() (val http: HttpClientV2, val appConfig: ApplicationConfig)(implicit val ec: ExecutionContext) extends ApplicationLogger {

  private lazy val serviceBaseUrl = appConfig.apiPlatformMicroserviceBaseUrl

  def fetchApiDefinitionsByCollaborator(developerId: Option[DeveloperIdentifier])(implicit hc: HeaderCarrier): Future[Seq[ApiDefinition]] = {
    logger.info(s"${getClass.getSimpleName} - fetchApiDefinitionsByCollaborator")
    val r = http.get(url"${definitionsUrl(serviceBaseUrl)}?${queryParams(developerId)}").execute[Seq[ApiDefinition]]

    r.map(defns => defns.foreach(defn => logger.info(s"Found ${defn.name}")))

    r.map(e => e.sortBy(_.name))
  }

  def fetchExtendedApiDefinition(serviceName: ServiceName, developerId: Option[DeveloperIdentifier])(implicit hc: HeaderCarrier): Future[Option[ExtendedApiDefinition]] = {
    logger.info(s"${getClass.getSimpleName} - fetchApiDefinition")

    val r = http.get(url"${definitionUrl(serviceBaseUrl, serviceName)}?${queryParams(developerId)}").execute[Option[ExtendedApiDefinition]]

    r.map(_.map(defn => logger.info(s"Found ${defn.name}")))

    r.recover {
      case e => logger.error(s"Failed $e"); throw e
    }
  }
}

object ApiPlatformMicroserviceConnector {
  type Params = Seq[(String, String)]

  val noParams: Params = Seq.empty

  def queryParams(developerIdOpt: Option[DeveloperIdentifier]): Params =
    developerIdOpt.fold(noParams)(developerId => Seq("developerId" -> developerId.asText))

  def definitionsUrl(serviceBaseUrl: String)                          = s"$serviceBaseUrl/combined-api-definitions"
  def definitionUrl(serviceBaseUrl: String, serviceName: ServiceName) = s"$serviceBaseUrl/combined-api-definitions/$serviceName"
}
