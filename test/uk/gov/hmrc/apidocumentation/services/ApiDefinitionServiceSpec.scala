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

package uk.gov.hmrc.apidocumentation.services

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.reflect.ClassTag

import org.apache.pekko.Done

import play.api.cache._
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ServiceName
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.http.metrics.common.NoopApiMetrics

import uk.gov.hmrc.apidocumentation.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.apidocumentation.connectors.ApiPlatformMicroserviceConnector
import uk.gov.hmrc.apidocumentation.mocks.connectors.ApiPlatformMicroserviceConnectorMockingHelper
import uk.gov.hmrc.apidocumentation.models.{UserId, UuidIdentifier}
import uk.gov.hmrc.apidocumentation.utils.ApiDefinitionTestDataHelper

class ApiDefinitionServiceSpec extends AsyncHmrcSpec
    with ApiDefinitionTestDataHelper {

  val doNothingCache = new AsyncCacheApi {
    def set(key: String, value: Any, expiration: Duration = Duration.Inf): Future[Done] = Future.successful(Done)
    def remove(key: String): Future[Done]                                               = Future.successful(Done)

    def getOrElseUpdate[A: ClassTag](key: String, expiration: Duration = Duration.Inf)(orElse: => Future[A]): Future[A] = orElse

    def get[T: ClassTag](key: String): Future[Option[T]] = Future.successful(None)

    def removeAll(): Future[Done] = Future.successful(Done)
  }

  trait LocalSetup extends ApiPlatformMicroserviceConnectorMockingHelper {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val loggedInUserEmail          = "3rdparty@example.com"
    val loggedInUserId             = UuidIdentifier(UserId.random)
    val serviceName                = ServiceName("buddist-calendar")

    val apiPlatformMicroserviceConnector = mock[ApiPlatformMicroserviceConnector]
    val underTest                        = new ApiDefinitionService(doNothingCache, apiPlatformMicroserviceConnector, new NoopApiMetrics)

  }

  "fetchAllDefinitions with user session handling" should {

    "fetch all APIs if there is no user logged in" in new LocalSetup {
      whenFetchAllDefinitions(apiPlatformMicroserviceConnector)(apiDefinition("gregorian-calendar"), apiDefinition("roman-calendar"))

      val result = await(underTest.fetchAllDefinitions(None))

      result.size shouldBe 2
      result.map(_.name) shouldBe Seq("gregorian-calendar", "roman-calendar")
    }

    "fetch APIs for user email if a user is logged in" in new LocalSetup {
      whenFetchAllDefinitionsWithEmail(apiPlatformMicroserviceConnector)(loggedInUserId)(apiDefinition("gregorian-calendar"), apiDefinition("roman-calendar"))

      val result = await(underTest.fetchAllDefinitions(Some(loggedInUserId)))

      result.size shouldBe 2
      result.map(_.name) shouldBe Seq("gregorian-calendar", "roman-calendar")
    }
  }

  "fetchExtendedDefinition with user session handling" should {

    "fetch a single API if there is no user logged in" in new LocalSetup {
      whenFetchExtendedDefinition(apiPlatformMicroserviceConnector)(serviceName)(extendedApiDefinition(serviceName.value))

      val result = await(underTest.fetchExtendedDefinition(serviceName, None))

      result shouldBe defined
      result.get.serviceName shouldBe serviceName
    }

    "fetch a single API for user email if a user is logged in" in new LocalSetup {
      whenFetchExtendedDefinitionWithEmail(apiPlatformMicroserviceConnector)(serviceName, loggedInUserId)(extendedApiDefinition("buddist-calendar"))

      val result = await(underTest.fetchExtendedDefinition(serviceName, Some(loggedInUserId)))

      result shouldBe defined
      result.get.serviceName shouldBe serviceName
    }

    "reject for an unsubscribed API for user email if a user is logged in" in new LocalSetup {
      whenFetchExtendedDefinitionFails(apiPlatformMicroserviceConnector)(new NotFoundException("Expected unit test exception"))

      intercept[NotFoundException] {
        await(underTest.fetchExtendedDefinition(serviceName, Some(loggedInUserId)))
      }
    }
  }
}
