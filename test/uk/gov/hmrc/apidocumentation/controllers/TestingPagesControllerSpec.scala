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

import play.api.test.Helpers._
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.mocks.services.NavigationServiceMock
import uk.gov.hmrc.apidocumentation.controllers.utils.PageRenderVerification
import uk.gov.hmrc.apidocumentation.views.html._

import scala.concurrent.ExecutionContext.Implicits.global

class TestingPagesControllerSpec extends CommonControllerBaseSpec with PageRenderVerification {

  trait Setup extends NavigationServiceMock {
    implicit val appConfig                 = app.injector.instanceOf[ApplicationConfig]
    val testingView                        = app.injector.instanceOf[TestingView]
    val testUsersDataStatefulBehaviourView = app.injector.instanceOf[TestUsersDataStatefulBehaviourView]

    val testingPages = new TestingPagesController(navigationService, mcc, testingView, testUsersDataStatefulBehaviourView)
  }

  "TestingPagesController" should {
    "display the testing page" in new Setup {
      verifyPageRendered(pageTitle("Testing in the sandbox"))(testingPages.testingPage()(request))
    }

    "redirect to the test users test data and stateful behaviour page" in new Setup {
      import play.api.http.Status.MOVED_PERMANENTLY

      val result = testingPages.testingStatefulBehaviourPage()(request)
      status(result) shouldBe MOVED_PERMANENTLY
      headers(result).get("Location") shouldBe Some("/api-documentation/docs/testing/test-users-test-data-stateful-behaviour")
    }

    "display the test users stateful behaviour page" in new Setup {
      verifyPageRendered(pageTitle("Test users, test data and stateful behaviour"))(testingPages.testUsersDataStatefulBehaviourPage()(request))
    }
  }
}
