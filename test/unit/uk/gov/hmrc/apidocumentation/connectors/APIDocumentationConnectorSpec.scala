/*
 * Copyright 2018 HM Revenue & Customs
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

import java.net.URLEncoder

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import uk.gov.hmrc.apidocumentation.config.{ApiDocumentationFrontendAuditConnector, WSHttp}
import uk.gov.hmrc.apidocumentation.models.{APIAccess, APIAccessType, VersionVisibility}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.apidocumentation.connectors.APIDocumentationConnector
import uk.gov.hmrc.http.{HeaderCarrier, Upstream5xxResponse}
import uk.gov.hmrc.play.http.metrics.{API, NoopMetrics}
import uk.gov.hmrc.play.test.UnitSpec

class APIDocumentationConnectorSpec extends UnitSpec with ScalaFutures with BeforeAndAfterEach with GuiceOneAppPerSuite {

  val apiDocumentationPort = sys.env.getOrElse("WIREMOCK", "11114").toInt
  var apiDocumentationHost = "localhost"
  val apiDocumentationUrl = s"http://$apiDocumentationHost:$apiDocumentationPort"
  val wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(apiDocumentationPort))

  trait Setup {
    implicit val hc = HeaderCarrier()
    val connector = new APIDocumentationConnector(new WSHttp(ApiDocumentationFrontendAuditConnector), NoopMetrics)
  }

  override def beforeEach() {
    wireMockServer.start()
    WireMock.configureFor(apiDocumentationHost, apiDocumentationPort)
  }

  override def afterEach() {
    wireMockServer.stop()
  }

  "api" should {
    "be api-documentation" in new Setup {
      connector.api shouldEqual API("api-documentation")
    }
  }

  "fetchExtendedDefinitionByServiceName" should {

    "return a fetched API Definition" in new Setup {
      val serviceName = "calendar"
      stubFor(get(urlEqualTo(s"/apis/$serviceName/definition"))
        .willReturn(aResponse().withStatus(200).withBody(extendedApiDefinitionJson("Calendar"))))
      val result = await(connector.fetchExtendedDefinitionByServiceName(serviceName))
      result.name shouldBe "Calendar"
      result.versions should have size 2
      result.versions.find(_.version == "1.0").flatMap(_.visibility) shouldBe Some(VersionVisibility(APIAccessType.PUBLIC, false, true))
    }

    "return a fetched API Definition with access levels" in new Setup {
      val serviceName = "calendar"
      stubFor(get(urlEqualTo(s"/apis/$serviceName/definition"))
        .willReturn(aResponse().withStatus(200).withBody(extendedApiDefinitionJson("Hello with access levels"))))
      val result = await(connector.fetchExtendedDefinitionByServiceName(serviceName))
      result.name shouldBe "Hello with access levels"
      result.versions.size shouldBe 2
      result.versions flatMap (_.visibility.map(_.privacy)) shouldBe Seq(APIAccessType.PUBLIC, APIAccessType.PRIVATE)

      result.versions should have size 2
      result.versions.find(_.version == "2.0").flatMap(_.visibility) shouldBe Some(VersionVisibility(APIAccessType.PRIVATE, false, false))

    }

    "throw an http-verbs Upstream5xxResponse exception if the API Definition service responds with an error" in new Setup {
      val serviceName = "calendar"
      stubFor(get(urlEqualTo(s"/apis/$serviceName/definition"))
        .willReturn(aResponse().withStatus(500)))
      intercept[Upstream5xxResponse](await(connector.fetchExtendedDefinitionByServiceName(serviceName)))
    }
  }

  "fetchExtendedDefinitionByServiceName with logged in email" should {

    val loggedInUserEmail = "test@example.com"
    val encodedLoggedInUserMail = URLEncoder.encode(loggedInUserEmail, "UTF-8")

    "return a fetched API Definition" in new Setup {
      val serviceName = "calendar"
      stubFor(get(urlEqualTo(s"/apis/$serviceName/definition?email=$encodedLoggedInUserMail"))
        .willReturn(aResponse().withStatus(200).withBody(extendedApiDefinitionJson("Calendar"))))
      val result = await(connector.fetchExtendedDefinitionByServiceNameAndEmail(serviceName, loggedInUserEmail))
      result.name shouldBe "Calendar"
      result.versions should have size 2
      result.versions.find(_.version == "1.0").flatMap(_.visibility) shouldBe Some(VersionVisibility(APIAccessType.PUBLIC, false, true))
    }

    "throw an http-verbs Upstream5xxResponse exception if the API Definition service responds with an error" in new Setup {
      val serviceName = "calendar"
      stubFor(get(urlEqualTo(s"/apis/$serviceName/definition?email=$encodedLoggedInUserMail"))
        .willReturn(aResponse().withStatus(500)))
      intercept[Upstream5xxResponse](await(connector.fetchExtendedDefinitionByServiceNameAndEmail(serviceName, loggedInUserEmail)))
    }
  }

  "fetchAll" should {

    "return all API Definitions sorted by name" in new Setup {
      stubFor(get(urlEqualTo("/apis/definition"))
        .willReturn(aResponse().withStatus(200).withBody(apiDefinitionsJson("Hello", "Calendar"))))
      val result = await(connector.fetchAll())
      result.size shouldBe 2
      result(0).name shouldBe "Calendar"
      result(1).name shouldBe "Hello"
    }

    "throw an http-verbs Upstream5xxResponse exception if the API Definition service responds with an error" in new Setup {
      stubFor(get(urlEqualTo("/apis/definition")).willReturn(aResponse().withStatus(500)))
      intercept[Upstream5xxResponse](await(connector.fetchAll()))
    }
  }

  "fetchByEmail" should {

    val loggedInUserEmail = "email@example.com"
    val encodedLoggedInUserEmail = URLEncoder.encode(loggedInUserEmail, "UTF-8")

    "return all API Definitions sorted by name for an email address" in new Setup {
      stubFor(get(urlEqualTo(s"/apis/definition?email=$encodedLoggedInUserEmail"))
        .willReturn(aResponse().withStatus(200).withBody(apiDefinitionsJson("Hello", "Calendar"))))
      val result = await(connector.fetchByEmail(loggedInUserEmail))
      result.size shouldBe 2
      result(0).name shouldBe "Calendar"
      result(1).name shouldBe "Hello"
    }

    "return all API Definitions sorted by name for a strange email address" in new Setup {
      val loggedInUserStrangeEmail = "email+strange@example.com"
      val encodedLoggedInUserStrangeEmail = URLEncoder.encode(loggedInUserStrangeEmail, "UTF-8")
      stubFor(get(urlEqualTo(s"/apis/definition?email=$encodedLoggedInUserStrangeEmail"))
        .willReturn(aResponse().withStatus(200).withBody(apiDefinitionsJson("Hello", "Calendar"))))
      val result = await(connector.fetchByEmail(loggedInUserStrangeEmail))
      result.size shouldBe 2
      result(0).name shouldBe "Calendar"
      result(1).name shouldBe "Hello"
    }

    "throw an http-verbs Upstream5xxResponse exception if the API Definition service responds with an error" in new Setup {
      stubFor(get(urlEqualTo(s"/apis/definition?email=$encodedLoggedInUserEmail"))
        .willReturn(aResponse().withStatus(500)))
      intercept[Upstream5xxResponse](await(connector.fetchByEmail(loggedInUserEmail)))
    }
  }

  private def apiDefinitionsJson(names: String*) = {
    names.map(apiDefinitionJson).mkString("[", ",", "]")
  }
  
  private def extendedApiDefinitionJson(name: String) = {
    s"""{
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
     """.stripMargin
  }

  private def apiDefinitionJson(name: String) = {
    s"""{
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
        |}""".stripMargin.replaceAll("\n", " ")
  }
}
