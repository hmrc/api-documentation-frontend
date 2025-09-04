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

class NavLinkSpec extends HmrcSpec {

  "NavigationHelper" should {

    "return static navlinks for devhub" in {
      StaticNavLinks() shouldBe
        Seq(
          NavLink("Getting started", "/api-documentation/docs/using-the-hub"),
          NavLink("API documentation", "/api-documentation/docs/api"),
          NavLink("Applications", "/developer/applications"),
          NavLink("Support", "/devhub-support"),
          NavLink("Service availability", "https://api-platform-status.production.tax.service.gov.uk/", openInNewWindow = true)
        )
    }
  }
}
