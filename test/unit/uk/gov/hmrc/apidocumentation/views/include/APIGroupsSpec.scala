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

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.HtmlFormat.Appendable
import uk.gov.hmrc.apidocumentation.models.{APIDefinition, APIVersion}
import uk.gov.hmrc.apidocumentation.models.APICategory._
import uk.gov.hmrc.apidocumentation.models.APIStatus._
import uk.gov.hmrc.apidocumentation.views
import uk.gov.hmrc.play.test.UnitSpec

import scala.collection.JavaConverters._

class APIGroupsSpec extends UnitSpec {
  case class Page(doc: Appendable) {
    lazy val dom: Document = Jsoup.parse(doc.body)
    lazy val tableBodies = dom.getElementsByTag("tbody")
    lazy val tableHeadings = dom.getElementsByClass("api-group")
    lazy val serviceTags = dom.getElementsByClass("service-tag")
  }

  private def anApiDefinition(name: String, isTestSupport: Option[Boolean] = None, isXmlApi: Option[Boolean] = None) =
    APIDefinition("serviceName", name, "description", "context", None, isTestSupport, Seq(APIVersion("1.0", None, STABLE, Seq.empty)), None, isXmlApi)

  trait Setup {
    val customsApis = Seq(
      anApiDefinition("customsTestSupport1", isTestSupport = Some(true)),
      anApiDefinition("customsXmlApi2", isXmlApi = Some(true)),
      anApiDefinition("customsRestApi2"),
      anApiDefinition("customsRestApi1"),
      anApiDefinition("customsXmlApi1", isXmlApi = Some(true)),
      anApiDefinition("customsTestSupport2", isTestSupport = Some(true)))
    val vatApis = Seq(
      anApiDefinition("vatTestSupport1", isTestSupport = Some(true)),
      anApiDefinition("vatXmlApi1", isXmlApi = Some(true)),
      anApiDefinition("vatRestApi2"),
      anApiDefinition("vatRestApi1"),
      anApiDefinition("vatTestSupport2", isTestSupport = Some(true)))

    val apisByCategory: Map[APICategory, Seq[APIDefinition]] = Map(CUSTOMS -> customsApis, VAT -> vatApis)
    val page = Page(views.html.include.apiGroups(apisByCategory))
  }

  "API Groups view" should {
    "display each category as a group" in new Setup {
      page.tableBodies.size shouldBe 2
      page.tableHeadings.first.text shouldBe CUSTOMS.displayName
      page.tableHeadings.last.text shouldBe VAT.displayName
    }

    "sort the definitions by their label" in new Setup {
      val classList = page.serviceTags.eachAttr("class").asScala.map(_.stripPrefix("service-tag service-tag--")).toSeq
      classList shouldBe Seq("rest", "rest", "test", "test", "xml", "xml", "rest", "rest", "test", "test", "xml")
    }
  }
}
