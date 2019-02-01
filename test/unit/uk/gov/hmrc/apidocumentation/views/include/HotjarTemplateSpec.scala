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

package unit.uk.gov.hmrc.apidocumentation.views.include

import junit.framework.TestCase
import org.mockito.BDDMockito.given
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.mvc.Request
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.models.NavLink
import uk.gov.hmrc.apidocumentation.views
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class HotjarTemplateSpec extends UnitSpec with WithFakeApplication with MockitoSugar {

  val pageTitle = "pageTitle"
  val navLinks = Seq[NavLink]()
  val mockRequest = mock[Request[Any]]
  val mockApplicationConfig = mock[ApplicationConfig]

  val mockMessages = mock[Messages]

  "htmlView" must {
    "render hotjar script when hotjar id is defined and hotjar feature enabled" in new TestCase {
      given(mockApplicationConfig.hotjarEnabled) willReturn Some(true)
      given(mockApplicationConfig.hotjarId) willReturn Some(123)

      val renderedHtml = views.html.index.render(pageTitle, navLinks, mockRequest, mockApplicationConfig, mockMessages)
      renderedHtml.body should include("hotjar")
      renderedHtml.body should include("hjid:123")
    }

    "render without hotjar script when hotjar id is not defined and hotjar feature is disabled" in new TestCase {
      given(mockApplicationConfig.hotjarEnabled) willReturn Some(false)
      given(mockApplicationConfig.hotjarId) willReturn None

      val renderedHtml = views.html.index.render(pageTitle, navLinks, mockRequest, mockApplicationConfig, mockMessages)
      renderedHtml.body should not include "hotjar"
      renderedHtml.body should not include "hjid:"
    }

    "render without hotjar script when hotjar id is not defined and hotjar feature is enabled" in new TestCase {
      given(mockApplicationConfig.hotjarEnabled) willReturn Some(true)
      given(mockApplicationConfig.hotjarId) willReturn None

      val renderedHtml = views.html.index.render(pageTitle, navLinks, mockRequest, mockApplicationConfig, mockMessages)
      renderedHtml.body should not include "hotjar"
      renderedHtml.body should not include "hjid:"
    }

    "render without hotjar script when hotjar id is defined and hotjar feature is disabled" in new TestCase {
      given(mockApplicationConfig.hotjarEnabled) willReturn Some(false)
      given(mockApplicationConfig.hotjarId) willReturn Some(123)

      val renderedHtml = views.html.index.render(pageTitle, navLinks, mockRequest, mockApplicationConfig, mockMessages)
      renderedHtml.body should not include "hotjar"
      renderedHtml.body should not include "hjid:"
    }
  }
}

