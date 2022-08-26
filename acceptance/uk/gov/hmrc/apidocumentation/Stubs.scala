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

package uk.gov.hmrc.apidocumentation

import com.github.tomakehurst.wiremock.client.WireMock._
import play.utils.UriEncoding
import scala.io.Source
import play.api.libs.json.Json
import uk.gov.hmrc.apidocumentation.models.apispecification.ApiSpecification

trait Stubs extends ApiMicroservice with DeveloperFrontend with ApiPlatformMicroservice with XmlServices

trait XmlServices {
  def fetchAllXmlApis(): Unit = {
    val allXmlApisJson = Source.fromURL(getClass.getResource("/acceptance/api-platform-xml-services/xml_apis.json")).mkString

    stubFor(
      get(urlPathEqualTo("/api-platform-xml-services/xml/apis"))
        .willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
          .withBody(allXmlApisJson))
    )
  }
}

trait ApiPlatformMicroservice{
  def fetchAll() {
    val allDefinitionJson = Source.fromURL(getClass.getResource(s"/acceptance/api-definition/all.json")).mkString

    stubFor(
      get(urlMatching("/combined-api-definitions"))
        .willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
          .withBody(allDefinitionJson))
    )
  }

  def fetchDefinition(serviceName: String) {
    val definitionJson = Source.fromURL(getClass.getResource(s"/acceptance/api-definition/$serviceName.json")).mkString
    stubFor(
      get(urlMatching(s"/combined-api-definitions/$serviceName"))
        .willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
          .withBody(definitionJson))
    )
  }

  def fetchApiSpec(serviceName: String, version: String) = {
    val url = getClass.getResource(s"/services/$serviceName/spec_${version}.json")
    val file = Source.fromURL(url).mkString
    import uk.gov.hmrc.apidocumentation.models.apispecification.ApiSpecificationFormatters._
    Json.fromJson[ApiSpecification](Json.parse(file))

    stubFor(get(urlPathEqualTo(s"/combined-api-definitions/$serviceName/$version/specification"))
      .willReturn(
        aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody(file)
      )
    )
  }

  def failToFetch(serviceName: String) {
    stubFor(
      get(urlPathEqualTo(s"/combined-api-definitions/$serviceName/definition"))
        .willReturn(aResponse()
          .withStatus(404))
    )
  }
}

trait ApiMicroservice {

  def documentation(serviceName: String, version: String, endpointName: String) {
    val documentationXml = Source.fromURL(
      getClass.getResource(s"/acceptance/$serviceName/${endpointName.toLowerCase.replace(" ", "-")}/documentation.xml")
    ).mkString

    stubFor(
      get(urlPathEqualTo(s"/combined-api-definitions/$serviceName/$version/documentation/${UriEncoding.encodePathSegment(endpointName, "UTF-8")}"))
        .willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/xml")
          .withBody(documentationXml))
    )
  }
}

trait DeveloperFrontend {

  def developerIsSignedIn() {
    stubFor(
      get(urlPathEqualTo(s"/developer/user-navlinks"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody("""[{"label": "John Doe", "href": "/developer/profile", "truncate" : false, "openInNewWindow": false, "isSensitive": false}, {"label":"Sign out", "href":"/developer/logout", "truncate" : false, "openInNewWindow": false, "isSensitive": false}]"""))
    )
  }

  def developerIsSignedOut() {
    stubFor(
      get(urlPathEqualTo(s"/developer/user-navlinks"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody("""[{"label": "Sign in", "href": "/developer/login", "truncate" : false, "openInNewWindow": false, "isSensitive": false}, {"label":"Register", "href":"/developer/registration", "truncate" : false, "openInNewWindow": false, "isSensitive": false}]"""))
    )
  }
}



object ExternalServicesConfig {

  val stubPort = sys.env.getOrElse("WIREMOCK_PORT", "11111").toInt
  val stubHost = "localhost"
  val wireMockUrl = s"http://$stubHost:$stubPort"

}
