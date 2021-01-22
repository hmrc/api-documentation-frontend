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

package unit.uk.gov.hmrc.apidocumentation

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{RequestHeader, Result, Session}
import play.api.test.FakeRequest
import uk.gov.hmrc.apidocumentation.SessionRedirectFilter
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}

class FiltersSpec(implicit ec: ExecutionContext) extends UnitSpec with MockitoSugar {
  trait Setup {
    implicit val sys = ActorSystem("FiltersSpec")
    implicit val mat: Materializer = ActorMaterializer()

    val mockResult = mock[Result]
    when(mockResult.withSession(any[Session])).thenReturn(mockResult)

    val filter = new SessionRedirectFilter()
    val nextFilter = (rh: RequestHeader) => Future.successful(mockResult)
    val defaultSession = Seq("authToken" -> "AUTH_TOKEN")
    val rootPath = "/api-documentation"
  }

  "SessionRedirectFilter" should {
    "save the current uri in the session when the path is for a documentation page" in new Setup {
      val controller = "uk.gov.hmrc.apidocumentation.controllers.DocumentationController"
      val path = s"$rootPath/docs/api"
      implicit val requestHeader = FakeRequest("GET", path)
        .withSession(defaultSession: _*)
        .withTag("ROUTE_CONTROLLER", controller)
        .withTag("ROUTE_PATTERN", path)

      val result = await(filter.apply(nextFilter)(requestHeader))

      verify(mockResult).withSession(meq(Session(defaultSession.toMap + ("access_uri" -> path))))
    }

    "remove the current uri in the session when the path is for the index page" in new Setup {
      val controller = "uk.gov.hmrc.apidocumentation.controllers.DocumentationController"
      val path = rootPath
      implicit val requestHeader = FakeRequest("GET", path)
        .withSession(defaultSession ++ Seq("access_uri" -> path): _*)
        .withTag("ROUTE_CONTROLLER", controller)
        .withTag("ROUTE_PATTERN", path)

      val result = await(filter.apply(nextFilter)(requestHeader))

      verify(mockResult).withSession(meq(Session(defaultSession.toMap)))
    }

    "not add the current uri to the session when the path is not for a documentation page" in new Setup {
      val controller = "controllers.AssetsController"
      val path = s"$rootPath/assets/main.js"
      implicit val requestHeader = FakeRequest("GET", path)
        .withSession(defaultSession: _*)
        .withTag("ROUTE_CONTROLLER", controller)
        .withTag("ROUTE_PATTERN", path)

      val result = await(filter.apply(nextFilter)(requestHeader))

      verify(mockResult, never).withSession(any[Session])
    }

    "not add the current uri to the session when the request is not tagged with the ROUTE_CONTROLLER or ROUTE_PATTERN" in new Setup {
      val controller = "controllers.AssetsController"
      val path = s"$rootPath/assets/main.js"
      implicit val requestHeader = FakeRequest("OPTIONS", path).withSession(defaultSession: _*)

      val result = await(filter.apply(nextFilter)(requestHeader))

      verify(mockResult, never).withSession(any[Session])
    }
  }
}
