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

package unit.uk.gov.hmrc.apidocumentation.models

import org.scalatest.{Matchers, WordSpec}
import play.api.data.validation.ValidationError
import play.api.libs.json._
import uk.gov.hmrc.apidocumentation.models.EnumerationValue

class EnumerationValueSpec extends WordSpec with Matchers {

  trait Setup {
    def read(json: JsValue): JsResult[EnumerationValue] =
      EnumerationValue.format.reads(json)
  }

  "schema enumerations" when {
    "strings" should {
      "return a enumeration value containing the string" in new Setup {
        read(JsString("example string")) shouldBe JsSuccess(EnumerationValue("example string"))
      }
    }

    "booleans" should {
      "return a enumeration value containing true" in new Setup {
        read(JsBoolean(true)) shouldBe JsSuccess(EnumerationValue("true"))
      }

      "return a enumeration value containing false" in new Setup {
        read(JsBoolean(false)) shouldBe JsSuccess(EnumerationValue("false"))
      }
    }

    "numbers" should {
      "return a enumeration value containing a number" in new Setup {
        read(JsNumber(BigDecimal(10000000000L))) shouldBe JsSuccess(EnumerationValue("10000000000"))
      }
    }

    "objects" should {
      "fail due to complexity of showing an object in html form (one Of should be used)" in new Setup {
        read(JsObject(Map("value" -> JsString("x")))) shouldBe JsError(ValidationError(List("Unsupported enum format (Json object): use NUMBER, BOOLEAN OR STRING")))
      }
    }

    "arrays" should {
      "fail due to complexity of showing an array in html form" in new Setup {
        read(JsArray(Seq(Json.obj("value" -> "x")))) shouldBe JsError(ValidationError(List("Unsupported enum format (Json array): use NUMBER, BOOLEAN OR STRING")))
      }
    }
  }
}
