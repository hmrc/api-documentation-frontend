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

package uk.gov.hmrc.apidocumentation

import java.time.Duration
import scala.jdk.CollectionConverters._

import org.openqa.selenium.support.ui.{ExpectedConditions, FluentWait, Select, Wait}
import org.openqa.selenium.{By, Keys, WebDriver, WebElement}

import uk.gov.hmrc.selenium.webdriver.Driver

trait PageObject {

  protected def click(locator: By): Unit = {
    waitForElementToBePresent(locator)
    unsafeFindElement(locator).click()
  }

  protected def get(url: String): Unit =
    Driver.instance.get(url)

  protected def getCurrentUrl: String =
    Driver.instance.getCurrentUrl

  protected def getPageSource: String =
    Driver.instance.getPageSource

  protected def getText(locator: By): String = {
    waitForElementToBePresent(locator)
    unsafeFindElement(locator).getText
  }

  protected def isDisplayed(locator: By): Boolean = {
    waitForElementToBePresent(locator)
    findElement(locator).fold(false)(_.isDisplayed())
  }

  protected def getTitle: String =
    Driver.instance.getTitle

  protected def waitForInvisibilityOfElementWithText(locator: By, value: String): Boolean =
    fluentWait.until(ExpectedConditions.invisibilityOfElementWithText(locator, value))

  protected def findElement(locator: By): Option[WebElement] =
    findElements(locator).headOption

  protected def findElements(locator: By): List[WebElement] =
    Driver.instance.findElements(locator).asScala.toList

  protected def waitForElementToBePresent(locator: By): WebElement =
    fluentWait.until(ExpectedConditions.presenceOfElementLocated(locator))

  protected def fluentWait: Wait[WebDriver] = new FluentWait[WebDriver](Driver.instance)
    .withTimeout(Duration.ofSeconds(3))
    .pollingEvery(Duration.ofSeconds(1))

  private def unsafeFindElement(locator: By): WebElement =
    findElement(locator).get
}

trait TextInput {
  self: PageObject =>

  private def clear(locator: By): Unit = {
    waitForElementToBePresent(locator)
    findElement(locator).get.clear()
  }

  protected def sendKeys(locator: By, value: String): Unit = {
    clear(locator)
    findElement(locator).get.sendKeys(value)
  }

  protected def sendKeys(locator: By, keys: Keys*): Unit = {
    clear(locator)
    val element = findElement(locator).get
    keys.foreach(key => element.sendKeys(key))
  }
}

trait CheckBox {
  self: PageObject =>

  protected def isSelected(locator: By): Boolean =
    findElement(locator).get.isSelected()

  protected def selectCheckbox(locator: By): Unit =
    if (!isSelected(locator))
      click(locator)

  protected def deselectCheckbox(locator: By): Unit =
    if (isSelected(locator))
      click(locator)

}

trait SelectChoice {
  self: PageObject =>

  private def withSelect(locator: By)(action: Select => Unit): Unit = {
    waitForElementToBePresent(locator)
    val select = new Select(findElement(locator).get)
    action(select)
  }

  protected def selectByValue(locator: By, value: String): Unit =
    withSelect(locator)(_.selectByValue(value))

  protected def deselectByValue(locator: By, value: String): Unit =
    withSelect(locator)(_.deselectByValue(value))

  protected def selectByVisibleText(locator: By, value: String): Unit =
    withSelect(locator)(_.selectByVisibleText(value))

  protected def deselectByVisibleText(locator: By, value: String): Unit =
    withSelect(locator)(_.deselectByVisibleText(value))
}
