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

package uk.gov.hmrc.apidocumentation.controllers

import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc._
import play.api.http.Status._
import play.api.test.FakeRequest
import uk.gov.hmrc.apidocumentation.models.APIAccessType.APIAccessType
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.apidocumentation.utils.ApiDefinitionTestDataHelper
import uk.gov.hmrc.apidocumentation.models._

import scala.concurrent.Future
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

class CommonControllerBaseSpec
  extends UnitSpec
    with ScalaFutures
    with MockitoSugar
    with ApiDefinitionTestDataHelper
    with GuiceOneAppPerSuite
    {

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(("metrics.jvm", false))
      .build()

  implicit lazy val request: Request[AnyContent] = FakeRequest()
  implicit lazy val materializer = app.materializer
  lazy val mcc = app.injector.instanceOf[MessagesControllerComponents]

  implicit val hc = HeaderCarrier()

  val serviceName = "hello-world"
  val endpointName = "Say Hello World!"

  def anApiDefinition(serviceName: String, version: String): APIDefinition = {
    APIDefinition(serviceName, "Hello World", "Say Hello World", "hello", None, None,
      Seq(APIVersion(version, None, APIStatus.STABLE, Seq(endpoint()))))
  }

  def anXmlApiDefinition(name: String) = XmlApiDocumentation(name, "description", "context")

  def extendedApiDefinition(serviceName: String, version: String): ExtendedAPIDefinition =
    extendedApiDefinition(serviceName, version, APIAccessType.PUBLIC, loggedIn = false, authorised = true)

  def extendedApiDefinition(serviceName: String,
                            version: String,
                            access: APIAccessType,
                            loggedIn: Boolean,
                            authorised: Boolean,
                            isTrial: Option[Boolean] = None): ExtendedAPIDefinition = {
    ExtendedAPIDefinition(serviceName, "http://service", "Hello World", "Say Hello World", "hello", requiresTrust = false, isTestSupport = false,
      Seq(
        ExtendedAPIVersion(version, APIStatus.STABLE, Seq(Endpoint(endpointName, "/world", HttpMethod.GET, None)),
          Some(APIAvailability(endpointsEnabled = true, APIAccess(access, whitelistedApplicationIds = Some(Seq.empty), isTrial = isTrial), loggedIn, authorised)), None)
      ))
  }

  def extendedApiDefinitionWithRetiredVersion(serviceName: String, retiredVersion: String, nonRetiredVersion: String) = {
    ExtendedAPIDefinition(serviceName, "http://service", "Hello World", "Say Hello World", "hello", requiresTrust = false, isTestSupport = false,
      Seq(
        ExtendedAPIVersion(retiredVersion, APIStatus.RETIRED, Seq(endpoint(endpointName)),
          Some(APIAvailability(endpointsEnabled = true, APIAccess(APIAccessType.PUBLIC), loggedIn = false, authorised = true)),
          None),
        ExtendedAPIVersion(nonRetiredVersion, APIStatus.STABLE, Seq(endpoint(endpointName)),
          Some(APIAvailability(endpointsEnabled = true, APIAccess(APIAccessType.PUBLIC, Some(Seq.empty)), loggedIn = false, authorised = true)),
          None)
      ))
  }

  def extendedApiDefinitionWithRetiredVersionAndInaccessibleLatest(serviceName: String): ExtendedAPIDefinition = {
    ExtendedAPIDefinition(serviceName, "http://service", "Hello World", "Say Hello World", "hello", requiresTrust = false, isTestSupport = false,
      Seq(
        ExtendedAPIVersion("1.0", APIStatus.RETIRED, Seq(endpoint(endpointName)),
          Some(APIAvailability(endpointsEnabled = true, APIAccess(APIAccessType.PUBLIC), loggedIn = false, authorised = true)),
          None),
        ExtendedAPIVersion("1.1", APIStatus.BETA, Seq(endpoint(endpointName)),
          Some(APIAvailability(endpointsEnabled = true, APIAccess(APIAccessType.PUBLIC), loggedIn = false, authorised = true)),
          None),
        ExtendedAPIVersion("1.2", APIStatus.STABLE, Seq(endpoint(endpointName)),
          Some(APIAvailability(endpointsEnabled = true, APIAccess(APIAccessType.PRIVATE), loggedIn = false, authorised = false)),
          None)
      ))
  }

  def aServiceGuide(name: String) = ServiceGuide(name, "context")

  def verifyRedirectToLoginPage(actualPageFuture: Future[Result], service: String, version: String) {
    val actualPage = await(actualPageFuture)
    status(actualPage) shouldBe 303

    actualPage.header.headers.get("Location") shouldBe Some("/developer/login")
    actualPage.session.get("access_uri") shouldBe Some(s"/api-documentation/docs/api/service/$service/$version")
  }

  def pageTitle(pagePurpose: String) = s"$pagePurpose - HMRC Developer Hub - GOV.UK"

  def isPresentAndCorrect(includesText: String, title: String)(fResult: Future[Result]): Unit = {
    val result = await(fResult)
    status(result) shouldBe OK
    bodyOf(result) should include(includesText)
    bodyOf(result) should include(pageTitle(title))
  }
}

