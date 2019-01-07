/*
 * Copyright 2019 HM Revenue & Customs
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

package acceptance.uk.gov.hmrc.apidocumentation

import java.util.concurrent.TimeUnit

import org.openqa.selenium._
import org.openqa.selenium.support.ui._
import org.scalatest.Matchers
import org.scalatest.concurrent.Eventually
import org.scalatest.selenium.{Page, WebBrowser}

trait WebPage extends Page with WebBrowser with Matchers with Eventually {

  implicit val webDriver: WebDriver = Env.driver

  def isCurrentPage: Boolean = false

  def heading = tagName("h1").element.text

  def bodyText = tagName("body").element.text

  def containsFragment(fragment: String) =
    webDriver.getPageSource.contains(fragment)

  def dumpPageSource =
    println(webDriver.getPageSource)

  def clickElement(elementId: String) {
    click on id(elementId)
  }

  def on(page: WebPage)(implicit webDriver: WebDriver) = {
    eventually {
      webDriver.findElement(By.tagName("body"))
    }
    withClue(s"Currently in page: $currentUrl " + find(tagName("h1")).map(_.text).fold(" - ")(h1 => s", with title '$h1' - ")) {
      assert(page.isCurrentPage, s"Page was not loaded: ${page.url}")
    }
  }

  def waitForElement(by: By, timeout: Int  = 5): WebElement = {
    val wait = new FluentWait[WebDriver](Env.driver).withTimeout(timeout, TimeUnit.SECONDS).pollingEvery(500, TimeUnit.MILLISECONDS)
      .ignoring(classOf[NoSuchElementException],classOf[StaleElementReferenceException])
    wait.until(ExpectedConditions.visibilityOfElementLocated(by))
  }
}
