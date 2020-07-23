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

import uk.gov.hmrc.apidocumentation.models.apispecification.ApiSpecification
import uk.gov.hmrc.apidocumentation.models.apispecification._

case class ViewModel(
  apiSpecification: ApiSpecification,
  relationships: Map[Resource, Option[Resource]]
) {
  lazy val title: String = apiSpecification.title
  lazy val version: String = apiSpecification.version
  lazy val deprecationMessage: Option[String] = apiSpecification.deprecationMessage
  lazy val documentationItems: List[DocumentationItem] = apiSpecification.documentationItems
  lazy val resourceGroups: List[ResourceGroup] = apiSpecification.resourceGroups
  lazy val types: List[TypeDeclaration] = apiSpecification.types
  lazy val isFieldOptionalityKnown: Boolean = apiSpecification.isFieldOptionalityKnown
}

object ViewModel {
  def apply(apiSpecification: ApiSpecification): ViewModel = {

    val allResources: List[Resource] = apiSpecification.resourceGroups.flatMap(_.resources)

    val parentChildRelationships: Map[Resource, Option[Resource]] = {
      val startWithoutParents: Map[Resource, Option[Resource]] = allResources.map(r => (r->None)).toMap
      allResources.map { p =>
        p.children.map { c =>
          (c -> Option(p))
        }.toMap
      }
      .foldLeft(startWithoutParents)( (m1, m2) => m1 ++ m2)
    }

    ViewModel(
      apiSpecification,
      relationships = parentChildRelationships
   )
  }
}
