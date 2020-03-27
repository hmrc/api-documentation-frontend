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

import javax.inject.Inject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.Messages
import play.api.mvc.Request
import play.twirl.api.HtmlFormat.Appendable
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.apidocumentation.services.RAML
import uk.gov.hmrc.apidocumentation.views.html.include.apiMain
import uk.gov.hmrc.apidocumentation.views.html.serviceDocumentation
import uk.gov.hmrc.play.test.UnitSpec
import unit.uk.gov.hmrc.apidocumentation.utils.ApiDefinitionTestDataHelper

class ServiceDocumentationViewSpec @Inject()(apiMain: apiMain) extends UnitSpec with MockitoSugar with GuiceOneAppPerSuite with ApiDefinitionTestDataHelper {
  case class Page(doc: Appendable) {
    lazy val dom: Document = Jsoup.parse(doc.body)
    lazy val versionsDropdown = dom.getElementById("version")
    lazy val selectedVersion = versionsDropdown.getElementsByAttribute("selected")
  }

  val mockAppConfig = mock[ApplicationConfig]

  val pageAttributes: PageAttributes = mock[PageAttributes]
  when(pageAttributes.headerLinks).thenReturn(Seq())
  when(pageAttributes.sidebarLinks).thenReturn(Seq())
  when(pageAttributes.breadcrumbs).thenReturn(Breadcrumbs())
  when(pageAttributes.contentHeader).thenReturn(None)

  val ramlAndSchemas: RamlAndSchemas = mock[RamlAndSchemas]
  when(ramlAndSchemas.raml).thenReturn(mock[RAML])

  val messages = mock[Messages]
  val request = mock[Request[Any]]

  trait Setup {
    val publicAvailability = someApiAvailability().asPublic
    val privateAvailability = someApiAvailability().asPrivate
    val privateTrialAvailability = someApiAvailability().asPrivate.asTrial

    val publicVersion: ExtendedAPIVersion = ExtendedAPIVersion("1.0", APIStatus.STABLE, Seq(), publicAvailability, publicAvailability)
    val privateVersion: ExtendedAPIVersion = ExtendedAPIVersion("2.0", APIStatus.BETA, Seq(), privateAvailability, privateAvailability)
    val privateTrialVersion: ExtendedAPIVersion = ExtendedAPIVersion("3.0", APIStatus.BETA, Seq(), privateTrialAvailability, privateTrialAvailability)
    val versions: Seq[ExtendedAPIVersion] = Seq(publicVersion, privateVersion, privateTrialVersion)
    val api: ExtendedAPIDefinition = ExtendedAPIDefinition("test", "", "Test Service", "", "a context", requiresTrust = true, isTestSupport = true, versions)
    val currentVersion = versions.head

    val page = Page(new serviceDocumentation(apiMain)(pageAttributes, api, currentVersion, ramlAndSchemas, loggedIn = true)(request, mockAppConfig, messages))
  }

  "service documentation view" when {
    "rendering the versions dropdown" should {
      "include the public version" in new Setup {
        page.versionsDropdown.text should include("v1.0 (Stable)")
      }

      "include the private trial version" in new Setup {
        page.versionsDropdown.text should include("v3.0 (Private Beta)")
      }

      "not include the private version" in new Setup {
        page.versionsDropdown.text should not include "v2.0 (Private Beta)"
      }

      "highlight the selected version" in new Setup {
        page.selectedVersion.text shouldBe "v1.0 (Stable)"
      }
    }
  }
}
