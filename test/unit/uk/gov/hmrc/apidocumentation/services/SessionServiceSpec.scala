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

package unit.uk.gov.hmrc.apidocumentation.services

import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.apidocumentation.connectors.UserSessionConnector
import uk.gov.hmrc.apidocumentation.models.{Developer, LoggedInState, Session}
import uk.gov.hmrc.apidocumentation.services.SessionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SessionServiceSpec extends UnitSpec with WithFakeApplication with MockitoSugar with ScalaFutures {

  trait Setup {
    implicit val hc = HeaderCarrier()

    val userSessionConnectorMock = mock[UserSessionConnector]

    val underTest = new SessionService(userSessionConnectorMock)
  }

  "The SessionService" should {
    val developer = Developer("email","John", "Smith")

    "Return session if the session is logged in" in new Setup {
      val session = Session("sessionId", LoggedInState.LOGGED_IN, developer)

      when(userSessionConnectorMock.fetchSession(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(session))

      val result = await(underTest.fetch("sessionId"))

      result shouldBe Some(session)
    }

    "Return None when the session is part logged in" in new Setup {
      val session = Session("sessionId", LoggedInState.PART_LOGGED_IN_ENABLING_MFA, developer)

      when(userSessionConnectorMock.fetchSession(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(session))

      val result = await(underTest.fetch("sessionId"))

      result shouldBe None
    }
  }
}
