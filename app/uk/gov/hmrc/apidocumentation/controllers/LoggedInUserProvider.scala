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

package uk.gov.hmrc.apidocumentation.controllers

import java.security.MessageDigest

import javax.inject.Inject
import play.api.libs.crypto.CookieSigner
import play.api.mvc.{Request, RequestHeader}
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.models.{Developer, Session}
import uk.gov.hmrc.apidocumentation.services.SessionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class LoggedInUserProvider @Inject()(config: ApplicationConfig,
                                     sessionService: SessionService,
                                     val cookieSigner : CookieSigner)
                                     (implicit ec: ExecutionContext) extends CookieEncoding with HeaderCarrierConversion {

  def fetchLoggedInUser()(implicit request: Request[_], hc: HeaderCarrier): Future[Option[Developer]] = {
    // TODO: Tidy this up
    loadSession
      .map(_.map(_.developer))
  }

    private val cookieName = "PLAY2AUTH_SESS_ID"

    private def loadSession[A](implicit ec: ExecutionContext, request: Request[A]): Future[Option[Session]] = {
      (for {
        cookie <- request.cookies.get(cookieName)
        sessionId <- decodeCookie(cookie.value)
      } yield fetchDeveloperSession(sessionId))
        .getOrElse(Future.successful(None))
    }

  private def fetchDeveloperSession[A](sessionId: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[Session]] = {
    sessionService
      .fetch(sessionId)
  }
}

trait CookieEncoding {

  val cookieSigner : CookieSigner

  def encodeCookie(token : String) : String = {
    cookieSigner.sign(token) + token
  }

  def decodeCookie(token : String) : Option[String] = {
    val (hmac, value) = token.splitAt(40)

    val signedValue = cookieSigner.sign(value)

    if (MessageDigest.isEqual(signedValue.getBytes, hmac.getBytes)) {
      Some(value)
    } else {
      None
    }
  }
}

trait HeaderCarrierConversion
  extends uk.gov.hmrc.play.bootstrap.controller.BaseController
    with uk.gov.hmrc.play.bootstrap.controller.Utf8MimeTypes {

  override implicit def hc(implicit rh: RequestHeader): HeaderCarrier =
    HeaderCarrierConverter.fromHeadersAndSessionAndRequest(rh.headers, Some(rh.session), Some(rh))
}
