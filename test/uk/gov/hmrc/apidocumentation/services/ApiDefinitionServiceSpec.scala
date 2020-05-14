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

import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.apidocumentation.connectors.ApiDefinitionConnector
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.http.metrics.NoopApiMetrics
import uk.gov.hmrc.play.test.{UnitSpec}
import uk.gov.hmrc.apidocumentation.utils.{ApiDefinitionConnectorMockingHelper, ApiDefinitionTestDataHelper}

import scala.concurrent.ExecutionContext.Implicits.global

class ApiDefinitionServiceSpec extends UnitSpec
  with MockitoSugar
  with ScalaFutures
  with ApiDefinitionTestDataHelper {

  trait LocalSetup extends ApiDefinitionConnectorMockingHelper {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val loggedInUserEmail = "3rdparty@example.com"

    val connector = mock[ApiDefinitionConnector]

    val underTest = new ApiDefinitionService(connector, new NoopApiMetrics)

  }

  "fetchAllDefinitions with user session handling" should {

    "fetch all APIs if there is no user logged in" in new LocalSetup {
      whenFetchAllDefinitions(connector)(apiDefinition("gregorian-calendar"), apiDefinition("roman-calendar"))

      val result = await(underTest.fetchAllDefinitions(None))

      result.size shouldBe 2
      result.map(_.name) shouldBe Seq("gregorian-calendar", "roman-calendar")
    }

    "fetch APIs for user email if a user is logged in" in new LocalSetup {
      whenFetchAllDefinitionsWithEmail(connector)(loggedInUserEmail)(apiDefinition("gregorian-calendar"), apiDefinition("roman-calendar"))

      val result = await(underTest.fetchAllDefinitions(Some(loggedInUserEmail)))

      result.size shouldBe 2
      result.map(_.name) shouldBe Seq("gregorian-calendar", "roman-calendar")
    }
  }

  "fetchExtendedDefinition with user session handling" should {

    "fetch a single API if there is no user logged in" in new LocalSetup {
      whenFetchExtendedDefinition(connector)("buddist-calendar")(extendedApiDefinition("buddist-calendar"))

      val result = await(underTest.fetchExtendedDefinition("buddist-calendar", None))

      result shouldBe defined
      result.get.name shouldBe "buddist-calendar"
    }

    "fetch a single API for user email if a user is logged in" in new LocalSetup {
      whenFetchExtendedDefinitionWithEmail(connector)("buddist-calendar", loggedInUserEmail)(extendedApiDefinition("buddist-calendar"))

      val result = await(underTest.fetchExtendedDefinition("buddist-calendar", Some(loggedInUserEmail)))

      result shouldBe defined
      result.get.name shouldBe "buddist-calendar"
    }

    "reject for an unsubscribed API for user email if a user is logged in" in new LocalSetup {
      whenApiDefinitionFails(connector)(new NotFoundException("Expected unit test exception"))

      intercept[NotFoundException] {
        await(underTest.fetchExtendedDefinition("buddist-calendar", Some(loggedInUserEmail)))
      }
    }
  }
}



