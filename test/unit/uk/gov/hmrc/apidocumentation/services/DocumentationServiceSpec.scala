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

package unit.uk.gov.hmrc.apidocumentation.services

import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.Application
import play.api.cache.CacheApi
import uk.gov.hmrc.apidocumentation
import uk.gov.hmrc.apidocumentation.connectors.APIDocumentationConnector
import uk.gov.hmrc.apidocumentation.models.APIStatus._
import uk.gov.hmrc.apidocumentation.models.{RamlAndSchemas, TestEndpoint, _}
import uk.gov.hmrc.apidocumentation.services.{DocumentationService, RAML, SchemaService}
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.ramltools.domain.{RamlParseException}
import uk.gov.hmrc.ramltools.loaders.{RamlLoader}
import unit.uk.gov.hmrc.apidocumentation.utils.FileRamlLoader

import scala.concurrent.Future
import scala.util.{Failure, Success}

class DocumentationServiceSpec extends UnitSpec with WithFakeApplication with MockitoSugar with ScalaFutures {

  val contentType = "application/xml"
  val rawXml = "<date>2001-01-01</date>"
  val html = "<b>Today is 01 January 2001</b>"
  val serviceName = "calendar"
  val serviceUrl = "http://localhost:1234"
  val api: APIDefinition = apiDefinition("gregorian-calendar")

  trait Setup {
    private val cacheApiCache = Application.instanceCache[CacheApi]
    implicit val hc = HeaderCarrier()
    val apiDocumentationConnector = mock[APIDocumentationConnector]
    val cache = cacheApiCache(fakeApplication)
    val ramlLoader = mock[RamlLoader]
    val schemaLoader = mock[SchemaService]
    val underTest = new DocumentationService(apiDocumentationConnector, cache, ramlLoader, schemaLoader)
  }

  "fetchAPIs with user session handling" should {
    "fetch all APIs if there is no user logged in" in new Setup {
      val apis = Seq(apiDefinition("gregorian-calendar"), apiDefinition("roman-calendar"))
      when(apiDocumentationConnector.fetchAll()).thenReturn(Future.successful(apis))
      val result = await(underTest.fetchAPIs(None))
      result.size shouldBe 2
      result(0).name shouldBe "gregorian-calendar"
      result(1).name shouldBe "roman-calendar"
    }

    "fetch APIs for user email if a user is logged in" in new Setup {
      val loggedInUserEmail = "3rdparty@example.com"
      val apis = Seq(apiDefinition("gregorian-calendar"), apiDefinition("roman-calendar"))
      when(apiDocumentationConnector.fetchByEmail(loggedInUserEmail)).thenReturn(Future.successful(apis))
      val result = await(underTest.fetchAPIs(Some(loggedInUserEmail)))
      result.size shouldBe 2
      result(0).name shouldBe "gregorian-calendar"
      result(1).name shouldBe "roman-calendar"
    }
  }

  "fetchAPI with user session handling" should {

    "fetch a single API if there is no user logged in" in new Setup {
      val api = extendedApiDefinition("buddist-calendar")
      when(apiDocumentationConnector.fetchExtendedDefinitionByServiceName("buddist-calendar")).thenReturn(Future.successful(api))
      val result = await(underTest.fetchExtendedApiDefinition("buddist-calendar", None))
      result shouldBe defined
      result.get.name shouldBe "buddist-calendar"
    }

    "fetch a single API for user email if a user is logged in" in new Setup {
      val loggedInUserEmail = "3rdparty@example.com"
      val api = extendedApiDefinition("buddist-calendar")
      when(apiDocumentationConnector.fetchExtendedDefinitionByServiceNameAndEmail("buddist-calendar", loggedInUserEmail)).thenReturn(Future.successful(api))
      val result = await(underTest.fetchExtendedApiDefinition("buddist-calendar", Some(loggedInUserEmail)))
      result shouldBe defined
      result.get.name shouldBe "buddist-calendar"
    }

    "reject for an unsubscribed API for user email if a user is logged in" in new Setup {
      val loggedInUserEmail = "3rdparty@example.com"
      val api = apiDefinition("buddist-calendar")
      when(apiDocumentationConnector.fetchExtendedDefinitionByServiceNameAndEmail("buddist-calendar", loggedInUserEmail)).thenReturn(Future.failed(new NotFoundException("Expected unit test exception")))
      intercept[NotFoundException] {
        await(underTest.fetchExtendedApiDefinition("buddist-calendar", Some(loggedInUserEmail)))
      }
    }
  }

