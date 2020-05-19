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
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Results
import uk.gov.hmrc.apidocumentation.connectors.DownloadConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class DownloadServiceSpec extends UnitSpec with MockitoSugar with ScalaFutures {

  val serviceName = "calendar"
  val resource = "some/resource"
  val version = "1.0"

  trait Setup {
    implicit val hc = HeaderCarrier()
    val downloadConnector = mock[DownloadConnector]
    val underTest = new DownloadService(downloadConnector)
  }

  "fetchResource" should {
    "succeed if resource is available" in new Setup {
      when(downloadConnector.fetch(serviceName, version, resource)).thenReturn(Future.successful(Results.Ok))
      val result = await(underTest.fetchResource(serviceName, version, resource))
      result.header.status shouldBe(200)
    }

    "return 404 if resource not found" in new Setup {
      when(downloadConnector.fetch(serviceName, version, resource)).thenReturn(Future.successful(Results.NotFound))
      val result = await(underTest.fetchResource(serviceName, version, resource))
      result.header.status shouldBe(404)
    }

    "propagate internal server error" in new Setup {
      when(downloadConnector.fetch(serviceName, version, resource)).thenReturn(Future.successful(Results.InternalServerError))
      val result = await(underTest.fetchResource(serviceName, version, resource))
      result.header.status shouldBe(500)
    }
  }
}
