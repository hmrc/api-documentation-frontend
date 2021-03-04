/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.apidocumentation.models.{APICategory => _, _}
import uk.gov.hmrc.apidocumentation.models.APICategory._
import uk.gov.hmrc.apidocumentation.models.APIStatus._
import uk.gov.hmrc.apidocumentation.views
import uk.gov.hmrc.play.test.UnitSpec

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

class APIGroupsSpec extends UnitSpec {
  case class Page(doc: Appendable) {
    lazy val dom: Document = Jsoup.parse(doc.body)
    lazy val tableBodies = dom.getElementsByTag("tbody")
    lazy val tableHeadings = dom.getElementsByClass("api-group")
    lazy val serviceTags = dom.getElementsByClass("govuk-tag")
  }

  private def anApiDefinition(name: String, isTestSupport: Option[Boolean] = None) =
    APIDefinition("serviceName", name, "description", "context", None, isTestSupport, Seq(APIVersion("1.0", None, STABLE, Seq.empty)), None)

  def anXmlApiDefinition(name: String) =
    XmlApiDocumentation(name, "description", "context")

  def aServiceGuide(name: String) =
    ServiceGuide(name, "context")

  trait Setup {
    val customsApis = Seq(
      anApiDefinition("customsTestSupport1", isTestSupport = Some(true)),
      anXmlApiDefinition("customsXmlApi2"),
      anApiDefinition("customsRestApi2"),
      anApiDefinition("customsRestApi1"),
      anXmlApiDefinition("customsXmlApi1"),
      anApiDefinition("customsTestSupport2", isTestSupport = Some(true)))
    val vatApis = Seq(
      anApiDefinition("vatTestSupport1", isTestSupport = Some(true)),
      anXmlApiDefinition("vatXmlApi1"),
      anApiDefinition("vatRestApi2"),
      anApiDefinition("vatRestApi1"),
      anApiDefinition("vatTestSupport2", isTestSupport = Some(true)))

    val apisByCategory: Map[APICategory, Seq[Documentation]] = Map(CUSTOMS -> customsApis, VAT -> vatApis)
    val page = Page(views.html.include.documentGroups(apisByCategory))
  }

  "API Groups view" should {
    "display each category as a group" in new Setup {
      page.tableBodies.size shouldBe 2
      page.tableHeadings.first.text shouldBe CUSTOMS.displayName
      page.tableHeadings.last.text shouldBe VAT.displayName
    }

    "sort the definitions by their label" in new Setup {
      val classList = page.serviceTags.eachAttr("class").asScala.map(_.stripPrefix("govuk-tag govuk-tag--"))
      classList shouldBe Seq("rest", "rest", "test", "test", "xml", "xml", "rest", "rest", "test", "test", "xml")
    }
  }
}
