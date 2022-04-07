/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.apidocumentation.connectors.XmlServicesConnector
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.play.http.metrics.common._

import scala.concurrent.Future.successful


@Singleton
class XmlServicesService @Inject()(val xmlServicesConnector: XmlServicesConnector, val apiMetrics: ApiMetrics)
                                    (implicit ec: ExecutionContext) extends RecordMetrics {
  val api: API = API("api-platform-xml-services")

  def fetchAllXmlApis()(implicit hc: HeaderCarrier): Future[Seq[XmlApiDocumentation]] =
    record {
        xmlServicesConnector.fetchAllXmlApis()
    }

  def fetchXmlApi(name: String)(implicit hc: HeaderCarrier): Future[Option[XmlApiDocumentation]] =
    record {
      xmlServicesConnector.fetchXmlApi(name) flatMap  {
        case Right(x) => successful(x)
        case Left(_) => xmlServicesConnector.fetchXmlApiByServiceName(name)
      }
    }


}
