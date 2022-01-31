/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.apidocumentation.models.apispecification

import play.api.libs.json._

case class EnumerationValue(value: String)

object EnumerationValue {

  implicit val format: Format[EnumerationValue] = new Format[EnumerationValue] {
    override def writes(o: EnumerationValue): JsValue = Json.writes[EnumerationValue].writes(o)

    override def reads(json: JsValue): JsResult[EnumerationValue] = json match {
      case JsNumber(value)  => JsSuccess(EnumerationValue(value.toString))
      case JsBoolean(value) => JsSuccess(EnumerationValue(value.toString))
      case JsString(value)  => JsSuccess(EnumerationValue(value))
      case JsObject(_)      => JsError("Unsupported enum format (Json object): use NUMBER, BOOLEAN OR STRING")
      case JsArray(_)       => JsError("Unsupported enum format (Json array): use NUMBER, BOOLEAN OR STRING")
      case JsNull           => JsError("Unsupported enum format (Json null): use NUMBER, BOOLEAN OR STRING")
    }
  }
}
