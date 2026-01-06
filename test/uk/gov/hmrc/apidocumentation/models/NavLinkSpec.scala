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

package uk.gov.hmrc.apidocumentation.models

import uk.gov.hmrc.apiplatform.modules.common.utils.HmrcSpec
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.servicenavigation.ServiceNavigationItem

class NavLinkSpec extends HmrcSpec {

  "NavigationHelper" should {

    "return static navlinks for devhub for the getting started page" in {
      StaticNavLinks("/api-documentation/docs/using-the-hub") shouldBe
        Seq(
          ServiceNavigationItem(Text("Getting started"), "/api-documentation/docs/using-the-hub", active = true),
          ServiceNavigationItem(Text("API documentation"), "/api-documentation/docs/api"),
          ServiceNavigationItem(Text("Applications"), "/developer/applications"),
          ServiceNavigationItem(Text("Support"), "/devhub-support"),
          ServiceNavigationItem(Text("Service availability"), "https://api-platform-status.production.tax.service.gov.uk/", attributes = Map("target" -> "_blank"))
        )
    }
    "return static navlinks for devhub for the index page" in {
      StaticNavLinks("/api-documentation") shouldBe
        Seq(
          ServiceNavigationItem(Text("Getting started"), "/api-documentation/docs/using-the-hub"),
          ServiceNavigationItem(Text("API documentation"), "/api-documentation/docs/api"),
          ServiceNavigationItem(Text("Applications"), "/developer/applications"),
          ServiceNavigationItem(Text("Support"), "/devhub-support"),
          ServiceNavigationItem(Text("Service availability"), "https://api-platform-status.production.tax.service.gov.uk/", attributes = Map("target" -> "_blank"))
        )
    }
    "return static navlinks for devhub for the api page" in {
      StaticNavLinks("/api-documentation/docs/api/service/api-documentation-test-service") shouldBe
        Seq(
          ServiceNavigationItem(Text("Getting started"), "/api-documentation/docs/using-the-hub"),
          ServiceNavigationItem(Text("API documentation"), "/api-documentation/docs/api", active = true),
          ServiceNavigationItem(Text("Applications"), "/developer/applications"),
          ServiceNavigationItem(Text("Support"), "/devhub-support"),
          ServiceNavigationItem(Text("Service availability"), "https://api-platform-status.production.tax.service.gov.uk/", attributes = Map("target" -> "_blank"))
        )
    }
  }
}
