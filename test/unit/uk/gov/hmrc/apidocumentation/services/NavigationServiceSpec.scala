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

import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.models.NavLink
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.apidocumentation.connectors.DeveloperFrontendConnector
import uk.gov.hmrc.apidocumentation.services.NavigationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future

class NavigationServiceSpec extends UnitSpec with WithFakeApplication with MockitoSugar with ScalaFutures {

  class Setup {
    implicit val hc = HeaderCarrier()
    val connector: DeveloperFrontendConnector = mock[DeveloperFrontendConnector]
    val config = mock[ApplicationConfig]
    when(config.title).thenReturn("Unit Test Title")
    val underTest = new NavigationService(connector, config)
  }

  "navigationService" should {
    "return sidebar navigation links" in new Setup {
      val sidebarNavLinks = underTest.sidebarNavigation()
      sidebarNavLinks.size shouldBe 8
      sidebarNavLinks.head.href shouldBe "/api-documentation/docs/using-the-hub"
      sidebarNavLinks.head.label shouldBe "Using the Unit Test Title"
      sidebarNavLinks(6).href shouldBe "/api-documentation/docs/terms-of-use"
      sidebarNavLinks(6).label shouldBe "Terms of use"
    }
    "should produce a reduced set of options in sandbox mode" in new Setup {
      when(config.isExternalTestEnvironment).thenReturn(true)
      val sidebarNavLinks = underTest.sidebarNavigation()
      sidebarNavLinks.size shouldBe 4
      sidebarNavLinks.head.label should be("Using the Sandbox")
      sidebarNavLinks.head.href should be("/api-documentation/docs/sandbox/introduction")
      sidebarNavLinks(1).label should be("API documentation")
      sidebarNavLinks(1).href should be("/api-documentation/docs/api")
      sidebarNavLinks(2).label should be("Reference guide")
      sidebarNavLinks(2).href should be("/api-documentation/docs/reference-guide")
      sidebarNavLinks(3).label should be("Making Tax Digital guide")
      sidebarNavLinks(3).href should be("/api-documentation/docs/mtd")
    }
  }

  "navigationService" should {
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
  }

  "navigationService" should {
    "return empty header navigation links" in new Setup {
      when(connector.fetchNavLinks()(any())).thenReturn(Future.successful(Seq.empty[NavLink]))
      val headerNavLinks = await(underTest.headerNavigation())
      verify(connector, times(1)).fetchNavLinks()(any())
      headerNavLinks.size shouldBe 0
    }
  }
}
