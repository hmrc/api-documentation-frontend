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

import org.mockito.Mockito.when
import org.mockito.Matchers._
import org.scalatestplus.mockito.MockitoSugar
import scala.concurrent.Future.{successful, failed}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.apidocumentation.services.ApiDefinitionService

trait ApiDefinitionServiceMock extends MockitoSugar {
  val apiDefinitionService = mock[ApiDefinitionService]

  def theDefinitionServiceWillReturnAnApiDefinition(apiDefinition: ExtendedAPIDefinition) = {
    when(apiDefinitionService.fetchExtendedDefinition(any(), any())(any[HeaderCarrier])).thenReturn(successful(Some(apiDefinition)))
  }

  def theDefinitionServiceWillReturnNoApiDefinition() = {
    when(apiDefinitionService.fetchExtendedDefinition(any(), any())(any[HeaderCarrier])).thenReturn(successful(None))
  }

  def theDefinitionServiceWillFail(exception: Throwable) = {
    when(apiDefinitionService.fetchExtendedDefinition(any(), any())(any[HeaderCarrier])).thenReturn(failed(exception))

    when(apiDefinitionService.fetchAllDefinitions(any())(any[HeaderCarrier]))
      .thenReturn(failed(exception))
  }

  def theDefinitionServiceWillReturnApiDefinitions(apis: Seq[APIDefinition]) = {
    when(apiDefinitionService.fetchAllDefinitions(any())(any[HeaderCarrier]))
      .thenReturn(successful(apis))
  }
}
