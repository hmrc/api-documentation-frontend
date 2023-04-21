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

package uk.gov.hmrc.apidocumentation.services

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import play.api.libs.crypto.CookieSigner
import play.api.test.FakeRequest

import uk.gov.hmrc.apidocumentation.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.models.{Developer, LoggedInState, Session, UserId}

class LoggedInUserServiceSpec extends AsyncHmrcSpec {

  import LoggedInUserService.cookieName

  "Fetching logged in user" should {

    val mockApplicationConfig = mock[ApplicationConfig]
    val mockSessionService    = mock[SessionService]
    val mockCookieSigner      = mock[CookieSigner]

    val developer = Developer("email", "John", "Smith", UserId.random)
    val session   = Session("sessionId", LoggedInState.LOGGED_IN, developer)

    val cookie                   = play.api.mvc.Cookie(cookieName, "bobbins")
    val fakeRequestWithoutCookie = FakeRequest()
    val fakeRequestWithCookie    = FakeRequest().withCookies(cookie)

    "Be None when no cookie" in {
      implicit val request    = fakeRequestWithoutCookie
      val loggedInUserService = new LoggedInUserService(mockApplicationConfig, mockSessionService, mockCookieSigner)

      val result: Option[Developer] = await(loggedInUserService.fetchLoggedInUser())

      result shouldBe None
    }

    "Be None when cookie is present but there is no valid signed cookie token" in {
      implicit val request = fakeRequestWithoutCookie

      val loggedInUserService = new LoggedInUserService(mockApplicationConfig, mockSessionService, mockCookieSigner)

      val result: Option[Developer] = await(loggedInUserService.fetchLoggedInUser())

      result shouldBe None
    }

    "Be None when no session from sessionService" in {
      implicit val request = fakeRequestWithCookie

      val fakeId              = "123"
      val decodeSessionResult = Some(fakeId)

      when(mockSessionService.fetch(eqTo(fakeId))(*))
        .thenReturn(Future.successful(None))

      val loggedInUserService =
        new LoggedInUserService(mockApplicationConfig, mockSessionService, mockCookieSigner) {
          override def decodeCookie(token: String): Option[String] = decodeSessionResult
        }

      val result: Option[Developer] = await(loggedInUserService.fetchLoggedInUser())

      result shouldBe None
    }

    "Be a Developer when a valid cookie and session" in {
      implicit val request = fakeRequestWithCookie

      val fakeId              = "123"
      val decodeSessionResult = Some(fakeId)

      when(mockSessionService.fetch(eqTo(fakeId))(*))
        .thenReturn(Future.successful(Some(session)))

      val loggedInUserService =
        new LoggedInUserService(mockApplicationConfig, mockSessionService, mockCookieSigner) {
          override def decodeCookie(token: String): Option[String] = decodeSessionResult
        }

      val result: Option[Developer] = await(loggedInUserService.fetchLoggedInUser())

      result shouldBe Some(developer)
    }
  }
}
