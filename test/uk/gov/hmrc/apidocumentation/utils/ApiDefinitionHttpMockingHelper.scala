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

import org.mockito.Mockito.when
import uk.gov.hmrc.apidocumentation.connectors.ApiDefinitionConnector
import uk.gov.hmrc.apidocumentation.models.{APIDefinition, ExtendedAPIDefinition}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import org.mockito.Matchers.{any, eq => eqTo}

import scala.concurrent.Future

trait ApiDefinitionHttpMockingHelper {
  val mockHttpClient: HttpClient
  val apiDefinitionUrl: String

  import ApiDefinitionConnector.{noParams, queryParams}

  def whenGetDefinitionByEmail(serviceName: String, email: String)(definition: ExtendedAPIDefinition): Unit = {
    val url = ApiDefinitionConnector.definitionUrl(apiDefinitionUrl, serviceName)
    when(
      mockHttpClient.GET[ExtendedAPIDefinition](
        eqTo(url),
        eqTo(queryParams(Some(email)))
      )
        (any(), any(), any())
    )
      .thenReturn(Future.successful(definition))
  }

  def whenGetDefinition(serviceName: String)(definition: ExtendedAPIDefinition): Unit = {
    val url = ApiDefinitionConnector.definitionUrl(apiDefinitionUrl, serviceName)
    when(
      mockHttpClient.GET[ExtendedAPIDefinition](
        eqTo(url),
        eqTo(noParams)
      )
        (any(), any(), any())
    )
      .thenReturn(Future.successful(definition))
  }

  def whenGetDefinitionFails(serviceName: String)(exception: Throwable): Unit = {
    val url = ApiDefinitionConnector.definitionUrl(apiDefinitionUrl, serviceName)
    when(
      mockHttpClient.GET[ExtendedAPIDefinition](
        eqTo(url),
        eqTo(noParams)
      )
        (any(), any(), any())
    )
      .thenReturn(Future.failed(exception))
  }

  def whenGetAllDefinitions(definitions: APIDefinition*): Unit = {
    val url = ApiDefinitionConnector.definitionsUrl(apiDefinitionUrl)
    when(
      mockHttpClient.GET[Seq[APIDefinition]](
        eqTo(url),
        eqTo(noParams)
      )
        (any(), any(), any())
    )
      .thenReturn(Future.successful(definitions.toSeq))
  }

  def whenGetAllDefinitionsByEmail(email: String)(definitions: APIDefinition*): Unit = {
    val url = ApiDefinitionConnector.definitionsUrl(apiDefinitionUrl)
    when(
      mockHttpClient.GET[Seq[APIDefinition]](
        eqTo(url),
        eqTo(queryParams(Some(email)))
      )
        (any(), any(), any())
    )
      .thenReturn(Future.successful(definitions.toSeq))
  }
  def whenGetAllDefinitionsFails(exception: Throwable): Unit = {
    val url = ApiDefinitionConnector.definitionsUrl(apiDefinitionUrl)
    when(
      mockHttpClient.GET[ExtendedAPIDefinition](
        eqTo(url),
        eqTo(noParams)
      )
        (any(), any(), any())
    )
      .thenReturn(Future.failed(exception))
  }
}