  "filterDefinitions" should {

    "return all API Definitions" in new Setup {
      val apis = Seq(apiDefinition("gregorian-calendar"), apiDefinition("roman-calendar"))
      underTest.filterDefinitions(apis) shouldBe apis
    }

    "filter APIs which requires trust" in new Setup {
      val apis = Seq(apiDefinition("gregorian-calendar").copy(requiresTrust = Some(false)), apiDefinition("roman-calendar").copy(requiresTrust = Some(true)))
      underTest.filterDefinitions(apis) shouldBe Seq(apiDefinition("gregorian-calendar").copy(requiresTrust = Some(false)))
    }

    "return versions in expected order" in new Setup {
      val apis = Seq(apiDefinition("api-1", Seq(
        apiVersion("2.0", STABLE),
        apiVersion("3.0", BETA),
        apiVersion("1.0", DEPRECATED),
        apiVersion("2.5", BETA))))

      underTest.filterDefinitions(apis).flatMap(_.statusSortedActiveVersions) shouldBe Seq(
        apiVersion("2.0", STABLE),
        apiVersion("3.0", BETA),
        apiVersion("2.5", BETA),
        apiVersion("1.0", DEPRECATED)
      )
    }
  }

  "fetchRAML" should {

    "fail when raml loader fails" in new Setup {
      val url = s"$serviceUrl/apis/$serviceName/1.0/documentation/application.raml"
      when(apiDocumentationConnector.serviceBaseUrl).thenReturn(serviceUrl)
      when(ramlLoader.load(url)).thenReturn(Failure(RamlParseException("Expected test failure")))
      intercept[RamlParseException] {
        await(underTest.fetchRAML(serviceName, "1.0", true))
      }
    }

    "clear the cache key when the load fails" in new Setup {
      val url = s"$serviceUrl/apis/$serviceName/1.0/documentation/application.raml"
      when(apiDocumentationConnector.serviceBaseUrl).thenReturn(serviceUrl)
      cache.set(url, mock[RAML])
      when(ramlLoader.load(url)).thenReturn(Failure(RamlParseException("Expected test failure")))
      intercept[RamlParseException] {
        await(underTest.fetchRAML(serviceName, "1.0", false))
      }
      cache.get(url) shouldBe None
    }

    "return a RAML API object when the load is successful" in new Setup {
      val url = s"$serviceUrl/apis/$serviceName/1.1/documentation/application.raml"
      val schemaBase = s"$serviceUrl/apis/$serviceName/1.1/documentation/schemas"

      when(apiDocumentationConnector.serviceBaseUrl).thenReturn(serviceUrl)

      val expectedRaml = mock[RAML]
      when(ramlLoader.load(url)).thenReturn(Success(expectedRaml))
      val expectedSchemas = mock[Map[String,JsonSchema]]
      when(schemaLoader.loadSchemas(schemaBase, expectedRaml)).thenReturn(expectedSchemas)

      await(underTest.fetchRAML(serviceName, "1.1", true)) shouldBe apidocumentation.models.RamlAndSchemas(expectedRaml, expectedSchemas)
    }

    "clear the cached RAML when cachebuster is set" in new Setup {
      val url = s"$serviceUrl/apis/$serviceName/1.1/documentation/application.raml"
      val schemaBase = s"$serviceUrl/apis/$serviceName/1.1/documentation/schemas"

      when(apiDocumentationConnector.serviceBaseUrl).thenReturn(serviceUrl)

      val expectedRaml1 = mock[RAML]
      when(ramlLoader.load(url)).thenReturn(Success(expectedRaml1))
      val expectedSchemas1 = mock[Map[String,JsonSchema]]
      when(schemaLoader.loadSchemas(schemaBase, expectedRaml1)).thenReturn(expectedSchemas1)
      await(underTest.fetchRAML(serviceName, "1.1", true)) shouldBe apidocumentation.models.RamlAndSchemas(expectedRaml1, expectedSchemas1)

      val expectedRaml2 = mock[RAML]
      when(ramlLoader.load(url)).thenReturn(Success(expectedRaml2))
      val expectedSchemas2 = mock[Map[String,JsonSchema]]
      when(schemaLoader.loadSchemas(schemaBase, expectedRaml2)).thenReturn(expectedSchemas2)
      await(underTest.fetchRAML(serviceName, "1.1", false)) shouldBe apidocumentation.models.RamlAndSchemas(expectedRaml1, expectedSchemas1)

      val expectedRaml3 = mock[RAML]
      when(ramlLoader.load(url)).thenReturn(Success(expectedRaml3))
      val expectedSchemas3 = mock[Map[String,JsonSchema]]
      when(schemaLoader.loadSchemas(schemaBase, expectedRaml3)).thenReturn(expectedSchemas3)
      await(underTest.fetchRAML(serviceName, "1.1", true)) shouldBe RamlAndSchemas(expectedRaml3, expectedSchemas3)
    }
  }

