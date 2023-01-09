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

package uk.gov.hmrc.apidocumentation.specs
import uk.gov.hmrc.apidocumentation.BaseSpec

import uk.gov.hmrc.apidocumentation.pages._
import org.scalatest.prop.TableDrivenPropertyChecks
import uk.gov.hmrc.apidocumentation.TableDrivenPropertyMocks

class NavigationSpec extends BaseSpec with ComponentTestsSpec with TableDrivenPropertyChecks with TableDrivenPropertyMocks {

  def getPageYOffset(): Int = {
    executeScript("return window.pageYOffset;").toString.toInt
  }

  Feature("Navigation across documentation") {

    ignore("User is navigated to the top when Back to top link is clicked") {
      Given("I have navigated to the API documentation page")
      Given apiServicesIsDeployed()
      goOn(APIDocumentationPage)
      on(APIDocumentationPage)

      When("I select to view the Hello World documentation")
      Given helloWorldIsDeployed("api-example-microservice", "1.0")
      APIDocumentationPage.selectHelloWorld()
      on(HelloWorldPage)
      loadPage()

      Then("user is navigated to the Top of the page when skip to main content link is clicked from the section below")
      val topLinkClickedFromSection =
        Table(
          "Section",
          "Errors",
          "Endpoints"
        )
      executeScript("window.scrollTo(0, document.body.scrollHeight)")
      val bottomY = getPageYOffset()

      forAll(topLinkClickedFromSection) {
        case "Errors" =>
          HelloWorldPage.selectErrorsBackToTop()
          val nowY = getPageYOffset()
          assert(nowY < bottomY)
        case "Endpoints" =>
          HelloWorldPage.selectEndpointsBackToTop()
          val nowY = getPageYOffset()
          assert(nowY < bottomY)
      }
    }

    Scenario("Left menu options are displayed when selected an API") {
      Given("I have navigated to the API documentation page")
      Given apiServicesIsDeployed()
      goOn(APIDocumentationPage)
      on(APIDocumentationPage)

      When("I select to view the Hello World documentation")
      Given helloWorldIsDeployed("api-example-microservice", "1.0")
      APIDocumentationPage.selectHelloWorld()
      on(HelloWorldPage)
      loadPage()

      Then("left menus are displayed")
      HelloWorldPage.assertLeftMenuIsDisplayed()
    }

    Scenario("User is navigated to the appropriate sections when user clicks on the sections on the left side") {
      Given("I have navigated to the API documentation page")
      Given apiServicesIsDeployed()
      goOn(APIDocumentationPage)
      on(APIDocumentationPage)

      When("I select to view the Hello World documentation")
      Given helloWorldIsDeployed("api-example-microservice", "1.0")
      APIDocumentationPage.selectHelloWorld()
      on(HelloWorldPage)
      loadPage()

      Then("user is navigated to the appropriate section on the page when clicked on the left Menu option")
      HelloWorldPage.waitUntilLinksGetToTheTopOfThePage()
    }

    Scenario("Dev Hub Name") {
      val expectedApplicationName = "HMRC Developer Hub"
      Given("I have navigated to the Home page")
      Given apiServicesIsDeployed()
      goOn(HomePage)
      on(HomePage)

      Then("the application name is HMRC Developer Hub")
      HomePage.applicationName shouldBe expectedApplicationName

      Given("I have navigated to the API Documentation Page")
      goOn(APIDocumentationPage)
      on(APIDocumentationPage)

      Then("the application name is HMRC Developer Hub")
      APIDocumentationPage.applicationName shouldBe expectedApplicationName

      Given("I have navigated to the Hello World Page")
      Given helloWorldIsDeployed("api-example-microservice", "1.0")
      goOn(HelloWorldPage)
      on(HelloWorldPage)

      Then("the application name is HMRC Developer Hub")
      HelloWorldPage.applicationName shouldBe expectedApplicationName
    }

    Scenario("Ensure back to the top link only exists after Errors section") {
      Given("I have navigated to the API documentation page")
      Given apiServicesIsDeployed()
      goOn(APIDocumentationPage)
      on(APIDocumentationPage)

      When("I select to view the Hello World documentation")
      Given helloWorldIsDeployed("api-example-microservice", "1.0")
      APIDocumentationPage.selectHelloWorld()
      on(HelloWorldPage)
      loadPage()

      Then("back to the top link only appears after Errors section")
      HelloWorldPage.checkBackToTopLinkAfterErrorsSection()
    }
  }
}
