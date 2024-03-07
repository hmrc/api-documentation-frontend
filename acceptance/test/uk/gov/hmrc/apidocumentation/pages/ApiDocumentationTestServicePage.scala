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

import org.openqa.selenium.By
import org.openqa.selenium.support.ui.Select
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks

import uk.gov.hmrc.apidocumentation.pages.APIDocumentationPage.waitForPageToStopMoving
import uk.gov.hmrc.apidocumentation.{Env, WebPage}

object ApiDocumentationTestServicePage extends WebPage with TableDrivenPropertyChecks with Matchers with Eventually {

  override val url = s"http://localhost:${Env.port}/api-documentation/docs/api/service/api-documentation-test-service/1.0"

  val pageHeading = "Developer Forum API"

  private val locationFieldOptional = By.className("parameter-optional")
  private val createUser            = By.className("govuk-details__summary")

  def selectCreateUser(): Unit = {
    click(createUser)
    waitForPageToStopMoving()
  }

  def checkVersionSortOrder(): Unit = {
    val versions =
      Table(
        ("SortNumber", "Version"),
        ("0", "v2.0 (Alpha)"),
        ("1", "v1.5 (Beta)"),
        ("2", "v1.3 (Beta)"),
        ("3", "v1.2 (Beta)"),
        ("4", "v1.1 (Stable)"),
        ("5", "v1.0 (Stable)"),
        ("6", "v0.4 (Deprecated)"),
        ("7", "v0.3 (Deprecated)"),
        ("8", "v0.2 (Deprecated)")
      )
    forAll(versions) { (sortNumber: String, version: String) =>
      val indexes = sortNumber.toInt
      eventually {
        val DropdownList = new Select(waitForElementToBePresent(By.id("version")))
        DropdownList.getOptions.get(indexes).getText shouldBe version
      }
    }
  }

  def checkDefaultVersion(expectedVersion: String): Unit = {
    val versionDropDown = new Select(waitForElementToBePresent(By.id("version")))
    eventually {
      versionDropDown.getFirstSelectedOption.getText shouldBe expectedVersion
    }
  }

  def checkLocationFieldIsOptional(): Unit = {
    getText(locationFieldOptional) should include("optional")
  }

  def checkAPIVersionInRequestHeader(): Unit = {
    click(By.id("users_post_post_details"))
    getText(By.id("application/vnd.hmrc.1.1+json")) should include("application/vnd.hmrc.1.1+json")

  }

}
