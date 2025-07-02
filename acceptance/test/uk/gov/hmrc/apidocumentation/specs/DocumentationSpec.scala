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

import org.scalatest.prop.TableDrivenPropertyChecks

import uk.gov.hmrc.apidocumentation.pages.{APIDocumentationPage, _}
import uk.gov.hmrc.apidocumentation.{BaseSpec, NavigationSugar, TableDrivenPropertyMocks}

class DocumentationSpec extends BaseSpec with ComponentTestsSpec with TableDrivenPropertyChecks with TableDrivenPropertyMocks with NavigationSugar {

  Feature("API Documentation") {

    Scenario("Show endpoint page for the default version of a selected API") {
      Given("I have navigated to the API documentation page")
      Given.apiServicesIsDeployed()
      goOn(APIDocumentationPage)

      When("I select to view the Hello World documentation")
      Given.helloWorldIsDeployed("api-example-microservice", "1.0")
      APIDocumentationPage.selectHelloWorld()
      on(HelloWorldPage)
    }

    Scenario("OPTIONS endpoints are not displayed for the selected API") {
      Given("I have navigated to the API documentation page")
      Given.apiServicesIsDeployed()
      goOn(APIDocumentationPage)

      When("I select to view the Hello World documentation")
      Given.helloWorldIsDeployed("api-example-microservice", "1.0")
      APIDocumentationPage.selectHelloWorld()
      on(HelloWorldPage)
    }

    Scenario("Breadcrumb for the API endpoint") {
      Given("I have navigated to the API documentation page")
      Given.apiServicesIsDeployed()
      goOn(APIDocumentationPage)

      When("I select to view the Hello World documentation")
      Given.helloWorldIsDeployed("api-example-microservice", "1.0")
      APIDocumentationPage.selectHelloWorld()
      on(HelloWorldPage)

      Then("the breadcrumb is Home>API Documentation")
      val breadcrumb = "Home>API Documentation"
      withClue(s"Breadcrumb $breadcrumb has not been found") {
        HelloWorldPage.breadCrumbText.replaceAll("\\s", "") shouldBe breadcrumb.replaceAll("\\s", "").replaceAll(">", "")
      }
    }

    Scenario("Ensure end point details are displayed when endpoint HTTP verb button is selected") {
      Given("I have navigated to the API documentation page")
      Given.apiServicesIsDeployed()
      goOn(APIDocumentationPage)

      When("I select to view the Hello World documentation")
      Given.helloWorldIsDeployed("api-example-microservice", "1.0")
      APIDocumentationPage.selectHelloWorld()
      on(HelloWorldPage)
    }

    Scenario("Ensure the same version that is displayed on the API index page is also displayed by default when API Documentation Test Service is selected") {
      Given.apiServicesIsDeployed()
      Given.apiDocumentationTestServiceVersionsIsDeployed()

      Given("I have navigated to the API documentation page")
      goOn(APIDocumentationPage)

      When("I select to view the API  Documentation Test documentation")
      APIDocumentationPage.selectAPIDocumentationTestService()

      Then("the API type 'REST' is displayed")
      ApiDocumentationTestServicePage.checkApiType("REST")

      And("the Latest version 'Version 2.0 - alpha' is displayed")
      ApiDocumentationTestServicePage.checkDefaultVersion("Version 2.0 - alpha")

      And("the heading 'Development base URL' is displayed")
      ApiDocumentationTestServicePage.checkSubordinateName("Development base URL")

      And("the Development base URL 'https://api.development.tax.service.gov.uk' is displayed")
      ApiDocumentationTestServicePage.checkSubordinateUrl("https://api.development.tax.service.gov.uk")

      And("the heading 'QA base URL' is displayed")
      ApiDocumentationTestServicePage.checkPrincipalName("QA base URL")

      And("the QA base URL 'https://api.qa.tax.service.gov.uk' is displayed")
      ApiDocumentationTestServicePage.checkPrincipalUrl("https://api.qa.tax.service.gov.uk")
    }

    Scenario("Ensure all API versions appear in the table and can be viewed by the user") {
      Given.apiServicesIsDeployed()
      Given.apiDocumentationTestServiceVersionsIsDeployed()

      Given("I have navigated to the API documentation page")
      goOn(APIDocumentationPage)

      When("I select to view the API  Documentation Test documentation")
      APIDocumentationPage.selectAPIDocumentationTestService()

      Then("all applicable API versions are displayed in the table")
      ApiDocumentationTestServicePage.checkVersionsInTable()
    }

    Scenario("Optional header displays as 'optional' in API docs") {
      Given.apiServicesIsDeployed()
      Given.apiDocumentationTestServiceVersionsIsDeployed()

      Given("I have navigated to the API documentation page")
      goOn(APIDocumentationPage)

      When("I select to view the API  Documentation Test documentation")
      APIDocumentationPage.selectAPIDocumentationTestService()

      Then("the default version 'Version 2.0 - alpha' is displayed")
      ApiDocumentationTestServicePage.checkDefaultVersion("Version 2.0 - alpha")
    }

    Scenario("Ensure user can access the Hello World API Summary Details page and view all endpoints for a Beta version") {
      Given("I have navigated to the API documentation page")
      Given.apiServicesIsDeployed()
      goOn(APIDocumentationPage)

      When("I select to view the Hello World documentation")
      helloWorldVersionsIsDeployed()
      APIDocumentationPage.selectHelloWorld()
      on(HelloWorldPage)

      Then("the default version 'Version 1.2 - beta' is displayed")
      HelloWorldPage.checkDefaultVersion("Version 1.2 - beta")
    }

    Scenario("Update API Version in the request header") {
      Given.apiServicesIsDeployed()
      Given.apiDocumentationTestServiceVersionsIsDeployed()

      Given("I have navigated to the API documentation page")
      goOn(APIDocumentationPage)

      When("I select to view the API Documentation Test documentation")
      APIDocumentationPage.selectAPIDocumentationTestService()

      Then("the default version 'Version 2.0 - alpha' is displayed")
      ApiDocumentationTestServicePage.checkDefaultVersion("Version 2.0 - alpha")
    }
  }
}
