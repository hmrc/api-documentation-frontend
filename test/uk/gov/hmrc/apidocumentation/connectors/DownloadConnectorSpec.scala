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

package uk.gov.hmrc.apidocumentation.connectors

import play.api.http.Status._
import play.api.mvc.Results._
import play.api.test.Helpers.status
import play.api.routing.sird._
import play.api.test.WsTestClient
import play.core.server.Server
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Configuration

class DownloadConnectorSpec extends ConnectorSpec {
  val apiDocumentationUrl = "https://api-documentation.example.com"

  val serviceName = "hello-world"
  val version = "1.0"
  val stubConfig = Configuration(
    "metrics.jvm" -> false
  )

  trait Setup {

    implicit val hc = HeaderCarrier()
    val mockAppConfig = mock[ApplicationConfig]
    when(mockAppConfig.apiPlatformMicroserviceBaseUrl).thenReturn("")
  }

  "downloadResource" should {
    "return resource when found" in new Setup {
      Server.withRouterFromComponents() { components =>
        import components.{defaultActionBuilder => Action}
        {
          case GET(p"/combined-api-definitions/hello-world/1.0/documentation/some/resource") => Action {
            Ok("hello world")
          }
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val connector = new DownloadConnector(client, mockAppConfig)
          val result = connector.fetch(serviceName, version, "some/resource")

          status(result.map(_.value)) shouldBe OK
        }
      }
    }

    "return None when not found" in new Setup {
      Server.withRouterFromComponents() { components =>
        import components.{defaultActionBuilder => Action}
        {
          case GET(p"/combined-api-definitions/hello-world/1.0/documentation/some/resourceNotThere") => Action {
            NotFound
          }
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val connector = new DownloadConnector(client, mockAppConfig)

          val result = await(connector.fetch(serviceName, version, "some/resourceNotThere"))

          result shouldBe None
        }
      }
    }

    "throw InternalServerException for any other response" in new Setup {
      Server.withRouterFromComponents() { components =>
        import components.{defaultActionBuilder => Action}
        {
          case GET(p"/combined-api-definitions/hello-world/1.0/documentation/some/resourceInvalid") => Action {
            ServiceUnavailable
          }
          case GET(p"/combined-api-definitions/hello-world/1.0/documentation/some/timeout") => Action {
            RequestTimeout
          }
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val connector = new DownloadConnector(client, mockAppConfig)

          intercept[InternalServerException] {
            await(connector.fetch(serviceName, version, "some/resourceInvalid"))
          }

          intercept[InternalServerException] {
            await(connector.fetch(serviceName, version, "some/timeout"))
          }
        }
      }
    }
  }
}
