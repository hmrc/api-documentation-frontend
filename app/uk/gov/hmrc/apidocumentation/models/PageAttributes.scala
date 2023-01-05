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

import play.twirl.api.Html

/**
  * Group all page attributes that need to be passed to views into a single object,
  * to aid extensibility.
  */
case class PageAttributes(
  title: String,
  breadcrumbs: Breadcrumbs,
  headerLinks: Seq[NavLink] = Seq.empty,
  sidebarLinks: Seq[SidebarLink] = Seq.empty,
  contentHeader: Option[Html] = None
)
