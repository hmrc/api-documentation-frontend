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

package uk.gov.hmrc.apidocumentation

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

import javax.inject.{Inject, Singleton}

import akka.stream.Materializer
import play.api.http.HttpFilters
import play.api.mvc._
import uk.gov.hmrc.apidocumentation.controllers.DocumentationController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Filters @Inject()(override val filters: Seq[EssentialFilter]) extends HttpFilters {

}

@Singleton
class SessionRedirectFilter @Inject()(implicit override val mat: Materializer,
                            exec: ExecutionContext) extends Filter {

  override def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    nextFilter(requestHeader).map { result =>
      if (requestHeader.tags("ROUTE_CONTROLLER") == classOf[DocumentationController].getCanonicalName) {
        result.withSession(requestHeader.session + ("access_uri" -> requestHeader.uri))
      } else {
        result
      }
    }
  }
}