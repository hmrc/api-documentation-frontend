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

package uk.gov.hmrc.apidocumentation.pages

import uk.gov.hmrc.apidocumentation.{Env, WebPage}
import org.openqa.selenium.support.ui.WebDriverWait
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.By
import org.scalatestplus.selenium.WebBrowser

object APIDocumentationPage extends WebPage {

  override val url = s"http://localhost:${Env.port}/api-documentation/docs/api"

  override def isCurrentPage: Boolean = find(className("govuk-heading-xl")).fold(false)(_.text == "API documentation")

  def selectHelloWorld() {
    val helloWorldLink = find(linkText("Hello World")).get
    click on helloWorldLink
    waitForPageToStopMoving()
  }

  def selectAPIDocumentationTestService() {
    val apiDocumentationTestService = find(linkText("API Documentation Test")).get
    click on apiDocumentationTestService
    waitForPageToStopMoving()
  }

  def applicationName = WebBrowser.id("HMRC-Developer-Hub").element.text

  def waitForPageToStopMoving() = {
    new WebDriverWait(webDriver, 5).until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("""main:not([style*="margin-top"])""")))
  }

}
