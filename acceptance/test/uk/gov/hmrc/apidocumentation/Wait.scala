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

package uk.gov.hmrc.apidocumentation

import scala.concurrent.duration._

import org.openqa.selenium._
import org.scalatest.concurrent.Eventually

trait Wait extends Eventually {

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = 2 seconds)

  def waitForPageToReload(oldPageElement: WebElement): Unit = {
    eventually {
      try {
        oldPageElement.getText
        throw new RuntimeException("Old element still present")
      } catch {
        case _: StaleElementReferenceException => ()
      }
    }
  }
}
