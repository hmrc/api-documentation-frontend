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

package uk.gov.hmrc.apidocumentation.mocks.services

import org.mockito.MockitoSugar
import uk.gov.hmrc.apidocumentation.services.LoggedInUserService
import uk.gov.hmrc.apidocumentation.models.Developer

import scala.concurrent.Future.successful
import uk.gov.hmrc.apidocumentation.models.UserId
import org.mockito.ArgumentMatchersSugar

trait LoggedInUserServiceMock extends MockitoSugar with ArgumentMatchersSugar {
  val loggedInEmail  = "mr.abcd@example.com"
  val noUserLoggedIn = None
  val userLoggedIn   = Some(Developer(loggedInEmail, "Anony", "Mouse", UserId.random))

  lazy val loggedInUserService: LoggedInUserService = mock[LoggedInUserService]

  def theUserIsLoggedIn() = {
    when(loggedInUserService.fetchLoggedInUser()(*)).thenReturn(successful(userLoggedIn))
  }

  def theUserIsNotLoggedIn() = {
    when(loggedInUserService.fetchLoggedInUser()(*)).thenReturn(successful(noUserLoggedIn))
  }
}
