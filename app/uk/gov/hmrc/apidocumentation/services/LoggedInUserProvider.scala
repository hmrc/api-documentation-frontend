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

package uk.gov.hmrc.apidocumentation.services

import java.security.MessageDigest

import javax.inject.{Inject, Singleton}
import play.api.libs.crypto.CookieSigner
import play.api.mvc.{Request, RequestHeader}
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.models.{Developer, Session}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import play.api.mvc.ControllerComponents

// TODO: Rename to LoggedInUserService (using IntelliJ)
@Singleton
class LoggedInUserProvider @Inject()(config: ApplicationConfig,
                                     sessionService: SessionService,
                                     val cookieSigner : CookieSigner,
                                     cc: ControllerComponents)
                                    (implicit ec: ExecutionContext)
                                    extends BackendController(cc)
                                    with CookieEncoding
                                    with HeaderCarrierConversion {

  import LoggedInUserProvider._

  def fetchLoggedInUser()(implicit request: Request[_]): Future[Option[Developer]] = {
    loadSession
      .map(_.map(_.developer))
  }

    private def loadSession[A](implicit request: Request[A]): Future[Option[Session]] = {
      (for {
        cookie <- request.cookies.get(cookieName)
        sessionId <- decodeCookie(cookie.value)
      } yield fetchDeveloperSession(sessionId))
        .getOrElse(Future.successful(None))
    }

  private def fetchDeveloperSession[A](sessionId: String)(implicit hc: HeaderCarrier): Future[Option[Session]] = {
    sessionService
      .fetch(sessionId)
  }
}

object LoggedInUserProvider {
  val cookieName = "PLAY2AUTH_SESS_ID"
}

trait CookieEncoding {

  val cookieSigner : CookieSigner

  def encodeCookie(token : String) : String = {
    cookieSigner.sign(token) + token
  }

  def decodeCookie(token : String) : Option[String] = {
    Try({
      val (hmac, value) = token.splitAt(40)

      val signedValue = cookieSigner.sign(value)

      if (MessageDigest.isEqual(signedValue.getBytes, hmac.getBytes)) {
        Some(value)
      } else {
        None
      }
    }).toOption.flatten
  }
}

trait HeaderCarrierConversion extends uk.gov.hmrc.play.bootstrap.controller.BackendBaseController {

  override implicit def hc(implicit rh: RequestHeader): HeaderCarrier =
    HeaderCarrierConverter.fromHeadersAndSessionAndRequest(rh.headers, Some(rh.session), Some(rh))
}
