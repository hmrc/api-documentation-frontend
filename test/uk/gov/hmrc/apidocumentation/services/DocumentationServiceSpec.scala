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

package unit.uk.gov.hmrc.apidocumentation.services

import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.cache.SyncCacheApi
import uk.gov.hmrc.apidocumentation
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.models.{RamlAndSchemas, TestEndpoint, _}
import uk.gov.hmrc.apidocumentation.services.{DocumentationService, RAML, SchemaService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.ramltools.domain.RamlParseException
import uk.gov.hmrc.ramltools.loaders.RamlLoader
import unit.uk.gov.hmrc.apidocumentation.utils.{ApiDefinitionTestDataHelper, FileRamlLoader}

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Application
import play.api.inject.bind
import play.api.cache.CacheApi
import play.api.cache.ehcache.EhCacheModule
import play.api.Mode

class DocumentationServiceSpec extends UnitSpec
  with GuiceOneAppPerTest
  with MockitoSugar
  with ScalaFutures
  with ApiDefinitionTestDataHelper {

  val contentType = "application/xml"
  val rawXml = "<date>2001-01-01</date>"
  val html = "<b>Today is 01 January 2001</b>"
  val serviceName = "calendar"
  val serviceUrl = "http://localhost:1234"
  val api: APIDefinition = apiDefinition("gregorian-calendar")

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure("metrics.jvm" -> false)
      .build()

  trait Setup {
    implicit val hc = HeaderCarrier()
    val cache = app.injector.instanceOf[CacheApi]
    val ramlLoader = mock[RamlLoader]
    val schemaLoader = mock[SchemaService]
    val appConfig = mock[ApplicationConfig]
    when(appConfig.apiDefinitionBaseUrl).thenReturn(serviceUrl)

    val underTest = new DocumentationService(appConfig, cache, ramlLoader, schemaLoader)
  }

  "fetchRAML" should {

    "fail when raml loader fails" in new Setup {
      val url = DocumentationService.ramlUrl(serviceUrl,serviceName,"1.0")
      when(ramlLoader.load(url)).thenReturn(Failure(RamlParseException("Expected test failure")))
      intercept[RamlParseException] {
        await(underTest.fetchRAML(serviceName, "1.0", cacheBuster = true))
      }
    }

    "clear the cache key when the load fails" in new Setup {
      val url = DocumentationService.ramlUrl(serviceUrl,serviceName,"1.0")
      cache.set(url, mock[RAML])
      when(ramlLoader.load(url)).thenReturn(Failure(RamlParseException("Expected test failure")))
      intercept[RamlParseException] {
        await(underTest.fetchRAML(serviceName, "1.0", cacheBuster = false))
      }
      cache.get(url) shouldBe None
    }

    "return a RAML API object when the load is successful" in new Setup {
      val url = DocumentationService.ramlUrl(serviceUrl,serviceName,"1.1")
      val schemaBase = DocumentationService.schemasUrl(serviceUrl,serviceName,"1.1")

      val expectedRaml = mock[RAML]
      when(ramlLoader.load(url)).thenReturn(Success(expectedRaml))
      val expectedSchemas = mock[Map[String,JsonSchema]]
      when(schemaLoader.loadSchemas(schemaBase, expectedRaml)).thenReturn(expectedSchemas)

      await(underTest.fetchRAML(serviceName, "1.1", cacheBuster = true)) shouldBe apidocumentation.models.RamlAndSchemas(expectedRaml, expectedSchemas)
    }

    "clear the cached RAML when cachebuster is set" in new Setup {
      val url = DocumentationService.ramlUrl(serviceUrl,serviceName,"1.1")
      val schemaBase = DocumentationService.schemasUrl(serviceUrl,serviceName,"1.1")

      val expectedRaml1 = mock[RAML]
      when(ramlLoader.load(url)).thenReturn(Success(expectedRaml1))
      val expectedSchemas1 = mock[Map[String,JsonSchema]]
      when(schemaLoader.loadSchemas(schemaBase, expectedRaml1)).thenReturn(expectedSchemas1)
      await(underTest.fetchRAML(serviceName, "1.1", cacheBuster = true)) shouldBe apidocumentation.models.RamlAndSchemas(expectedRaml1, expectedSchemas1)

      val expectedRaml2 = mock[RAML]
      when(ramlLoader.load(url)).thenReturn(Success(expectedRaml2))
      val expectedSchemas2 = mock[Map[String,JsonSchema]]
      when(schemaLoader.loadSchemas(schemaBase, expectedRaml2)).thenReturn(expectedSchemas2)
      await(underTest.fetchRAML(serviceName, "1.1", cacheBuster = false)) shouldBe apidocumentation.models.RamlAndSchemas(expectedRaml1, expectedSchemas1)

      val expectedRaml3 = mock[RAML]
      when(ramlLoader.load(url)).thenReturn(Success(expectedRaml3))
      val expectedSchemas3 = mock[Map[String,JsonSchema]]
      when(schemaLoader.loadSchemas(schemaBase, expectedRaml3)).thenReturn(expectedSchemas3)
      await(underTest.fetchRAML(serviceName, "1.1", cacheBuster = true)) shouldBe RamlAndSchemas(expectedRaml3, expectedSchemas3)
    }
  }

  "buildTestEndpoints" should {

    "create a simple testers URL output file with just endpoint information" in new Setup {
      val service = "minimal"
      val raml = new FileRamlLoader().load(s"test/resources/raml/$service.raml")
      when(ramlLoader.load(any[String])).thenReturn(Future.successful(raml))
      await(underTest.buildTestEndpoints("minimal", "1.0")) shouldBe Seq.empty
    }

    "create a simple testers URL output file with just endpoint information for a single endpoint" in new Setup {
      val service = "single-endpoint"
      val raml = new FileRamlLoader().load(s"test/resources/raml/$service.raml")
      when(ramlLoader.load(any[String])).thenReturn(Future.successful(raml))
      val expected = Seq(TestEndpoint("{service-url}/hello/world", "GET"))
      await(underTest.buildTestEndpoints("single-endpoint", "1.0")) shouldBe expected
    }

    "create a complex testers URL output file with just endpoint information for a multiple endpoints" in new Setup {
      val service = "multiple-endpoints"
      val raml = new FileRamlLoader().load(s"test/resources/raml/$service.raml")
      when(ramlLoader.load(any[String])).thenReturn(Future.successful(raml))
      val expected = Seq(
        TestEndpoint("{service-url}/hello/there", "GET", "OPTIONS", "PUT"),
        TestEndpoint("{service-url}/hello/there/{empref}", "DELETE"),
        TestEndpoint("{service-url}/hello/there/{empref}/year", "POST"),
        TestEndpoint("{service-url}/hello/there/{empref}/year/{taxYear}", "PUT"))
      await(underTest.buildTestEndpoints("multiple-endpoints", "1.0")) shouldBe expected
    }
  }
}
