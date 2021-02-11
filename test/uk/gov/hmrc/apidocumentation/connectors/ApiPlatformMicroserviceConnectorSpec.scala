/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.apidocumentation.connectors

import java.util.UUID

import org.mockito.Mockito.when
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.utils.ApiPlatformMicroserviceHttpMockingHelper
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.apidocumentation.models.UuidIdentifier
import uk.gov.hmrc.apidocumentation.models.UserId

class ApiPlatformMicroserviceConnectorSpec extends ConnectorSpec {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val UpstreamException = UpstreamErrorResponse("Internal server error", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)

  val bearer = "TestBearerToken"
  val apiKeyTest = UUID.randomUUID().toString

  trait LocalSetup extends ApiPlatformMicroserviceHttpMockingHelper {
    val mockConfig = mock[ApplicationConfig]
    override val mockHttpClient: HttpClient = mock[HttpClient]

    override val apiPlatformMicroserviceBaseUrl = "/mockUrl"
    when(mockConfig.apiPlatformMicroserviceBaseUrl).thenReturn(apiPlatformMicroserviceBaseUrl)

    val serviceName = "someService"
    val userId = UuidIdentifier(UserId.random)

    val apiName1 = "Calendar"
    val apiName2 = "HelloWorld"

    val underTest = new ApiPlatformMicroserviceConnector(mockHttpClient, mockConfig)

  }

  "fetchApiDefinitionsByCollaborator" should {
    "call the underlying http client with the email argument" in new LocalSetup {
      whenGetAllDefinitionsByEmail(Some(userId))(apiDefinition(apiName1), apiDefinition(apiName2))

      val result = await(underTest.fetchApiDefinitionsByCollaborator(Some(userId)))

      result.size shouldBe 2
      result.map(_.name) shouldBe Seq(apiName1, apiName2)
    }

    "call the underlying http client without an email argument" in new LocalSetup {
      whenGetAllDefinitionsByEmail(None)(apiDefinition(apiName1), apiDefinition(apiName2))

      val result = await(underTest.fetchApiDefinitionsByCollaborator(None))

      result.size shouldBe 2
      result.map(_.name) shouldBe Seq(apiName1, apiName2)
    }
    "throw an exception correctly" in new LocalSetup {
      whenGetAllDefinitionsFails(UpstreamException)

      intercept[UpstreamException.type] {
        await(underTest.fetchApiDefinitionsByCollaborator(None))
      }
    }
  }

  "fetchApiDefinition" should {

    "call the underlying http client with the email argument" in new LocalSetup {
      whenGetDefinitionByEmail(serviceName, userId)(extendedApiDefinition(apiName1))

      val result = await(underTest.fetchApiDefinition(serviceName, Some(userId)))

      result should be('defined)
      result.head.name shouldBe apiName1
    }

    "call the underlying http client without an email argument" in new LocalSetup {
      whenGetDefinition(serviceName)(extendedApiDefinition(apiName1))

      val result = await(underTest.fetchApiDefinition(serviceName, None))

      result should be('defined)
      result.head.name shouldBe apiName1
    }

    "throw an exception correctly" in new LocalSetup {
      whenGetDefinitionFails(serviceName)(UpstreamException)

      intercept[UpstreamException.type] {
        await(underTest.fetchApiDefinition(serviceName, None))
      }
    }

    "handle the get returning None" in new LocalSetup {
      whenGetDefinitionFindsNothing(serviceName)

      val result = await(underTest.fetchApiDefinition(serviceName, None))
      result should not be 'defined
    }
  }
}
