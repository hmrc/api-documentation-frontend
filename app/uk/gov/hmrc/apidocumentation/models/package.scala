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
  implicit val formatVersionVisibility: OFormat[VersionVisibility] = Json.format[VersionVisibility]
  implicit val formatServiceDetails: OFormat[ServiceDetails]       = Json.format[ServiceDetails]
  implicit val formatTestEndpoint: OFormat[TestEndpoint]           = Json.format[TestEndpoint]
  implicit val formatDeveloper: OFormat[Developer]                 = Json.format[Developer]
  implicit val formatSession: OFormat[Session]                     = Json.format[Session]
  implicit val formatSidebarLink: OFormat[SidebarLink]             = Json.format[SidebarLink]
  implicit val formatNavLink: OFormat[NavLink]                     = Json.format[NavLink]
  implicit val formatErrorResponse: OFormat[ErrorResponse]         = Json.format[ErrorResponse]
}
