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

import uk.gov.hmrc.apidocumentation.models.{Developer, LoggedInState, Session, SessionInvalid}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.metrics.API

import uk.gov.hmrc.apidocumentation.models.UserId
import play.api.Configuration
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.test.Helpers._
import uk.gov.hmrc.apidocumentation.models.JsonFormatters._

class UserSessionConnectorSpec extends ConnectorSpec {
  val thirdPartyDeveloperUrl = "https://third-party-developer.example.com"
  val sessionId = "A_SESSION_ID"

  val stubConfig = Configuration(
    "Test.metrics.jvm" -> false,
    "Test.microservice.services.third-party-developer.host" -> stubHost,
    "Test.microservice.services.third-party-developer.port" -> stubPort
  )
  trait Setup {
    implicit val hc = HeaderCarrier()
    val connector = app.injector.instanceOf[UserSessionConnector]
  }

  "api" should {
    "be third-party-developer" in new Setup {
      connector.api shouldBe API("third-party-developer")
    }
  }

  "fetchSession" should {
    "return the session when found" in new Setup {
      val session = Session(sessionId, LoggedInState.LOGGED_IN, Developer("developer@example.com", "Firstname", "Lastname", UserId.random))

      stubFor(
        get(
          urlPathEqualTo(s"/session/$sessionId")
        )
        .willReturn(
          aResponse()
          .withStatus(OK)
          .withJsonBody(session)
        )
      )

      val result = await(connector.fetchSession(sessionId))

      result shouldBe session
    }

    "throw SessionInvalid when not found" in new Setup {
      stubFor(
        get(
          urlPathEqualTo(s"/session/$sessionId")
        )
        .willReturn(
          aResponse()
          .withStatus(NOT_FOUND)
        )
      )
      intercept[SessionInvalid] {
        await(connector.fetchSession(sessionId))
      }
    }
  }
}
