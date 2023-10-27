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

import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.Tables.Table

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiCategory._
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiStatus._
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.{ApiAccess, ApiCategory}

import uk.gov.hmrc.apidocumentation.common.utils.HmrcSpec
import uk.gov.hmrc.apidocumentation.utils.ApiDefinitionTestDataHelper

class DocumentationSpec extends HmrcSpec with ApiDefinitionTestDataHelper {

  "APIVersion.displayedStatus for PUBLIC apis" should {

    val scenarios = Table(
      ("API Status", "Expected Displayed Status"),
      (APIStatus.ALPHA, "Alpha"),
      (APIStatus.BETA, "Beta"),
      (APIStatus.STABLE, "Stable"),
      (APIStatus.DEPRECATED, "Deprecated"),
      (APIStatus.RETIRED, "Retired")
    )

    "return the status to display" in {
      forAll(scenarios) { (status, expectedDisplayedStatus) =>
        val v = APIVersion("version", None, status, Seq.empty)
        v.displayedStatus shouldEqual expectedDisplayedStatus
      }
    }
  }

  "APIVersion.displayedStatus for PRIVATE Apis" should {

    val scenarios = Table(
      ("API Status", "Expected Displayed Status"),
      (APIStatus.BETA, "Private Beta"),
      (APIStatus.STABLE, "Private Stable"),
      (APIStatus.DEPRECATED, "Private Deprecated"),
      (APIStatus.RETIRED, "Private Retired")
    )

    "return the status to display" in {
      forAll(scenarios) { (status, expectedDisplayedStatus) =>
        val v = APIVersion("version", Some(APIAccess(APIAccessType.PRIVATE)), status, Seq.empty)
        v.displayedStatus shouldEqual expectedDisplayedStatus
      }
    }
  }

  "ApiDefinition.defaultVersion" should {

    val v1Retired          = apiVersion("1.0", RETIRED)
    val v2Published        = apiVersion("2.0", STABLE)
    val v201Published      = apiVersion("2.0.1", STABLE)
    val v2Deprecated       = apiVersion("2.0", DEPRECATED)
    val v3Prototyped       = apiVersion("3.0", BETA)
    val v4Alpha            = apiVersion("4.0", ALPHA)
    val v3PrivatePublished = apiVersion("3.0rc", BETA, ApiAccess.Private(true))
    val v10Published       = apiVersion("10.0", STABLE)
    val v3Published        = apiVersion("3.0", STABLE)
    val vTestPublished     = apiVersion("test", STABLE)
    val v0_9Published      = apiVersion("0.9", STABLE)

    val scenarios = Table(
      ("Versions", "Expected Default Version"),
      (Seq(v3Prototyped, v2Published), Some(v2Published)),
      (Seq(v2Deprecated, v3Published), Some(v3Published)),
      (Seq(v2Published, v201Published), Some(v201Published)),
      (Seq(v201Published, v2Published), Some(v201Published)),
      (Seq(v2Deprecated, v1Retired), Some(v2Deprecated)),
      (Seq(v3Prototyped, v2Published, v1Retired), Some(v2Published)),
      (Seq(v3Prototyped, v2Published, v10Published, v1Retired), Some(v10Published)),
      (Seq(v1Retired), None),
      (Seq(v3Prototyped, v3PrivatePublished), Some(v3PrivatePublished)),
      (Seq(v3PrivatePublished, v3Prototyped), Some(v3PrivatePublished)),
      (Seq(v4Alpha, v3Prototyped), Some(v3Prototyped)),
      (Seq(v4Alpha, v3Prototyped, v2Published), Some(v2Published)),
      // non-decimal version treated as 1.0.0
      (Seq(vTestPublished, v0_9Published), Some(vTestPublished)),
      (Seq(vTestPublished, v2Published), Some(v2Published))
    )

    "return the default version depending on the status and version" in {

      forAll(scenarios) { (versions, expectedDefaultVersion) =>
        val api = WrappedApiDefinition(apiDefinition("serviceName", versions))

        api.defaultVersion shouldEqual expectedDefaultVersion
      }
    }
  }

