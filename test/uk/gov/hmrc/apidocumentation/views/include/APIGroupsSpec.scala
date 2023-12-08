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

import scala.jdk.CollectionConverters._

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import play.twirl.api.HtmlFormat.Appendable
import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models._

import uk.gov.hmrc.apidocumentation.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.apidocumentation.utils.ApiDefinitionTestDataHelper
import uk.gov.hmrc.apidocumentation.views

class APIGroupsSpec extends AsyncHmrcSpec {

  case class Page(doc: Appendable) {
    lazy val dom: Document = Jsoup.parse(doc.body)
    lazy val tableBodies   = dom.getElementsByTag("tbody")
    lazy val tableHeadings = dom.getElementsByClass("api-group")
    lazy val serviceTags   = dom.getElementsByClass("govuk-tag")
  }

  def anXmlApiDefinition(name: String) =
    XmlApiDocumentation(name, "description", "context")

  def aServiceGuide(name: String) =
    ServiceGuide(name, "context")

  trait Setup extends ApiDefinitionTestDataHelper {

    val customsApis = Seq(
      WrappedApiDefinition(apiDefinition("customsTestSupport1").copy(isTestSupport = true)),
      anXmlApiDefinition("customsXmlApi2"),
      WrappedApiDefinition(apiDefinition("customsRestApi2")),
      WrappedApiDefinition(apiDefinition("customsRestApi1")),
      anXmlApiDefinition("customsXmlApi1"),
      WrappedApiDefinition(apiDefinition("customsTestSupport2").copy(isTestSupport = true))
    )

    val vatApis = Seq(
      WrappedApiDefinition(apiDefinition("vatTestSupport1").copy(isTestSupport = true)),
      anXmlApiDefinition("vatXmlApi1"),
      WrappedApiDefinition(apiDefinition("vatRestApi2")),
      WrappedApiDefinition(apiDefinition("vatRestApi1")),
      WrappedApiDefinition(apiDefinition("vatTestSupport2").copy(isTestSupport = true))
    )

    val apisByCategory: Map[ApiCategory, Seq[Documentation]] = Map(ApiCategory.CUSTOMS -> customsApis, ApiCategory.VAT -> vatApis)
    val page                                                 = Page(views.html.include.documentGroups(apisByCategory))
  }

  "API Groups view" should {
    "display each category as a group" in new Setup {
      page.tableBodies.size shouldBe 2
      page.tableHeadings.first.text shouldBe ApiCategory.CUSTOMS.displayText
      page.tableHeadings.last.text shouldBe ApiCategory.VAT.displayText
    }

    "sort the definitions by their label" in new Setup {
      val classList = page.serviceTags.eachAttr("class").asScala.map(_.stripPrefix("govuk-tag govuk-tag--"))
      classList shouldBe Seq("rest", "rest", "test", "test", "xml", "xml", "rest", "rest", "test", "test", "xml")
    }
  }
}
