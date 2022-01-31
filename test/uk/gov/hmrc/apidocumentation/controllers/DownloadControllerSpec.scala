/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.http.Status._
import play.api.mvc._
import uk.gov.hmrc.apidocumentation.ErrorHandler
import uk.gov.hmrc.apidocumentation.mocks.services._
import uk.gov.hmrc.apidocumentation.models.APIAccessType
import uk.gov.hmrc.apidocumentation.services.DownloadService
import uk.gov.hmrc.apidocumentation.mocks.config._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DownloadControllerSpec extends CommonControllerBaseSpec {

  trait Setup
      extends ApiDocumentationServiceMock
      with AppConfigMock
      with ApiDefinitionServiceMock
      with LoggedInUserServiceMock {

    val downloadService = mock[DownloadService]

    val errorHandler = app.injector.instanceOf[ErrorHandler]

    val version = "2.0"
    val resourceName = "some/resource"

    val underTest = new DownloadController(documentationService, apiDefinitionService, downloadService, loggedInUserService, errorHandler, appConfig, mcc)

    def theDownloadServiceWillReturnTheResult(result: Results.Status) = {
      when(downloadService.fetchResource(any[String], any[String], any[String])).thenReturn(Future.successful(result))
    }

    theUserIsNotLoggedIn()
  }

  "DownloadController" should {
    "download the resource when found" in new Setup {
      theDefinitionServiceWillReturnAnApiDefinition(extendedApiDefinition(serviceName, version))
      theDownloadServiceWillReturnTheResult(Results.Ok)

      await(underTest.downloadResource(serviceName, version, resourceName)(request)).header.status shouldBe OK
    }

    "return 404 code when the resource not found" in new Setup {
      theDefinitionServiceWillReturnAnApiDefinition(extendedApiDefinition(serviceName, version))
      theDownloadServiceWillReturnTheResult(Results.NotFound)

      await(underTest.downloadResource(serviceName, version, resourceName)(request)).header.status shouldBe NOT_FOUND
    }

    "error when the resource name contains '..'" in new Setup {
      theDefinitionServiceWillReturnAnApiDefinition(extendedApiDefinition(serviceName, version))

      await(underTest.downloadResource(serviceName, version, "../secret")(request)).header.status shouldBe INTERNAL_SERVER_ERROR
    }

    "redirect to the login page when the API is private and the user is not logged in" in new Setup {
      theUserIsNotLoggedIn()
      theDefinitionServiceWillReturnAnApiDefinition(
        extendedApiDefinition(serviceName, version, APIAccessType.PRIVATE, false, false))

      val result = underTest.downloadResource(serviceName, version, resourceName)(request)

      verifyRedirectToLoginPage(result, serviceName, version)
    }
  }
}
