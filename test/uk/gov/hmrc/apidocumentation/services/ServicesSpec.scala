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

package uk.gov.hmrc.apidocumentation.services

import org.mockito.Mockito._
import org.raml.v2.api.model.v10.api.DocumentationItem
import org.raml.v2.api.model.v10.system.types.AnnotableStringType
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.apidocumentation.utils.ApiDefinitionTestDataHelper

import scala.collection.JavaConverters._

class ServicesSpec extends UnitSpec with MockitoSugar with ApiDefinitionTestDataHelper {
  trait Setup {
    val raml = mock[RAML]
    val (overview, versioning, errors) = (mock[DocumentationItem], mock[DocumentationItem], mock[DocumentationItem])
    val (overviewTitle, versioningTitle, errorsTitle) = (mock[AnnotableStringType], mock[AnnotableStringType], mock[AnnotableStringType])
    val documentation = List(overview, versioning, errors)

    when(overviewTitle.value).thenReturn("Overview")
    when(versioningTitle.value).thenReturn("Versioning")
    when(errorsTitle.value).thenReturn("Errors")
    when(overview.title).thenReturn(overviewTitle)
    when(versioning.title).thenReturn(versioningTitle)
    when(errors.title).thenReturn(errorsTitle)
    when(raml.documentation).thenReturn(documentation)
  }

  "documentationForVersion" should {
    "return all documentation when the version is visible" in new Setup {
      val availability = someApiAvailability()
      val visibleVersion = Some(ExtendedAPIVersion("1.0", APIStatus.STABLE, endpoints = Seq.empty, productionAvailability = availability, sandboxAvailability = availability))
      val result = raml.documentationForVersion(visibleVersion)

      result shouldBe documentation
    }

    "return only the overview documentation when the version is in private trial" in new Setup {
      val availability = someApiAvailability().asPrivate.asTrial.notAuthorised
      val overviewOnlyVersion =
        Some(
          ExtendedAPIVersion("1.0", APIStatus.STABLE, endpoints = Seq.empty, productionAvailability = availability, sandboxAvailability = availability)
        )
      val result = raml.documentationForVersion(overviewOnlyVersion)

      result shouldBe List(overview)
    }

    "return no documentation when the version is not visible" in new Setup {
      val availability = someApiAvailability().asPrivate.notTrial.notAuthorised
      val notVisibleVersion = Some(ExtendedAPIVersion("1.0", APIStatus.STABLE, endpoints = Seq.empty, productionAvailability = availability, sandboxAvailability = availability))
      val result = raml.documentationForVersion(notVisibleVersion)

      result shouldBe List.empty
    }
  }
}
