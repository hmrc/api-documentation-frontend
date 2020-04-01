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

package uk.gov.hmrc.apidocumentation.connectors

import javax.inject.{Inject, Singleton}
import play.api.Logger
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.connectors.ApiDefinitionConnector.{definitionUrl, definitionsUrl, queryParams}
import uk.gov.hmrc.apidocumentation.models.JsonFormatters._
import uk.gov.hmrc.apidocumentation.models.{APIDefinition, ExtendedAPIDefinition}
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, Upstream5xxResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApiDefinitionConnector @Inject()(
                                        val http: HttpClient,
                                        val appConfig: ApplicationConfig
                                      )
                                      (implicit val ec: ExecutionContext) {

  private lazy val serviceBaseUrl = appConfig.apiDefinitionBaseUrl

  def fetchAllApiDefinitions(email: Option[String])(implicit hc: HeaderCarrier): Future[Seq[APIDefinition]] = {
    Logger.info(s"${this.getClass.getSimpleName} - fetchAllApiDefinitions")
    val r= http.GET[Seq[APIDefinition]](definitionsUrl(serviceBaseUrl), queryParams(email))

    r.map(defns => defns.foreach(defn => Logger.info(s"Found ${defn.name}")))

    r.map(e => e.sortBy(
      _.name
    ))
    .recover {
      case _ : NotFoundException => { Logger.info("Not found"); Seq.empty}
      case e : Upstream5xxResponse => { Logger.error(s"Failed ${e}"); throw e}
      case e => { Logger.error(s"Failed ${e}"); throw e}
    }
  }

  def fetchApiDefinition(serviceName: String, email: Option[String])(implicit hc: HeaderCarrier): Future[Option[ExtendedAPIDefinition]] = {
    Logger.info(s"${this.getClass.getSimpleName} - fetchApiDefinition")
    val r = http.GET[ExtendedAPIDefinition](definitionUrl(serviceBaseUrl,serviceName), queryParams(email))

    r.map(defn => Logger.info(s"Found ${defn.name}"))

    r.map(Some(_))
      .recover {
        case _ : NotFoundException => { Logger.info("Not found"); None}
        case e : Upstream5xxResponse => { Logger.error(s"Failed ${e}"); throw e}
        case e => { Logger.error(s"Failed ${e}"); throw e}
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
