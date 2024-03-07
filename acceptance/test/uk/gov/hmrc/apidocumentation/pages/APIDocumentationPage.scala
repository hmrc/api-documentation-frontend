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

package uk.gov.hmrc.apidocumentation.pages

import org.openqa.selenium.By

import uk.gov.hmrc.apidocumentation.{Env, WebPage}

object APIDocumentationPage extends WebPage with HasApplicationName {

  override val url = s"http://localhost:${Env.port}/api-documentation/docs/api"

  val pageHeading = "API documentation"

  private val helloWorldLink = By.linkText("Hello World")
  private val docTestLink    = By.linkText("API Documentation Test")
  private val detailsSection = By.cssSelector("""main:not([style*="margin-top"])""")

  def selectHelloWorld(): Unit = {
    click(helloWorldLink)
    waitForPageToStopMoving()
  }

  def selectAPIDocumentationTestService(): Unit = {
    click(docTestLink)
    waitForPageToStopMoving()
  }

  def waitForPageToStopMoving() = waitForElementToBePresent(detailsSection)

}
