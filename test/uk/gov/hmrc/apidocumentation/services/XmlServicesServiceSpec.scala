/*
 * Copyright 2021 HM Revenue & Customs
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

import uk.gov.hmrc.apidocumentation.connectors.ApiPlatformMicroserviceConnector
import uk.gov.hmrc.apidocumentation.utils.ApiDefinitionTestDataHelper
import uk.gov.hmrc.apidocumentation.mocks.connectors.ApiPlatformMicroserviceConnectorMockingHelper
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.http.metrics.NoopApiMetrics
import uk.gov.hmrc.apidocumentation.common.utils.AsyncHmrcSpec

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.apidocumentation.models.UserId
import uk.gov.hmrc.apidocumentation.models.UuidIdentifier
import uk.gov.hmrc.apidocumentation.connectors.XmlServicesConnector
import scala.concurrent.Future

class XmlServicesServiceSpec extends AsyncHmrcSpec {

  trait LocalSetup extends ApiPlatformMicroserviceConnectorMockingHelper{
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val loggedInUserEmail = "3rdparty@example.com"
    val loggedInUserId = UuidIdentifier(UserId.random)

    val xmlServicesConnector = mock[XmlServicesConnector]

    val underTest = new XmlServicesService(xmlServicesConnector, new NoopApiMetrics)

  }

  "fetchAllXmlApis" should {

    "fetch all APIs if there is no user logged in" in new LocalSetup {
      when(xmlServicesConnector.fetchAllXmlApis()).thenReturn(Future.successful(Seq.empty))
      val result = await(underTest.fetchAllXmlApis())

      result.size shouldBe 0
    }

  }

}
