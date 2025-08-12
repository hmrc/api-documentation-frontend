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

import java.io.FileNotFoundException
import scala.concurrent.ExecutionContext.Implicits.global

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.core.extensions.SwaggerParserExtension
import io.swagger.v3.parser.core.models.SwaggerParseResult
import org.apache.pekko.actor.ActorSystem

import play.api.http.Status.{NOT_FOUND, OK}
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ServiceName
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApiVersionNbr
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

import uk.gov.hmrc.apidocumentation.ErrorHandler
import uk.gov.hmrc.apidocumentation.controllers.utils.PageRenderVerification
import uk.gov.hmrc.apidocumentation.mocks.config._
import uk.gov.hmrc.apidocumentation.mocks.connectors.DownloadConnectorMockModule
import uk.gov.hmrc.apidocumentation.mocks.services._
import uk.gov.hmrc.apidocumentation.views.html._

class OpenApiDocumentationControllerSpec extends CommonControllerBaseSpec with PageRenderVerification {

  trait Setup
      extends DownloadConnectorMockModule
      with ApiDefinitionServiceMock
      with LoggedInUserServiceMock
      with NavigationServiceMock
      with AppConfigMock {

    val emptySwaggerParseResult   = new SwaggerParseResult()
    val openApiSwaggerParseResult = new SwaggerParseResult()
    openApiSwaggerParseResult.setOpenAPI(new OpenAPI())

    lazy val openApiViewRedoc             = app.injector.instanceOf[OpenApiViewRedoc]
    lazy val openApiPreviewRedoc          = app.injector.instanceOf[OpenApiPreviewRedoc]
    lazy val openApiPreviewView           = app.injector.instanceOf[OpenApiPreviewView]
    lazy val mcc                          = app.injector.instanceOf[MessagesControllerComponents]
    lazy val errorHandler                 = app.injector.instanceOf[ErrorHandler]
    lazy val openAPIV3ParserMock          = mock[SwaggerParserExtension]
    val serviceName                       = ServiceName("Test-Service")
    implicit lazy val system: ActorSystem = app.injector.instanceOf[ActorSystem]

    val underTest = new OpenApiDocumentationController(
      openApiViewRedoc,
      openApiPreviewRedoc,
      openApiPreviewView,
      DownloadConnectorMock.aMock,
      mcc,
      apiDefinitionService,
      loggedInUserService,
      errorHandler,
      navigationService,
      openAPIV3ParserMock
    )
  }

  "OpenApiDocumentationController" should {
    "successfully fetch resolved OAS specification" in new Setup {
      when(appConfig.oasFetchResolvedMaxDuration).thenReturn(1000)
      when(openAPIV3ParserMock.readLocation(*, *, *)).thenReturn(openApiSwaggerParseResult)

      val result = underTest.fetchOasResolved(serviceName, ApiVersionNbr("Test-Version"))(request)

      status(result) shouldBe OK
    }

    "successfully handle and error when fetching resolved OAS specification" in new Setup {
      when(appConfig.oasFetchResolvedMaxDuration).thenReturn(1000)
      when(openAPIV3ParserMock.readLocation(*, *, *)).thenReturn(emptySwaggerParseResult)

      val result = underTest.fetchOasResolved(serviceName, ApiVersionNbr("Test-Version"))(request)

      status(result) shouldBe NOT_FOUND
    }

    "successfully handle FileNotFoundException when fetching resolved OAS specification" in new Setup {
      when(appConfig.oasFetchResolvedMaxDuration).thenReturn(1000)
      when(openAPIV3ParserMock.readLocation(*, *, *)).thenThrow(new FileNotFoundException())

      val result = underTest.fetchOasResolved(serviceName, ApiVersionNbr("Test-Version"))(request)

      status(result) shouldBe NOT_FOUND
    }

    "should successfully show the open api preview page when flagged on" in new Setup {
      when(appConfig.openApiPreviewEnabled).thenReturn(true)

      val result = underTest.previewApiDocumentationPage()(request)
      verifyPageRendered(pageTitle("OpenAPI Documentation Preview"))(result)
    }

    "should NOT FOUND show the open api preview page when flagged off" in new Setup {
      when(appConfig.openApiPreviewEnabled).thenReturn(false)

      val result = underTest.previewApiDocumentationPage()(request)
      status(result) shouldBe NOT_FOUND
    }

    "should successfully show the open api preview action when flagged on but no URL" in new Setup {
      when(appConfig.openApiPreviewEnabled).thenReturn(true)

      val result = underTest.previewApiDocumentationAction(None)(request)
      verifyPageRendered(pageTitle("OpenAPI Documentation Preview"))(result)
    }

    "should successfully show the open api preview action when flagged on" in new Setup {
      when(appConfig.openApiPreviewEnabled).thenReturn(true)

      val result = underTest.previewApiDocumentationAction(Some(RedirectUrl("http://localhost:1234")))(request)
      status(result) shouldBe OK
    }

    "should NOT FOUND show the open api preview action when flagged off" in new Setup {
      when(appConfig.openApiPreviewEnabled).thenReturn(false)

      val result = underTest.previewApiDocumentationAction(None)(request)
      status(result) shouldBe NOT_FOUND
    }
  }
}
