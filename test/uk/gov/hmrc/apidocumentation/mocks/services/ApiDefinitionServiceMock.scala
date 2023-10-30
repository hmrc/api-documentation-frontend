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

package uk.gov.hmrc.apidocumentation.mocks.services

import scala.concurrent.Future.{failed, successful}

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._

import uk.gov.hmrc.apidocumentation.services.ApiDefinitionService

trait ApiDefinitionServiceMock extends MockitoSugar with ArgumentMatchersSugar {
  val apiDefinitionService = mock[ApiDefinitionService]

  def theDefinitionServiceWillReturnAnApiDefinition(apiDefinition: ExtendedApiDefinition) = {
    when(apiDefinitionService.fetchExtendedDefinition(*, *)(*)).thenReturn(successful(Some(apiDefinition)))
  }

  def theDefinitionServiceWillReturnNoApiDefinition() = {
    when(apiDefinitionService.fetchExtendedDefinition(*, *)(*)).thenReturn(successful(None))
  }

  def theDefinitionServiceWillFail(exception: Throwable) = {
    when(apiDefinitionService.fetchExtendedDefinition(*, *)(*)).thenReturn(failed(exception))

    when(apiDefinitionService.fetchAllDefinitions(*)(*))
      .thenReturn(failed(exception))
  }

  def theDefinitionServiceWillReturnApiDefinitions(apis: Seq[ApiDefinition]) = {
    when(apiDefinitionService.fetchAllDefinitions(*)(*))
      .thenReturn(successful(apis))
  }
}
