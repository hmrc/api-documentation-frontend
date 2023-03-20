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

package uk.gov.hmrc.apidocumentation.services

import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Application
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.models.TestEndpoint
import uk.gov.hmrc.apidocumentation.utils.ApiDefinitionTestDataHelper
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.Future.successful
import uk.gov.hmrc.apidocumentation.connectors.ApiPlatformMicroserviceConnector
import uk.gov.hmrc.apidocumentation.models.apispecification.ApiSpecification
import uk.gov.hmrc.apidocumentation.models.apispecification.ResourceGroup
import uk.gov.hmrc.apidocumentation.models.apispecification.Resource
import uk.gov.hmrc.apidocumentation.models.apispecification.Method
import uk.gov.hmrc.apidocumentation.models.APIDefinition

import scala.concurrent.ExecutionContext.Implicits.global

import uk.gov.hmrc.apidocumentation.common.utils.AsyncHmrcSpec
import play.api.cache.AsyncCacheApi

class DocumentationServiceSpec extends AsyncHmrcSpec with GuiceOneAppPerTest with ApiDefinitionTestDataHelper {

  val contentType        = "application/xml"
  val rawXml             = "<date>2001-01-01</date>"
  val html               = "<b>Today is 01 January 2001</b>"
  val serviceName        = "calendar"
  val serviceUrl         = "http://localhost:1234"
  val api: APIDefinition = apiDefinition("gregorian-calendar")

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure("metrics.jvm" -> false)
      .build()

  trait Setup {
    implicit val hc = HeaderCarrier()
    val cache       = app.injector.instanceOf[AsyncCacheApi]
    val appConfig   = mock[ApplicationConfig]
    val apm         = mock[ApiPlatformMicroserviceConnector]
    when(appConfig.apiPlatformMicroserviceBaseUrl).thenReturn(serviceUrl)

    val underTest = new DocumentationService(appConfig, cache, apm)
  }

  "buildTestEndpoints" should {

    val defaultApiSpecification = ApiSpecification(
      title = "Title",
      version = "1.0",
      None,
      List.empty,
      List.empty,
      List.empty,
      false
    )

    def defaultResource(path: String, methods: String*) =
      Resource(
        resourcePath = path,
        methods = toMethods(methods: _*),
        uriParameters = List.empty,
        relativeUri = "",
        displayName = "",
        children = List.empty
      )

    def toMethods(methods: String*): List[Method] = {
      methods.toList.map(m => defaultMethod(m))
    }

    def defaultMethod(verb: String) = Method(
      method = verb,
      displayName = "name",
      body = List.empty,
      headers = List.empty,
      queryParameters = List.empty,
      description = None,
      securedBy = None,
      responses = List.empty,
      sandboxData = None
    )

    "create a simple testers URL output file with just endpoint information" in new Setup {
      val specification = defaultApiSpecification
      when(apm.fetchApiSpecification(*, *)(*)).thenReturn(successful(Some(specification)))
      await(underTest.buildTestEndpoints("minimal", "1.0")) shouldBe Seq.empty
    }

    "create a simple testers URL output file with just endpoint information for a single endpoint" in new Setup {
      val specification = defaultApiSpecification.copy(
        resourceGroups = List(
          ResourceGroup(
            name = None,
            description = None,
            resources = List(
              defaultResource("/hello")
                .copy(
                  children = List(
                    defaultResource("/hello/world", "get")
                  )
                )
            )
          )
        )
      )
      when(apm.fetchApiSpecification(*, *)(*)).thenReturn(successful(Some(specification)))

      val expected = Seq(TestEndpoint("{service-url}/hello/world", "GET"))
      await(underTest.buildTestEndpoints("single-endpoint", "1.0")) shouldBe expected
    }

    "create a complex testers URL output file with just endpoint information for a multiple endpoints" in new Setup {
      val specification = defaultApiSpecification.copy(
        resourceGroups = List(
          ResourceGroup(
            name = None,
            description = None,
            resources = List(
              defaultResource("/hello")
                .copy(
                  children = List(
                    defaultResource("/hello/there", "options", "get", "put").copy(
                      children = List(
                        defaultResource("/hello/there/{empref}", "delete").copy(
                          children = List(
                            defaultResource("/hello/there/{empref}/year", "post")
                          )
                        )
                      )
                    )
                  )
                )
            )
          )
        )
      )
      when(apm.fetchApiSpecification(*, *)(*)).thenReturn(successful(Some(specification)))

      val expected = Seq(
        TestEndpoint("{service-url}/hello/there", "GET", "OPTIONS", "PUT"),
        TestEndpoint("{service-url}/hello/there/{empref}", "DELETE"),
        TestEndpoint("{service-url}/hello/there/{empref}/year", "POST")
      )
      await(underTest.buildTestEndpoints("multiple-endpoints", "1.0")) shouldBe expected
    }
  }
}
