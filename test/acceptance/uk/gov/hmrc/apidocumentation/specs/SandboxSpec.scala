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

import acceptance.uk.gov.hmrc.apidocumentation.SandboxBaseSpec
import acceptance.uk.gov.hmrc.apidocumentation.pages._
import org.scalatest.Tag
import org.scalatest.prop.TableDrivenPropertyChecks
import utils.uk.gov.hmrc.apidocumentation.mocks.TableDrivenPropertyMocks

class SandboxSpec extends SandboxBaseSpec with ComponentTestsSpec with TableDrivenPropertyChecks with TableDrivenPropertyMocks {

  feature("Sandbox - Navigation across documentation") {

    scenario("Strategic Sandbox - API Base URL", Tag("SandboxTest")) {

      val expectedBaseUrl = "https://test-api.service.hmrc.gov.uk/"

      Given("I have navigated to the API documentation page on environment External Test")
      Given apiServicesIsDeployed()
      goOn(APIDocumentationPage)
      on(APIDocumentationPage)

      Given("I have navigated to the Reference Guide page on environment External Test")
      goOn(ReferenceGuidePage)
      on(ReferenceGuidePage)

      Then("following base url is displayed")
      ReferenceGuidePage.baseUrl shouldBe expectedBaseUrl

      Given("I have navigated to the Authorisation page on environment External Test")
      goOn(AuthorisationPage)
      on(AuthorisationPage)

      And("I click on user restricted end point")
      click on linkText("User-restricted endpoints")

      Then("following base url is displayed")
      AuthorisationPage.baseUrl contains expectedBaseUrl
    }

    scenario("Strategic Sandbox - Dev Hub Name", Tag("SandboxTest")) {
      val expectedApplicationName  = "HMRC Developer Sandbox"
      Given("I have navigated to the Home page on environment External Test")
      goOn(HomePage)
      on(HomePage)

      Then("the application name is HMRC Developer Sandbox")
      HomePage.applicationName shouldBe expectedApplicationName

      Given("I have navigated to the API Documentation Page on environment External Test")
      Given apiServicesIsDeployed()
      goOn(APIDocumentationPage)
      on(APIDocumentationPage)

      Then("the application name is HMRC Developer Sandbox")
      APIDocumentationPage.applicationName shouldBe expectedApplicationName

      Given("I have navigated to the Hello World Page on environment External Test")
      Given helloWorldIsDeployed("api-example-microservice", "1.0")
      goOn(HelloWorldPage)
      on(HelloWorldPage)

      Then("the application name is HMRC Developer Sandbox")
      HelloWorldPage.applicationName shouldBe expectedApplicationName
    }
  }
}
