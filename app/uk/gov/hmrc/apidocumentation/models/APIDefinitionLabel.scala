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

import scala.collection.immutable.ListSet

import play.api.libs.json.Format
import uk.gov.hmrc.apiplatform.modules.common.domain.services.SealedTraitJsonFormatting

sealed trait DocumentationLabel {
  lazy val displayName: String = DocumentationLabel.displayName(this)
  lazy val modifier: String    = DocumentationLabel.modifier(this)
}

object DocumentationLabel {
  case object ROADMAP          extends DocumentationLabel
  case object SERVICE_GUIDE    extends DocumentationLabel
  case object REST_API         extends DocumentationLabel
  case object TEST_SUPPORT_API extends DocumentationLabel
  case object XML_API          extends DocumentationLabel

  def displayName(label: DocumentationLabel): String = label match {
    case ROADMAP          => "Roadmap"
    case SERVICE_GUIDE    => "Service Guide"
    case REST_API         => "REST API"
    case TEST_SUPPORT_API => "Test Support API"
    case XML_API          => "XML API"
  }

  def modifier(label: DocumentationLabel): String = label match {
    case ROADMAP          => "roadmap"
    case SERVICE_GUIDE    => "service-guide"
    case REST_API         => "rest"
    case TEST_SUPPORT_API => "test"
    case XML_API          => "xml"
  }

  /* The order of the following declarations is important since it defines the ordering of the enumeration.
   * Be very careful when changing this, code may be relying on certain values being larger/smaller than others. */
  val values: ListSet[DocumentationLabel] = ListSet(REST_API, ROADMAP, SERVICE_GUIDE, TEST_SUPPORT_API, XML_API)

  def apply(text: String): Option[DocumentationLabel] = DocumentationLabel.values.find(_.toString.toUpperCase == text.toUpperCase())

  implicit val ordering: Ordering[DocumentationLabel] = Ordering.by(_.toString)

  implicit val formats: Format[DocumentationLabel] = SealedTraitJsonFormatting.createFormatFor[DocumentationLabel]("DocumentationLabel", apply)
}
