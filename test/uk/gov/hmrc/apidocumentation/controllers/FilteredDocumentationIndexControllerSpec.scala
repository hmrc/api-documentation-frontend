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

package uk.gov.hmrc.apidocumentation.controllers

import scala.concurrent.ExecutionContext.Implicits.global

import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.http.UpstreamErrorResponse

import uk.gov.hmrc.apidocumentation.ErrorHandler
import uk.gov.hmrc.apidocumentation.controllers.utils._
import uk.gov.hmrc.apidocumentation.mocks.config._
import uk.gov.hmrc.apidocumentation.mocks.services._
import uk.gov.hmrc.apidocumentation.models.DocumentationTypeFilter
import uk.gov.hmrc.apidocumentation.views.html.documentationList.FilteredIndexView

class FilteredDocumentationIndexControllerSpec extends CommonControllerBaseSpec with PageRenderVerification {

  trait Setup
      extends AppConfigMock
      with ApiDefinitionServiceMock
      with LoggedInUserServiceMock
      with NavigationServiceMock
      with XmlServicesServiceMock {

    val mcc                                 = app.injector.instanceOf[MessagesControllerComponents]
    private lazy val indexV2View            = app.injector.instanceOf[FilteredIndexView]
    val errorHandler                        = app.injector.instanceOf[ErrorHandler]
    val definitionList: List[ApiDefinition] = List(apiDefinition("service1"), apiDefinition("service2"))

    val underTest = new FilteredDocumentationIndexController(
      loggedInUserService,
      navigationService,
      mcc,
      errorHandler,
      apiDefinitionService,
      xmlServicesService,
      indexV2View
    )
  }

  "V2DocumentationController" when {
    "routing to the V2 index page" should {
      "render the API List" in new Setup {
        theUserIsLoggedIn()
        theDefinitionServiceWillReturnApiDefinitions(definitionList)
        fetchAllXmlApisReturnsApis()

        val result = underTest.apiListIndexPage(Nil, Nil)(request)
        verifyPageRendered(pageTitle("API Documentation"), breadcrumbs = List(apiDocsBreadcrumb), sideNavLinkRendered = false, bodyContains = Seq("API documentation"))(result)
      }

      "render the filtered API list when doc type filter is road map and service guides but no category filter" in new Setup {
        theUserIsLoggedIn()
        theDefinitionServiceWillReturnApiDefinitions(definitionList)
        fetchAllXmlApisReturnsVatApi()

        val result = underTest.apiListIndexPage(List(DocumentationTypeFilter.ROADMAPANDSERVICEGUIDE), List.empty)(request)
        // There are currently 22 Service Guides and 5 roadmaps so should be 27 results
        verifyPageRendered(pageTitle("API Documentation"), sideNavLinkRendered = false, breadcrumbs = List(apiDocsBreadcrumb), bodyContains = Seq("29 results "))(result)
      }

      "render the filtered API list when doc type filter is road map and service guides and customs category filter" in new Setup {
        theUserIsLoggedIn()
        theDefinitionServiceWillReturnApiDefinitions(definitionList)
        fetchAllXmlApisReturnsVatApi()

        val result = underTest.apiListIndexPage(List(DocumentationTypeFilter.ROADMAPANDSERVICEGUIDE), List(ApiCategory.INCOME_TAX_MTD))(request)
        // There are currently 21 Service Guides and 5 roadmaps but only 1 roadmap and 2 service guide are in the INCOME_TAX_MTD category
        verifyPageRendered(
          pageTitle("API Documentation"),
          sideNavLinkRendered = false,
          breadcrumbs = List(apiDocsBreadcrumb),
          bodyContains = Seq("3 results ", "Income Tax (MTD) end-to-end service guide", "Tax Logic service guide", "Income Tax (MTD) roadmap")
        )(result)
      }

      "render the filtered API list when only customs category filter" in new Setup {
        theUserIsLoggedIn()
        theDefinitionServiceWillReturnApiDefinitions(definitionList)
        fetchAllXmlApisReturnsVatApi()

        val result = underTest.apiListIndexPage(List.empty, List(ApiCategory.INCOME_TAX_MTD))(request)
        verifyPageRendered(
          pageTitle("API Documentation"),
          sideNavLinkRendered = false,
          breadcrumbs = List(apiDocsBreadcrumb),
          bodyContains = Seq("3 results ", "Income Tax (MTD) end-to-end service guide", "Tax Logic service guide", "Income Tax (MTD) roadmap")
        )(result)
      }

      "display the error page when the documentationService throws an exception" in new Setup {
        val exception = UpstreamErrorResponse("message", 503)
        theUserIsLoggedIn()
        theDefinitionServiceWillFail(exception)

        val result = underTest.apiListIndexPage(Nil, Nil)(request)

        verifyErrorPageRendered(INTERNAL_SERVER_ERROR, "Sorry, there is a problem with the service")(result)
      }

      "display the error page when the xmlServicesService throws an exception" in new Setup {
        val exception = new RuntimeException("message")
        theUserIsLoggedIn()
        theDefinitionServiceWillReturnApiDefinitions(definitionList)
        fetchAllXmlApisFails(exception)

        val result = underTest.apiListIndexPage(Nil, Nil)(request)

        verifyErrorPageRendered(INTERNAL_SERVER_ERROR, "Sorry, there is a problem with the service")(result)
      }

    }

  }
}
