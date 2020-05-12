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

package unit.uk.gov.hmrc.apidocumentation.views.include

import junit.framework.TestCase
import org.mockito.BDDMockito.given
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.{Messages, DefaultMessagesApi, Lang}
import play.api.mvc.Request
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.models.NavLink
import uk.gov.hmrc.apidocumentation.views
import uk.gov.hmrc.play.test.{UnitSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import java.util.Locale

class MainTemplateSpec extends UnitSpec with MockitoSugar with GuiceOneAppPerSuite {

  val pageTitle = "pageTitle"
  val navLinks = Seq[NavLink]()
  val mockRequest = mock[Request[Any]]
  val mockApplicationConfig = mock[ApplicationConfig]
  val mockMessages = (new DefaultMessagesApi()).preferred(Seq(Lang(Locale.ENGLISH)))

  "htmlView" must {
    "render with no indexing meta tags" in new TestCase {
      val renderedHtml = views.html.index.render(pageTitle, navLinks, mockRequest, mockApplicationConfig, mockMessages)
      renderedHtml.body shouldNot include("<meta name=\"robots\" content=\"noindex\">")
      renderedHtml.body shouldNot include("<meta name=\"googlebot\" content=\"noindex\">")
    }
  }
}

