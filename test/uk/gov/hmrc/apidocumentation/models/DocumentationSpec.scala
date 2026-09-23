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

package uk.gov.hmrc.apidocumentation.models

import org.scalatest.prop.TableDrivenPropertyChecks.*
import org.scalatest.prop.Tables.Table

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiCategory.*
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.{ApiStatus, *}
import uk.gov.hmrc.apiplatform.modules.common.utils.HmrcSpec

import uk.gov.hmrc.apidocumentation.utils.ApiDefinitionTestDataHelper

class DocumentationSpec extends HmrcSpec with ApiDefinitionTestDataHelper {

  "ApiDefinition.defaultVersion" should {

    val v1Retired          = apiVersion("1.0", ApiStatus.Retired)
    val v2Published        = apiVersion("2.0", ApiStatus.Stable)
    val v2Deprecated       = apiVersion("2.0", ApiStatus.Deprecated)
    val v3Prototyped       = apiVersion("3.0", ApiStatus.Beta)
    val v4Alpha            = apiVersion("4.0", ApiStatus.Alpha)
    val v3PrivatePublished = apiVersion("3.1", ApiStatus.Beta, ApiAccessType.Controlled)
    val v10Published       = apiVersion("10.0", ApiStatus.Stable)
    val v3Published        = apiVersion("3.0", ApiStatus.Stable)
    val v0_9Published      = apiVersion("0.9", ApiStatus.Stable)

    val scenarios = Table(
      ("Versions", "Expected Default Version"),
      (Seq(v3Prototyped, v2Published), v2Published),
      (Seq(v2Deprecated, v3Published), v3Published),
      (Seq(v2Deprecated, v1Retired), v2Deprecated),
      (Seq(v3Prototyped, v2Published, v1Retired), v2Published),
      (Seq(v2Published, v3Published, v0_9Published), v3Published),
      (Seq(v3Prototyped, v2Published, v10Published, v1Retired), v10Published),
      (Seq(v3Prototyped, v3PrivatePublished), v3PrivatePublished),
      (Seq(v3PrivatePublished, v3Prototyped), v3PrivatePublished),
      (Seq(v4Alpha, v3Prototyped), v3Prototyped),
      (Seq(v4Alpha, v3Prototyped, v2Published), v2Published)
    )

    "return the default version depending on the status and version" in {

      forAll(scenarios) { (versions, expectedDefaultVersion) =>
        val api = WrappedApiDefinition(apiDefinition("serviceName", versions))

        api.defaultVersion `shouldEqual` expectedDefaultVersion
      }
    }
  }

