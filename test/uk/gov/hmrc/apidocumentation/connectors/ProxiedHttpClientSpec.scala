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

package uk.gov.hmrc.apidocumentation.connectors

import java.util.UUID

import akka.actor.ActorSystem
import play.api.{ConfigLoader, Configuration}
import play.api.libs.ws.{WSClient, WSRequest}
import uk.gov.hmrc.http.Authorization
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.apidocumentation.common.utils.AsyncHmrcSpec

class ProxiedHttpClientSpec extends AsyncHmrcSpec {

  private val actorSystem = ActorSystem("test-actor-system")

  trait Setup {
    val apiKey: String = UUID.randomUUID().toString
    val bearerToken: String = UUID.randomUUID().toString
    val url = "http://example.com"
    val mockConfig: Configuration = mock[Configuration]
    val mockHttpAuditing: HttpAuditing = mock[HttpAuditing]
    val mockWsClient: WSClient = mock[WSClient]

    when(mockConfig.get[String](any[String])(any[ConfigLoader[String]])).thenReturn("")
    when(mockConfig.get[Int](any[String])(any[ConfigLoader[Int]])).thenReturn(0)
    when(mockConfig.get[Boolean]("proxy.proxyRequiredForThisEnvironment")).thenReturn(true)
    when(mockWsClient.url(url)).thenReturn(mock[WSRequest])

    val underTest = new ProxiedHttpClient(mockConfig, mockHttpAuditing, mockWsClient, actorSystem)
  }

  "withHeaders" should {

    "creates a ProxiedHttpClient with passed in headers" in new Setup {

      private val result = underTest.withHeaders(bearerToken, apiKey)

      result.authorization shouldBe Some(Authorization(s"Bearer $bearerToken"))
      result.apiKeyHeader shouldBe Some(apiKey)
    }

    "when apiKey is empty String, apiKey header is None" in new Setup {

      private val result = underTest.withHeaders(bearerToken, "")

      result.apiKeyHeader shouldBe None
    }

    "when apiKey isn't provided, apiKey header is None" in new Setup {

      private val result = underTest.withHeaders(bearerToken)

      result.apiKeyHeader shouldBe None
    }
  }
}
