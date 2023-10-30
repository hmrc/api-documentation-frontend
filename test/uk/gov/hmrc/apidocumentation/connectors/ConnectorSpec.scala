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

import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.{Application, Configuration}
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiDefinition

import uk.gov.hmrc.apidocumentation.common.utils._
import uk.gov.hmrc.apidocumentation.models.ExtendedAPIDefinition
import uk.gov.hmrc.apidocumentation.models.jsonFormatters._

trait ConnectorSpec extends AsyncHmrcSpec with WireMockSugar with WireMockSugarExtensions with GuiceOneAppPerSuite {

  def stubConfig: Configuration

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(stubConfig)
      .build()

  def extendedApiDefinition(name: String) = {
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
                  |          "throttlingTier" : "UNLIMITED",
                  |          "queryParameters": []
                  |        }
                  |      ],
                  |      "endpointsEnabled": true,
                  |      "versionSource": "OAS",
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
                  |          "scope": "read:hello",
                  |          "queryParameters": []
                  |        }
                  |      ],
                  |      "endpointsEnabled": true,
                  |      "versionSource": "OAS",
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

  def apiDefinition(name: String)    = {
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
                  |      "access": {
                  |         "isTrial": false,
                  |         "type": "PRIVATE"
                  |       },
                  |      "endpoints" : [
                  |        {
                  |          "uriPattern" : "/hello",
                  |          "endpointName" : "Say Hello",
                  |          "method" : "GET",
                  |          "authType" : "NONE",
                  |          "throttlingTier" : "UNLIMITED",
                  |          "queryParameters": []
                  |        }
                  |      ],
                  |      "endpointsEnabled": true,
                  |      "versionSource": "OAS"
                  |    },
                  |    {
                  |      "version" : "2.0",
                  |      "status" : "STABLE",
                  |      "access": {
                  |           "isTrial": false,
                  |           "type": "PRIVATE"
                  |         },
                  |      "endpoints" : [
                  |        {
                  |          "uriPattern" : "/hello",
                  |          "endpointName" : "Say Hello",
                  |          "method" : "GET",
                  |          "authType" : "NONE",
                  |          "throttlingTier" : "UNLIMITED",
                  |          "queryParameters": [],
                  |          "scope": "read:hello"
                  |        }
                  |      ],
                  |      "endpointsEnabled": true,
                  |      "versionSource": "OAS"
                  |    }
                  |  ],
                  |  "categories":[ "AGENTS" ]
                  |}""".stripMargin.replaceAll("\n", " ")).as[ApiDefinition]
  }
  def apiDefinitions(names: String*) = names.map(apiDefinition)
}
