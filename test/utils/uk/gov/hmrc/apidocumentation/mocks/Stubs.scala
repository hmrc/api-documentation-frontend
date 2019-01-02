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

package utils.uk.gov.hmrc.apidocumentation.mocks

import java.io.File
import java.net.URLDecoder

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.Logger
import play.api.http.ContentTypes
import play.utils.UriEncoding

import scala.collection.immutable.Seq
import scala.io.Source
import scala.util.{Failure, Success, Try}

trait Stubs extends ApiDefinition with ApiMicroservice with DeveloperFrontend with ServiceLocator

trait ApiDefinition {

  def fetchAll() {
    val allDefinitionJson = Source.fromURL(getClass.getResource(s"/acceptance/api-definition/all.json")).mkString

    stubFor(
      get(urlMatching("/apis/definition"))
        .willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
          .withBody(allDefinitionJson))
    )
  }

  def fetchDefinition(serviceName: String) {
    val definitionJson = Source.fromURL(getClass.getResource(s"/acceptance/api-definition/$serviceName.json")).mkString
    stubFor(
      get(urlMatching(s"/apis/$serviceName/definition"))
        .willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
          .withBody(definitionJson))
    )
  }

  def fetchDefinitionExtended(serviceName: String) {
    val definitionJson = Source.fromURL(getClass.getResource(s"/acceptance/$serviceName/definition.json")).mkString
    stubFor(
      get(urlMatching(s"/apis/$serviceName/definition/extended"))
        .willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
          .withBody(definitionJson))
    )
  }

  def fetchRaml(serviceName: String, version: String) = {

    def fetchFile(filename: String, contentType: String) = {
      val url = getClass.getResource(s"/services/$serviceName/conf/$version/$filename")
      val file = Source.fromURL(url).mkString
      stubFor(get(urlMatching(s"/apis/$serviceName/$version/documentation/$filename"))
        .willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", contentType)
          .withBody(file))
      )
    }

    fetchFile("application.raml", "application/yaml+raml")
    fetchFile("docs/overview.md", "text/markdown")
    fetchFile("docs/versioning.md", "text/markdown")
    fetchFile("modules/oauth2.raml", "application/yaml+raml")
  }

  def fetchDocRaml(serviceName: String, version: String) = {

    def fetchFile(filename: String, contentType: String) = {
      val file = Source.fromURL(getClass.getResource(s"/services/$serviceName/conf/$version/$filename")).mkString
      stubFor(get(urlMatching(s"/apis/$serviceName/$version/documentation/$filename"))
        .willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", contentType)
          .withBody(file))
      )
    }

    def fetchJsonFile(path: String) = {
      val smt: Try[String] = Try(getClass.getResource(s"/services/$serviceName/conf/$version/$path").getPath)

      val listOfFiles: Seq[File] = smt match {
        case Success(s) =>
          val dir = new File(URLDecoder.decode(s))

          if (dir.exists()) {
             dir.listFiles
               .filter(f => f.exists() && f.isFile)
               .toList
           }
           else {
            List.empty[File]
          }
        case Failure(f) => List.empty[File]
      }

      listOfFiles.foreach {
        r =>
          val file: String = Source.fromURL(getClass.getResource(s"/services/$serviceName/conf/$version/$path/${r.getName}")).mkString
          stubFor(get(urlMatching(s"/apis/$serviceName/$version/documentation/$path/${r.getName}"))
            .willReturn(aResponse()
              .withStatus(200)
              .withHeader("Content-Type", ContentTypes.JSON)
              .withBody(file))
          )
      }
    }

    fetchFile("application.raml", "application/yaml+raml")
    fetchFile("docs/overview.md", "text/markdown")
    fetchJsonFile("examples")
    fetchJsonFile("schemas")
  }

  def failToFetch(serviceName: String) {
    stubFor(
      get(urlMatching(s"/apis/$serviceName/definition"))
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
      get(urlPathEqualTo(s"/apis/$serviceName/$version/documentation/${UriEncoding.encodePathSegment(endpointName, "UTF-8")}"))
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
            .withBody("""[{"label": "John Doe", "href": "/developer/profile", "truncate" : false}, {"label":"Sign out", "href":"/developer/logout", "truncate" : false}]"""))
    )
  }

  def developerIsSignedOut() {
    stubFor(
      get(urlPathEqualTo(s"/developer/user-navlinks"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody("""[{"label": "Sign in", "href": "/developer/login", "truncate" : false}, {"label":"Register", "href":"/developer/registration", "truncate" : false}]"""))
    )
  }
}

trait ServiceLocator {

  def register(serviceName: String) {
    stubFor(
      get(urlPathEqualTo(s"/service/$serviceName"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody( s"""{"serviceName":"$serviceName","serviceUrl":"${ExternalServicesConfig.wireMockUrl}"}"""))
    )
  }
}

object ExternalServicesConfig {

  val stubPort = sys.env.getOrElse("WIREMOCK_PORT", "11111").toInt
  val stubHost = "localhost"
  val wireMockUrl = s"http://$stubHost:$stubPort"

}
