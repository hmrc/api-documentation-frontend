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

package uk.gov.hmrc.apidocumentation.controllers

import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.apidocumentation.services.DocumentationService
import uk.gov.hmrc.apidocumentation.views.html._
import uk.gov.hmrc.apidocumentation.ErrorHandler

import play.api.http.Status.INTERNAL_SERVER_ERROR

import scala.concurrent.ExecutionContext.Implicits.global

class ApiDocumentationControllerSpec extends CommonControllerBaseSpec with PageRenderVerification {
  trait Setup {
    val documentationService = mock[DocumentationService]

    val errorHandler = app.injector.instanceOf[ErrorHandler]
    val mcc = app.injector.instanceOf[MessagesControllerComponents]

    private lazy val apiIndexView = app.injector.instanceOf[ApiIndexView]
    private lazy val retiredVersionJumpView = app.injector.instanceOf[RetiredVersionJumpView]
    private lazy val apisFilteredView = app.injector.instanceOf[ApisFilteredView]
    private lazy val previewDocumentationView = app.injector.instanceOf[PreviewDocumentationView]
    private lazy val serviceDocumentationView = app.injector.instanceOf[ServiceDocumentationView]
    private lazy val xmlDocumentationView = app.injector.instanceOf[XmlDocumentationView]

    val underTest = new ApiDocumentationController(
      documentationService,
      apiDefinitionService,
      navigationService,
      loggedInUserProvider,
      errorHandler,
      mcc,
      apiIndexView,
      retiredVersionJumpView,
      apisFilteredView,
      previewDocumentationView,
      serviceDocumentationView,
      xmlDocumentationView
    )
  }

  "ApiDocumentationController" when {
    "routing to the ApiIndexPage" should {
      "render the API List" in new Setup {
        theUserIsLoggedIn()
        theDefinitionServiceWillReturnApiDefinitions(List(anApiDefinition("service1", "1.0"), anApiDefinition("service2", "1.0")))

        val result = underTest.apiIndexPage(None, None, None)(request)
        verifyPageRendered(pageTitle("API Documentation"), bodyContains = Seq("API documentation"))(result)
      }

      "render the filtered API list" in new Setup {
        theUserIsLoggedIn()
        theDefinitionServiceWillReturnApiDefinitions(List(anApiDefinition("service1", "1.0"), anApiDefinition("service2", "1.0")))

        val result = underTest.apiIndexPage(None, None, Some("vat"))(request)

        verifyPageRendered(pageTitle("Filtered API Documentation"), bodyContains = Seq("Filtered API documentation", "1 document found in", "VAT"))(result)
      }

      "display the error page when the documentationService throws an exception" in new Setup {
        theUserIsLoggedIn()
        theDefinitionServiceWillFail(new Exception("Expected unit test failure"))

        val result = underTest.apiIndexPage(None, None, None)(request)

        verifyErrorPageRendered(INTERNAL_SERVER_ERROR, "Sorry, we’re experiencing technical difficulties")(result)
      }
    }
  }
}
