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

package uk.gov.hmrc.apidocumentation

import akka.stream.Materializer
import javax.inject.{Inject, Singleton}
import play.api.http.DefaultHttpFilters
import play.api.mvc._
import uk.gov.hmrc.apidocumentation.controllers._
import uk.gov.hmrc.play.bootstrap.filters.FrontendFilters

import scala.concurrent.{ExecutionContext, Future}

class Filters @Inject()(defaultFilters: FrontendFilters, sessionRedirectFilter: SessionRedirectFilter)
    extends DefaultHttpFilters(defaultFilters.filters :+ sessionRedirectFilter: _*)

@Singleton
class SessionRedirectFilter @Inject()(implicit override val mat: Materializer,
                            exec: ExecutionContext) extends Filter {

  private val classesToReWrite = List(
    classOf[ApiDocumentationController],
    classOf[DocumentationController],
    classOf[AuthorisationController],
    classOf[TestingPagesController],
    classOf[HelpPagesController]
    )

  private val rewriteControllers = classesToReWrite.map(_.getCanonicalName)

  override def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    nextFilter(requestHeader).map { result =>
      val root = "/api-documentation"
      val routePattern = requestHeader.tags.getOrElse("ROUTE_PATTERN", root)
      val controllerName = requestHeader.tags.getOrElse("ROUTE_CONTROLLER", "")

      if (rewriteControllers.contains(controllerName)) {
        val newSession = if (routePattern == root) {
          requestHeader.session - "access_uri"
        } else {
          requestHeader.session + ("access_uri" -> requestHeader.uri)
        }

        result.withSession(newSession)
      } else {
        result
      }
    }
  }
}
