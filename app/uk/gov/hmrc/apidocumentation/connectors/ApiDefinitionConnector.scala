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

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.models.{APIDefinition, ExtendedAPIDefinition}
import uk.gov.hmrc.apidocumentation.models.JsonFormatters._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

trait ApiDefinitionConnector {
  def fetchAllApiDefinitions(email: Option[String])(implicit hc: HeaderCarrier): Future[Seq[APIDefinition]]

  def fetchApiDefinition(serviceName: String, email: Option[String])(implicit hc: HeaderCarrier): Future[Option[ExtendedAPIDefinition]]

}

object ApiDefinitionConnector {
  type Params = Seq[(String, String)]

  val noParams: Params = Seq.empty

  def queryParams(oemail: Option[String]): Params =
    oemail.fold(noParams)(email => Seq("email" -> email))

  def definitionsUrl(serviceBaseUrl: String) = s"$serviceBaseUrl/apis/definition"

  def definitionUrl(serviceBaseUrl: String, serviceName: String) = s"$serviceBaseUrl/apis/$serviceName/definition"

}

@Singleton
class LocalApiDefinitionConnector @Inject()(
      val http: HttpClient,
      val appConfig: ApplicationConfig
    )
   (implicit val ec: ExecutionContext) extends ApiDefinitionConnector {

  import ApiDefinitionConnector._

  private lazy val serviceBaseUrl = appConfig.localApiDefinitionUrl

  def fetchAllApiDefinitions(email: Option[String] = None)
                            (implicit hc: HeaderCarrier): Future[Seq[APIDefinition]] = {

    http.GET[Seq[APIDefinition]](definitionsUrl(serviceBaseUrl), queryParams(email))
      .map(_.sortBy(_.name))
  }

  def fetchApiDefinition(serviceName: String, email: Option[String] = None)
                        (implicit hc: HeaderCarrier): Future[Option[ExtendedAPIDefinition]] = {

    http.GET[ExtendedAPIDefinition](definitionUrl(serviceBaseUrl,serviceName), queryParams(email)).map(Some(_))
  }
}

@Singleton
class RemoteApiDefinitionConnector @Inject()(
      ws: ProxiedApiPlatformWsClient,
      appConfig: ApplicationConfig
    )
    (implicit val ec: ExecutionContext)
    extends ApiDefinitionConnector {

  import ApiDefinitionConnector._

  private lazy val serviceBaseUrl = appConfig.remoteApiDefinitionUrl

  def fetchAllApiDefinitions(email: Option[String] = None)
                            (implicit hc: HeaderCarrier): Future[Seq[APIDefinition]] = {

    ws
      .buildRequest(definitionsUrl(serviceBaseUrl))
      .withQueryString(queryParams(email): _*)
      .get()
      .map(_.json.as[Seq[APIDefinition]])
      .map(_.sortBy(_.name))
      .recover {
        case _ => Seq()
      }

  }

  def fetchApiDefinition(serviceName: String, email: Option[String] = None)
                        (implicit hc: HeaderCarrier): Future[Option[ExtendedAPIDefinition]] = {

    ws.buildRequest(definitionUrl(serviceBaseUrl, serviceName))
      .withQueryString(queryParams(email): _*)
      .get()
      .map(_.json.as[ExtendedAPIDefinition])
      .map(Some(_))
      .recover {
        case _ => None
      }
  }
}
