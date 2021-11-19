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

package uk.gov.hmrc.apidocumentation.connectors

import javax.inject.{Inject, Singleton}
import play.api.Logger
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.connectors.ApiPlatformMicroserviceConnector.{definitionUrl, definitionsUrl, queryParams}
import uk.gov.hmrc.apidocumentation.models.JsonFormatters._
import uk.gov.hmrc.apidocumentation.models.{APIDefinition, ExtendedAPIDefinition}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.HttpReads.Implicits._

import uk.gov.hmrc.apidocumentation.models.DeveloperIdentifier

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.apidocumentation.models.apispecification.ApiSpecification

@Singleton
class ApiPlatformMicroserviceConnector @Inject() (val http: HttpClient, val appConfig: ApplicationConfig)
                                      (implicit val ec: ExecutionContext) {

  private lazy val serviceBaseUrl = appConfig.apiPlatformMicroserviceBaseUrl

  def fetchApiSpecification(serviceName: String, version: String)(implicit hc: HeaderCarrier): Future[ApiSpecification] = {
    import uk.gov.hmrc.apidocumentation.models.apispecification.ApiSpecificationFormatters._
    val url = s"$serviceBaseUrl/combined-api-definitions/$serviceName/$version/documentation/packed(application.raml)"
    http.GET[ApiSpecification](url)
  }

  def fetchApiDefinitionsByCollaborator(developerId: Option[DeveloperIdentifier])(implicit hc: HeaderCarrier): Future[Seq[APIDefinition]] = {
    Logger.info(s"${getClass.getSimpleName} - fetchApiDefinitionsByCollaborator")
    val r = http.GET[Seq[APIDefinition]](definitionsUrl(serviceBaseUrl), queryParams(developerId))

    r.map(defns => defns.foreach(defn => Logger.info(s"Found ${defn.name}")))

    r.map(e => e.sortBy(_.name))
  }

  def fetchApiDefinition(serviceName: String, developerId: Option[DeveloperIdentifier])(implicit hc: HeaderCarrier): Future[Option[ExtendedAPIDefinition]] = {
    Logger.info(s"${getClass.getSimpleName} - fetchApiDefinition")
    
    val r = http.GET[Option[ExtendedAPIDefinition]](definitionUrl(serviceBaseUrl, serviceName), queryParams(developerId))

    r.map(_.map(defn => Logger.info(s"Found ${defn.name}")))
    
    r.recover {
      case e => Logger.error(s"Failed $e"); throw e
    }
  }
}

object ApiPlatformMicroserviceConnector {
  type Params = Seq[(String, String)]

  val noParams: Params = Seq.empty

  def queryParams(developerIdOpt: Option[DeveloperIdentifier]): Params =
    developerIdOpt.fold(noParams)(developerId => Seq("developerId" -> developerId.asText))

  def definitionsUrl(serviceBaseUrl: String) = s"$serviceBaseUrl/combined-api-definitions"
  def definitionUrl(serviceBaseUrl: String, serviceName: String) = s"$serviceBaseUrl/combined-api-definitions/$serviceName"
}
