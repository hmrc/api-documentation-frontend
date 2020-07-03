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

package uk.gov.hmrc.apidocumentation.models

import uk.gov.hmrc.apidocumentation.models.wiremodel._

case class ViewModel(
  title: String,
  version: String,
  deprecationMessage: Option[String],
  documentationItems: List[DocumentationItem],
  resourceGroups: List[ResourceGroup2],
  types: List[TypeDeclaration2],
  isFieldOptionalityKnown: Boolean,
  relationships: Map[HmrcResource, Option[HmrcResource]]
)

object ViewModel {
  def apply(wireModel: WireModel): ViewModel = {

    val allResources: List[HmrcResource] = wireModel.resourceGroups.flatMap(_.resources)

    val parentChildRelationships: Map[HmrcResource, Option[HmrcResource]] = {
      val startWithoutParents: Map[HmrcResource, Option[HmrcResource]] = allResources.map(r => (r->None)).toMap
      allResources.map { p =>
        p.children.map { c =>
          (c -> Option(p))
        }.toMap
      }
      .foldLeft(startWithoutParents)( (m1, m2) => m1 ++ m2)
    }

    ViewModel(
      wireModel.title,
      wireModel.version,
      wireModel.deprecationMessage,
      wireModel.documentationItems,
      wireModel.resourceGroups,
      wireModel.types,
      wireModel.isFieldOptionalityKnown,
      relationships = parentChildRelationships
   )
  }
}
