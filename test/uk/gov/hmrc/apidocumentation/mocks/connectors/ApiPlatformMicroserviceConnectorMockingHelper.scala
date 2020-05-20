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

package uk.gov.hmrc.apidocumentation.mocks.connectors

import org.mockito.Matchers.any
import org.mockito.Mockito.when
import uk.gov.hmrc.apidocumentation.connectors.ApiPlatformMicroserviceConnector
import uk.gov.hmrc.apidocumentation.models.APIDefinition
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait ApiPlatformMicroserviceConnectorMockingHelper {

  def whenFetchAllDefinitions[T <: ApiPlatformMicroserviceConnector](base: T)(apis: APIDefinition*)(implicit hc: HeaderCarrier) = {
    when(base.fetchApiDefinitionsByCollaborator(any[None.type]())(any[HeaderCarrier]))
      .thenReturn(Future.successful(apis.toSeq))
  }

  def whenFetchAllDefinitionsWithEmail[T <: ApiPlatformMicroserviceConnector](base: T)(email: String)(apis: APIDefinition*)(implicit hc: HeaderCarrier) = {
    when(base.fetchApiDefinitionsByCollaborator(any[Some[String]]())(any[HeaderCarrier]))
      .thenReturn(Future.successful(apis.toSeq))
  }
}
