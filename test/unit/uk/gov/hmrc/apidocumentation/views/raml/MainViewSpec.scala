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

package unit.uk.gov.hmrc.apidocumentation.views.raml

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.mockito.Mockito.{when => When}
import org.scalatest.mockito.MockitoSugar
import play.twirl.api.HtmlFormat.Appendable
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.apidocumentation.services.RAML
import uk.gov.hmrc.apidocumentation.views
import uk.gov.hmrc.play.test.UnitSpec
import unit.uk.gov.hmrc.apidocumentation.utils.FileRamlLoader

import scala.collection.JavaConversions._

class MainViewSpec extends UnitSpec with MockitoSugar {
  case class Page(doc: Appendable) {
    lazy val dom: Document = Jsoup.parse(doc.body)
    lazy val sandboxAvailability: String = environmentAvailability("sandbox")
    lazy val productionAvailability: String = environmentAvailability("production")
    lazy val docHeadings: Set[String] = dom.getElementsByTag("h2").eachText.toSet
    lazy val callToSignIn: Option[Element] = Option(dom.getElementById("read-more-sign-in"))

    private def elementExistsByText(elementType: String, elementText: String): Boolean = {
      dom.select(elementType).exists(node => node.text.trim == elementText)
    }

    private def environmentAvailability(env: String) =
      dom.getElementsContainingOwnText(s"Available in ${env.capitalize}").first.parent.nextElementSibling.text

    def productionBaseUrl =
      dom.getElementsContainingOwnText("Production base URL").first.parent.nextElementSibling.text

    def showsProductionBaseUrl= elementExistsByText("span", "Production base URL")

    def sandboxBaseUrl =
      dom.getElementsContainingOwnText("Sandbox base URL").first.parent.nextElementSibling.text

    def showsSandboxBaseUrl = elementExistsByText("span", "Sandbox base URL")
  }

  private def renderAllDocumentation(page: Page) = {
    page.docHeadings shouldBe Set("Overview", "Versioning", "Errors", "Resources")
  }

  val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  val ramlAndSchemas: RAML = new FileRamlLoader().load("test/resources/unit/raml/multiple-docs.raml").get
  val schemas: Map[String, JsonSchema] = Map()

  private def showEnvironmentAvailability(isAvailable: Boolean) = {
    When(mockAppConfig.showSandboxAvailability).thenReturn(isAvailable)
    When(mockAppConfig.showProductionAvailability).thenReturn(isAvailable)
  }

  private val productionBaseUrl = "https://production.example.com/"
  private val sandboxBaseUrl = "https://sandbox.example.com/"

  private def showBaseUrl = {
    When(mockAppConfig.productionBaseUrl).thenReturn(Some(productionBaseUrl))
    When(mockAppConfig.sandboxBaseUrl).thenReturn(Some(sandboxBaseUrl))
  }

