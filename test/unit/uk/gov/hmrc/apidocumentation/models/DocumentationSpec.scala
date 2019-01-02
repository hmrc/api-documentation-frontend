/*
 * Copyright 2019 HM Revenue & Customs
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

package unit.uk.gov.hmrc.apidocumentation.models

import uk.gov.hmrc.apidocumentation.models.APIStatus._
import uk.gov.hmrc.apidocumentation.models.APICategory._
import uk.gov.hmrc.apidocumentation.models._
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.Tables.Table
import uk.gov.hmrc.play.test.UnitSpec

class DocumentationSpec extends UnitSpec {

  "APIVersion.displayedStatus for PUBLIC apis" should {

    val scenarios = Table(
      ("API Status", "Expected Displayed Status"),
      (ALPHA, "Alpha"),
      (BETA, "Beta"),
      (STABLE, "Stable"),
      (DEPRECATED, "Deprecated"),
      (RETIRED, "Retired"))

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
      (BETA, "Private Beta"),
      (STABLE, "Private Stable"),
      (DEPRECATED, "Private Deprecated"),
      (RETIRED, "Private Retired"))

    "return the status to display" in {
      forAll(scenarios) { (status, expectedDisplayedStatus) =>

        val v = APIVersion("version", Some(APIAccess(APIAccessType.PRIVATE)), status, Seq.empty)
        v.displayedStatus shouldEqual expectedDisplayedStatus
      }
    }
  }

  "APIDefinition.defaultVersion" should {

    val v1Retired = APIVersion("1.0", None, RETIRED, Seq.empty)
    val v2Published = APIVersion("2.0", None, STABLE, Seq.empty)
    val v201Published = APIVersion("2.0.1", None, STABLE, Seq.empty)
    val v2Deprecated = APIVersion("2.0", None, DEPRECATED, Seq.empty)
    val v3Prototyped = APIVersion("3.0", None, BETA, Seq.empty)
    val v4Alpha = APIVersion("4.0", None, ALPHA, Seq.empty)
    val v3PrivatePublished = APIVersion("3.0rc", Some(APIAccess(APIAccessType.PRIVATE)), BETA, Seq.empty)
    val v10Published = APIVersion("10.0", None, STABLE, Seq.empty)
    val v3Published = APIVersion("3.0", None, STABLE, Seq.empty)
    val vTestPublished = APIVersion("test", None, STABLE, Seq.empty)
    val v0_9Published = APIVersion("0.9", None, STABLE, Seq.empty)


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

        val api = APIDefinition("serviceName", "name", "description", "context", None, None, versions)

        api.defaultVersion shouldEqual expectedDefaultVersion
      }
    }

    def version(version: String, apiStatus: APIStatus) = APIVersion(version, None, apiStatus, Seq.empty)
  }

  "APIDefinition.reverseSortedVersions" should {

    val v1Retired = APIVersion("1.0", None, RETIRED, Seq.empty)
    val v2Deprecated = APIVersion("2.0", None, DEPRECATED, Seq.empty)
    val v3Deprecated = APIVersion("3.0", None, DEPRECATED, Seq.empty)
    val v3Prototyped = APIVersion("3.0", None, BETA, Seq.empty)
    val v3PrivatePublished = APIVersion("3.0rc", Some(APIAccess(APIAccessType.PRIVATE)), BETA, Seq.empty)
    val v3Published = APIVersion("3.0", None, STABLE, Seq.empty)
    val v4Prototyped = APIVersion("4.0", None, BETA, Seq.empty)

    val scenarios = Table(
      ("Versions", "Expected Default Version"),
      (Seq(v3Prototyped, v2Deprecated), Seq(v3Prototyped, v2Deprecated)),
      (Seq(v2Deprecated, v3Published), Seq(v3Published, v2Deprecated)),
      (Seq(v2Deprecated, v1Retired), Seq(v2Deprecated, v1Retired)),
      (Seq(v3Prototyped, v2Deprecated, v1Retired), Seq(v3Prototyped, v2Deprecated, v1Retired)),
      (Seq(v2Deprecated, v4Prototyped, v3Published, v1Retired), Seq(v4Prototyped, v3Published, v2Deprecated, v1Retired)),
      (Seq(v3Prototyped, v3PrivatePublished), Seq(v3PrivatePublished, v3Prototyped)),
      (Seq(v3PrivatePublished, v3Prototyped), Seq(v3PrivatePublished, v3Prototyped))
    )

    "return the versions sorted by the status and version" in {

      forAll(scenarios) { (versions, expectedVersions) =>
        val api = APIDefinition("serviceName", "name", "description", "context", None, None, versions)

        api.sortedVersions shouldEqual expectedVersions
      }
    }
  }

  "APIDefinition.statusSortedVersions" should {

    val v1Retired = APIVersion("1.0", None, RETIRED, Seq.empty)
    val v2Published = APIVersion("2.0", None, STABLE, Seq.empty)
    val v2Deprecated = APIVersion("2.0", None, DEPRECATED, Seq.empty)
    val v3Prototyped = APIVersion("3.0", None, BETA, Seq.empty)
    val v3Published = APIVersion("3.0", None, STABLE, Seq.empty)
    val v4Prototyped = APIVersion("4.0", None, BETA, Seq.empty)

    val scenarios = Table(
      ("Versions", "Expected Default Version"),
      (Seq(v3Prototyped, v2Published), Seq(v2Published, v3Prototyped)),
      (Seq(v2Deprecated, v3Published), Seq(v3Published, v2Deprecated)),
      (Seq(v2Deprecated, v1Retired), Seq(v2Deprecated, v1Retired)),
      (Seq(v3Prototyped, v2Published, v1Retired), Seq(v2Published, v3Prototyped, v1Retired)),
      (Seq(v2Deprecated, v4Prototyped, v3Published, v1Retired), Seq(v3Published, v4Prototyped, v2Deprecated, v1Retired))
    )

    "return the versions sorted by the status and version" in {

      forAll(scenarios) { (versions, expectedVersions) =>
        val api = APIDefinition("serviceName", "name", "description", "context", None, None, versions)

        api.statusSortedVersions shouldEqual expectedVersions
      }
    }
  }

  "APIDefinition.statusSortedNoRetiredVersions" should {
    val v01Retired = APIVersion("0.1", None, RETIRED, Seq.empty)
    val v1Retired = APIVersion("1.0", None, RETIRED, Seq.empty)
    val v2Deprecated = APIVersion("2.0", None, DEPRECATED, Seq.empty)
    val v21Deprecated = APIVersion("2.1", None, DEPRECATED, Seq.empty)
    val v3Published = APIVersion("3.0", None, STABLE, Seq.empty)
    val v4Prototyped = APIVersion("4.0", None, BETA, Seq.empty)

    val scenarios = Table(
      ("Versions", "Expected Default Version", "Has non retired versions?"),
      (Seq(v2Deprecated, v1Retired), Seq(v2Deprecated), true),
      (Seq(v2Deprecated, v1Retired, v21Deprecated), Seq(v21Deprecated, v2Deprecated), true),
      (Seq(v01Retired, v1Retired), Seq.empty, false),
      (Seq(v2Deprecated, v01Retired, v4Prototyped, v3Published, v1Retired), Seq(v3Published, v4Prototyped, v2Deprecated), true)
    )

    "return the versions sorted by the status and version" in {

      forAll(scenarios) { (versions, expectedVersions, nonRetiredVersionsAvailable) =>
        val api = APIDefinition("serviceName", "name", "description", "context", None, None, versions)

        api.hasActiveVersions shouldBe nonRetiredVersionsAvailable
        api.statusSortedActiveVersions shouldEqual expectedVersions
      }
    }
  }

  "APIDefinition.retiredVersions" should {

    val v01Retired = APIVersion("0.1", None, RETIRED, Seq.empty)
    val v1Retired = APIVersion("1.0", None, RETIRED, Seq.empty)
    val v2Deprecated = APIVersion("2.0", None, DEPRECATED, Seq.empty)
    val v3Published = APIVersion("3.0", None, STABLE, Seq.empty)
    val v4Prototyped = APIVersion("4.0", None, BETA, Seq.empty)

    val scenarios = Table(
      ("Versions", "Expected retired versions"),
      (Seq(v01Retired, v1Retired, v2Deprecated, v3Published, v4Prototyped), Seq(v01Retired, v1Retired)),
      (Seq(v01Retired, v3Published), Seq(v01Retired)),
      (Seq(v2Deprecated, v3Published, v4Prototyped), Nil)
    )

    "return the retired versions" in {
      forAll(scenarios) { (versions, expectedRetiredVersions) =>
        val api = APIDefinition("serviceName", "name", "description", "context", None, None, versions)
        api.retiredVersions shouldBe expectedRetiredVersions
      }
    }
  }

  "APIDefinition.groupedByCategory" should {
    "group definitions by category when each definition has a single category" in {
      val api1 = anApiDefinition("name1", categories = Some(Seq(CUSTOMS)))
      val api2 = anApiDefinition("name2", categories = Some(Seq(PAYE)))
      val expected = Map(
        CUSTOMS -> Seq(api1),
        PAYE -> Seq(api2))

      val result = APIDefinition.groupedByCategory(Seq(api1, api2), Seq.empty)

      result shouldBe expected
    }

    "group definitions by the categories in the API definition into each specified category when a definition has defined categories" in {
      val api1 = anApiDefinition("name1", categories = Some(Seq(CUSTOMS, VAT)))
      val api2 = anApiDefinition("name2", categories = Some(Seq(PAYE, VAT)))
      val categoryMap = Map(
        "name1" -> Seq(INCOME_TAX_MTD),
        "name2" -> Seq(CORPORATION_TAX))
      val expected = Map(
        CUSTOMS -> Seq(api1),
        PAYE -> Seq(api2),
        VAT -> Seq(api1, api2))

      val result = APIDefinition.groupedByCategory(Seq(api1, api2), Seq.empty, categoryMap)

      result shouldBe expected
    }

    "group definitions into 'Other' when the definition has no categories and no matching context in the category map" in {
      val api1 = anApiDefinition("name1")
      val api2 = anApiDefinition("name2")
      val categoryMap = Map("name3" -> Seq(CUSTOMS))
      val expected = Map(OTHER -> Seq(api1, api2))

      val result = APIDefinition.groupedByCategory(Seq(api1, api2), Seq.empty, categoryMap)

      result shouldBe expected
    }

    "group definitions by the category map when no categories specified in the definition and there are matching contexts in the category map" in {
      val api1 = anApiDefinition("name1")
      val api2 = anApiDefinition("name2")
      val categoryMap = Map(
        "name1" -> Seq(CUSTOMS, VAT),
        "name2" -> Seq(PAYE, VAT))

      val expected = Map(
        CUSTOMS -> Seq(api1),
        PAYE -> Seq(api2),
        VAT -> Seq(api1, api2))

      val result = APIDefinition.groupedByCategory(Seq(api1, api2), Seq.empty, categoryMap)

      result shouldBe expected
    }

    "include XML API definitions" in {
      val api1 = anApiDefinition("name1")
      val api2 = anApiDefinition("name2")
      val xmlApi1 = anApiDefinition("xmlName1", isXmlApi = Some(true))
      val xmlApi2 = anApiDefinition("xmlName2", isXmlApi = Some(true))
      val categoryMap = Map(
        "name1" -> Seq(CUSTOMS, VAT),
        "name2" -> Seq(PAYE, VAT),
        "xmlName1" -> Seq(PAYE),
        "xmlName2" -> Seq(VAT))

      val expected = Map(
        CUSTOMS -> Seq(api1),
        PAYE -> Seq(api2, xmlApi1),
        VAT -> Seq(api1, api2, xmlApi2))

      val result = APIDefinition.groupedByCategory(Seq(api1, api2), Seq(xmlApi1, xmlApi2), categoryMap)

      result shouldBe expected
    }

    "include REST and Test Support APIS" in {
      val restApi = anApiDefinition("restApi")
      val testSupportApi = anApiDefinition("testSupportApi", isTestSupport = Some(true))
      val categoryMap = Map(
        "restApi" -> Seq(CUSTOMS),
        "testSupportApi" -> Seq(CUSTOMS))
      val expected = Map(CUSTOMS -> Seq(restApi, testSupportApi))

      val result = APIDefinition.groupedByCategory(Seq(restApi, testSupportApi), Seq.empty, categoryMap)

      result shouldBe expected
    }

    "include XML and Test Support APIS" in {
      val xmlApi = anApiDefinition("xmlApi", isXmlApi = Some(true))
      val testSupportApi = anApiDefinition("testSupportApi", isTestSupport = Some(true))
      val categoryMap = Map(
        "xmlApi" -> Seq(CUSTOMS),
        "testSupportApi" -> Seq(CUSTOMS))
      val expected = Map(CUSTOMS -> Seq(testSupportApi, xmlApi))

      val result = APIDefinition.groupedByCategory(Seq(testSupportApi), Seq(xmlApi), categoryMap)

      result shouldBe expected
    }

    "filter out categories without REST or XML APIs" in {
      val testSupportApi = anApiDefinition("testSupportApi", isTestSupport = Some(true))
      val categoryMap = Map("name1" -> Seq(CUSTOMS, VAT))

      val result = APIDefinition.groupedByCategory(Seq(testSupportApi), Seq.empty, categoryMap)

      result shouldBe Map.empty
    }

    def anApiDefinition(name: String, categories: Option[Seq[APICategory]] = None, isTestSupport: Option[Boolean] = None, isXmlApi: Option[Boolean] = None) =
      APIDefinition("serviceName", name, "description", "context", None, isTestSupport, Seq(APIVersion("1.0", None, STABLE, Seq.empty)), categories, isXmlApi)
  }

  "decoratedUriPattern" should {
    case class Scenario(outputUriPattern: String, inputUriPattern: String, inputParameters: Option[Seq[Parameter]] = None)

    val mandatory = Parameter("mandatory", required = true)
    val optional = Parameter("optional", required = false)
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
      Endpoint("Get Today's Date", uriPattern, HttpMethod.GET, parameters)
    }

  }

}
