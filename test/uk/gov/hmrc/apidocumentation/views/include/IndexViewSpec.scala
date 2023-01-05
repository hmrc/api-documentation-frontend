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

package uk.gov.hmrc.apidocumentation.views.include

import java.util.Locale

import junit.framework.TestCase
import play.api.i18n.{DefaultMessagesApi, Lang}
import play.api.mvc.Request
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.models.NavLink
import uk.gov.hmrc.apidocumentation.views.CommonViewSpec
import uk.gov.hmrc.apidocumentation.views.html.IndexView
import uk.gov.hmrc.apidocumentation.views.html.templates.LayoutHomePage

class IndexViewSpec extends CommonViewSpec {

  val pageTitle = "pageTitle"
  val navLinks = Seq[NavLink]()
  val mockRequest = mock[Request[Any]]
  val mockMessages = (new DefaultMessagesApi()).preferred(Seq(Lang(Locale.ENGLISH)))
  implicit val mockApplicationConfig = mock[ApplicationConfig]

  val main = app.injector.instanceOf[LayoutHomePage]

  "htmlView" must {
    "render with no indexing meta tags" in new TestCase {
      val renderedHtml = new IndexView(main)(pageTitle, navLinks)
      renderedHtml.body shouldNot include("<meta name=\"robots\" content=\"noindex\">")
      renderedHtml.body shouldNot include("<meta name=\"googlebot\" content=\"noindex\">")
    }
  }
}

