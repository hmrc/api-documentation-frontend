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

package uk.gov.hmrc.apidocumentation.controllers.utils

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc._
import play.api.http.Status._
import play.api.test.FakeRequest
import uk.gov.hmrc.apidocumentation.models.APIAccessType.APIAccessType
import uk.gov.hmrc.apidocumentation.models.{Developer, _}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.apidocumentation.utils.ApiDefinitionTestDataHelper

import scala.concurrent.Future
import scala.concurrent.Future.{successful, failed}
import uk.gov.hmrc.apidocumentation.services.ApiDefinitionService
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.apidocumentation.services.DocumentationService

trait ApiDocumentationServiceMock extends MockitoSugar {
  val documentationService = mock[DocumentationService]

  def theDocumentationServiceWillFetchRaml(ramlAndSchemas: RamlAndSchemas) = {
    when(documentationService.fetchRAML(any(), any(), any())).thenReturn(successful(ramlAndSchemas))
  }

  def theDocumentationServiceWillFailWhenFetchingRaml(exception: Throwable) = {
    when(documentationService.fetchRAML(any(), any(), any())).thenReturn(failed(exception))
  }
}