  "buildTestEndpoints" should {

    "create a simple testers URL output file with just endpoint information" in new Setup {
      val service = "minimal"
      val raml = new FileRamlLoader().load(s"test/resources/unit/raml/$service.raml")
      when(ramlLoader.load(any[String])).thenReturn(Future.successful(raml))
      await(underTest.buildTestEndpoints("minimal", "1.0")) shouldBe Seq.empty
    }

    "create a simple testers URL output file with just endpoint information for a single endpoint" in new Setup {
      val service = "single-endpoint"
      val raml = new FileRamlLoader().load(s"test/resources/unit/raml/$service.raml")
      when(ramlLoader.load(any[String])).thenReturn(Future.successful(raml))
      val expected = Seq(TestEndpoint("{service-url}/hello/world", "GET"))
      await(underTest.buildTestEndpoints("single-endpoint", "1.0")) shouldBe expected
    }

    "create a complex testers URL output file with just endpoint information for a multiple endpoints" in new Setup {
      val service = "multiple-endpoints"
      val raml = new FileRamlLoader().load(s"test/resources/unit/raml/$service.raml")
      when(ramlLoader.load(any[String])).thenReturn(Future.successful(raml))
      val expected = Seq(
        TestEndpoint("{service-url}/hello/there", "GET", "OPTIONS", "PUT"),
        TestEndpoint("{service-url}/hello/there/{empref}", "DELETE"),
        TestEndpoint("{service-url}/hello/there/{empref}/year", "POST"),
        TestEndpoint("{service-url}/hello/there/{empref}/year/{taxYear}", "PUT"))
      await(underTest.buildTestEndpoints("multiple-endpoints", "1.0")) shouldBe expected
    }
  }

  private def apiDefinition(name: String, versions: Seq[APIVersion] = Seq(apiVersion("1.0", STABLE))) = {
    APIDefinition(name, name, name, name, None, None, versions)
  }

  private def extendedApiDefinition(name: String) = {
    ExtendedAPIDefinition(name, "http://service", name, name, name, requiresTrust = false, isTestSupport = false,
      Seq(
        ExtendedAPIVersion("1.0", APIStatus.STABLE, Seq(Endpoint("Today's Date", "/today", HttpMethod.GET, None),
          Endpoint("Yesterday's Date", "/yesterday", HttpMethod.GET, None)),
          Some(APIAvailability(endpointsEnabled = true, APIAccess(APIAccessType.PUBLIC), loggedIn = false, authorised = true)), None)
      ))
  }

  def apiVersion(version: String, status: APIStatus = STABLE, access: Option[APIAccess] = None): APIVersion = {
    APIVersion(version, access, status, Seq(
      Endpoint("Today's Date", "/today", HttpMethod.GET, None),
      Endpoint("Yesterday's Date", "/yesterday", HttpMethod.GET, None)))
  }
}
