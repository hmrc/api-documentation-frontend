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

package unit.uk.gov.hmrc.apidocumentation.utils

import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import uk.gov.hmrc.apidocumentation.connectors.ApiDefinitionConnector
import uk.gov.hmrc.apidocumentation.models.{APIDefinition, ExtendedAPIDefinition}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait ApiDefinitionConnectorMockingHelper {
  def whenFetchAllDefinitions[T <: ApiDefinitionConnector](base: T)
                             (apis: APIDefinition*)
                             (implicit hc: HeaderCarrier) = {
    when(base.fetchAllApiDefinitions(any[None.type]())(any[HeaderCarrier]))
      .thenReturn(Future.successful(apis.toSeq))
  }
  def whenFetchAllDefinitionsWithEmail[T <: ApiDefinitionConnector](base: T)
                                      (email: String)
                                      (apis: APIDefinition*)
                                      (implicit hc: HeaderCarrier) = {
    when(base.fetchAllApiDefinitions(any[Some[String]]())(any[HeaderCarrier]))
      .thenReturn(Future.successful(apis.toSeq))
  }
  def whenFetchExtendedDefinition[T <: ApiDefinitionConnector](base: T)
                                 (serviceName: String)
                                 (api: ExtendedAPIDefinition)
                                 (implicit hc: HeaderCarrier) = {
    when(base.fetchApiDefinition(eqTo(serviceName), eqTo(None))(any[HeaderCarrier]))
      .thenReturn(Future.successful(Some(api)))
  }
  def whenFetchExtendedDefinitionWithEmail[T <: ApiDefinitionConnector](base: T)
                                          (serviceName: String, email: String)
                                          (api: ExtendedAPIDefinition)
                                          (implicit hc: HeaderCarrier) = {
    when(base.fetchApiDefinition(eqTo(serviceName), any[Some[String]]())(any[HeaderCarrier]))
      .thenReturn(Future.successful(Some(api)))
  }

  def whenApiDefinitionFails[T <: ApiDefinitionConnector](base: T)
                            (exception: Throwable)
                            (implicit hc: HeaderCarrier) = {
    when(base.fetchApiDefinition(any[String],any[None.type]())(any[HeaderCarrier]))
      .thenReturn(Future.failed(exception))
    when(base.fetchAllApiDefinitions(any())(any[HeaderCarrier]))
      .thenReturn(Future.failed(exception))
  }

  def whenNoApiDefinitions[T <: ApiDefinitionConnector](base: T) = {
    when(base.fetchApiDefinition(any[String],any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(None))
    when(base.fetchAllApiDefinitions(any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(Seq.empty))
  }
}
