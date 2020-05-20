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

package uk.gov.hmrc.apidocumentation.connectors

import java.util.UUID

import org.mockito.Mockito._
import org.scalatest.OptionValues
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.utils.ApiDefinitionHttpMockingHelper
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, Upstream5xxResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global

class ApiDefinitionConnectorSpec
  extends ConnectorSpec
  with OptionValues {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val UpstreamException = Upstream5xxResponse("Internal server error", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)

  val bearer = "TestBearerToken"
  val apiKeyTest = UUID.randomUUID().toString

  trait LocalSetup extends ApiDefinitionHttpMockingHelper {
    val mockConfig = mock[ApplicationConfig]
    val mockHttpClient = mock[HttpClient]

    val apiDefinitionUrl = "/mockUrl"
    when(mockConfig.apiDefinitionBaseUrl).thenReturn(apiDefinitionUrl)

    val serviceName = "someService"
    val userEmail = "3rdparty@example.com"

    val apiName1 = "Calendar"
    val apiName2 = "HelloWorld"

    val underTest = new ApiDefinitionConnector(mockHttpClient, mockConfig)

  }

  "query parameter construction" should {
    "create an empty sequence without an email" in {
      ApiDefinitionConnector.queryParams(None) shouldBe Seq.empty
    }
    "create an sequence with email key to email address when presented with an email" in {
      val email = "bob@example.com"
      ApiDefinitionConnector.queryParams(Some(email)) shouldBe Seq("email" -> email)
    }
  }

  "api definition connector" should {

    "when requesting an extended api definition" should {

      "call the underlying http client with the email argument" in new LocalSetup {
        whenGetDefinitionByEmail(serviceName, userEmail)(extendedApiDefinition(apiName1))

        val result = await(underTest.fetchApiDefinition(serviceName, Some(userEmail)))

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

      "do not throw exception when not found but instead return None" in new LocalSetup {
        whenGetDefinitionFails(serviceName)(new NotFoundException("Bang"))

        val result = await(underTest.fetchApiDefinition(serviceName, None))
        result should not be 'defined
      }
    }


  }
}
