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

package uk.gov.hmrc.apidocumentation.models

import play.api.libs.json.Format
import uk.gov.hmrc.apiplatform.modules.common.domain.services.SimpleEnumJsonFormatting

enum LoggedInState {
  case LoggedIn, PartLoggedInEnablingMfa
}

object LoggedInState {

  def apply(text: String): Option[LoggedInState] = LoggedInState.values.find(_.toString.equalsIgnoreCase(text))

  def unsafeApply(text: String): LoggedInState = {
    apply(text).getOrElse(throw new RuntimeException(s"$text is not a valid Logged In State"))
  }

  implicit val format: Format[LoggedInState] = SimpleEnumJsonFormatting.screamingSnakeCaseFormatFor[LoggedInState]("Logged In State", apply(_))
}
