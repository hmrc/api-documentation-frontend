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
import play.api.libs.ws.{WSClient, WSProxyServer}
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.play.http.ws.WSProxyConfiguration

@Singleton
class ProxiedApiPlatformWsClient @Inject()(config: ApplicationConfig, wsClient: WSClient) {
  lazy val wsProxyServer: Option[WSProxyServer] = WSProxyConfiguration(s"${config.env}.proxy", config.runModeConfiguration)

  def buildRequest(url: String)(implicit hc: HeaderCarrier) = {
    def headers = {
      config.apiPlatformBearerToken match {
        case Some(bearerToken) => hc.copy(authorization = Some(Authorization(s"Bearer $bearerToken"))).headers
        case _ => hc.headers
      }
    }

    val wsRequest = wsClient.url(url).withHeaders(headers: _*)
    wsProxyServer.fold(wsRequest) {
      p => wsRequest.withProxyServer(p)
    }
  }
}
