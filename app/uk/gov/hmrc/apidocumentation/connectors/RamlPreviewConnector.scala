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
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.apidocumentation.models.apispecification.ApiSpecification
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.HeaderCarrier
import java.net.URLEncoder
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.play.http.metrics.common._

@Singleton
class RamlPreviewConnector @Inject()(http: HttpClient, appConfig: ApplicationConfig)(implicit ec: ExecutionContext) {

  private lazy val serviceBaseUrl = appConfig.ramlPreviewMicroserviceBaseUrl

  val api = API("api-definition")
  
  def fetchPreviewApiSpecification(ramlUrl: String)(implicit hc: HeaderCarrier): Future[ApiSpecification] = {
    import uk.gov.hmrc.apidocumentation.models.apispecification.ApiSpecificationFormatters._
    http.GET[ApiSpecification](s"$serviceBaseUrl/preview?ramlUrl=${URLEncoder.encode(ramlUrl, "UTF-8")}")
  }
}

