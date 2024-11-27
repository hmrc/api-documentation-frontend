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

import java.time.Duration

import org.openqa.selenium.By
import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait}
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.selenium.WebBrowser

import uk.gov.hmrc.selenium.webdriver.Driver

import uk.gov.hmrc.apidocumentation.{Env, WebPage}

object HelloWorldPage extends WebPage with HasApplicationName with TableDrivenPropertyChecks with Matchers with WebBrowser {

  val pageHeading = "Hello World API"

  override val url = s"http://localhost:${Env.port}/api-documentation/docs/api/service/api-example-microservice/1.0"

  private val breadcrumbs     = By.cssSelector(".govuk-breadcrumbs__list")
  private val errorsBackToTop = By.id("bottom-skip-to-main")

  def breadCrumbText = getText(breadcrumbs)

  private val endpointsBackToTop = By.id("middle-skip-to-main")

  def selectErrorsBackToTop(): Unit = {
    click(errorsBackToTop)
    waitForPageToStopMoving()
  }

  def selectEndpointsBackToTop(): Unit = {
    click(endpointsBackToTop)
    waitForPageToStopMoving()
  }

  def assertMenuLinkColors(): Unit = {
    val menuLink =
      Table(
        "menuLink",
        "Overview",
        "Versioning",
        "Errors",
        "Endpoints"
      )

    forAll(menuLink) { (menuLink: String) =>
      val locator = By.linkText(menuLink)
      click(locator)

      val backgroundColorAfterClick = findElement(locator).get.getCssValue("background-color")
      val colorAfterClick           = findElement(locator).get.getCssValue("color")

      backgroundColorAfterClick.replace(" ", "") shouldBe "rgba(255, 255, 255, 1)".replace(" ", "")
      assert(!colorAfterClick.equalsIgnoreCase(backgroundColorAfterClick))
    }
  }

  def assertLeftMenuIsDisplayed(): Unit = {
    val navigationItems =
      Table(
        ("Navigation links", "Number"),
        ("Overview", "1"),
        ("Errors", "2"),
        ("Testing", "3"),
        ("Versioning", "4"),
        ("Endpoints", "5")
      )
    forAll(navigationItems) { (navigationLink: String, number: String) =>
      val expectedCSSSelector = By.cssSelector("nav.side-nav > ul > li:nth-of-type(" + number + ") > a")
      getText(expectedCSSSelector) shouldBe navigationLink
    }
  }

  def waitUntilLinksGetToTheTopOfThePage(): Unit = {
    val navigationItems =
      Table(
        "Navigation links",
        "Overview",
        "Errors",
        "Testing",
        "Versioning",
        "Endpoints"
      )
    forAll(navigationItems) { (navigationLink: String) =>
      val links    = By.linkText(navigationLink)
      click(links)
      val id       = navigationLink.toLowerCase
      var position = 0

      waitForPageToStopMoving()
      position = executeScript(s"return document.getElementById('$id').getBoundingClientRect().top;")(Driver.instance).toString.toDouble.toInt
      // assert(position == 0)
    }
  }

  def waitForPageToStopMoving() = {
    new WebDriverWait(Driver.instance, Duration.ofSeconds(5)).until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("""main:not([style*="margin-top"])""")))
  }
}
