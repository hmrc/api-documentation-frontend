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
import scala.concurrent.Future

import play.api.http.Status._
import play.api.mvc._
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.{ApiAccess, ServiceName}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApiVersionNbr

import uk.gov.hmrc.apidocumentation.ErrorHandler
import uk.gov.hmrc.apidocumentation.connectors.DownloadConnector
import uk.gov.hmrc.apidocumentation.mocks.config._
import uk.gov.hmrc.apidocumentation.mocks.services._

class DownloadControllerSpec extends CommonControllerBaseSpec {

  trait Setup
      extends ApiDocumentationServiceMock
      with AppConfigMock
      with ApiDefinitionServiceMock
      with LoggedInUserServiceMock {

    val downloadConnector = mock[DownloadConnector]

    val errorHandler = app.injector.instanceOf[ErrorHandler]

    val version      = ApiVersionNbr("2.0")
    val resourceName = "some/resource"

    val underTest = new DownloadController(apiDefinitionService, downloadConnector, loggedInUserService, errorHandler, appConfig, mcc)

    def theDownloadConnectorWillReturnTheResult(result: Results.Status) = {
      when(downloadConnector.fetch(*[ServiceName], *[ApiVersionNbr], *[String])).thenReturn(Future.successful(Some(result)))
    }

    theUserIsNotLoggedIn()
  }

  "DownloadController" should {
    "download the resource when found" in new Setup {
      theDefinitionServiceWillReturnAnApiDefinition(
        extendedApiDefinition(serviceName = serviceName.value, version = version)
      )
      theDownloadConnectorWillReturnTheResult(Results.Ok)

      await(underTest.downloadResource(serviceName, version, resourceName)(request)).header.status shouldBe OK
    }

    "return 404 code when the resource not found" in new Setup {
      theDefinitionServiceWillReturnAnApiDefinition(
        extendedApiDefinition(serviceName = serviceName.value, version = version)
      )
      theDownloadConnectorWillReturnTheResult(Results.NotFound)

      await(underTest.downloadResource(serviceName, version, resourceName)(request)).header.status shouldBe NOT_FOUND
    }

    "error when the resource name contains '..'" in new Setup {
      theDefinitionServiceWillReturnAnApiDefinition(
        extendedApiDefinition(serviceName = serviceName.value, version = version)
      )

      await(underTest.downloadResource(serviceName, version, "../secret")(request)).header.status shouldBe INTERNAL_SERVER_ERROR
    }

    "redirect to the login page when the API is private and the user is not logged in" in new Setup {
      theUserIsNotLoggedIn()
      theDefinitionServiceWillReturnAnApiDefinition(
        extendedApiDefinition(serviceName = serviceName.value, version = version, access = ApiAccess.Private(), authorised = false)
      )

      val result = underTest.downloadResource(serviceName, version, resourceName)(request)

      verifyRedirectToLoginPage(result, serviceName, version)
    }
  }
}
