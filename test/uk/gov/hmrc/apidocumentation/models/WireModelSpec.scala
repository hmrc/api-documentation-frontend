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

package uk.gov.hmrc.apidocumentation.models

import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.ramltools.loaders.FileRamlLoader
import scala.util.Failure
import scala.util.Success
import uk.gov.hmrc.apidocumentation.services.RAML
import play.api.libs.json.Json
import uk.gov.hmrc.apidocumentation.views.helpers.ResourceGroup2
import uk.gov.hmrc.ramltools.loaders.ComprehensiveClasspathRamlLoader

// TODO: Rebrand as wireModel spec
class WireModelSpec extends UnitSpec {
  "RAML to wireModel" should {
    "Simple.raml should parse title and version to our model" in {
      val raml = loadRaml("V2/simple.raml")

      val wireModel: WireModel = OurModel(raml)._2
      wireModel.title shouldBe "My simple title"
      wireModel.version shouldBe "My version"
    }

    "With single method" in {
      val raml = loadRaml("V2/single-method.raml")

      val wireModel = OurModel(raml)._2
      wireModel.resourceGroups.size shouldBe 1

      val rg = wireModel.resourceGroups(0)
      rg.description shouldBe None
      rg.name shouldBe None

      val r = rg.resources(0)

      r.resourcePath shouldBe "/my/endpoint"
      r.methods.length shouldBe 1

      val m = r.methods(0)
      m.displayName shouldBe "My endpoint"
      m.description shouldBe Some("My description")

      // TODO: Check endpoint URL, description
      // TODO: Doesn't handle missing description (null pointer)
    }

    "With multiple endpoints" in {
      val raml = loadRaml("multiple-endpoints.raml")

      // val wireModel = OurModel(raml)._2
      // wireModel.resourceGroups.size shouldBe 1

      // val rg = wireModel.resourceGroups(0)
      // rg.description shouldBe None
      // rg.name shouldBe None

      // val r = rg.resources(0)

      // r.resourcePath shouldBe "/my/endpoint"
      // r.methods.length shouldBe 1

      // val m = r.methods(0)
      // m.displayName shouldBe "My endpoint"
      // m.description shouldBe "My description"

      // TODO: Check endpoint URL, description
      // TODO: Doesn't handle missing description (null pointer)
    }
  }

  object wireModelFormatters {
    implicit val hmrcExampleSpecJF = Json.format[HmrcExampleSpec]
    implicit val typeDeclaration2JF = Json.format[TypeDeclaration2]

    implicit val securitySchemeJF = Json.format[SecurityScheme]

    implicit val groupJF = Json.format[Group]
    implicit val hmrcResponseJF = Json.format[HmrcResponse]
    implicit val hmrcMethodJF = Json.format[HmrcMethod]
    implicit val hmrcResourceJF = Json.format[HmrcResource]

    implicit val documentationItemJF = Json.format[DocumentationItem]
    implicit val resourceGroup2JF = Json.format[ResourceGroup2]

    implicit val wireModelJF = Json.format[WireModel]
  }

  "What does the JSON look like?" in {
      import wireModelFormatters._

      val raml = loadRaml("multiple-endpoints.raml")

      val wireModel : WireModel = OurModel(raml)._2

      println(Json.prettyPrint(Json.toJson(wireModel)))
  }

  "What does business-rates look like?" in {
    import wireModelFormatters._

    val raml = loadRaml("V2/business-rates/2.0/application.raml")

    val wireModel : WireModel = OurModel(raml)._2

    val json = Json.toJson(wireModel)
    println(Json.prettyPrint(json))

    saveToFile("business-rates-as-json.json", Json.prettyPrint(json))
  }


  def saveToFile(filename: String, contents: String) = {
    import java.io.PrintWriter
    new PrintWriter(filename) { write(contents); close }
  }

  def loadRaml(filename: String) : RAML = {
    new ComprehensiveClasspathRamlLoader().load(s"test/resources/raml/$filename") match {
      case Failure(exception) => throw exception
      case Success(raml) => raml
    }
  }
}
