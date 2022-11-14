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
import org.openqa.selenium.By
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.support.ui.Select
import org.scalatest.prop.TableDrivenPropertyChecks
import org.openqa.selenium.support.ui.WebDriverWait
import org.openqa.selenium.support.ui.ExpectedConditions
import org.scalatestplus.selenium.WebBrowser
import uk.gov.hmrc.apidocumentation.pages.APIDocumentationPage.waitForPageToStopMoving
import java.time.Duration

object HelloWorldPage extends WebPage with TableDrivenPropertyChecks {

  override val url = s"http://localhost:${Env.port}/api-documentation/docs/api/service/api-example-microservice/1.0"

  override def isCurrentPage: Boolean = find(id("title")).fold(false)(_.text == "Hello World API")

  def breadCrumbText = cssSelector(".govuk-breadcrumbs__list").element.text

  def errorsBackToTop = find(id("bottom-skip-to-main")).get

  def endpointsBackToTop = find(id("middle-skip-to-main")).get

  def applicationName = WebBrowser.id("HMRC-Developer-Hub").element.text

  def selectErrorsBackToTop() {
    click on errorsBackToTop
    waitForPageToStopMoving()
  }

  def selectEndpointsBackToTop() {
    click on endpointsBackToTop
    waitForPageToStopMoving()
  }

  def assertEndpointsDetails() {
    val ids =
      Table(
        ("ID", "id"),
        ("_say-hello-world_get_details", "_say-hello-world_get_content"),
        ("_say-hello-user_get_details", "_say-hello-user_get_content"),
        ("_say-hello-application_get_details", "_say-hello-application_get_content")
      )
    forAll(ids) { (ID: String, id: String) =>
      clickOn(ID)
      find(id).get.isDisplayed
    }
  }

  def assertAPIEndpoints() {
    val endpoints =
      Table(
        ("ID", "Endpoint Title", "Endpoint Request Type", "Endpoint URI"),
        ("_say-hello-world_get_details", "Say hello world", "GET", "/hello/world"),
        ("_say-hello-user_get_details", "Say hello user", "GET", "/hello/user"),
        ("_say-hello-application_get_details", "Say hello application", "GET", "/hello/application")
      )

    forAll(endpoints) { (id: String, endpointTitle: String, endpointRequestType: String, endpointUri: String) =>
      WebBrowser.id(id).element.text should endWith(endpointRequestType)
      WebBrowser.id(endpointUri).element.text shouldBe endpointUri
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
      find(cssSelector(s"${id}_options_accordion .accordion__button")) shouldBe None
      find(cssSelector(s"${id}_options_accordion .http-verb.http-verb--options.float--right")) shouldBe None
    }
  }

  def assertMenuLinkColors() {
    val menuLink =
      Table(
        "menuLink",
        "Overview",
        "Versioning",
        "Errors",
        "Endpoints"
      )

    forAll(menuLink) { (menuLink: String) =>
      val menuLinkText = find(linkText(menuLink)).get
      click on menuLinkText

      val backgroundColorAfterClick = linkText(menuLink).webElement.getCssValue("background-color")
      val colorAfterClick = linkText(menuLink).webElement.getCssValue("color")

      backgroundColorAfterClick.replace(" ", "") shouldBe "rgba(255, 255, 255, 1)".replace(" ", "")
      assert(!colorAfterClick.equalsIgnoreCase(backgroundColorAfterClick))
    }
  }

  def assertLeftMenuIsDisplayed() {
    val navigationItems =
      Table(
        ("Navigation links", "Number"),
        ("Overview", "1"),
        ("Versioning", "2"),
        ("Errors", "3"),
        ("Endpoints", "4")
      )
    forAll(navigationItems) { (navigationLink: String, number: String) =>
      val expectedCSSSelector = cssSelector("nav.side-nav > ul > li:nth-of-type(" + number + ") > a").element
      expectedCSSSelector.text shouldBe navigationLink
    }
  }

  def waitUntilLinksGetToTheTopOfThePage() {
    val navigationItems =
      Table(
        "Navigation links",
        "Overview",
        "Versioning",
        "Errors",
        "Endpoints")
    forAll(navigationItems) { (navigationLink: String) =>
      val links = find(linkText(navigationLink)).get
      click on links
      val id = navigationLink.toLowerCase
      var position = 0

      waitForPageToStopMoving()
      position = executeScript(s"return document.getElementById('$id').getBoundingClientRect().top;").toString.toDouble.toInt
      // assert(position == 0)
    }
  }

  def waitForPageToStopMoving() = {
    new WebDriverWait(webDriver, Duration.ofSeconds(5)).until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("""main:not([style*="margin-top"])""")))
  }

  def checkBackToTopLinkAfterErrorsSection(): Unit = {
    assert(errorsBackToTop.isDisplayed)
    errorsBackToTop.text shouldBe "Skip to main content"
    assert(endpointsBackToTop.isDisplayed)
    endpointsBackToTop.text shouldBe "Skip to main content"
  }
}

object ApiDocumentationTestServicePage extends WebPage with TableDrivenPropertyChecks {

  override val url = s"http://localhost:${Env.port}/api-documentation/docs/api/service/api-documentation-test-service/1.0"

  override def isCurrentPage: Boolean = find(className("page-header")).fold(false)(_.text == "Developer Forum API")

  private def locationFieldOptionalId = "parameter-optional"

  def locationFieldOptional = find(cssSelector(locationFieldOptionalId)).get


  def selectCreateUser() {
    click on className("govuk-details__summary")
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
      val DropdownList = new Select(waitForElement(By.id("version")))
      eventually {
        DropdownList.getOptions.get(indexes).getText shouldBe version
      }
    }
  }

  def checkDefaultVersion(expectedVersion: String): Unit = {
    val versionDropDown = new Select(waitForElement(By.id("version")))
    eventually {
      versionDropDown.getFirstSelectedOption.getText shouldBe expectedVersion
    }
  }

  def checkLocationFieldIsOptional(): Unit = {
    waitForElement(By.id(locationFieldOptionalId)).getText should include ("optional")
  }

  def checkAPIVersionInRequestHeader(): Unit = {
    clickOn("users_post_post_details")
    waitForElement(By.id("application/vnd.hmrc.1.1+json")).getText should include("application/vnd.hmrc.1.1+json")

  }

}

object CommonPage extends WebPage with TableDrivenPropertyChecks {
  override val url = s"http://localhost:${Env.port}/api-documentation/docs/api/service/api-documentation-test-service/1.0"

  def selectVersion(expectedVersion: String): Unit = {
    val versionDropDown = new Select(waitForElement(By.id("version")))
    versionDropDown.selectByVisibleText(expectedVersion)
    val firstSelectedOption = versionDropDown.getFirstSelectedOption
    firstSelectedOption.submit()
    waitForPageToReload(firstSelectedOption)
  }
}
