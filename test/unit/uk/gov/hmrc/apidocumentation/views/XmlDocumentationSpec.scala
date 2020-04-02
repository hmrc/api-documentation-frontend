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

package unit.uk.gov.hmrc.apidocumentation.views

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Mockito.{when => When}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.mvc.Request
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat.Appendable
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.apidocumentation.views
import uk.gov.hmrc.play.test.UnitSpec

class XmlDocumentationSpec extends UnitSpec with MockitoSugar with OneAppPerSuite {
  case class Page(doc: Appendable) {
    lazy val dom: Document = Jsoup.parse(doc.body)
    lazy val heading = dom.getElementsByTag("h1").first
    lazy val description = dom.getElementById("xml-api-description")
    lazy val link = dom.getElementById("xml-api-link")
  }

  trait Setup {
    val baseUrl = "http://example.com"
    val context = "/example/path"
    val description = "An XML API for testing"
    val name = "Test Online Service"

    val appConfig = mock[ApplicationConfig]
    val messages = mock[Messages]
    val pageAttributes: PageAttributes = mock[PageAttributes]
    val request = mock[Request[Any]]

    When(appConfig.xmlApiBaseUrl).thenReturn(baseUrl)
    When(pageAttributes.headerLinks).thenReturn(Seq())
    When(pageAttributes.sidebarLinks).thenReturn(Seq())
    When(pageAttributes.breadcrumbs).thenReturn(Breadcrumbs())
    When(pageAttributes.contentHeader).thenReturn(None)

    val apiDefinition = XmlApiDocumentation(name, context, description)
    val page = Page(views.html.xmlDocumentation(pageAttributes, apiDefinition)(request, appConfig, messages))
  }

  "XmlDocumentation view" should {
    "render the api definition's name" in new Setup {
      page.heading.text should include(name)
    }

    "render the api definition's description" in new Setup {
      page.description.text should include(description)
    }

    "render a link to view the full documentation" in new Setup {
      page.link.attr("href") shouldBe s"$baseUrl$context"
    }
  }
}
