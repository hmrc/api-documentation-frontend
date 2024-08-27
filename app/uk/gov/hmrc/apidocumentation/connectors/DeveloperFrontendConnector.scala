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

import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.play.http.metrics.common._
import uk.gov.hmrc.play.partials.HtmlPartial
import uk.gov.hmrc.play.partials.HtmlPartial.connectionExceptionsAsHtmlPartialFailure

import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.apidocumentation.models.jsonFormatters._

@Singleton
class DeveloperFrontendConnector @Inject() (http: HttpClientV2, appConfig: ApplicationConfig, val apiMetrics: ApiMetrics)(implicit ec: ExecutionContext) extends RecordMetrics {

  val api                         = API("third-party-developer-frontend")
  private lazy val serviceBaseUrl = appConfig.developerFrontendBaseUrl

  def fetchNavLinks()(implicit hc: HeaderCarrier): Future[Seq[NavLink]] = record {
    import uk.gov.hmrc.http.HttpReads.Implicits._
    http.get(url"$serviceBaseUrl/developer/user-navlinks").execute[Seq[NavLink]]
  }

  def fetchTermsOfUsePartial()(implicit hc: HeaderCarrier): Future[HtmlPartial] = record {
    // Copy 'useNewUpliftJourney' header from incoming request to ensure TPDFE does not display the new Terms of Use page before the new ToU journey has been enabled
    val useNewUpliftJourneyHeader = hc.headers(Seq("useNewUpliftJourney"))

    http.get(url"$serviceBaseUrl/developer/partials/terms-of-use")
      .setHeader(useNewUpliftJourneyHeader: _*)
      .execute[HtmlPartial]
      .recover(connectionExceptionsAsHtmlPartialFailure)
  }
}
