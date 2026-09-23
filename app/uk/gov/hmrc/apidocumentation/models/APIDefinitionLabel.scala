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

import play.api.libs.json.Format
import uk.gov.hmrc.apiplatform.modules.common.domain.services.SimpleEnumJsonFormatting

enum DocumentationLabel {
  // These are in a specific order
  case RestApi, Roadmap, ServiceGuide, TestSupportApi, XmlApi

  def displayName: String = DocumentationLabel.displayName(this)
  def modifier: String    = DocumentationLabel.modifier(this)
}

object DocumentationLabel {

  def displayName(label: DocumentationLabel): String = label match {
    case Roadmap        => "Roadmap"
    case ServiceGuide   => "Service Guide"
    case RestApi        => "REST API"
    case TestSupportApi => "Test Support API"
    case XmlApi         => "XML API"
  }

  def modifier(label: DocumentationLabel): String = label match {
    case Roadmap        => "roadmap"
    case ServiceGuide   => "service-guide"
    case RestApi        => "rest"
    case TestSupportApi => "test"
    case XmlApi         => "xml"
  }

  def apply(text: String): Option[DocumentationLabel] = DocumentationLabel.values.find(_.toString.toUpperCase == text.toUpperCase())

  implicit val ordering: Ordering[DocumentationLabel] = Ordering.by(_.toString)

  implicit val formats: Format[DocumentationLabel] = SimpleEnumJsonFormatting.screamingSnakeCaseFormatFor[DocumentationLabel]("DocumentationLabel", apply)
}
