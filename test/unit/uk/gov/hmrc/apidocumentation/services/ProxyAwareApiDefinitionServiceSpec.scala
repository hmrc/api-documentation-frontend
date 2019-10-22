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

package unit.uk.gov.hmrc.apidocumentation.services

import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.apidocumentation.services.{PrincipalApiDefinitionService, ProxyAwareApiDefinitionService, SubordinateApiDefinitionService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import unit.uk.gov.hmrc.apidocumentation.utils.{ApiDefinitionTestDataHelper, BaseApiDefinitionServiceMockingHelper}

import scala.concurrent.ExecutionContext.Implicits.global

class ProxyAwareApiDefinitionServiceSpec
  extends UnitSpec
    with ScalaFutures
    with MockitoSugar
    with ApiDefinitionTestDataHelper
    with BaseApiDefinitionServiceMockingHelper {

  trait Setup {
    implicit val hc = new HeaderCarrier()
    val serviceName = "api-example-microservice"
    val loggedInUserEmail = "john.doe@example.com"
    val localDef1 = APIDefinition(serviceName, "Hello World", "Example", "hello", None, None, Seq(apiVersion().asAlpha))
    val localDef2 = APIDefinition("api-person", "Hello Person", "Example", "hello-person", None, None, Seq(apiVersion()))
    val remoteDef = localDef1.copy(versions = Seq(APIVersion("2.0", None, APIStatus.BETA, Seq.empty)))
    val APIDefinitions = Seq(localDef1, localDef2)

    val local = mock[PrincipalApiDefinitionService]
    val remote = mock[SubordinateApiDefinitionService]

    val productionV1Availability = apiAvailability().asPrivate
    val sandboxV1Availability = apiAvailability().asPublic
    val sandboxV2Availability = apiAvailability().asPublic.endpointsDisabled

    val productionAPIDefinition = ExtendedAPIDefinition(serviceName, "http://localhost", "Hello World", "Example", "hello",
      requiresTrust = false, isTestSupport = false, Seq(
        ExtendedAPIVersion("1.0", APIStatus.STABLE, Seq.empty, Some(productionV1Availability), None)
      ))

    val sandboxAPIDefinition = ExtendedAPIDefinition(serviceName, "http://localhost", "Hello World", "Example", "hello",
      requiresTrust = false, isTestSupport = false, Seq(
        ExtendedAPIVersion("1.0", APIStatus.STABLE, Seq.empty, None, Some(sandboxV1Availability)),
        ExtendedAPIVersion("2.0", APIStatus.ALPHA, Seq.empty, None, Some(sandboxV2Availability))
      ))

    val combinedAPIDefinition = ExtendedAPIDefinition(serviceName, "http://localhost", "Hello World", "Example", "hello",
      requiresTrust = false, isTestSupport = false, Seq(
        ExtendedAPIVersion("1.0", APIStatus.STABLE, Seq.empty, Some(productionV1Availability), Some(sandboxV1Availability)),
        ExtendedAPIVersion("2.0", APIStatus.ALPHA, Seq.empty, None, Some(sandboxV2Availability))
      ))

    val underTest = new ProxyAwareApiDefinitionService(local, remote)

    whenNoApiDefinitions(remote)

    def theLocalSvcWillReturnTheAPIDefinition = {
      whenFetchExtendedDefinitionWithEmail(local)(serviceName, loggedInUserEmail)(productionAPIDefinition)
    }

    def theRemoteSvcWillReturnTheAPIDefinition = {
      whenFetchExtendedDefinitionWithEmail(remote)(serviceName, loggedInUserEmail)(sandboxAPIDefinition)
    }

    def theLocalSvcWillReturnNoAPIDefinition = {
      whenNoApiDefinitions(local)
    }

    def theRemoteSvcWillReturnNoAPIDefinition = {
      whenNoApiDefinitions(remote)
    }

    def theLocalSvcWillFailToReturnTheAPIDefinition = {
      whenApiDefinitionFails(local)(new RuntimeException)
    }

    def theRemoteSvcWillFailToReturnTheAPIDefinition = {
      whenApiDefinitionFails(remote)(new RuntimeException)
    }

    def theLocalSvcWillReturnTheAPIDefinitions = {
      whenFetchAllDefinitions(local)(APIDefinitions:_*)
    }

    def theLocalSvcWillFailToReturnTheAPIDefinitions = {
      whenApiDefinitionFails(local)(new RuntimeException)
    }
  }

  "fetchAPIDefinition" should {

    "call the local and remote services to fetch the API definition" in new Setup {
      theLocalSvcWillReturnTheAPIDefinition
      theRemoteSvcWillReturnNoAPIDefinition

      await(underTest.fetchExtendedDefinition(serviceName, Some(loggedInUserEmail)))

      verify(local).fetchExtendedDefinition(eqTo(serviceName), eqTo(Some(loggedInUserEmail)))(any[HeaderCarrier])
      verify(remote).fetchExtendedDefinition(eqTo(serviceName), eqTo(Some(loggedInUserEmail)))(any[HeaderCarrier])
    }

    "return the API definition when it exists in prod only" in new Setup {
      theLocalSvcWillReturnTheAPIDefinition
      theRemoteSvcWillReturnNoAPIDefinition

      val result = await(underTest.fetchExtendedDefinition(serviceName, Some(loggedInUserEmail)))

      result shouldBe Some(productionAPIDefinition)
    }

    "return the API definition when it exists in sandbox only" in new Setup {
      theLocalSvcWillReturnNoAPIDefinition
      theRemoteSvcWillReturnTheAPIDefinition

      val result = await(underTest.fetchExtendedDefinition(serviceName, Some(loggedInUserEmail)))

      result shouldBe Some(sandboxAPIDefinition)
    }

    "return the API combined definition when it exists in prod and sandbox" in new Setup {
      theLocalSvcWillReturnTheAPIDefinition
      theRemoteSvcWillReturnTheAPIDefinition

      val result = await(underTest.fetchExtendedDefinition(serviceName, Some(loggedInUserEmail)))

      result shouldBe Some(combinedAPIDefinition)
    }

    "return None when both connectors return no API definition" in new Setup {
      theLocalSvcWillReturnNoAPIDefinition
      theRemoteSvcWillReturnNoAPIDefinition

      val result = await(underTest.fetchExtendedDefinition(serviceName, Some(loggedInUserEmail)))

      result shouldBe None
    }

    "fail when the API definition connector fails to return the API definition" in new Setup {
      theLocalSvcWillFailToReturnTheAPIDefinition
      theRemoteSvcWillReturnTheAPIDefinition

      intercept[RuntimeException] {
        await(underTest.fetchExtendedDefinition(serviceName, Some(loggedInUserEmail)))
      }
    }
  }

  "fetchAPIDefinitions" should {

    "call the connector to fetch the API definitions" in new Setup {
      theLocalSvcWillReturnTheAPIDefinitions
      whenNoApiDefinitions(remote)

      await(underTest.fetchAllDefinitions(Some(loggedInUserEmail)))

      verify(local).fetchAllDefinitions(eqTo(Some(loggedInUserEmail)))(any[HeaderCarrier])
    }

    "return the API definitions" in new Setup {
      theLocalSvcWillReturnTheAPIDefinitions

      val result = await(underTest.fetchAllDefinitions(Some(loggedInUserEmail)))

      result shouldBe APIDefinitions.sortBy(_.name)
    }

    "return the API definitions keeping remote ones first" in new Setup {
      theLocalSvcWillReturnTheAPIDefinitions
      whenFetchAllDefinitions(remote)(remoteDef)

      val result = await(underTest.fetchAllDefinitions(Some(loggedInUserEmail)))

      result shouldBe Seq(localDef2, remoteDef)
    }

    "fail when the connector fails to return the API definitions" in new Setup {
      theLocalSvcWillFailToReturnTheAPIDefinitions
      whenNoApiDefinitions(remote)

      intercept[RuntimeException] {
        await(underTest.fetchAllDefinitions(Some(loggedInUserEmail)))
      }
    }
  }
}
