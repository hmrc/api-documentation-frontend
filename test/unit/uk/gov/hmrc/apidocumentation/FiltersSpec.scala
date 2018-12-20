/*
 * Copyright 2018 HM Revenue & Customs
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

package unit.uk.gov.hmrc.apidocumentation

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito.{never, verify, when}
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.{RequestHeader, Result, Session}
import play.api.test.FakeRequest
import uk.gov.hmrc.apidocumentation.SessionRedirectFilter
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FiltersSpec extends UnitSpec with MockitoSugar with WithFakeApplication {
  trait Setup {
    implicit val sys = ActorSystem("FiltersSpec")
    implicit val mat: Materializer = ActorMaterializer()

    val mockResult = mock[Result]
    when(mockResult.withSession(any[Session])).thenReturn(mockResult)

    val filter = new SessionRedirectFilter()
    val nextFilter = (rh: RequestHeader) => Future.successful(mockResult)
  }

  "SessionRedirectFilter" should {
    "save the current uri in the session when the path is for a documentation page" in new Setup {
      val controller = "uk.gov.hmrc.apidocumentation.controllers.DocumentationController"
      val path = "/path/to/save"
      implicit val requestHeader = FakeRequest("GET", path).withTag("ROUTE_CONTROLLER", controller)

      val result = await(filter.apply(nextFilter)(requestHeader))

      verify(mockResult).withSession(meq(Session(Map(("access_uri" -> path)))))
    }

    "not add the current uri to the session when the path is not for a documentation page" in new Setup {
      val controller = "controllers.AssetsController"
      val path = "/path/to/save"
      implicit val requestHeader = FakeRequest("GET", path).withTag("ROUTE_CONTROLLER", controller)

      val result = await(filter.apply(nextFilter)(requestHeader))

      verify(mockResult, never).withSession(any[Session])
    }
  }
}
