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

import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.apidocumentation.connectors.UserSessionConnector
import uk.gov.hmrc.apidocumentation.models.{Developer, LoggedInState, Session}
import uk.gov.hmrc.apidocumentation.services.SessionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future

class SessionServiceSpec extends UnitSpec with WithFakeApplication with MockitoSugar with ScalaFutures {

  private val userSessionConnectorMock = mock[UserSessionConnector]

  def sessionService = new SessionService(userSessionConnectorMock)

  "The SessionService" should {
    val developer = Developer("email","John", "Smith")

    "Return session if the session is logged in" in {
      val session = Session("sessionId", LoggedInState.LOGGED_IN, developer)

      when(userSessionConnectorMock.fetchSession(any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(session))

      val result = await(sessionService.fetch(meq("sessionId"))(any[HeaderCarrier]))

      result shouldBe Some(session)
    }

    "Return None when the session is part logged in" in {
      val session = Session("sessionId", LoggedInState.PART_LOGGED_IN_ENABLING_MFA, developer)

      when(userSessionConnectorMock.fetchSession(any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(session))

      val result = await(sessionService.fetch(meq("sessionId"))(any[HeaderCarrier]))

      result shouldBe None
    }
  }
}
