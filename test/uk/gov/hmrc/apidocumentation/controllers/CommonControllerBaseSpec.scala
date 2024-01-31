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

package uk.gov.hmrc.apidocumentation.controllers

import scala.concurrent.Future

import org.apache.pekko.stream.Materializer
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apidocumentation.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.apidocumentation.utils.ApiDefinitionTestDataHelper

class CommonControllerBaseSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper with GuiceOneAppPerSuite {

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(("metrics.jvm", false))
      .build()

  implicit lazy val request: Request[AnyContent] = FakeRequest()
  implicit lazy val materializer: Materializer   = app.materializer
  lazy val mcc                                   = app.injector.instanceOf[MessagesControllerComponents]
  implicit val hc: HeaderCarrier                 = HeaderCarrier()

  val serviceName  = ServiceName("hello-world")
  val endpointName = "Say Hello World!"

  def anXmlApiDefinition(name: String) = XmlApiDocumentation(name, "description", "context")

  def extendedApiDefinition(
      serviceName: String,
      name: String = "Hello World",
      version: ApiVersionNbr = ApiVersionNbr("1.0"),
      access: ApiAccess = ApiAccess.PUBLIC,
      loggedIn: Boolean = false,
      authorised: Boolean = true,
      isTestSupport: Boolean = false
    ): ExtendedApiDefinition = {
    ExtendedApiDefinition(
      ServiceName(serviceName),
      "/world",
      name,
      "Say Hello World",
      ApiContext("hello"),
      versions = List(
        ExtendedApiVersion(
          version,
          ApiStatus.STABLE,
          List(Endpoint(endpointName, "/world", HttpMethod.GET, AuthType.NONE)),
          Some(ApiAvailability(endpointsEnabled = true, access, loggedIn, authorised)),
          None
        )
      ),
      isTestSupport = isTestSupport,
      lastPublishedAt = None,
      categories = List(ApiCategory.OTHER)
    )
  }

  def extendedApiDefinitionWithNoAPIAvailability(serviceName: ServiceName, version: ApiVersionNbr): ExtendedApiDefinition = {
    ExtendedApiDefinition(
      serviceName,
      "/world",
      "Hello World",
      "Say Hello World",
      ApiContext("hello"),
      List(ExtendedApiVersion(version, ApiStatus.STABLE, List(Endpoint(endpointName, "/world", HttpMethod.GET, AuthType.NONE)), None, None)),
      isTestSupport = false,
      lastPublishedAt = None,
      categories = List(ApiCategory.OTHER)
    )
  }

  def extendedApiDefinitionWithPrincipalAndSubordinateAPIAvailability(
      serviceName: ServiceName,
      version: ApiVersionNbr,
      principalApiAvailability: Option[ApiAvailability],
      subordinateApiAvailability: Option[ApiAvailability]
    ): ExtendedApiDefinition = {
    ExtendedApiDefinition(
      serviceName,
      "hello",
      "Hello World",
      "Say Hello World",
      ApiContext("hello"),
      versions = List(
        ExtendedApiVersion(version, ApiStatus.STABLE, List(Endpoint(endpointName, "/world", HttpMethod.GET, AuthType.NONE)), principalApiAvailability, subordinateApiAvailability)
      ),
      isTestSupport = false,
      lastPublishedAt = None,
      categories = List(ApiCategory.OTHER)
    )
  }

  def extendedApiDefinitionWithRetiredVersion(serviceName: ServiceName, retiredVersion: ApiVersionNbr, nonRetiredVersion: ApiVersionNbr) = {
    ExtendedApiDefinition(
      serviceName,
      serviceBaseUrl = "/world",
      name = "Hello World",
      description = "Say Hello World",
      context = ApiContext("hello"),
      versions = List(
        ExtendedApiVersion(
          retiredVersion,
          ApiStatus.RETIRED,
          List(endpoint(endpointName)),
          Some(ApiAvailability(endpointsEnabled = true, access = ApiAccess.PUBLIC, loggedIn = false, authorised = true)),
          None
        ),
        ExtendedApiVersion(
          nonRetiredVersion,
          ApiStatus.STABLE,
          List(endpoint(endpointName)),
          Some(ApiAvailability(endpointsEnabled = true, access = ApiAccess.PUBLIC, loggedIn = false, authorised = true)),
          None
        )
      ),
      isTestSupport = false,
      lastPublishedAt = None,
      categories = List(ApiCategory.OTHER)
    )
  }

  def extendedApiDefinitionWithRetiredVersionAndInaccessibleLatest(serviceName: String): ExtendedApiDefinition = {
    ExtendedApiDefinition(
      ServiceName(serviceName),
      serviceBaseUrl = "/world",
      name = "Hello World",
      description = "Say Hello World",
      context = ApiContext("hello"),
      versions = List(
        ExtendedApiVersion(
          ApiVersionNbr("1.0"),
          ApiStatus.RETIRED,
          List(endpoint(endpointName)),
          Some(ApiAvailability(endpointsEnabled = true, access = ApiAccess.PUBLIC, loggedIn = false, authorised = true)),
          None
        ),
        ExtendedApiVersion(
          ApiVersionNbr("1.1"),
          ApiStatus.BETA,
          List(endpoint(endpointName)),
          Some(ApiAvailability(endpointsEnabled = true, access = ApiAccess.PUBLIC, loggedIn = false, authorised = true)),
          None
        ),
        ExtendedApiVersion(
          ApiVersionNbr("1.2"),
          ApiStatus.STABLE,
          List(endpoint(endpointName)),
          Some(ApiAvailability(endpointsEnabled = true, access = ApiAccess.Private(false), loggedIn = false, authorised = false)),
          None
        )
      ),
      isTestSupport = false,
      lastPublishedAt = None,
      categories = List(ApiCategory.OTHER)
    )
  }

  def aServiceGuide(name: String) = ServiceGuide(name, "context")

  def verifyRedirectToLoginPage(actualPage: Future[Result], service: ServiceName, version: ApiVersionNbr): Unit = {
    status(actualPage) shouldBe 303

    headers(actualPage).get("Location") shouldBe Some("/developer/login")
    session(actualPage).get("access_uri") shouldBe Some(s"/api-documentation/docs/api/service/$service/$version")
  }

  def pageTitle(pagePurpose: String) = s"$pagePurpose - HMRC Developer Hub - GOV.UK"

  def isPresentAndCorrect(includesText: String, title: String)(result: Future[Result]): Unit = {
    status(result) shouldBe OK
    contentAsString(result) should include(includesText)
    contentAsString(result) should include(pageTitle(title))
  }
}
