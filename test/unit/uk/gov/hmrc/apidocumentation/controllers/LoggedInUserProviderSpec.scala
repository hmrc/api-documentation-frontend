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

package unit.uk.gov.hmrc.apidocumentation.controllers

import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.crypto.CookieSigner
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.controllers.LoggedInUserProvider
import uk.gov.hmrc.apidocumentation.models.{Developer, LoggedInState, Session}
import uk.gov.hmrc.apidocumentation.services.SessionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LoggedInUserProviderSpec extends UnitSpec with ScalaFutures with MockitoSugar {

  import LoggedInUserProvider.cookieName

  "Fetching logged in user" should {

    val mockApplicationConfig = mock[ApplicationConfig]
    val mockSessionService = mock[SessionService]
    val mockCookieSigner = mock[CookieSigner]

    val mcc = Helpers.stubControllerComponents()

    implicit val hc = HeaderCarrier()

    val developer = Developer("email","John", "Smith")
    val session = Session("sessionId", LoggedInState.LOGGED_IN, developer)

    val cookie = play.api.mvc.Cookie(cookieName, "bobbins")
    val fakeRequestWithoutCookie = FakeRequest()
    val fakeRequestWithCookie = FakeRequest().withCookies(cookie)

    val tokenId = "tokenId"
    val userId = "userId"

    "Be None when no cookie" in {
      implicit val request = fakeRequestWithoutCookie
      val loggedInUserProvider = new LoggedInUserProvider(mockApplicationConfig, mockSessionService, mockCookieSigner, mcc)

      val result: Option[Developer] = await(loggedInUserProvider.fetchLoggedInUser())

      result shouldBe None
    }

    "Be None when cookie is present but there is no valid signed cookie token" in {
      implicit val request = fakeRequestWithoutCookie

      val loggedInUserProvider = new LoggedInUserProvider(mockApplicationConfig, mockSessionService, mockCookieSigner, mcc)

      val result: Option[Developer] = await(loggedInUserProvider.fetchLoggedInUser())

      result shouldBe None
    }

   "Be None when no session from sessionService" in {
      implicit val request = fakeRequestWithCookie

      val fakeId = "123"
      val decodeSessionResult = Some(fakeId)

      when(mockSessionService.fetch(eqTo(fakeId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(None))

      val loggedInUserProvider =
        new LoggedInUserProvider(mockApplicationConfig, mockSessionService, mockCookieSigner, mcc) {
          override def decodeCookie(token: String) : Option[String] = decodeSessionResult
        }

      val result: Option[Developer] = await(loggedInUserProvider.fetchLoggedInUser())

      result shouldBe None
    }


    "Be a Developer when a valid cookie and session" in {
      implicit val request = fakeRequestWithCookie

      val fakeId = "123"
      val decodeSessionResult = Some(fakeId)

      when(mockSessionService.fetch(eqTo(fakeId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(session)))

      val loggedInUserProvider =
        new LoggedInUserProvider(mockApplicationConfig, mockSessionService, mockCookieSigner, mcc) {
          override def decodeCookie(token: String) : Option[String] = decodeSessionResult
        }

      val result: Option[Developer] = await(loggedInUserProvider.fetchLoggedInUser())

      result shouldBe Some(developer)
    }
  }
}
