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
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks

import uk.gov.hmrc.apidocumentation.pages.APIDocumentationPage.waitForPageToStopMoving
import uk.gov.hmrc.apidocumentation.{Env, WebPage}

object ApiDocumentationTestServicePage extends WebPage with TableDrivenPropertyChecks with Matchers with Eventually {

  override val url = s"http://localhost:${Env.port}/api-documentation/docs/api/service/api-documentation-test-service/1.0"

  val pageHeading = "Developer Forum API"

  private val createUser = By.className("govuk-details__summary")

  def selectCreateUser(): Unit = {
    click(createUser)
    waitForPageToStopMoving()
  }

  def checkVersionsInTable(): Unit = {
    val versions =
      Table(
        ("version-2.0", "Version 2.0 - alpha"),
        ("version-1.5", "Version 1.5 - beta"),
        ("version-1.3", "Version 1.3 - beta"),
        ("version-1.2", "Version 1.2 - beta"),
        ("version-1.1", "Version 1.1 - stable"),
        ("version-1.0", "Version 1.0 - stable"),
        ("version-0.4", "Version 0.4 - deprecated"),
        ("version-0.3", "Version 0.3 - deprecated"),
        ("version-0.2", "Version 0.2 - deprecated")
      )
    forAll(versions) { (versionId: String, expectedVersionText: String) =>
      eventually {
        waitForElementToBePresent(By.id(versionId)).getText should include(expectedVersionText)
        waitForElementToBePresent(By.id(versionId)).getText should include("(opens in new tab)")
      }
    }
  }

  def checkDefaultVersion(expectedVersionText: String): Unit = {
    eventually {
      waitForElementToBePresent(By.id("currentVersion")).getText shouldBe expectedVersionText
    }
  }

  def checkApiType(apiType: String): Unit = {
    eventually {
      waitForElementToBePresent(By.id("apiType")).getText shouldBe apiType
    }
  }

  def checkSubordinateName(subordinateName: String): Unit = {
    eventually {
      waitForElementToBePresent(By.id("subordinateName")).getText shouldBe subordinateName
    }
  }

  def checkSubordinateUrl(subordinateUrl: String): Unit = {
    eventually {
      waitForElementToBePresent(By.id("subordinateUrl")).getText shouldBe subordinateUrl
    }
  }

  def checkPrincipalName(principalName: String): Unit = {
    eventually {
      waitForElementToBePresent(By.id("principalName")).getText shouldBe principalName
    }
  }

  def checkPrincipalUrl(principalUrl: String): Unit = {
    eventually {
      waitForElementToBePresent(By.id("principalUrl")).getText shouldBe principalUrl
    }
  }

}
