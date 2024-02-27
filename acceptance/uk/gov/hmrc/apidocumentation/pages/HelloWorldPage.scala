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

import uk.gov.hmrc.apidocumentation.{Env, WebPage}
import org.openqa.selenium.By
import org.scalatest.prop.TableDrivenPropertyChecks
import org.openqa.selenium.support.ui.WebDriverWait
import org.openqa.selenium.support.ui.ExpectedConditions
import java.time.Duration
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.selenium.WebBrowser
import uk.gov.hmrc.selenium.webdriver.Driver

object HelloWorldPage extends WebPage with HasApplicationName with TableDrivenPropertyChecks with Matchers with WebBrowser {

  val pageHeading = "Hello World API"

  override val url = s"http://localhost:${Env.port}/api-documentation/docs/api/service/api-example-microservice/1.0"

  private val breadcrumbs = By.cssSelector(".govuk-breadcrumbs__list")
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

  def assertEndpointsDetails(): Unit = {
    val ids =
      Table(
        ("ID", "id"),
        ("_say-hello-world_get_details", "_say-hello-world_get_content"),
        ("_say-hello-user_get_details", "_say-hello-user_get_content"),
        ("_say-hello-application_get_details", "_say-hello-application_get_content")
      )
    forAll(ids) { (ID: String, id: String) =>
      click(By.id(ID))
      isDisplayed(By.id(id)) shouldBe true
    }
  }

  def assertAPIEndpoints(): Unit = {
    val endpoints =
      Table(
        ("ID", "Endpoint Title", "Endpoint Request Type", "Endpoint URI"),
        ("_say-hello-world_get_details", "Say hello world", "GET", "/hello/world"),
        ("_say-hello-user_get_details", "Say hello user", "GET", "/hello/user"),
        ("_say-hello-application_get_details", "Say hello application", "GET", "/hello/application")
      )

    forAll(endpoints) { (id: String, endpointTitle: String, endpointRequestType: String, endpointUri: String) =>
      getText(By.id(id)) should endWith(endpointRequestType)
      getText(By.id(endpointUri)) shouldBe endpointUri
    }
  }

  def assertOptionsEndpointsNotPresent() = {
    val endpoints =
      Table(
        "ID",
        "#_say-hello-world-supported-http-methods",
        "#_say-hello-user-supported-http-methods",
        "#_say-hello-application-supported-http-methods"
      )

    forAll(endpoints) { (id: String) =>
      findElement(By.cssSelector(s"${id}_options_accordion .accordion__button")) shouldBe None
      findElement(By.cssSelector(s"${id}_options_accordion .http-verb.http-verb--options.float--right")) shouldBe None
    }
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
      val colorAfterClick = findElement(locator).get.getCssValue("color")

      backgroundColorAfterClick.replace(" ", "") shouldBe "rgba(255, 255, 255, 1)".replace(" ", "")
      assert(!colorAfterClick.equalsIgnoreCase(backgroundColorAfterClick))
    }
  }

  def assertLeftMenuIsDisplayed(): Unit = {
    val navigationItems =
      Table(
        ("Navigation links", "Number"),
        ("Overview", "1"),
        ("Versioning", "2"),
        ("Errors", "3"),
        ("Endpoints", "4")
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
        "Versioning",
        "Errors",
        "Endpoints")
    forAll(navigationItems) { (navigationLink: String) =>
      val links = By.linkText(navigationLink)
      click(links)
      val id = navigationLink.toLowerCase
      var position = 0

      waitForPageToStopMoving()
      position = executeScript(s"return document.getElementById('$id').getBoundingClientRect().top;")(Driver.instance).toString.toDouble.toInt
      // assert(position == 0)
    }
  }

  def waitForPageToStopMoving() = {
    new WebDriverWait(Driver.instance, Duration.ofSeconds(5)).until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("""main:not([style*="margin-top"])""")))
  }

  def checkBackToTopLinkAfterErrorsSection(): Unit = {
    isDisplayed(errorsBackToTop) shouldBe true
    getText(errorsBackToTop) shouldBe "Skip to main content"
    isDisplayed(endpointsBackToTop) shouldBe true
    getText(endpointsBackToTop) shouldBe "Skip to main content"
  }
}

