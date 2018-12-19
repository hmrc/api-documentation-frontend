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

package acceptance.uk.gov.hmrc.apidocumentation.specs

import acceptance.uk.gov.hmrc.apidocumentation.pages._
import acceptance.uk.gov.hmrc.apidocumentation.BaseSpec
import acceptance.uk.gov.hmrc.apidocumentation.pages.APIDocumentationPage
import org.scalatest.Tag
import org.scalatest.prop.TableDrivenPropertyChecks
import utils.uk.gov.hmrc.apidocumentation.mocks.TableDrivenPropertyMocks

class DocumentationSpec extends BaseSpec with ComponentTestsSpec with TableDrivenPropertyChecks with TableDrivenPropertyMocks {

  feature("API Documentation") {

    scenario("Show endpoint page for the default version of a selected API") {
      Given("I have navigated to the API documentation page")
      Given apiServicesIsDeployed()
      goOn(APIDocumentationPage)
      on(APIDocumentationPage)

      When("I select to view the Hello World documentation")
      Given helloWorldIsDeployed("api-example-microservice", "1.0")
      APIDocumentationPage.selectHelloWorld()
      on(HelloWorldPage)

      Then("I see the following API endpoints for the default version of my selected API")
      HelloWorldPage.assertAPIEndpoints()
    }

    scenario("OPTIONS endpoints are not displayed for the selected API") {
      Given("I have navigated to the API documentation page")
      Given apiServicesIsDeployed()
      goOn(APIDocumentationPage)
      on(APIDocumentationPage)

      When("I select to view the Hello World documentation")
      Given helloWorldIsDeployed("api-example-microservice", "1.0")
      APIDocumentationPage.selectHelloWorld()
      on(HelloWorldPage)

      Then("I do not see the OPTIONS API endpoints for the selected API")
      HelloWorldPage.assertOptionsEndpointsNotPresent()
    }

    scenario("Breadcrumb for the API endpoint") {
      Given("I have navigated to the API documentation page")
      Given apiServicesIsDeployed()
      goOn(APIDocumentationPage)
      on(APIDocumentationPage)

      When("I select to view the Hello World documentation")
      Given helloWorldIsDeployed("api-example-microservice", "1.0")
      APIDocumentationPage.selectHelloWorld()
      on(HelloWorldPage)

      Then("the breadcrumb is Home>API Documentation>Hello World API v1.0 (Stable)")
      val breadcrumb = "Home>API Documentation>Hello World API v1.0 (Stable)"
      withClue(s"Breadcrumb $breadcrumb has not been found") {
        HelloWorldPage.breadCrumbText.replaceAll("\\s", "") shouldBe breadcrumb.replaceAll("\\s", "").replaceAll(">", "")
      }
    }

    scenario("Ensure end point details are displayed when endpoint HTTP verb button is selected") {
      Given("I have navigated to the API documentation page")
      Given apiServicesIsDeployed()
      goOn(APIDocumentationPage)
      on(APIDocumentationPage)

      When("I select to view the Hello World documentation")
      Given helloWorldIsDeployed("api-example-microservice", "1.0")
      APIDocumentationPage.selectHelloWorld()
      on(HelloWorldPage)
      loadPage()

      Then("I can see the details of the following end points when a HTTP verb button is clicked on")
      HelloWorldPage.assertEndpointsDetails()
    }

    scenario("Ensure the same version that is displayed on the API index page is also displayed by default when API Documentation Test Service is selected") {
      Given apiServicesIsDeployed()
      Given apiDocumentationTestServiceVersionsIsDeployed()

      Given("I have navigated to the API documentation page")
      goOn(APIDocumentationPage)
      on(APIDocumentationPage)

      When("I select to view the API  Documentation Test documentation")
      APIDocumentationPage.selectAPIDocumentationTestService()

      Then("the default version 'v1.1 (Stable)' is displayed as selected")
      ApiDocumentationTestServicePage.checkDefaultVersion("v1.1 (Stable)")
    }

    scenario("Ensure all API versions are sorted correctly and can be viewed by the user") {
      Given apiServicesIsDeployed()
      Given apiDocumentationTestServiceVersionsIsDeployed()

      Given("I have navigated to the API documentation page")
      goOn(APIDocumentationPage)
      on(APIDocumentationPage)

      When("I select to view the API  Documentation Test documentation")
      APIDocumentationPage.selectAPIDocumentationTestService()

      Then("all applicable API versions are displayed and sorted in the following order")
      ApiDocumentationTestServicePage.checkVersionSortOrder()
    }

    scenario("Optional header displays as 'optional' in API docs") {
      Given apiServicesIsDeployed()
      Given apiDocumentationTestServiceVersionsIsDeployed()

      Given("I have navigated to the API documentation page")
      goOn(APIDocumentationPage)
      on(APIDocumentationPage)

      When("I select to view the API  Documentation Test documentation")
      APIDocumentationPage.selectAPIDocumentationTestService()

      And("I select version 'v1.5 (Beta)")
      CommonPage.selectVersion("v1.5 (Beta)")

      And("I click on the 'Create user' link")
      clickOnLink("Create user")

      Then("location field is optional")
      ApiDocumentationTestServicePage.checkLocationFieldIsOptional()
    }

    scenario("Ensure user can access the Hello World API Summary Details page and view all endpoints for a Beta version") {
      Given("I have navigated to the API documentation page")
      Given apiServicesIsDeployed()
      goOn(APIDocumentationPage)
      on(APIDocumentationPage)

      When("I select to view the Hello World documentation")
      helloWorldVersionsIsDeployed()
      APIDocumentationPage.selectHelloWorld()
      on(HelloWorldPage)

      And("I select version 'v1.2 (Beta)")
      CommonPage.selectVersion("v1.2 (Beta)")

      Then("I can see the details of the following end points when a HTTP verb button is clicked on")
      HelloWorldPage.assertEndpointsDetails()
    }

    scenario("Update API Version in the request header") {
      Given apiServicesIsDeployed()
      Given apiDocumentationTestServiceVersionsIsDeployed()

      Given("I have navigated to the API documentation page")
      goOn(APIDocumentationPage)
      on(APIDocumentationPage)

      When("I select to view the API  Documentation Test documentation")
      APIDocumentationPage.selectAPIDocumentationTestService()

      And("I select version 'v1.1 (Stable)")
      CommonPage.selectVersion("v1.1 (Stable)")

      Then("API version in the request header matches the API version selected")
      ApiDocumentationTestServicePage.checkAPIVersionInRequestHeader()
    }
  }
}
