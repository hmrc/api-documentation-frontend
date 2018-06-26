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

package unit.uk.gov.hmrc.apidocumentation.views.include

import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import junit.framework.TestCase
import uk.gov.hmrc.apidocumentation.models.NavLink
import org.mockito.BDDMockito.given
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import play.api.mvc.Request
import uk.gov.hmrc.apidocumentation.views

class HotjarTemplateSpec extends PlaySpec with MockitoSugar {

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
      renderedHtml.body must include("hotjar")
      renderedHtml.body must include("hjid:123")
    }

    "render without hotjar script when hotjar id is not defined and hotjar feature is disabled" in new TestCase {
      given(mockApplicationConfig.hotjarEnabled) willReturn Some(false)
      given(mockApplicationConfig.hotjarId) willReturn None

      val renderedHtml = views.html.index.render(pageTitle, navLinks, mockRequest, mockApplicationConfig, mockMessages)
      renderedHtml.body must not include "hotjar"
      renderedHtml.body must not include "hjid:"
    }

    "render without hotjar script when hotjar id is not defined and hotjar feature is enabled" in new TestCase {
      given(mockApplicationConfig.hotjarEnabled) willReturn Some(true)
      given(mockApplicationConfig.hotjarId) willReturn None

      val renderedHtml = views.html.index.render(pageTitle, navLinks, mockRequest, mockApplicationConfig, mockMessages)
      renderedHtml.body must not include "hotjar"
      renderedHtml.body must not include "hjid:"
    }

    "render without hotjar script when hotjar id is defined and hotjar feature is disabled" in new TestCase {
      given(mockApplicationConfig.hotjarEnabled) willReturn Some(false)
      given(mockApplicationConfig.hotjarId) willReturn Some(123)

      val renderedHtml = views.html.index.render(pageTitle, navLinks, mockRequest, mockApplicationConfig, mockMessages)
      renderedHtml.body must not include "hotjar"
      renderedHtml.body must not include "hjid:"
    }
  }
}

