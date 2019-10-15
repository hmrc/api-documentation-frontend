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

trait RawApiDefinitionConnector {
  def fetchApiDefinitions(email: Option[String])(implicit hc: HeaderCarrier): Future[Seq[APIDefinition]]

  def fetchApiDefinition(serviceName: String, email: Option[String])(implicit hc: HeaderCarrier): Future[Option[ExtendedAPIDefinition]]

  type Params = Seq[(String,String)]
  val noParams: Params = Seq.empty

  def queryParams(oemail: Option[String]): Params =
    oemail.fold(noParams)(email => Seq("email" -> email))
}

@Singleton
class LocalRawApiDefinitionConnector @Inject()(
      val http: HttpClient,
      val appConfig: ApplicationConfig
    )
    (implicit val ec: ExecutionContext) extends RawApiDefinitionConnector {

  private lazy val serviceBaseUrl = appConfig.localApiDefinitionUrl

  def fetchApiDefinitions(email: Option[String] = None)
                         (implicit hc: HeaderCarrier): Future[Seq[APIDefinition]] = {

    http.GET[Seq[APIDefinition]](s"$serviceBaseUrl/apis/definition", queryParams(email))
      .map(_.sortBy(_.name))
  }

  def fetchApiDefinition(serviceName: String, email: Option[String] = None)
                        (implicit hc: HeaderCarrier): Future[Option[ExtendedAPIDefinition]] = {

    http.GET[ExtendedAPIDefinition](s"$serviceBaseUrl/apis/$serviceName/definition", queryParams(email))
      .map(Some(_))
  }
}

@Singleton
class RemoteRawApiDefinitionConnector @Inject()(
     ws: ProxiedApiPlatformWsClient,
     appConfig: ApplicationConfig
   )
   (implicit val ec: ExecutionContext) extends RawApiDefinitionConnector {

  private lazy val serviceBaseUrl = appConfig.remoteApiDefinitionUrl

  def fetchApiDefinitions(email: Option[String] = None)
                         (implicit hc: HeaderCarrier): Future[Seq[APIDefinition]] = {

    ws
      .buildRequest(s"$serviceBaseUrl/apis/definition")
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

    ws.buildRequest(s"$serviceBaseUrl/apis/$serviceName/definition")
      .withQueryString(queryParams(email): _*)
      .get()
      .map(_.json.as[ExtendedAPIDefinition])
      .map(Some(_))
      .recover {
        case _ => None
      }
  }
}
