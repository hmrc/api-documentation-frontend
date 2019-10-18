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

import akka.actor.ActorSystem
import akka.pattern.FutureTimeoutSupport
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.connectors.ApiDefinitionConnector.{definitionUrl, definitionsUrl, queryParams}
import uk.gov.hmrc.apidocumentation.models.{APIDefinition, ExtendedAPIDefinition}
import uk.gov.hmrc.apidocumentation.models.JsonFormatters._
import uk.gov.hmrc.apidocumentation.utils.Retries
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, Upstream5xxResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

trait ApiDefinitionConnector {
  def http: HttpClient
  def serviceBaseUrl: String
  implicit val ec: ExecutionContext

  def fetchAllApiDefinitions(email: Option[String])(implicit hc: HeaderCarrier): Future[Seq[APIDefinition]] = {
    val r= http.GET[Seq[APIDefinition]](definitionsUrl(serviceBaseUrl), queryParams(email))
      r.map(e => e.sortBy(
        _.name
      ))
      .recover {
        case _ : NotFoundException => Seq()
        case e : Upstream5xxResponse => throw e
      }
  }

  def fetchApiDefinition(serviceName: String, email: Option[String])(implicit hc: HeaderCarrier): Future[Option[ExtendedAPIDefinition]] = {
    http.GET[ExtendedAPIDefinition](definitionUrl(serviceBaseUrl,serviceName), queryParams(email))
      .map(Some(_))
      .recover {
        case _ : NotFoundException => None
        case e : Upstream5xxResponse => throw e
      }
  }
}

object ApiDefinitionConnector {
  type Params = Seq[(String, String)]

  val noParams: Params = Seq.empty

  def queryParams(oemail: Option[String]): Params =
    oemail.fold(noParams)(email => Seq("email" -> email))

  def definitionsUrl(serviceBaseUrl: String) = s"$serviceBaseUrl/api-definition"

  def definitionUrl(serviceBaseUrl: String, serviceName: String) = s"$serviceBaseUrl/api-definition/$serviceName/extended"
}

@Singleton
class LocalApiDefinitionConnector @Inject()(
      val http: HttpClient,
      val appConfig: ApplicationConfig
    )
   (implicit val ec: ExecutionContext) extends ApiDefinitionConnector {

  val serviceBaseUrl = appConfig.apiDefinitionProductionBaseUrl
}

@Singleton
class RemoteApiDefinitionConnector @Inject()(
    val appConfig: ApplicationConfig,
    val httpClient: HttpClient,
    val proxiedHttpClient: ProxiedHttpClient,
    val actorSystem: ActorSystem,
    val futureTimeout: FutureTimeoutSupport
    )
    (implicit val ec: ExecutionContext)
    extends ApiDefinitionConnector with Retries {

  val serviceBaseUrl = appConfig.apiDefinitionSandboxBaseUrl

  val useProxy = appConfig.apiDefinitionSandboxUseProxy
  val bearerToken = appConfig.apiDefinitionSandboxBearerToken
  val apiKey = appConfig.apiDefinitionSandboxApiKey

  override def http: HttpClient = if (useProxy)
    proxiedHttpClient.withHeaders(bearerToken, apiKey)
  else
    httpClient

  override def fetchAllApiDefinitions(email: Option[String] = None)
                            (implicit hc: HeaderCarrier): Future[Seq[APIDefinition]] = {
    retry {
      super.fetchAllApiDefinitions(email)
    }
  }

  override def fetchApiDefinition(serviceName: String, email: Option[String] = None)
                                 (implicit hc: HeaderCarrier): Future[Option[ExtendedAPIDefinition]] = {
    retry {
      super.fetchApiDefinition(serviceName, email)
    }
  }
}
