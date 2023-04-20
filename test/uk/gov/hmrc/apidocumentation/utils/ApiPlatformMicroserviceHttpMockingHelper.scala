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

package uk.gov.hmrc.apidocumentation.utils

import com.github.tomakehurst.wiremock.client.WireMock._

import play.api.test.Helpers._

import uk.gov.hmrc.apidocumentation.common.utils._
import uk.gov.hmrc.apidocumentation.connectors.ApiPlatformMicroserviceConnector.{definitionUrl, definitionsUrl}
import uk.gov.hmrc.apidocumentation.models.{APIDefinition, ExtendedAPIDefinition, UuidIdentifier}

trait ApiPlatformMicroserviceHttpMockingHelper extends WireMockSugarExtensions {

  def apiPlatformMicroserviceBaseUrl: String

  import uk.gov.hmrc.apidocumentation.models.jsonFormatters._

  def whenGetAllDefinitionsByUserId(userId: UuidIdentifier)(definitions: APIDefinition*): Unit = {
    val url = definitionsUrl("")
    stubFor(
      get(
        urlPathEqualTo(url)
      )
        .withQueryParam("developerId", equalTo(userId.asText))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withJsonBody(definitions.toSeq)
        )
    )
  }

  def whenGetAllDefinitionsByUserId(definitions: APIDefinition*): Unit = {
    val url = definitionsUrl("")
    stubFor(
      get(
        urlEqualTo(url)
      )
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withJsonBody(definitions.toSeq)
        )
    )
  }

  def whenGetAllDefinitionsFails(status: Int): Unit = {
    val url = definitionsUrl(apiPlatformMicroserviceBaseUrl)
    stubFor(
      get(
        urlEqualTo(url)
      )
        .willReturn(
          aResponse()
            .withStatus(status)
        )
    )
  }

  def whenGetDefinitionByEmail(serviceName: String, userId: UuidIdentifier)(definition: ExtendedAPIDefinition): Unit = {
    val url = definitionUrl("", serviceName)
    stubFor(
      get(
        urlPathEqualTo(url)
      )
        .withQueryParam("developerId", equalTo(userId.asText))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withJsonBody(definition)
        )
    )
  }

  def whenGetDefinition(serviceName: String)(definition: ExtendedAPIDefinition): Unit = {
    val url = definitionUrl("", serviceName)
    stubFor(
      get(
        urlEqualTo(url)
      )
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withJsonBody(definition)
        )
    )
  }

  def whenGetDefinitionFindsNothing(serviceName: String): Unit = {
    val url = definitionUrl("", serviceName)
    stubFor(
      get(
        urlEqualTo(url)
      )
        .willReturn(
          aResponse()
            .withStatus(NOT_FOUND)
        )
    )
  }

  def whenGetDefinitionFails(serviceName: String)(status: Int): Unit = {
    val url = definitionUrl("", serviceName)
    stubFor(
      get(
        urlEqualTo(url)
      )
        .willReturn(
          aResponse()
            .withStatus(status)
        )
    )
  }
}
