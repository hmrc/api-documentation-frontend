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

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import org.scalatestplus.play.guice.GuiceOneAppPerTest

import play.api.cache.AsyncCacheApi
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apidocumentation.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.connectors.DeveloperFrontendConnector
import uk.gov.hmrc.apidocumentation.models._

class NavigationServiceSpec extends AsyncHmrcSpec with GuiceOneAppPerTest {

  class Setup {
    implicit val hc                           = HeaderCarrier()
    val connector: DeveloperFrontendConnector = mock[DeveloperFrontendConnector]
    val config                                = mock[ApplicationConfig]
    when(config.title).thenReturn("Unit Test Title")
    val underTest                             = new NavigationService(connector, config)

    val cache  = app.injector.instanceOf[AsyncCacheApi]
    val docSvc = app.injector.instanceOf[DocumentationService]
  }

  "sidebarNavigation" should {
    "return sidebar navigation links" in new Setup {
      val sidebarNavLinks = underTest.sidebarNavigation()
      sidebarNavLinks.size shouldBe 10
      sidebarNavLinks.head.href shouldBe "/api-documentation/docs/using-the-hub"
      sidebarNavLinks.head.label shouldBe "Using the Developer Hub"
      sidebarNavLinks(9).href shouldBe "/api-documentation/docs/terms-of-use"
      sidebarNavLinks(9).label shouldBe "Terms of use"
    }

  }

  "headerNavigation" should {
    "fetch and return header navigation links" in new Setup {
      when(config.developerFrontendUrl).thenReturn("http://localhost:9865")
      when(connector.fetchNavLinks()(*)).thenReturn(Future.successful(Seq(
        NavLink("Register", "/developer/registration"),
        NavLink("Sign in", "/developer/login")
      )))

      val headerNavLinks = await(underTest.headerNavigation())
      verify(connector, times(1)).fetchNavLinks()(*)
      headerNavLinks.size shouldBe 2
      headerNavLinks.head.href shouldBe "http://localhost:9865/developer/registration"
      headerNavLinks.head.label shouldBe "Register"
      headerNavLinks.last.href shouldBe "http://localhost:9865/developer/login"
      headerNavLinks.last.label shouldBe "Sign in"
    }

    "return empty header navigation links" in new Setup {
      when(connector.fetchNavLinks()(*)).thenReturn(Future.successful(Seq.empty[NavLink]))
      val headerNavLinks = await(underTest.headerNavigation())
      verify(connector, times(1)).fetchNavLinks()(*)
      headerNavLinks.size shouldBe 0
    }
  }
}
