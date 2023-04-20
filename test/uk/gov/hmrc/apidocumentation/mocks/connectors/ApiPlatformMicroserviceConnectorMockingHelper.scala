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

package uk.gov.hmrc.apidocumentation.mocks.connectors

import scala.concurrent.Future

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apidocumentation.connectors.ApiPlatformMicroserviceConnector
import uk.gov.hmrc.apidocumentation.models.{APIDefinition, ExtendedAPIDefinition, UuidIdentifier}

trait ApiPlatformMicroserviceConnectorMockingHelper extends MockitoSugar with ArgumentMatchersSugar {

  def whenFetchAllDefinitions[T <: ApiPlatformMicroserviceConnector](base: T)(apis: APIDefinition*)(implicit hc: HeaderCarrier) = {
    when(base.fetchApiDefinitionsByCollaborator(*)(eqTo(hc)))
      .thenReturn(Future.successful(apis.toSeq))
  }

  def whenFetchAllDefinitionsWithEmail[T <: ApiPlatformMicroserviceConnector](base: T)(userId: UuidIdentifier)(apis: APIDefinition*)(implicit hc: HeaderCarrier) = {
    when(base.fetchApiDefinitionsByCollaborator(*)(eqTo(hc)))
      .thenReturn(Future.successful(apis.toSeq))
  }

  def whenFetchExtendedDefinition[T <: ApiPlatformMicroserviceConnector](base: T)(serviceName: String)(api: ExtendedAPIDefinition)(implicit hc: HeaderCarrier) = {
    when(base.fetchApiDefinition(eqTo(serviceName), eqTo(None))(eqTo(hc)))
      .thenReturn(Future.successful(Some(api)))
  }

  def whenFetchExtendedDefinitionWithEmail[T <: ApiPlatformMicroserviceConnector](
      base: T
    )(
      serviceName: String,
      userId: UuidIdentifier
    )(
      api: ExtendedAPIDefinition
    )(implicit hc: HeaderCarrier
    ) = {
    when(base.fetchApiDefinition(eqTo(serviceName), *)(eqTo(hc)))
      .thenReturn(Future.successful(Some(api)))
  }

  def whenFetchExtendedDefinitionFails[T <: ApiPlatformMicroserviceConnector](base: T)(exception: Throwable)(implicit hc: HeaderCarrier) = {
    when(base.fetchApiDefinition(*, *)(eqTo(hc)))
      .thenReturn(Future.failed(exception))
  }
}
