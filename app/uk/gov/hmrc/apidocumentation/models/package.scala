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

import play.api.libs.json._

package object jsonFormatters {
  implicit val formatAPICategory: Format[APICategory.Value]     = enumJson.enumFormat(APICategory)
  implicit val formatLoggedInState: Format[LoggedInState.Value] = enumJson.enumFormat(LoggedInState)

  implicit val formatAPIAccess: OFormat[APIAccess]                 = Json.format[APIAccess]
  implicit val formatVersionVisibility: OFormat[VersionVisibility] = Json.format[VersionVisibility]
  implicit val formatServiceDetails: OFormat[ServiceDetails]       = Json.format[ServiceDetails]
  implicit val formatTestEndpoint: OFormat[TestEndpoint]           = Json.format[TestEndpoint]
  implicit val formatDeveloper: OFormat[Developer]                 = Json.format[Developer]
  implicit val formatSession: OFormat[Session]                     = Json.format[Session]
  implicit val formatSidebarLink: OFormat[SidebarLink]             = Json.format[SidebarLink]
  implicit val formatNavLink: OFormat[NavLink]                     = Json.format[NavLink]
  implicit val formatErrorResponse: OFormat[ErrorResponse]         = Json.format[ErrorResponse]
}

package object enumJson       {

  def enumReads[E <: Enumeration](`enum`: E): Reads[E#Value] = new Reads[E#Value] {

    def reads(json: JsValue): JsResult[E#Value] = json match {
      case JsString(s) => {
        try {
          JsSuccess(`enum`.withName(s))
        } catch {
          case _: NoSuchElementException =>
            JsError(s"Enumeration expected of type: '${`enum`.getClass}', but it does not contain '$s'")
        }
      }
      case _           => JsError("String value expected")
    }
  }

  implicit def enumWrites[E <: Enumeration]: Writes[E#Value] = new Writes[E#Value] {
    def writes(v: E#Value): JsValue = JsString(v.toString)
  }

  implicit def enumFormat[E <: Enumeration](`enum`: E): Format[E#Value] = {
    Format(enumReads(`enum`), enumWrites)
  }
}