  "ApiDefinition.groupedByCategory" should {
    "group definitions by category when each definition has a single category" in {
      val api1     = apiDefinition("name1", categories = List(Customs))
      val api2     = apiDefinition("name2", categories = List(Paye))
      val expected = Map(
        Customs -> Seq(WrappedApiDefinition(api1)),
        Paye    -> Seq(WrappedApiDefinition(api2))
      )

      val result = Documentation.groupedByCategory(Seq(api1, api2), Seq.empty, Seq.empty, Seq.empty)

      result shouldBe expected
    }

    "group definitions by the categories in the API definition into each specified category when a definition has defined categories" in {
      val api1        = apiDefinition("name1", categories = List(Customs, Vat))
      val api2        = apiDefinition("name2", categories = List(Paye, Vat))
      val categoryMap = Map(
        "name1" -> Seq(IncomeTaxMtd),
        "name2" -> Seq(CorporationTax)
      )
      val expected    = Map(
        Customs -> Seq(WrappedApiDefinition(api1)),
        Paye    -> Seq(WrappedApiDefinition(api2)),
        Vat     -> Seq(WrappedApiDefinition(api1), WrappedApiDefinition(api2))
      )

      val result = Documentation.groupedByCategory(Seq(api1, api2), Seq.empty, Seq.empty, Seq.empty, categoryMap)

      result shouldBe expected
    }

    "group definitions into 'Other' when the definition has no categories and no matching context in the category map" in {
      val api1        = apiDefinition("name1")
      val api2        = apiDefinition("name2")
      val categoryMap = Map("name3" -> Seq(Customs))
      val expected    = Map(Other -> Seq(WrappedApiDefinition(api1), WrappedApiDefinition(api2)))

      val result = Documentation.groupedByCategory(Seq(api1, api2), Seq.empty, Seq.empty, Seq.empty, categoryMap)

      result shouldBe expected
    }

    "group definitions by the category map when no categories specified in the definition and there are matching contexts in the category map" in {
      val api1        = apiDefinition("name1")
      val api2        = apiDefinition("name2")
      val categoryMap = Map(
        "name1" -> Seq(Customs, Vat),
        "name2" -> Seq(Paye, Vat)
      )

      val expected = Map(
        Customs -> Seq(WrappedApiDefinition(api1)),
        Paye    -> Seq(WrappedApiDefinition(api2)),
        Vat     -> Seq(WrappedApiDefinition(api1), WrappedApiDefinition(api2))
      )

      val result = Documentation.groupedByCategory(Seq(api1, api2), Seq.empty, Seq.empty, Seq.empty, categoryMap)

      result shouldBe expected
    }

    "include XML API definitions" in {
      val api1        = apiDefinition("name1")
      val api2        = apiDefinition("name2")
      val xmlApi1     = anXmlApiDefinition("xmlName1")
      val xmlApi2     = anXmlApiDefinition("xmlName2")
      val categoryMap = Map(
        "name1"    -> Seq(Customs, Vat),
        "name2"    -> Seq(Paye, Vat),
        "xmlName1" -> Seq(Paye),
        "xmlName2" -> Seq(Vat)
      )

      val expected = Map(
        Customs -> Seq(WrappedApiDefinition(api1)),
        Paye    -> Seq(WrappedApiDefinition(api2), xmlApi1),
        Vat     -> Seq(WrappedApiDefinition(api1), WrappedApiDefinition(api2), xmlApi2)
      )

      val result = Documentation.groupedByCategory(Seq(api1, api2), Seq(xmlApi1, xmlApi2), Seq.empty, Seq.empty, categoryMap)

      result shouldBe expected
    }

    "include REST and Test Support APIS" in {
      val restApi        = apiDefinition("restApi")
      val testSupportApi = apiDefinition("testSupportApi").copy(isTestSupport = true)
      val categoryMap    = Map(
        "restApi"        -> Seq(Customs),
        "testSupportApi" -> Seq(Customs)
      )
      val expected       = Map(Customs -> Seq(WrappedApiDefinition(restApi), WrappedApiDefinition(testSupportApi)))

      val result = Documentation.groupedByCategory(Seq(restApi, testSupportApi), Seq.empty, Seq.empty, Seq.empty, categoryMap)

      result shouldBe expected
    }

    "include XML and Test Support APIS" in {
      val xmlApi         = anXmlApiDefinition("xmlApi")
      val testSupportApi = apiDefinition("testSupportApi").copy(isTestSupport = true)
      val categoryMap    = Map(
        "xmlApi"         -> Seq(Customs),
        "testSupportApi" -> Seq(Customs)
      )
      val expected       = Map(Customs -> Seq(WrappedApiDefinition(testSupportApi), xmlApi))

      val result = Documentation.groupedByCategory(Seq(testSupportApi), Seq(xmlApi), Seq.empty, Seq.empty, categoryMap)

      result shouldBe expected
    }

    "include APIs and Service guides" in {
      val api          = apiDefinition("myApi")
      val serviceGuide = aServiceGuide("myServiceGuide")
      val categoryMap  = Map(
        "myApi"          -> Seq(Customs),
        "myServiceGuide" -> Seq(Customs)
      )
      val expected     = Map(Customs -> Seq(WrappedApiDefinition(api), serviceGuide))

      val result = Documentation.groupedByCategory(Seq(api), Seq.empty, Seq(serviceGuide), Seq.empty, categoryMap)

      result shouldBe expected
    }

    "include APIs and Road maps" in {
      val api         = apiDefinition("myApi")
      val roadMap     = aRoadMap("myRoadMap")
      val categoryMap = Map(
        "myApi"     -> Seq(Customs),
        "myRoadMap" -> Seq(Customs)
      )
      val expected    = Map(Customs -> Seq(WrappedApiDefinition(api), roadMap))

      val result = Documentation.groupedByCategory(Seq(api), Seq.empty, Seq.empty, Seq(roadMap), categoryMap)

      result shouldBe expected
    }

    "filter out categories without REST or XML APIs" in {
      val testSupportApi = apiDefinition("testSupportApi").copy(isTestSupport = true)
      val serviceGuide   = aServiceGuide("serviceGuide")
      val categoryMap    = Map("name1" -> Seq(Customs, Vat))

      val result = Documentation.groupedByCategory(Seq(testSupportApi), Seq.empty, Seq(serviceGuide), Seq.empty, categoryMap)

      result shouldBe Map.empty
    }
  }

  "nameAsId" should {
    "swap spaces for hyphens and lower case" in {
      val api1 = WrappedApiDefinition(apiDefinition("Hello World"))
      api1.nameAsId shouldBe "hello-world"
    }

    "remove brackets" in {
      val api1 = WrappedApiDefinition(apiDefinition("Income Tax (MTD) end-to-end service guide"))
      api1.nameAsId shouldBe "income-tax-mtd-end-to-end-service-guide"
    }

    "remove any other chars" in {
      val api1 = WrappedApiDefinition(apiDefinition("Income Tax (MTD):+{}=#@£!& [end-to-end service guide]"))
      api1.nameAsId shouldBe "income-tax-mtd-end-to-end-service-guide"
    }
  }

  def anXmlApiDefinition(name: String, categories: Option[Seq[ApiCategory]] = None) =
    XmlApiDocumentation(name, "description", "context", categories)

  def aServiceGuide(name: String, categories: Option[Seq[ApiCategory]] = None) =
    ServiceGuide(name, "context", categories)

  def aRoadMap(name: String, categories: Option[Seq[ApiCategory]] = None) =
    RoadMap(name, "context", categories)
}
