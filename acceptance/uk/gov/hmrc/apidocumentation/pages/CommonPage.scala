/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.apidocumentation.pages

import uk.gov.hmrc.apidocumentation.{Env, WebPage}
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.Select
import org.scalatest.prop.TableDrivenPropertyChecks
import uk.gov.hmrc.apidocumentation.Wait

object CommonPage extends WebPage with TableDrivenPropertyChecks with Wait {
  
  val pageHeading = "???"

  override val url = s"http://localhost:${Env.port}/api-documentation/docs/api/service/api-documentation-test-service/1.0"

  def selectVersion(expectedVersion: String): Unit = {
    val versionDropDown = new Select(waitForElementToBePresent(By.id("version")))
    versionDropDown.selectByVisibleText(expectedVersion)
    val firstSelectedOption = versionDropDown.getFirstSelectedOption
    firstSelectedOption.submit()
    waitForPageToReload(firstSelectedOption)
  }
}