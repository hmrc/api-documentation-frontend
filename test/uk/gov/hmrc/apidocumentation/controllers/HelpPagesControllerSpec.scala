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
import play.api.mvc.Result
import scala.concurrent.Future
import uk.gov.hmrc.apidocumentation.views.html.{CookiesView, PrivacyView, TermsAndConditionsView}

class HelpPagesControllerSpec extends CommonControllerBaseSpec {

  trait Setup {
    val cookiesView = app.injector.instanceOf[CookiesView]
    val privacyView = app.injector.instanceOf[PrivacyView]
    val termsAndConditionsView = app.injector.instanceOf[TermsAndConditionsView]

    val helpPages = new HelpPagesController(mcc, cookiesView, privacyView, termsAndConditionsView)
  }

  def isPresentAndCorrect(includesText: String, title: String)(fResult: Future[Result]): Unit = {
    val result = await(fResult)
    status(result) shouldBe OK
    bodyOf(result) should include(includesText)
    bodyOf(result) should include(pageTitle(title))
  }

  "helpPagesController" should {

    "display the cookies page" in new Setup {
      isPresentAndCorrect("Cookies","Cookies")(helpPages.cookiesPage()(request))
    }

    "display the privacy policy page" in new Setup {
      isPresentAndCorrect("Privacy","Privacy policy")(helpPages.privacyPolicyPage()(request))
    }

    "display the terms and conditions page" in new Setup {
      isPresentAndCorrect("Terms","Terms and conditions")(helpPages.termsAndConditionsPage()(request))
    }
  }
}
