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

package uk.gov.hmrc.apidocumentation.controllers

import scala.concurrent.Future

import play.api.mvc._
import play.api.test.Helpers._
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ServiceName
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApiVersionNbr

class RedirectControllerSpec extends CommonControllerBaseSpec {

  trait Setup {

    val underTest = new RedirectController(mcc)

    def verifyPageRedirected(actualPage: Future[Result], expectedUrl: String): Unit = {
      status(actualPage) shouldBe 301
      headers(actualPage).get("Location") shouldBe Some(expectedUrl)
    }
  }

  "RedirectController" should {
    "redirect to the index page" in new Setup {
      verifyPageRedirected(
        underTest.redirectToDocumentationIndexPage(None)(request),
        "/api-documentation/docs/api"
      )
    }

    "redirect to the service resource page" in new Setup {
      verifyPageRedirected(
        underTest.redirectToApiDocumentationPage(
          ServiceName("my-service"),
          ApiVersionNbr("1.0"),
          "my-endpoint",
          None
        )(request),
        "/api-documentation/docs/api/service/my-service/1.0"
      )

      verifyPageRedirected(
        underTest.redirectToApiDocumentationPage(
          ServiceName("my-other-service"),
          ApiVersionNbr("7.3"),
          "my-other-endpoint",
          None
        )(request),
        "/api-documentation/docs/api/service/my-other-service/7.3"
      )
    }

    "redirect from the legacy fraud prevention page to the fraud prevention guide" in new Setup {
      verifyPageRedirected(
        underTest.redirectToFraudPreventionGuide()(request),
        "/guides/fraud-prevention"
      )
    }
  }
}
