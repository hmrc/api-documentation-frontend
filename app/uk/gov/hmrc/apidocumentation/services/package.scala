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

package uk.gov.hmrc.apidocumentation

import uk.gov.hmrc.apidocumentation.models.apispecification.{ApiSpecification, DocumentationItem}
import uk.gov.hmrc.apidocumentation.models.{DocsVisibility, ExtendedAPIVersion, ViewModel}
import uk.gov.hmrc.apidocumentation.views.helpers.VersionDocsVisible

package object services {

  def versionVisibility(version: Option[ExtendedAPIVersion]): DocsVisibility.Value = version match {
    case Some(v) => VersionDocsVisible(v.visibility)
    case _       => DocsVisibility.VISIBLE
  }

  def filterForVisibility(version: Option[ExtendedAPIVersion]): (List[DocumentationItem]) => List[DocumentationItem] = (input) => {
    versionVisibility(version) match {
      case DocsVisibility.VISIBLE       => input
      case DocsVisibility.OVERVIEW_ONLY => input.filter(_.title == "Overview")
      case _                            => List.empty
    }
  }

  implicit class RicherApiSpecification(val x: ApiSpecification) {
    def documentationForVersionFilteredByVisibility(version: ExtendedAPIVersion): List[DocumentationItem] = filterForVisibility(Some(version))(x.documentationItems)
  }

  implicit class RicherViewModel(val x: ViewModel) {
    def documentationForVersion(version: Option[ExtendedAPIVersion]): List[DocumentationItem] = filterForVisibility(version)(x.documentationItems)
  }

}
