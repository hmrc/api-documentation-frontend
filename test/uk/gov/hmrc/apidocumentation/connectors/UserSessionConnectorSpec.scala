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

package unit.uk.gov.hmrc.apidocumentation.connectors

import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito.when
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.connectors.UserSessionConnector
import uk.gov.hmrc.apidocumentation.models.{Developer, LoggedInState, Session, SessionInvalid}
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.metrics.{API, NoopApiMetrics}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class UserSessionConnectorSpec extends ConnectorSpec {
  val thirdPartyDeveloperUrl = "https://third-party-developer.example.com"
  val sessionId = "A_SESSION_ID"

  trait Setup {
    implicit val hc = HeaderCarrier()
    val mockHttpClient = mock[HttpClient]
    val mockAppConfig = mock[ApplicationConfig]
    val connector = new UserSessionConnector(mockHttpClient, mockAppConfig, new NoopApiMetrics)

    when(mockAppConfig.thirdPartyDeveloperUrl).thenReturn(thirdPartyDeveloperUrl)
  }

  "api" should {
    "be third-party-developer" in new Setup {
      connector.api shouldBe API("third-party-developer")
    }
  }

  "fetchSession" should {
    "return the session when found" in new Setup {
      val session = Session(sessionId, LoggedInState.LOGGED_IN, Developer("developer@example.com", "Firstname", "Lastname"))

      when(mockHttpClient.GET[Session](meq(s"$thirdPartyDeveloperUrl/session/$sessionId"))(any(), any(), any()))
        .thenReturn(Future.successful(session))

      val result = await(connector.fetchSession(sessionId))

      result shouldBe session
    }

    "throw SessionInvalid when not found" in new Setup {
      when(mockHttpClient.GET[Session](meq(s"$thirdPartyDeveloperUrl/session/$sessionId"))(any(), any(), any()))
        .thenReturn(Future.failed(new NotFoundException("Not found")))

      intercept[SessionInvalid] {
        await(connector.fetchSession(sessionId))
      }
    }
  }
}
