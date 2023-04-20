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

import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apidocumentation.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apidocumentation.models.APIAccessType.APIAccessType
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.apidocumentation.utils.ApiDefinitionTestDataHelper

class CommonControllerBaseSpec extends AsyncHmrcSpec with ApiDefinitionTestDataHelper with GuiceOneAppPerSuite {

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(("metrics.jvm", false))
      .build()

  implicit lazy val request: Request[AnyContent] = FakeRequest()
  implicit lazy val materializer                 = app.materializer
  lazy val mcc                                   = app.injector.instanceOf[MessagesControllerComponents]

  implicit val hc = HeaderCarrier()

  val serviceName  = "hello-world"
  val endpointName = "Say Hello World!"

  def anApiDefinition(serviceName: String, version: String): APIDefinition = {
    APIDefinition(serviceName, "Hello World", "Say Hello World", "hello", None, None, Seq(APIVersion(version, None, APIStatus.STABLE, Seq(endpoint()))))
  }

  def anXmlApiDefinition(name: String) = XmlApiDocumentation(name, "description", "context")

  def extendedApiDefinition(
      serviceName: String,
      name: String = "Hello World",
      version: String = "1.0",
      access: APIAccessType = APIAccessType.PUBLIC,
      loggedIn: Boolean = false,
      authorised: Boolean = true,
      isTrial: Option[Boolean] = None,
      isTestSupport: Boolean = false
    ): ExtendedAPIDefinition = {
    ExtendedAPIDefinition(
      serviceName,
      name,
      "Say Hello World",
      "hello",
      requiresTrust = false,
      isTestSupport,
      Seq(
        ExtendedAPIVersion(
          version,
          APIStatus.STABLE,
          Seq(Endpoint(endpointName, "/world", HttpMethod.GET, None)),
          Some(APIAvailability(endpointsEnabled = true, APIAccess(access, whitelistedApplicationIds = Some(Seq.empty), isTrial = isTrial), loggedIn, authorised)),
          None
        )
      )
    )
  }

  def extendedApiDefinitionWithNoAPIAvailability(serviceName: String, version: String): ExtendedAPIDefinition = {
    ExtendedAPIDefinition(
      serviceName,
      "Hello World",
      "Say Hello World",
      "hello",
      requiresTrust = false,
      isTestSupport = false,
      Seq(
        ExtendedAPIVersion(version, APIStatus.STABLE, Seq(Endpoint(endpointName, "/world", HttpMethod.GET, None)), None, None)
      )
    )
  }

  def extendedApiDefinitionWithPrincipalAndSubordinateAPIAvailability(
      serviceName: String,
      version: String,
      principalApiAvailability: Option[APIAvailability],
      subordinateApiAvailability: Option[APIAvailability]
    ): ExtendedAPIDefinition = {
    ExtendedAPIDefinition(
      serviceName,
      "Hello World",
      "Say Hello World",
      "hello",
      requiresTrust = false,
      isTestSupport = false,
      Seq(
        ExtendedAPIVersion(version, APIStatus.STABLE, Seq(Endpoint(endpointName, "/world", HttpMethod.GET, None)), principalApiAvailability, subordinateApiAvailability)
      )
    )
  }

  def extendedApiDefinitionWithRetiredVersion(serviceName: String, retiredVersion: String, nonRetiredVersion: String) = {
    ExtendedAPIDefinition(
      serviceName,
      "Hello World",
      "Say Hello World",
      "hello",
      requiresTrust = false,
      isTestSupport = false,
      Seq(
        ExtendedAPIVersion(
          retiredVersion,
          APIStatus.RETIRED,
          Seq(endpoint(endpointName)),
          Some(APIAvailability(endpointsEnabled = true, APIAccess(APIAccessType.PUBLIC), loggedIn = false, authorised = true)),
          None
        ),
        ExtendedAPIVersion(
          nonRetiredVersion,
          APIStatus.STABLE,
          Seq(endpoint(endpointName)),
          Some(APIAvailability(endpointsEnabled = true, APIAccess(APIAccessType.PUBLIC, Some(Seq.empty)), loggedIn = false, authorised = true)),
          None
        )
      )
    )
  }

  def extendedApiDefinitionWithRetiredVersionAndInaccessibleLatest(serviceName: String): ExtendedAPIDefinition = {
    ExtendedAPIDefinition(
      serviceName,
      "Hello World",
      "Say Hello World",
      "hello",
      requiresTrust = false,
      isTestSupport = false,
      Seq(
        ExtendedAPIVersion(
          "1.0",
          APIStatus.RETIRED,
          Seq(endpoint(endpointName)),
          Some(APIAvailability(endpointsEnabled = true, APIAccess(APIAccessType.PUBLIC), loggedIn = false, authorised = true)),
          None
        ),
        ExtendedAPIVersion(
          "1.1",
          APIStatus.BETA,
          Seq(endpoint(endpointName)),
          Some(APIAvailability(endpointsEnabled = true, APIAccess(APIAccessType.PUBLIC), loggedIn = false, authorised = true)),
          None
        ),
        ExtendedAPIVersion(
          "1.2",
          APIStatus.STABLE,
          Seq(endpoint(endpointName)),
          Some(APIAvailability(endpointsEnabled = true, APIAccess(APIAccessType.PRIVATE), loggedIn = false, authorised = false)),
          None
        )
      )
    )
  }

  def aServiceGuide(name: String) = ServiceGuide(name, "context")

  def verifyRedirectToLoginPage(actualPage: Future[Result], service: String, version: String): Unit = {
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
