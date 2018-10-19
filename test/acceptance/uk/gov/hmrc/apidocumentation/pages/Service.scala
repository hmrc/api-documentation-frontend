/*
 * Copyright 2018 HM Revenue & Customs
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

package acceptance.uk.gov.hmrc.apidocumentation.pages

import acceptance.uk.gov.hmrc.apidocumentation.{Env, WebPage}
import org.openqa.selenium.By
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.support.ui.Select
import org.scalatest.prop.TableDrivenPropertyChecks

object HelloWorldPage extends WebPage with TableDrivenPropertyChecks {

  override val url = s"http://localhost:${Env.port}/api-documentation/docs/api/service/api-example-microservice/1.0"

  override def isCurrentPage: Boolean = find(id("title")).fold(false)(_.text == "Hello World API")

  def breadCrumbText = cssSelector(".breadcrumbs").element.text

  def errorsBackToTop = find(cssSelector("div.bold-small > a")).get

  def resourcesBackToTop = find(cssSelector("#section > div.bold-small > a")).get

  def applicationName = className("header__menu__proposition-name").element.text

  def selectErrorsBackToTop() {
    click on errorsBackToTop
  }

  def selectResourcesBackToTop() {
    click on resourcesBackToTop
  }

  def assertEndpointsDetails() {
    val ids =
      Table(
        ("ID", "id"),
        ("#_say-hello-world", "#say-hello-world"),
        ("#_say-hello-user", "#say-hello-user"),
        ("#_say-hello-application", "#say-hello-application")
      )
    forAll(ids) { (ID: String, id: String) =>
      val element = cssSelector(s"${ID}_get_accordion > div > div:nth-of-type(1) > div.accordion__row__right.align--middle > a > span.http-verb.http-verb--get.float--right").webElement
      val act = new Actions(webDriver)
      act.moveToElement(element).click().perform()
      find(cssSelector(s"$id-get > section:nth-of-type(1) > h4")).get.isDisplayed
      act.moveToElement(element).click().perform()
    }
  }

  def assertAPIEndpoints() {
    val endpoints =
      Table(
        ("ID", "Endpoint Title", "Endpoint Request Type", "Endpoint URI"),
        ("#_say-hello-world", "Say hello world", "GET", "/hello/world"),
        ("#_say-hello-user", "Say hello user", "GET", "/hello/user"),
        ("#_say-hello-application", "Say hello application", "GET", "/hello/application")
      )

    forAll(endpoints) { (id: String, endpointTitle: String, endpointRequestType: String, endpointUri: String) =>
      cssSelector(s"${id}_get_accordion .accordion__button").element.text shouldBe endpointTitle
      cssSelector(s"${id}_get_accordion .http-verb.http-verb--get.float--right").element.text shouldBe endpointRequestType
      cssSelector(s"${id}_get_accordion .snippet--inline").element.text shouldBe endpointUri
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
        "Resources"
      )

    forAll(menuLink) { (menuLink: String) =>
      val backgroundColorBeforeClick = linkText(menuLink).webElement.getCssValue("background-color")
      val colorBeforeClick = linkText(menuLink).webElement.getCssValue("color")

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
        ("Resources", "4")
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
        "Resources")
    forAll(navigationItems) { (navigationLink: String) =>
      val links = find(linkText(navigationLink)).get
      click on links
      val id = navigationLink.toLowerCase
      val startTime = System.currentTimeMillis()
      var position = 0
      do {
        position = executeScript(s"return document.getElementById('$id').getBoundingClientRect().top;").toString.toDouble.toInt
        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed > 5000) throw new RuntimeException(s"Did not scroll to $id within 5 seconds")
      } while (position != 0)
    }
  }

  def checkBackToTopLinkAfterErrorsSection(): Unit = {
    assert(errorsBackToTop.isDisplayed)
    errorsBackToTop.text shouldBe "Back to top"
    assert(resourcesBackToTop.isDisplayed)
    resourcesBackToTop.text shouldBe "Back to top"
  }
}

object ApiDocumentationTestServicePage extends WebPage with TableDrivenPropertyChecks {

  override val url = s"http://localhost:${Env.port}/api-documentation/docs/api/service/api-documentation-test-service/1.0"

  override def isCurrentPage: Boolean = find(className("page-header")).fold(false)(_.text == "Developer Forum API")

  def LocationFieldOptional = find(cssSelector("div.parameter-optional")).get


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
    LocationFieldOptional.text shouldBe "optional"
  }

  def checkAPIVersionInRequestHeader(): Unit = {
    click on linkText("post")
    cssSelector("#request-headers>table>tbody>tr>td:nth-child(2)>div").element.text shouldBe "application/vnd.hmrc.1.1+json"

  }

}

object CommonPage extends WebPage with TableDrivenPropertyChecks {
  override val url = s"http://localhost:${Env.port}/api-documentation/docs/api/service/api-documentation-test-service/1.0"

  def selectVersion(expectedVersion: String): Unit = {
    val versionDropDown = new Select(waitForElement(By.id("version")))
    versionDropDown.selectByVisibleText(expectedVersion)
    versionDropDown.getFirstSelectedOption.submit()
  }
}
