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
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.apidocumentation.models.APIStatus._
import uk.gov.hmrc.apidocumentation.services.{BaseApiDefinitionService, LocalApiDefinitionService, ProxyAwareApiDefinitionService, RemoteApiDefinitionService}
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future

class ApiDefinitionServiceSpec extends UnitSpec with WithFakeApplication with MockitoSugar with ScalaFutures {

  val html = "<b>Today is 01 January 2001</b>"
  val serviceName = "calendar"
  val serviceUrl = "http://localhost:1234"
  val api: APIDefinition = apiDefinition("gregorian-calendar")

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val appConfig = mock[ApplicationConfig]
    when(appConfig.localApiDocumentationUrl).thenReturn(serviceUrl)

    val local = mock[LocalApiDefinitionService]
    val remote = mock[RemoteApiDefinitionService]
    import scala.concurrent.ExecutionContext.Implicits.global
    val loggedInUserEmail = "3rdparty@example.com"

    val underTest = new ProxyAwareApiDefinitionService(local, remote)

    def whenFetchAllDefinitions(base: BaseApiDefinitionService)
                               (apis: APIDefinition*)
                               (implicit hc: HeaderCarrier) = {
      when(base.fetchAllDefinitions(any[None.type]())(any[HeaderCarrier]))
        .thenReturn(Future.successful(apis.toSeq))
    }
    def whenFetchAllDefinitionsWithEmail(base: BaseApiDefinitionService)
                                        (email: String)
                                        (apis: APIDefinition*)
                                        (implicit hc: HeaderCarrier) = {
      when(base.fetchAllDefinitions(any[Some[String]]())(any[HeaderCarrier]))
        .thenReturn(Future.successful(apis.toSeq))
    }
    def whenFetchExtendedDefinition(base: BaseApiDefinitionService)
                                   (serviceName: String)
                                   (api: ExtendedAPIDefinition)
                                   (implicit hc: HeaderCarrier) = {
      when(base.fetchExtendedDefinition(eqTo(serviceName), eqTo(None))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(api)))
    }
    def whenFetchExtendedDefinitionWithEmail(base: BaseApiDefinitionService)
                                            (serviceName: String, email: String)
                                            (api: ExtendedAPIDefinition)
                                            (implicit hc: HeaderCarrier) = {
      when(base.fetchExtendedDefinition(eqTo(serviceName), any[Some[String]]())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(api)))
    }

    def whenApiDefinitionFails(base: BaseApiDefinitionService = local)
                              (exception: Throwable)
                              (implicit hc: HeaderCarrier) = {
      when(base.fetchExtendedDefinition(any[String],any[None.type]())(any[HeaderCarrier]))
        .thenReturn(Future.failed(exception))
      when(base.fetchAllDefinitions(any())(any[HeaderCarrier]))
        .thenReturn(Future.failed(exception))
    }

    def whenNoApiDefinitions(base: BaseApiDefinitionService) = {
      when(base.fetchExtendedDefinition(any[String],any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(None))
      when(base.fetchAllDefinitions(any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Seq.empty))
    }
  }

  "fetchAPIs with user session handling" should {
    "fetch all APIs if there is no user logged in" in new Setup {
      whenFetchAllDefinitions(local)(apiDefinition("gregorian-calendar"), apiDefinition("roman-calendar"))
      whenNoApiDefinitions(remote)

      val result = await(underTest.fetchAllDefinitions(None))

      result.size shouldBe 2
      result(0).name shouldBe "gregorian-calendar"
      result(1).name shouldBe "roman-calendar"
    }

    "fetch APIs for user email if a user is logged in" in new Setup {
      whenFetchAllDefinitionsWithEmail(local)(loggedInUserEmail)(apiDefinition("gregorian-calendar"), apiDefinition("roman-calendar"))
      whenNoApiDefinitions(remote)

      val result = await(underTest.fetchAllDefinitions(Some(loggedInUserEmail)))

      result.size shouldBe 2
      result(0).name shouldBe "gregorian-calendar"
      result(1).name shouldBe "roman-calendar"
    }
  }

  "fetchAPI with user session handling" should {

    "fetch a single API if there is no user logged in" in new Setup {
      whenFetchExtendedDefinition(local)("buddist-calendar")(extendedApiDefinition("buddist-calendar"))
      whenNoApiDefinitions(remote)

      val result = await(underTest.fetchExtendedDefinition("buddist-calendar", None))

      result shouldBe defined
      result.get.name shouldBe "buddist-calendar"
    }

    "fetch a single API for user email if a user is logged in" in new Setup {
      whenFetchExtendedDefinitionWithEmail(local)("buddist-calendar",loggedInUserEmail)(extendedApiDefinition("buddist-calendar"))
      whenNoApiDefinitions(remote)

      val result = await(underTest.fetchExtendedDefinition("buddist-calendar", Some(loggedInUserEmail)))

      result shouldBe defined
      result.get.name shouldBe "buddist-calendar"
    }

    "reject for an unsubscribed API for user email if a user is logged in" in new Setup {
      whenApiDefinitionFails(local)(new NotFoundException("Expected unit test exception"))
      whenNoApiDefinitions(remote)

      intercept[NotFoundException] {
        await(underTest.fetchExtendedDefinition("buddist-calendar", Some(loggedInUserEmail)))
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
        apiVersion("3.0", BETA),
        apiVersion("2.0", STABLE),
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

  private def apiVersion(version: String, status: APIStatus = STABLE, access: Option[APIAccess] = None): APIVersion = {
    APIVersion(version, access, status, Seq(
      Endpoint("Today's Date", "/today", HttpMethod.GET, None),
      Endpoint("Yesterday's Date", "/yesterday", HttpMethod.GET, None)))
  }
}
