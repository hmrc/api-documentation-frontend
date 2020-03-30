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

import jp.t2v.lab.play2.auth.{AsyncIdContainer, CookieTokenAccessor}
import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.controllers.LoggedInUserProvider
import uk.gov.hmrc.apidocumentation.models.{Developer, LoggedInState, Session}
import uk.gov.hmrc.apidocumentation.services.SessionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class LoggedInUserProviderTest(config: ApplicationConfig,
                               sessionService: SessionService,
                               asyncIdContainer: AsyncIdContainer[String],
                               cookieTokenAccessor: CookieTokenAccessor)
  extends LoggedInUserProvider(config,
    sessionService) {
  override lazy val idContainer: AsyncIdContainer[String] = asyncIdContainer
  override lazy val tokenAccessor: CookieTokenAccessor = cookieTokenAccessor
}

class LoggedInUserProviderSpec extends UnitSpec with ScalaFutures with MockitoSugar {

  "Fetching logged in user" should {

    val mockApplicationConfig = mock[ApplicationConfig]
    val mockSessionService = mock[SessionService]
    val mockAsyncIdContainer = mock[AsyncIdContainer[String]]
    val mockCookieTokenAccessor = mock[CookieTokenAccessor]

    val fakeRequest = FakeRequest()

    implicit val hc = HeaderCarrier()

    val developer = Developer("email","John", "Smith")
    val session = Session("sessionId", LoggedInState.LOGGED_IN, developer)

    val tokenId = "tokenId"
    val userId = "userId"

    "Be None when no cookie token from cookieTokeExtractor" in {

      when(mockCookieTokenAccessor.extract(any[RequestHeader]))
        .thenReturn(None)

      val loggedInUserProvider = new LoggedInUserProviderTest(mockApplicationConfig, mockSessionService, mockAsyncIdContainer, mockCookieTokenAccessor)

      val result: Option[Developer] = await(loggedInUserProvider.fetchLoggedInUser()(fakeRequest,any[HeaderCarrier]))

      result shouldBe None
    }

    "Be None when no userId from idContainer" in {
      when(mockCookieTokenAccessor.extract(any[RequestHeader]))
        .thenReturn(Some(tokenId))

      when(mockAsyncIdContainer.get(tokenId))
        .thenReturn(Future.successful(None))

      val loggedInUserProvider = new LoggedInUserProviderTest(mockApplicationConfig, mockSessionService, mockAsyncIdContainer, mockCookieTokenAccessor)

      val result: Option[Developer] = await(loggedInUserProvider.fetchLoggedInUser()(fakeRequest,any[HeaderCarrier]))

      result shouldBe None
    }

    "Be None when no session from sessionService" in {
      when(mockCookieTokenAccessor.extract(any[RequestHeader]))
        .thenReturn(Some(tokenId))

      when(mockAsyncIdContainer.get(tokenId))
        .thenReturn(Future.successful(Some(userId)))

      when(mockSessionService.fetch( meq(userId))(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(None)

      val loggedInUserProvider = new LoggedInUserProviderTest(mockApplicationConfig, mockSessionService, mockAsyncIdContainer, mockCookieTokenAccessor)

      val result: Option[Developer] = await(loggedInUserProvider.fetchLoggedInUser()(fakeRequest,any[HeaderCarrier]))

      result shouldBe None
    }

    "Be a Developer when a valid cookie and session" in {
      when(mockCookieTokenAccessor.extract(any[RequestHeader]))
        .thenReturn(Some(tokenId))

      when(mockAsyncIdContainer.get(tokenId))
        .thenReturn(Future.successful(Some(userId)))

      when(mockSessionService.fetch( meq(userId))(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Some(session))

      val loggedInUserProvider = new LoggedInUserProviderTest(mockApplicationConfig, mockSessionService, mockAsyncIdContainer, mockCookieTokenAccessor)

      val result: Option[Developer] = await(loggedInUserProvider.fetchLoggedInUser()(fakeRequest,any[HeaderCarrier]))

      result shouldBe Some(developer)
    }
  }
}
