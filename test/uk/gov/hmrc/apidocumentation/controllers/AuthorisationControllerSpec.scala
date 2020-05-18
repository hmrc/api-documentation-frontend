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

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.apidocumentation.views.html._
import uk.gov.hmrc.apidocumentation.controllers.utils.{NavigationServiceMock, PageRenderVerification}

class AuthorisationControllerSpec extends CommonControllerBaseSpec with PageRenderVerification {
  trait Setup extends NavigationServiceMock {
    val authorisationView = app.injector.instanceOf[AuthorisationView]
    val authorisation2SVView = app.injector.instanceOf[Authorisation2SVView]
    val authorisationAppRestrictedEndpointsView = app.injector.instanceOf[AuthorisationAppRestrictedEndpointsView]
    val authorisationOpenAccessEndpointsView = app.injector.instanceOf[AuthorisationOpenAccessEndpointsView]
    val authorisationUserRestrictedEndpointsView = app.injector.instanceOf[AuthorisationUserRestrictedEndpointsView]
    val credentialsView = app.injector.instanceOf[CredentialsView]

    val authorisationController = new AuthorisationController(  mcc,
                                                                navigationService,
                                                                authorisationView,
                                                                authorisation2SVView ,
                                                                authorisationAppRestrictedEndpointsView ,
                                                                authorisationOpenAccessEndpointsView ,
                                                                authorisationUserRestrictedEndpointsView,
                                                                credentialsView)
  }

  "display the authorisation page" in new Setup {
    verifyPageRendered(pageTitle("Authorisation"))(authorisationController.authorisationPage()(request))
  }

  "display the authorisation credentials page" in new Setup {
    verifyPageRendered(pageTitle("Credentials"))(authorisationController.authorisationCredentialsPage()(request))
   }
}
