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

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.raml.v2.api.model.v10.api.DocumentationItem
import org.raml.v2.api.model.v10.system.types.AnnotableStringType
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.connectors.DeveloperFrontendConnector
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.apidocumentation.services._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerTest

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import play.api.cache.CacheApi

class NavigationServiceSpec extends UnitSpec with GuiceOneAppPerTest with MockitoSugar with ScalaFutures {

  class Setup {
    implicit val hc = HeaderCarrier()
    val connector: DeveloperFrontendConnector = mock[DeveloperFrontendConnector]
    val config = mock[ApplicationConfig]
    when(config.title).thenReturn("Unit Test Title")
    val underTest = new NavigationService(connector, config)

    val cache = app.injector.instanceOf[CacheApi]
    val docSvc = app.injector.instanceOf[DocumentationService]
  }

  "sidebarNavigation" should {
    "return sidebar navigation links" in new Setup {
      val sidebarNavLinks = underTest.sidebarNavigation()
      sidebarNavLinks.size shouldBe 10
      sidebarNavLinks.head.href shouldBe "/api-documentation/docs/using-the-hub"
      sidebarNavLinks.head.label shouldBe "Using the Developer Hub"
      sidebarNavLinks(7).href shouldBe "/api-documentation/docs/terms-of-use"
      sidebarNavLinks(7).label shouldBe "Terms of use"
    }

  }

  "headerNavigation" should {
    "fetch and return header navigation links" in new Setup {
      when(config.developerFrontendUrl).thenReturn("http://localhost:9865")
      when(connector.fetchNavLinks()(any())).thenReturn(Future.successful(Seq(
        NavLink("Register", "/developer/registration"),
        NavLink("Sign in", "/developer/login"))))

      val headerNavLinks = await(underTest.headerNavigation())
      verify(connector, times(1)).fetchNavLinks()(any())
      headerNavLinks.size shouldBe 2
      headerNavLinks.head.href shouldBe "http://localhost:9865/developer/registration"
      headerNavLinks.head.label shouldBe "Register"
      headerNavLinks.last.href shouldBe "http://localhost:9865/developer/login"
      headerNavLinks.last.label shouldBe "Sign in"
    }

    "return empty header navigation links" in new Setup {
      when(connector.fetchNavLinks()(any())).thenReturn(Future.successful(Seq.empty[NavLink]))
      val headerNavLinks = await(underTest.headerNavigation())
      verify(connector, times(1)).fetchNavLinks()(any())
      headerNavLinks.size shouldBe 0
    }
  }

  "apiSidebarNavigation" should {
    val raml = mock[RAML]
    val version = mock[ExtendedAPIVersion]
    val service = "a test service"
    val overviewTitle = mock[AnnotableStringType]
    val overview = mock[DocumentationItem]

    when(overviewTitle.value).thenReturn("Overview")
    when(overview.title).thenReturn(overviewTitle)

    "render documentation and resources links when the api is visible" in new Setup {
      val documentation: Seq[DocumentationItem] = List(overview)
      val visible = Some(VersionVisibility(APIAccessType.PUBLIC, loggedIn = true, authorised = true, isTrial = None))

      when(version.visibility).thenReturn(visible)
      when(raml.documentation).thenReturn(documentation)

      val apiSidebarNavLinks = await(underTest.apiSidebarNavigation(service, version, raml))

      apiSidebarNavLinks.size shouldBe 2
      apiSidebarNavLinks.head.href shouldBe "#overview"
      apiSidebarNavLinks.head.label shouldBe "Overview"
      apiSidebarNavLinks.last.href shouldBe "#endpoints"
      apiSidebarNavLinks.last.label shouldBe "Endpoints"
    }

    "render overview documentation and 'read more' links when the api is not visible" in new Setup {
      val documentation: Seq[DocumentationItem] = List(overview)
      val overviewOnly = Some(VersionVisibility(APIAccessType.PRIVATE, loggedIn = true, authorised = false, isTrial = Some(true)))

      when(version.visibility).thenReturn(overviewOnly)
      when(raml.documentation).thenReturn(documentation)

      val apiSidebarNavLinks = await(underTest.apiSidebarNavigation(service, version, raml))

      apiSidebarNavLinks.size shouldBe 2
      apiSidebarNavLinks.head.href shouldBe "#overview"
      apiSidebarNavLinks.head.label shouldBe "Overview"
      apiSidebarNavLinks.last.href shouldBe "#read-more"
      apiSidebarNavLinks.last.label shouldBe "Read more"
    }
  }
}
