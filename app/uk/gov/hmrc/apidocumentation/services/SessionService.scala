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

import javax.inject.Inject
import uk.gov.hmrc.apidocumentation.connectors.UserSessionConnector
import uk.gov.hmrc.apidocumentation.models.{LoggedInState, Session, SessionInvalid}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class SessionService @Inject() (connector: UserSessionConnector)(implicit ec: ExecutionContext) {

  def fetch(sessionId: String)(implicit hc: HeaderCarrier): Future[Option[Session]] = {

    def convertSession(session: Session): Option[Session] = {
      session.loggedInState match {
        case LoggedInState.LOGGED_IN => Some(session)
        case _                       => None
      }
    }

    connector
      .fetchSession(sessionId)
      .map(convertSession)
      .recover {
        case _: SessionInvalid => None
      }
  }
}
