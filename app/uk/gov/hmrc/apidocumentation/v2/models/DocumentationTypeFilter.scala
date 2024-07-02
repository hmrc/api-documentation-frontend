/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.apidocumentation.v2.models

import scala.collection.immutable.ListSet

import play.api.libs.json.Format
import uk.gov.hmrc.apiplatform.modules.common.domain.services.SealedTraitJsonFormatting

import uk.gov.hmrc.apidocumentation.models.DocumentationLabel
import uk.gov.hmrc.apidocumentation.models.DocumentationLabel._

sealed trait DocumentationTypeFilter {
  lazy val displayName: String = DocumentationTypeFilter.displayName(this)
  lazy val modifier: String    = DocumentationTypeFilter.modifier(this)
}

object DocumentationTypeFilter {
  case object ROADMAPANDSERVICEGUIDE extends DocumentationTypeFilter
  case object API                    extends DocumentationTypeFilter
  case object TEST_SUPPORT_API       extends DocumentationTypeFilter

  def displayName(label: DocumentationTypeFilter): String = label match {
    case ROADMAPANDSERVICEGUIDE => " Service guides and roadmaps"
    case API                    => "APIs"
    case TEST_SUPPORT_API       => "Test Support API"
  }

  def modifier(label: DocumentationTypeFilter): String = label match {
    case ROADMAPANDSERVICEGUIDE => "roadmap-serviceguides"
    case API                    => "api"
    case TEST_SUPPORT_API       => "test-support-api"
  }

  def byLabel(label: DocumentationLabel): DocumentationTypeFilter = label match {
    case REST_API                            => API
    case XML_API                             => API
    case SERVICE_GUIDE                       => ROADMAPANDSERVICEGUIDE
    case ROADMAP                             => ROADMAPANDSERVICEGUIDE
    case DocumentationLabel.TEST_SUPPORT_API => TEST_SUPPORT_API
  }

  /* The order of the following declarations is important since it defines the ordering of the enumeration.
   * Be very careful when changing this, code may be relying on certain values being larger/smaller than others. */
  val values: ListSet[DocumentationTypeFilter] = ListSet(API, ROADMAPANDSERVICEGUIDE, TEST_SUPPORT_API)

  def apply(text: String): Option[DocumentationTypeFilter] = DocumentationTypeFilter.values.find(_.toString.toUpperCase == text.toUpperCase())

  def unsafeApply(text: String): DocumentationTypeFilter =
    DocumentationTypeFilter.values.find(_.toString.toUpperCase == text.toUpperCase()).getOrElse(throw new RuntimeException(s"$text is not a valid DocumentTypeFilter"))

  implicit val ordering: Ordering[DocumentationTypeFilter] = Ordering.by(_.toString)

  implicit val formats: Format[DocumentationTypeFilter] = SealedTraitJsonFormatting.createFormatFor[DocumentationTypeFilter]("DocumentationTypeFilter", apply)
}
