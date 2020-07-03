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
import scala.util.{Failure, Success}
import uk.gov.hmrc.apidocumentation.services.RAML
import play.api.libs.json.Json
import uk.gov.hmrc.ramltools.loaders.ComprehensiveClasspathRamlLoader
import uk.gov.hmrc.apidocumentation.models.wiremodel.WireModelFormatters._
import uk.gov.hmrc.apidocumentation.models.wiremodel.WireModel

class WireModelSpec extends UnitSpec {
  "RAML to wireModel" should {
    "Simple.raml should parse title and version to our model" in {
      val raml = loadRaml("V2/simple.raml")

      val wireModel = WireModel(raml)
      wireModel.title shouldBe "My simple title"
      wireModel.version shouldBe "My version"
    }

    "With single method" in {
      val raml = loadRaml("V2/single-method.raml")

      val wireModel = WireModel(raml)
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
      /*val raml = */loadRaml("multiple-endpoints.raml")

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

    "With multiple endpoints maintain RAML ordering" in {
      val raml = loadRaml("multiple-methods.raml")

      val wireModel = WireModel(raml)
      wireModel.resourceGroups.size shouldBe 1

      val rg = wireModel.resourceGroups(0)

      println("**** Actual Method Ordering: " + rg.resources.map(r=>r.displayName).mkString(","))

      rg.resources(0).displayName shouldBe "/endpoint1"
      rg.resources(1).displayName shouldBe "/endpoint2"
      rg.resources(2).displayName shouldBe "/endpoint3"
    }

    "With global type with enums" in {
      val raml = loadRaml("V2/typed-enums.raml")

      val wireModel = WireModel(raml)
      wireModel.resourceGroups.size shouldBe 1

      val rg = wireModel.resourceGroups(0)

      rg.resources(0).displayName shouldBe "/my/endpoint"
      val qps = rg.resources(0).methods.head.queryParameters
      val qp1 = qps.head
      qp1.name shouldBe "aParam"
      qp1.enumValues shouldBe List("1","2")

      val qp2 = qps.tail.head

      qp2.name shouldBe "anotherParam"
      qp2.enumValues shouldBe List("a","b","c")
    }
  }

  "convert to json experiments" should {
    "What does the JSON look like?" ignore {
      val raml = loadRaml("multiple-endpoints.raml")

      val wireModel = WireModel(raml)

        println(Json.prettyPrint(Json.toJson(wireModel)))
    }

    "What does business-rates look like?" ignore {
      val raml = loadRaml("V2/business-rates/2.0/application.raml")

      val wireModel = WireModel(raml)

      val json = Json.toJson(wireModel)
      println(Json.prettyPrint(json))

      saveToFile("business-rates-as-json.json", Json.prettyPrint(json))
    }
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
