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

package uk.gov.hmrc.apidocumentation.utils

import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import uk.gov.hmrc.apidocumentation.models.{APIDefinition, ExtendedAPIDefinition}
import uk.gov.hmrc.apidocumentation.services.BaseApiDefinitionService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait BaseApiDefinitionServiceMockingHelper {

  def whenFetchAllDefinitions[T <: BaseApiDefinitionService](base: T)
                             (apis: APIDefinition*)
                             (implicit hc: HeaderCarrier) = {
    when(base.fetchAllDefinitions(any[None.type]())(eqTo(hc)))
      .thenReturn(Future.successful(apis.toSeq))
  }
  def whenFetchAllDefinitionsWithEmail[T <: BaseApiDefinitionService](base: T)
                                      (email: String)
                                      (apis: APIDefinition*)
                                      (implicit hc: HeaderCarrier) = {
    when(base.fetchAllDefinitions(any[Some[String]]())(eqTo(hc)))
      .thenReturn(Future.successful(apis.toSeq))
  }
  def whenFetchExtendedDefinition[T <: BaseApiDefinitionService](base: T)
                                 (serviceName: String)
                                 (api: ExtendedAPIDefinition)
                                 (implicit hc: HeaderCarrier) = {
    when(base.fetchExtendedDefinition(eqTo(serviceName), eqTo(None))(eqTo(hc)))
      .thenReturn(Future.successful(Some(api)))
  }
  def whenFetchExtendedDefinitionWithEmail[T <: BaseApiDefinitionService](base: T)
                                          (serviceName: String, email: String)
                                          (api: ExtendedAPIDefinition)
                                          (implicit hc: HeaderCarrier) = {
    when(base.fetchExtendedDefinition(eqTo(serviceName), any[Some[String]]())(eqTo(hc)))
      .thenReturn(Future.successful(Some(api)))
  }

  def whenApiDefinitionFails[T <: BaseApiDefinitionService](base: T)
                            (exception: Throwable)
                            (implicit hc: HeaderCarrier) = {
    when(base.fetchExtendedDefinition(any[String],any[None.type]())(eqTo(hc)))
      .thenReturn(Future.failed(exception))
    when(base.fetchAllDefinitions(any())(eqTo(hc)))
      .thenReturn(Future.failed(exception))
  }

  def whenNoApiDefinitions[T <: BaseApiDefinitionService](base: T) = {
    when(base.fetchExtendedDefinition(any[String],any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(None))
    when(base.fetchAllDefinitions(any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(Seq.empty))
  }
}
