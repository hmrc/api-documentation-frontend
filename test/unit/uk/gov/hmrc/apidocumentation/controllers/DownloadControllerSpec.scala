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

package unit.uk.gov.hmrc.apidocumentation.controllers

import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status._
import play.api.mvc._
import uk.gov.hmrc.apidocumentation.ErrorHandler
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.controllers.DownloadController
import uk.gov.hmrc.apidocumentation.models.APIAccessType
import uk.gov.hmrc.apidocumentation.services.DownloadService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class DownloadControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  class Setup extends ControllerCommonSetup {

    implicit val appConfig = mock[ApplicationConfig]
    val downloadService = mock[DownloadService]

    val errorHandler = fakeApplication.injector.instanceOf[ErrorHandler]
    val mcc = fakeApplication.injector.instanceOf[MessagesControllerComponents]

    val version = "2.0"
    val resourceName = "some/resource"

    val underTest = new DownloadController(documentationService, apiDefinitionService, downloadService, loggedInUserProvider, errorHandler, mcc)

    def theDownloadServiceWillReturnTheResult(result: Results.Status) = {
      when(downloadService.fetchResource(any[String], any[String], any[String])(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(result))
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
