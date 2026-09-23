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

package uk.gov.hmrc.apidocumentation.models

import play.api.libs.json.Format
import uk.gov.hmrc.apiplatform.modules.common.domain.services.SimpleEnumJsonFormatting

import uk.gov.hmrc.apidocumentation.models.DocumentationLabel

enum DocumentationTypeFilter {
  /* The order of the following declarations is important since it defines the ordering of the enumeration.
   * Be very careful when changing this, code may be relying on certain values being larger/smaller than others. */
  case Api, RoadmapAndServiceGuide, TestSupportApi

  def displayName: String = DocumentationTypeFilter.displayName(this)
  def modifier: String    = DocumentationTypeFilter.modifier(this)
}

object DocumentationTypeFilter {

  def displayName(label: DocumentationTypeFilter): String = label match {
    case RoadmapAndServiceGuide => " Service guides and roadmaps"
    case Api                    => "APIs"
    case TestSupportApi         => "Test Support API"
  }

  def modifier(label: DocumentationTypeFilter): String = label match {
    case RoadmapAndServiceGuide => "roadmap-serviceguides"
    case Api                    => "api"
    case TestSupportApi         => "test-support-api"
  }

  def byLabel(label: DocumentationLabel): DocumentationTypeFilter = label match {
    case DocumentationLabel.RestApi        => DocumentationTypeFilter.Api
    case DocumentationLabel.XmlApi         => DocumentationTypeFilter.Api
    case DocumentationLabel.ServiceGuide   => DocumentationTypeFilter.RoadmapAndServiceGuide
    case DocumentationLabel.Roadmap        => DocumentationTypeFilter.RoadmapAndServiceGuide
    case DocumentationLabel.TestSupportApi => DocumentationTypeFilter.TestSupportApi
  }

  def apply(text: String): Option[DocumentationTypeFilter] = DocumentationTypeFilter.values.find(_.toString.toUpperCase == text.toUpperCase())

  def unsafeApply(text: String): DocumentationTypeFilter =
    DocumentationTypeFilter.values.find(_.toString.toUpperCase == text.toUpperCase()).getOrElse(throw new RuntimeException(s"$text is not a valid DocumentTypeFilter"))

  implicit val ordering: Ordering[DocumentationTypeFilter] = Ordering.by(_.toString)

  implicit val formats: Format[DocumentationTypeFilter] = SimpleEnumJsonFormatting.screamingSnakeCaseFormatFor[DocumentationTypeFilter]("DocumentationTypeFilter", apply)
}
