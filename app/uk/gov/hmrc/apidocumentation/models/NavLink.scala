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

import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.header.NavigationItem

case class NavLink(label: String, href: String, truncate: Boolean = false, openInNewWindow: Boolean = false, isSensitive: Boolean = false)

case object NavLink {

  implicit class NavLinkSyntax(navLink: NavLink) {

    def asNavigationItem: NavigationItem = NavigationItem(
      content = Text(navLink.label),
      href = Some(navLink.href),
      attributes = if (navLink.openInNewWindow) Map("target" -> "_blank") else Map.empty // Note: not handling `truncate` or `isSensitive` fields
    )
  }

  implicit class NavLinksSyntax(navLinks: Seq[NavLink]) {
    def asNavigationItems: Seq[NavigationItem] = navLinks.map(_.asNavigationItem)
  }
}

case class SidebarLink(label: String, href: String, subLinks: Seq[SidebarLink] = Seq.empty, showSubLinks: Boolean = false)

case object StaticNavLinks {

  def apply() = {

    Seq(
      NavLink("Getting started", "/api-documentation/docs/using-the-hub"),
      NavLink("API documentation", "/api-documentation/docs/api"),
      NavLink("Applications", "/developer/applications"),
      NavLink("Support", "/developer/support"),
      NavLink("Service Availability", "https://api-platform-status.production.tax.service.gov.uk/", openInNewWindow = true)
    )
  }
}
