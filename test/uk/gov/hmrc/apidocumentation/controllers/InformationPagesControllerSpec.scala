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

import play.api.http.Status._
import uk.gov.hmrc.apidocumentation.views.html.CookiesView

class InformationPagesControllerSpec extends CommonControllerBaseSpec {

  trait Setup {
    val cookiesView = app.injector.instanceOf[CookiesView]

    val informationPages = new InformationPagesController(mcc,cookiesView)
  }

  "InformationPagesController" should {

    "display the cookies page" in new Setup {
      val actualPageFuture = informationPages.cookiesPage()(request)
      val actualPage = await(actualPageFuture)
      status(actualPage) shouldBe OK
      bodyOf(actualPage) should include("Cookies")
      bodyOf(actualPage) should include(pageTitle("Cookies"))
    }
  }
}
