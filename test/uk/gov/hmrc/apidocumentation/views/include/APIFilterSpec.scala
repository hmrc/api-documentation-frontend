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

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import play.twirl.api.HtmlFormat.Appendable

import uk.gov.hmrc.apidocumentation.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apidocumentation.models.APICategory._
import uk.gov.hmrc.apidocumentation.models.APIDefinition
import uk.gov.hmrc.apidocumentation.views

class APIFilterSpec extends AsyncHmrcSpec {

  case class Page(doc: Appendable) {
    lazy val dom: Document   = Jsoup.parse(doc.body)
    lazy val dropdown        = dom.getElementById("service-filter")
    lazy val options         = dropdown.getElementsByTag("option")
    lazy val selectedVersion = dropdown.getElementsByAttribute("selected").last
  }

  class Setup(filter: Option[APICategory] = None) {

    val apisByCategory: Map[APICategory, Seq[APIDefinition]] = Map(
      CUSTOMS -> Seq.empty,
      VAT     -> Seq.empty
    )
    val page                                                 = Page(views.html.include.documentFilter(apisByCategory, filter))
  }

  "api filter" when {
    "no filter provided" should {
      "render the dropdown based on the provided categories" in new Setup {
        page.options.size shouldBe apisByCategory.keys.size + 1
      }

      "select the default disabled option" in new Setup {
        page.selectedVersion.hasAttr("disabled") shouldBe true
      }
    }

    "filter is provided" should {
      "render the dropdown based on the provided categories" in new Setup(filter = Some(CUSTOMS)) {
        page.options.size shouldBe apisByCategory.keys.size + 1
      }

      "select the provided category" in new Setup(filter = Some(CUSTOMS)) {
        page.selectedVersion.attr("value") shouldBe "customs"
      }
    }
  }
}
