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

package uk.gov.hmrc.apidocumentation.controllers

import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.mvc._
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class RedirectControllerSpec extends UnitSpec with GuiceOneAppPerTest {

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(("metrics.jvm", false))
      .build()

  class Setup extends ControllerCommonSetup {
    val mcc = app.injector.instanceOf[MessagesControllerComponents]

    val underTest = new RedirectController(mcc)

    def verifyPageRedirected(actualPageFuture: Future[Result], expectedUrl: String) {
      val actualPage = await(actualPageFuture)
      status(actualPage) shouldBe 301
      actualPage.header.headers.get("Location") shouldBe Some(expectedUrl)
    }
  }

  "RedirectController" should {
    "redirect to the index page" in new Setup {
      verifyPageRedirected(underTest.redirectToDocumentationIndexPage()(request), "/api-documentation/docs/api")
    }

    "redirect to the service resource page" in new Setup {
      verifyPageRedirected(
        underTest.redirectToApiDocumentationPage("my-service", "1.0", "my-endpoint")(request),
        "/api-documentation/docs/api/service/my-service/1.0"
      )


      verifyPageRedirected(
        underTest.redirectToApiDocumentationPage("my-other-service", "7.3", "my-other-endpoint")(request),
        "/api-documentation/docs/api/service/my-other-service/7.3"
      )
    }
  }
}
