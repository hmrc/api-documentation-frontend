/*
 * Copyright 2019 HM Revenue & Customs
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

package unit.uk.gov.hmrc.apidocumentation.connectors

import org.mockito.Mockito.when
import org.mockito.Matchers.{any, eq => meq}
import org.scalatest.OptionValues
import play.api.libs.json.Json
import play.api.http.Status._
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.connectors.LocalRawApiDefinitionConnector
import uk.gov.hmrc.apidocumentation.models.{APIAccessType, APIDefinition, ExtendedAPIDefinition, VersionVisibility}
import uk.gov.hmrc.apidocumentation.models.JsonFormatters._
import uk.gov.hmrc.apidocumentation.services.LocalApiDefinitionService
import uk.gov.hmrc.http.{HeaderCarrier, Upstream5xxResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.metrics.{API, NoopMetrics}

import scala.concurrent.Future

class ConnectedApiDefinitionServiceSpec extends ConnectorSpec with  OptionValues {
  val apiDefinitionUrl = "https://api-definition.example.com"

  trait Setup {
    implicit val hc = HeaderCarrier()
    val mockHttpClient = mock[HttpClient]
    val mockAppConfig = mock[ApplicationConfig]
    when(mockAppConfig.localApiDefinitionUrl).thenReturn(apiDefinitionUrl)

    import scala.concurrent.ExecutionContext.Implicits.global

    val raw = new LocalRawApiDefinitionConnector(mockHttpClient, mockAppConfig)

    val connector = new LocalApiDefinitionService(raw, NoopMetrics)

    type Params = Seq[(String,String)]

    def params(email: String) = {
      Seq("email" -> email)
    }
    def params() = {
      Seq.empty
    }

    def onGetDefn(serviceName: String, email: String)(response: Future[ExtendedAPIDefinition]): Unit = {
      when[Future[ExtendedAPIDefinition]](
        mockHttpClient.GET[ExtendedAPIDefinition](
          meq(s"$apiDefinitionUrl/apis/$serviceName/definition"),
          meq(params(email))
        )
        (any(), any(), any())
      )
      .thenReturn(response)
    }
    def onGetDefn(serviceName: String)(response: Future[ExtendedAPIDefinition]): Unit = {
      when[Future[ExtendedAPIDefinition]](
        mockHttpClient.GET[ExtendedAPIDefinition](
          meq(s"$apiDefinitionUrl/apis/$serviceName/definition"),
          meq(params())
        )
        (any(), any(), any())
      )
      .thenReturn(response)
    }

    def onGetDefns(response: Future[Seq[APIDefinition]]): Unit = {
      when[Future[Seq[APIDefinition]]](
        mockHttpClient.GET[Seq[APIDefinition]](
          meq(s"$apiDefinitionUrl/apis/definition"),
          any[Params]
        )
        (any(), any(), any())
      )
      .thenReturn(response)
    }
  }

  "api" should {
    "be api-definition" in new Setup {
      connector.api shouldEqual API("local-api-definition")
    }
  }

  "fetchExtendedDefinitionByServiceName" should {

    "return a fetched API Definition" in new Setup {
      val serviceName = "calendar"
      onGetDefn(serviceName)(Future.successful(extendedApiDefinition("Calendar")))

      val oresult = await(connector.fetchExtendedDefinition(serviceName))
      oresult should be('defined)
      oresult.map(result => {
        result.name shouldBe "Calendar"
        result.versions should have size 2
        result.versions.find(_.version == "1.0").flatMap(_.visibility) shouldBe Some(VersionVisibility(APIAccessType.PUBLIC, false, true))
      })
    }

    "return a fetched API Definition with access levels" in new Setup {
      val serviceName = "calendar"
      onGetDefn(serviceName)(Future.successful(extendedApiDefinition("Hello with access levels")))

      val oresult = await(connector.fetchExtendedDefinition(serviceName))
      oresult should be('defined)
      oresult.map(result => {
        result.name shouldBe "Hello with access levels"
        result.versions.size shouldBe 2
        result.versions flatMap (_.visibility.map(_.privacy)) shouldBe Seq(APIAccessType.PUBLIC, APIAccessType.PRIVATE)

        result.versions should have size 2
        result.versions.find(_.version == "2.0").flatMap(_.visibility) shouldBe Some(VersionVisibility(APIAccessType.PRIVATE, false, false))
      })
    }
    "throw an http-verbs Upstream5xxResponse exception if the API Definition service responds with an error" in new Setup {
      val serviceName = "calendar"

      onGetDefn(serviceName)(Future.failed(new Upstream5xxResponse("Internal server error", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[Upstream5xxResponse](await(connector.fetchExtendedDefinition(serviceName)))
    }
  }

  "fetchExtendedDefinitionByServiceName with logged in email" should {

    val loggedInUserEmail = "test@example.com"

    "return a fetched API Definition" in new Setup {
      val serviceName = "calendar"
      onGetDefn(serviceName,loggedInUserEmail)(Future.successful(extendedApiDefinition("Calendar")))

      val oresult = await(connector.fetchExtendedDefinition(serviceName, Some(loggedInUserEmail)))
      oresult should be('defined)
      oresult.map(result => {
        result.name shouldBe "Calendar"
        result.versions should have size 2
        result.versions.find(_.version == "1.0").flatMap(_.visibility) shouldBe Some(VersionVisibility(APIAccessType.PUBLIC, false, true))
      })
    }

    "throw an http-verbs Upstream5xxResponse exception if the API Definition service responds with an error" in new Setup {
      val serviceName = "calendar"

      onGetDefn(serviceName, loggedInUserEmail)(Future.failed(new Upstream5xxResponse("Internal server error", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[Upstream5xxResponse](await(connector.fetchExtendedDefinition(serviceName, Some(loggedInUserEmail))))
    }
  }

  "fetchAll" should {

    "return all API Definitions sorted by name" in new Setup {
      onGetDefns(Future.successful(apiDefinitions("Hello", "Calendar")))

      val result = await(connector.fetchAllDefinitions())
      result.size shouldBe 2
      result.map(_.name) should contain("Calendar")
      result.map(_.name) should contain("Hello")
    }

    "throw an http-verbs Upstream5xxResponse exception if the API Definition service responds with an error" in new Setup {
      onGetDefns(Future.failed(new Upstream5xxResponse("Internal server error", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[Upstream5xxResponse](await(connector.fetchAllDefinitions()))
    }
  }

  "fetchByEmail" should {

    val loggedInUserEmail = "email@example.com"

    "return all API Definitions sorted by name for an email address" in new Setup {
      onGetDefns(Future.successful(apiDefinitions("Hello", "Calendar")))

      val result = await(connector.fetchAllDefinitions(Some(loggedInUserEmail)))
      result.size shouldBe 2
      result.map(_.name) should contain("Calendar")
      result.map(_.name) should contain("Hello")
    }

    "throw an http-verbs Upstream5xxResponse exception if the API Definition service responds with an error" in new Setup {
      onGetDefns(Future.failed(new Upstream5xxResponse("Internal server error", 500, 500)))

      intercept[Upstream5xxResponse](await(connector.fetchAllDefinitions(Some(loggedInUserEmail))))
    }
  }

  private def apiDefinitions(names: String*) = names.map(apiDefinition)
  
  private def extendedApiDefinition(name: String) = {
    Json.parse(s"""{
       |  "name" : "$name",
       |  "description" : "Test API",
       |  "context" : "test",
       |  "serviceBaseUrl" : "http://test",
       |  "serviceName" : "test",
       |  "requiresTrust": false,
       |  "isTestSupport": false,
       |  "versions" : [
       |    {
       |      "version" : "1.0",
       |      "status" : "STABLE",
       |      "endpoints" : [
       |        {
       |          "uriPattern" : "/hello",
       |          "endpointName" : "Say Hello",
       |          "method" : "GET",
       |          "authType" : "NONE",
       |          "throttlingTier" : "UNLIMITED"
       |        }
       |      ],
       |      "productionAvailability": {
       |        "endpointsEnabled": true,
       |        "access": {
       |          "type": "PUBLIC"
       |        },
       |        "loggedIn": false,
       |        "authorised": true
       |      }
       |    },
       |    {
       |      "version" : "2.0",
       |      "status" : "STABLE",
       |      "endpoints" : [
       |        {
       |          "uriPattern" : "/hello",
       |          "endpointName" : "Say Hello",
       |          "method" : "GET",
       |          "authType" : "NONE",
       |          "throttlingTier" : "UNLIMITED",
       |          "scope": "read:hello"
       |        }
       |      ],
       |      "productionAvailability": {
       |        "endpointsEnabled": true,
       |        "access": {
       |          "type": "PRIVATE"
       |        },
       |        "loggedIn": false,
       |        "authorised": false
       |      }
       |    }
       |  ]
       |}
     """.stripMargin).as[ExtendedAPIDefinition]
  }

  private def apiDefinition(name: String) = {
    Json.parse(s"""{
        |  "name" : "$name",
        |  "description" : "Test API",
        |  "context" : "test",
        |  "serviceBaseUrl" : "http://test",
        |  "serviceName" : "test",
        |  "versions" : [
        |    {
        |      "version" : "1.0",
        |      "status" : "STABLE",
        |      "endpoints" : [
        |        {
        |          "uriPattern" : "/hello",
        |          "endpointName" : "Say Hello",
        |          "method" : "GET",
        |          "authType" : "NONE",
        |          "throttlingTier" : "UNLIMITED"
        |        }
        |      ]
        |    },
        |    {
        |      "version" : "2.0",
        |      "status" : "STABLE",
        |      "endpoints" : [
        |        {
        |          "uriPattern" : "/hello",
        |          "endpointName" : "Say Hello",
        |          "method" : "GET",
        |          "authType" : "NONE",
        |          "throttlingTier" : "UNLIMITED",
        |          "scope": "read:hello"
        |        }
        |      ]
        |    }
        |  ]
        |}""".stripMargin.replaceAll("\n", " ")).as[APIDefinition]
  }
}