  "main view" when {
    showEnvironmentAvailability(true)
    showBaseUrl

    "api version is private, in trial and the user is logged in as a member of a whitelisted application" should {

      val isWhitelisted = true
      val apiAccess = APIAccess(APIAccessType.PRIVATE, isTrial = Some(true))
      val availability = Some(APIAvailability(endpointsEnabled = true, apiAccess, loggedIn = true, authorised = isWhitelisted))
      val version = ExtendedAPIVersion("1.0", APIStatus.BETA, Seq.empty, availability, availability)
      val page = Page(views.html.raml.main.render(ramlAndSchemas, schemas, Some(version), None, loggedIn = true, mockAppConfig))

      "render availability 'Yes - private trial'" in {
        page.sandboxAvailability shouldBe "Yes - private trial"
        page.productionAvailability shouldBe "Yes - private trial"
      }

      "render full documentation" in {
        renderAllDocumentation(page)
      }

      "render base urls" in {
        page.sandboxBaseUrl shouldBe sandboxBaseUrl
        page.productionBaseUrl shouldBe productionBaseUrl
      }
    }

    "api version is private, in trial and the user is logged in but not a member of a whitelisted application" should {

      val isWhitelisted = false
      val apiAccess = APIAccess(APIAccessType.PRIVATE, isTrial = Some(true))
      val availability = Some(APIAvailability(endpointsEnabled = true, apiAccess, loggedIn = true, authorised = isWhitelisted))
      val version = ExtendedAPIVersion("1.0", APIStatus.BETA, Seq.empty, availability, availability)

      val page = Page(views.html.raml.main.render(ramlAndSchemas, schemas, Some(version), None, loggedIn = true, mockAppConfig))

      "render availability 'Yes - private trial'" in {
        page.sandboxAvailability shouldBe "Yes - private trial"
        page.productionAvailability shouldBe "Yes - private trial"
      }

      "render Overview and Read more only" in {
        page.docHeadings shouldBe Set("Overview", "Read more")
        page.callToSignIn should not be 'defined
      }

      "render base urls" in {
        page.sandboxBaseUrl shouldBe sandboxBaseUrl
        page.productionBaseUrl shouldBe productionBaseUrl
      }

    }

    "api version is private, in trial and the user is not logged in" should {

      val isWhitelisted = false
      val apiAccess = APIAccess(APIAccessType.PRIVATE, isTrial = Some(true))
      val availability = Some(APIAvailability(endpointsEnabled = true, apiAccess, loggedIn = true, authorised = isWhitelisted))
      val version = ExtendedAPIVersion("1.0", APIStatus.BETA, Seq.empty, availability, availability)

      val page = Page(views.html.raml.main.render(ramlAndSchemas, schemas, Some(version), None, loggedIn = false, mockAppConfig))

      "render availability 'Yes - private trial'" in {
        page.sandboxAvailability shouldBe "Yes - private trial"
        page.productionAvailability shouldBe "Yes - private trial"
      }

      "render Overview and Read more with call to log in" in {
        page.docHeadings shouldBe Set("Overview", "Read more")
        page.callToSignIn shouldBe 'defined
      }

      "render base urls" in {
        page.sandboxBaseUrl shouldBe sandboxBaseUrl
        page.productionBaseUrl shouldBe productionBaseUrl
      }
    }

    "api version is private, not in trial and the user is a member of a whitelisted application" should {

      val isWhitelisted = true
      val apiAccess = APIAccess(APIAccessType.PRIVATE, isTrial = Some(false))
      val availability = Some(APIAvailability(endpointsEnabled = true, apiAccess, loggedIn = true, authorised = isWhitelisted))
      val version = ExtendedAPIVersion("1.0", APIStatus.BETA, Seq.empty, availability, availability)

      val page = Page(views.html.raml.main.render(ramlAndSchemas, schemas, Some(version), None, loggedIn = true, mockAppConfig))

      "render availability 'Yes - private trial" in {
        page.sandboxAvailability shouldBe "Yes"
        page.productionAvailability shouldBe "Yes"
      }

      "render full documentation" in {
        renderAllDocumentation(page)
      }

      "render base urls" in {
        page.sandboxBaseUrl shouldBe sandboxBaseUrl
        page.productionBaseUrl shouldBe productionBaseUrl
      }
    }

    "api version is private, not in trial and the user is not a member of a whitelisted application" should {

      val isWhitelisted = false
      val apiAccess = APIAccess(APIAccessType.PRIVATE, isTrial = Some(false))
      val availability = Some(APIAvailability(endpointsEnabled = true, apiAccess, loggedIn = true, authorised = isWhitelisted))
      val version = ExtendedAPIVersion("1.0", APIStatus.BETA, Seq.empty, availability, availability)

      val page = Page(views.html.raml.main.render(ramlAndSchemas, schemas, Some(version), None, loggedIn = true, mockAppConfig))

      "render 'No'" in {
        page.sandboxAvailability shouldBe "No"
        page.productionAvailability shouldBe "No"
      }

      "not render content" in {
        page.docHeadings shouldBe Set.empty
      }

      "not render base urls" in {
        page.showsSandboxBaseUrl shouldBe false
        page.showsProductionBaseUrl shouldBe false
      }
    }

    "api version is public, in trial and the user is a member of a whitelisted application" should {

      val isWhitelisted = true
      val apiAccess = APIAccess(APIAccessType.PUBLIC, isTrial = Some(true))
      val availability = Some(APIAvailability(endpointsEnabled = true, apiAccess, loggedIn = true, authorised = isWhitelisted))
      val version = ExtendedAPIVersion("1.0", APIStatus.BETA, Seq.empty, availability, availability)

      val page = Page(views.html.raml.main.render(ramlAndSchemas, schemas, Some(version), None, loggedIn = true, mockAppConfig))

      "render availability 'Yes'" in {
        page.sandboxAvailability shouldBe "Yes"
        page.productionAvailability shouldBe "Yes"
      }

      "render full documentation" in {
        renderAllDocumentation(page)
      }

      "render base urls" in {
        page.sandboxBaseUrl shouldBe sandboxBaseUrl
        page.productionBaseUrl shouldBe productionBaseUrl
      }
    }

    "api version is public, in trial and the user is not a member of a whitelisted application" should {

      val isWhitelisted = true
      val apiAccess = APIAccess(APIAccessType.PUBLIC, isTrial = Some(true))
      val availability = Some(APIAvailability(endpointsEnabled = true, apiAccess, loggedIn = true, authorised = isWhitelisted))
      val version = ExtendedAPIVersion("1.0", APIStatus.BETA, Seq.empty, availability, availability)

      val page = Page(views.html.raml.main.render(ramlAndSchemas, schemas, Some(version), None, loggedIn = true,  mockAppConfig))

      "render availability 'Yes'" in {
        page.sandboxAvailability shouldBe "Yes"
        page.productionAvailability shouldBe "Yes"
      }

      "render full documentation" in {
        renderAllDocumentation(page)
      }

      "render base urls" in {
        page.sandboxBaseUrl shouldBe sandboxBaseUrl
        page.productionBaseUrl shouldBe productionBaseUrl
      }
    }

    "api version is public, not in trial and the user is a member of a whitelisted application" should {

      val isWhitelisted = true
      val apiAccess = APIAccess(APIAccessType.PUBLIC, isTrial = Some(false))
      val availability = Some(APIAvailability(endpointsEnabled = true, apiAccess, loggedIn = true, authorised = isWhitelisted))
      val version = ExtendedAPIVersion("1.0", APIStatus.BETA, Seq.empty, availability, availability)

      val page = Page(views.html.raml.main.render(ramlAndSchemas, schemas, Some(version), None, loggedIn = true, mockAppConfig))

      "render availability 'Yes'" in {
        page.sandboxAvailability shouldBe "Yes"
        page.productionAvailability shouldBe "Yes"
      }

      "render full documentation" in {
        renderAllDocumentation(page)
      }

      "render base urls" in {
        page.sandboxBaseUrl shouldBe sandboxBaseUrl
        page.productionBaseUrl shouldBe productionBaseUrl
      }
    }

    "api version is public, not in trial and the user is not a member of a whitelisted application" should {

      val isWhitelisted = false
      val apiAccess = APIAccess(APIAccessType.PUBLIC, isTrial = Some(false))
      val availability = Some(APIAvailability(endpointsEnabled = true, apiAccess, loggedIn = true, authorised = isWhitelisted))
      val version = ExtendedAPIVersion("1.0", APIStatus.BETA, Seq.empty, availability, availability)

      val page = Page(views.html.raml.main.render(ramlAndSchemas, schemas, Some(version), None, loggedIn = true, mockAppConfig))

      "render availability: 'Yes'" in {
        page.sandboxAvailability shouldBe "Yes"
        page.productionAvailability shouldBe "Yes"
      }

      "render full documentation" in {
        renderAllDocumentation(page)
      }

      "render base urls" in {
        page.sandboxBaseUrl shouldBe sandboxBaseUrl
        page.productionBaseUrl shouldBe productionBaseUrl
      }
    }

    "previewing RAML" should {
      val page = Page(views.html.raml.main.render(ramlAndSchemas, schemas, None, None, loggedIn = true, mockAppConfig))

      "render full documentation" in {
        renderAllDocumentation(page)
      }
    }
  }
}