  "ApiDefinition.groupedByCategory" should {
    "group definitions by category when each definition has a single category" in {
      val api1     = apiDefinition("name1", categories = List(CUSTOMS))
      val api2     = apiDefinition("name2", categories = List(PAYE))
      val expected = Map(
        CUSTOMS -> Seq(api1),
        PAYE    -> Seq(api2)
      )

      val result = Documentation.groupedByCategory(Seq(api1, api2), Seq.empty, Seq.empty, Seq.empty)

      result shouldBe expected
    }

    "group definitions by the categories in the API definition into each specified category when a definition has defined categories" in {
      val api1        = apiDefinition("name1", categories = List(CUSTOMS, VAT))
      val api2        = apiDefinition("name2", categories = List(PAYE, VAT))
      val categoryMap = Map(
        "name1" -> Seq(INCOME_TAX_MTD),
        "name2" -> Seq(CORPORATION_TAX)
      )
      val expected    = Map(
        CUSTOMS -> Seq(api1),
        PAYE    -> Seq(api2),
        VAT     -> Seq(api1, api2)
      )

      val result = Documentation.groupedByCategory(Seq(api1, api2), Seq.empty, Seq.empty, Seq.empty, categoryMap)

      result shouldBe expected
    }

    "group definitions into 'Other' when the definition has no categories and no matching context in the category map" in {
      val api1        = apiDefinition("name1")
      val api2        = apiDefinition("name2")
      val categoryMap = Map("name3" -> Seq(CUSTOMS))
      val expected    = Map(OTHER -> Seq(api1, api2))

      val result = Documentation.groupedByCategory(Seq(api1, api2), Seq.empty, Seq.empty, Seq.empty, categoryMap)

      result shouldBe expected
    }

    "group definitions by the category map when no categories specified in the definition and there are matching contexts in the category map" in {
      val api1        = apiDefinition("name1")
      val api2        = apiDefinition("name2")
      val categoryMap = Map(
        "name1" -> Seq(CUSTOMS, VAT),
        "name2" -> Seq(PAYE, VAT)
      )

      val expected = Map(
        CUSTOMS -> Seq(api1),
        PAYE    -> Seq(api2),
        VAT     -> Seq(api1, api2)
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
        "name1"    -> Seq(CUSTOMS, VAT),
        "name2"    -> Seq(PAYE, VAT),
        "xmlName1" -> Seq(PAYE),
        "xmlName2" -> Seq(VAT)
      )

      val expected = Map(
        CUSTOMS -> Seq(api1),
        PAYE    -> Seq(api2, xmlApi1),
        VAT     -> Seq(api1, api2, xmlApi2)
      )

      val result = Documentation.groupedByCategory(Seq(api1, api2), Seq(xmlApi1, xmlApi2), Seq.empty, Seq.empty, categoryMap)

      result shouldBe expected
    }

    "include REST and Test Support APIS" in {
      val restApi        = apiDefinition("restApi")
      val testSupportApi = apiDefinition("testSupportApi").copy(isTestSupport = true)
      val categoryMap    = Map(
        "restApi"        -> Seq(CUSTOMS),
        "testSupportApi" -> Seq(CUSTOMS)
      )
      val expected       = Map(CUSTOMS -> Seq(restApi, testSupportApi))

      val result = Documentation.groupedByCategory(Seq(restApi, testSupportApi), Seq.empty, Seq.empty, Seq.empty, categoryMap)

      result shouldBe expected
    }

    "include XML and Test Support APIS" in {
      val xmlApi         = anXmlApiDefinition("xmlApi")
      val testSupportApi = apiDefinition("testSupportApi").copy(isTestSupport = true)
      val categoryMap    = Map(
        "xmlApi"         -> Seq(CUSTOMS),
        "testSupportApi" -> Seq(CUSTOMS)
      )
      val expected       = Map(CUSTOMS -> Seq(testSupportApi, xmlApi))

      val result = Documentation.groupedByCategory(Seq(testSupportApi), Seq(xmlApi), Seq.empty, Seq.empty, categoryMap)

      result shouldBe expected
    }

    "include APIs and Service guides" in {
      val api          = apiDefinition("myApi")
      val serviceGuide = aServiceGuide("myServiceGuide")
      val categoryMap  = Map(
        "myApi"          -> Seq(CUSTOMS),
        "myServiceGuide" -> Seq(CUSTOMS)
      )
      val expected     = Map(CUSTOMS -> Seq(api, serviceGuide))

      val result = Documentation.groupedByCategory(Seq(api), Seq.empty, Seq(serviceGuide), Seq.empty, categoryMap)

      result shouldBe expected
    }

    "include APIs and Road maps" in {
      val api         = apiDefinition("myApi")
      val roadMap     = aRoadMap("myRoadMap")
      val categoryMap = Map(
        "myApi"     -> Seq(CUSTOMS),
        "myRoadMap" -> Seq(CUSTOMS)
      )
      val expected    = Map(CUSTOMS -> Seq(api, roadMap))

      val result = Documentation.groupedByCategory(Seq(api), Seq.empty, Seq.empty, Seq(roadMap), categoryMap)

      result shouldBe expected
    }

    "filter out categories without REST or XML APIs" in {
      val testSupportApi = apiDefinition("testSupportApi").copy(isTestSupport = true)
      val serviceGuide   = aServiceGuide("serviceGuide")
      val categoryMap    = Map("name1" -> Seq(CUSTOMS, VAT))

      val result = Documentation.groupedByCategory(Seq(testSupportApi), Seq.empty, Seq(serviceGuide), Seq.empty, categoryMap)

      result shouldBe Map.empty
    }
  }

  "decoratedUriPattern" should {
    case class Scenario(outputUriPattern: String, inputUriPattern: String, inputParameters: Option[Seq[Parameter]] = None)

    val mandatory        = Parameter("mandatory", required = true)
    val optional         = Parameter("optional", required = false)
    val anotherMandatory = Parameter("anotherMandatory", required = true)

    val scenarios = Seq(
      Scenario("/sa/{utr}", "/sa/{utr}"),
      Scenario("/sa/{utr}?mandatory={mandatory}", "/sa/{utr}", Some(Seq(mandatory))),
      Scenario("/sa/{utr}", "/sa/{utr}", Some(Seq(optional))),
      Scenario("/sa/{utr}?mandatory={mandatory}", "/sa/{utr}", Some(Seq(optional, mandatory))),
      Scenario("/sa/{utr}?mandatory={mandatory}&anotherMandatory={anotherMandatory}", "/sa/{utr}", Some(Seq(optional, mandatory, anotherMandatory)))
    )

    scenarios.foreach(scenario => {
      s"return ${scenario.outputUriPattern} when given ${scenario.inputUriPattern} with parameters: ${scenario.inputParameters}" in {
        anEndpoint(scenario.inputUriPattern, scenario.inputParameters).decoratedUriPattern shouldBe scenario.outputUriPattern
      }
    })

    def anEndpoint(uriPattern: String, parameters: Option[Seq[Parameter]]) = {
      ExtendedEndpoint("Get Today's Date", uriPattern, HttpMethod.GET, parameters)
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
