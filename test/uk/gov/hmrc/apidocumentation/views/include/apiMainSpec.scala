/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.models.{Breadcrumbs, Crumb, PageAttributes}
import uk.gov.hmrc.apidocumentation.views.CommonViewSpec
import uk.gov.hmrc.apidocumentation.views.html.UsingTheHubView

import scala.jdk.CollectionConverters.asScalaBufferConverter

class apiMainSpec extends CommonViewSpec {

  trait Setup {
    val usingTheHubView = app.injector.instanceOf[UsingTheHubView]
    implicit val mockApplicationConfig = mock[ApplicationConfig]

    def elementExistsById(doc: Document, id: String): Boolean = doc.select(s"#$id").asScala.nonEmpty
  }

  "htmlView" must {

    "render the page with feedback banner" in new Setup {
      val document = Jsoup.parse(
        usingTheHubView.render(
          PageAttributes("testTile", Breadcrumbs(Crumb("testCrumb", "testCrumbUrl"))),
          mockApplicationConfig,
          request,
          messagesProvider
        ).body
      )

      elementExistsById(document, "feedback") shouldBe true
      elementExistsById(document, "show-survey") shouldBe true
      document.getElementById("feedback-title").text() shouldBe "Your feedback helps us improve our service"
    }
  }
}

