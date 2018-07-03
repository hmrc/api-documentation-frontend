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
import uk.gov.hmrc.apidocumentation.views
import junit.framework.TestCase
import uk.gov.hmrc.apidocumentation.models.NavLink
import org.mockito.BDDMockito.given
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import play.api.mvc.Request

class MainTemplateSpec extends PlaySpec with MockitoSugar {

  val pageTitle = "pageTitle"
  val navLinks = Seq[NavLink]()
  val mockRequest = mock[Request[Any]]
  val mockApplicationConfig = mock[ApplicationConfig]
  val mockMessages = mock[Messages]

  "htmlView" must {
    "render with no indexing meta tags" in new TestCase {
      given(mockApplicationConfig.hotjarEnabled) willReturn Some(false)
      given(mockApplicationConfig.hotjarId) willReturn None
      val renderedHtml = views.html.index.render(pageTitle, navLinks, mockRequest, mockApplicationConfig, mockMessages)
      renderedHtml.body must not include "<meta name=\"robots\" content=\"noindex\">"
      renderedHtml.body must not include "<meta name=\"googlebot\" content=\"noindex\">"
    }
  }
}

