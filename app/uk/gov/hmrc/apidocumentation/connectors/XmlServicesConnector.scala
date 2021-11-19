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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.play.http.metrics.common._
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.apidocumentation.models.XmlApiDocumentation

@Singleton
class XmlServicesConnector @Inject()(http: HttpClient, appConfig: XmlServicesConnector.Config, val apiMetrics: ApiMetrics)
                                    (implicit ec: ExecutionContext) extends RecordMetrics {

  val api = API("api-platform-xml-services")
  private lazy val serviceBaseUrl: String = appConfig.serviceBaseUrl

  def fetchAllXmlApis()(implicit hc: HeaderCarrier): Future[Seq[XmlApiDocumentation]] = record {
    http.GET[Seq[XmlApiDocumentation]](s"$serviceBaseUrl/api-platform-xml-services/xml/apis")
  }

  def fetchXmlApi(name: String)(implicit hc: HeaderCarrier): Future[Option[XmlApiDocumentation]] = record {
    http.GET[Option[XmlApiDocumentation]](s"$serviceBaseUrl/api-platform-xml-services/xml/api/$name")
  }
}

object XmlServicesConnector {
  case class Config(serviceBaseUrl: String)
}

