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

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.services.NavigationService
import uk.gov.hmrc.apidocumentation.views.html._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.apidocumentation.util.ApplicationLogger

@Singleton
class TestingPagesController @Inject()(
                                        val navigationService: NavigationService,
                                        mcc: MessagesControllerComponents,
                                        testingView: TestingView,
                                        testUsersDataStatefulBehaviourView: TestUsersDataStatefulBehaviourView
                                      )
                                      (implicit ec: ExecutionContext, applicationConfig: ApplicationConfig) extends FrontendController(mcc) with HeaderNavigation with ApplicationLogger with PageAttributesHelper with HomeCrumb {
  def testingPage(): Action[AnyContent] = headerNavigation { implicit request =>
    navLinks =>
      Future.successful(Ok(testingView(pageAttributes("Testing in the sandbox", routes.TestingPagesController.testingPage().url, navLinks))))
  }

  def testingStatefulBehaviourPage(): Action[AnyContent] = headerNavigation { _ =>
    _ => Future.successful(MovedPermanently(routes.TestingPagesController.testUsersDataStatefulBehaviourPage().url))
  }

  def testUsersDataStatefulBehaviourPage(): Action[AnyContent] = headerNavigation { implicit request =>
    navLinks =>
      val testUsersDataStatefulBehaviourUrl = routes.TestingPagesController.testUsersDataStatefulBehaviourPage().url
      Future.successful(Ok(testUsersDataStatefulBehaviourView(pageAttributes("Test users, test data and stateful behaviour", testUsersDataStatefulBehaviourUrl, navLinks))))
  }
}
