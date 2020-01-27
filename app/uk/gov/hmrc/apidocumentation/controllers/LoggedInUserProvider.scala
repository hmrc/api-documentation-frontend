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

import javax.inject.Inject
import jp.t2v.lab.play2.auth.{AsyncIdContainer, CookieTokenAccessor, TransparentIdContainer}
import play.api.mvc.Request
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.models.Developer
import uk.gov.hmrc.apidocumentation.services.SessionService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}


class LoggedInUserProvider @Inject()(config: ApplicationConfig,
                                     sessionService: SessionService
                                    ) {

  lazy val tokenAccessor = new CookieTokenAccessor(cookieSecureOption = config.securedCookie)

  lazy val idContainer = AsyncIdContainer(new TransparentIdContainer[String])

  def resolveUser(id: String)(implicit ctx: ExecutionContext, hc: HeaderCarrier): Future[Option[Developer]] =
    sessionService.fetch(id).map(_.map(_.developer))

  def fetchLoggedInUser()(implicit request: Request[_], hc: HeaderCarrier): Future[Option[Developer]] = {

    val oToken = tokenAccessor.extract(request)

    oToken match {
      case None => Future.successful(None)
      case Some(token) =>
        val foUserId = idContainer.get(token)

        foUserId.flatMap({
            case None => Future.successful(None)
            case Some(userId) => resolveUser(userId)
        })
    }
  }
}
