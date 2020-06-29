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

import org.raml.v2.api.model.v10.datamodel.{ExampleSpec, TypeDeclaration}
import org.raml.v2.api.model.v10.resources.Resource
import org.raml.v2.api.model.v10.system.types.MarkdownString
import uk.gov.hmrc.apidocumentation.services.RAML
import uk.gov.hmrc.apidocumentation.views.helpers.{Annotation, GroupedResources, MethodParameters, ResourceGroup, VersionDocsVisible}

import scala.collection.JavaConverters._

case class RamlAndSchemas(raml: RAML, schemas: Map[String, JsonSchema])

case class DocumentationItem(title: String, content: String)

case class OurResource(parentResource: OurResource, uriParameters: List[TypeDeclaration2])

//TODO: Change description from MarkDown and example from ExampleSpec
case class TypeDeclaration2(name: String, required: Boolean, description: MarkdownString, example: ExampleSpec, `type`: String)

case class OurModel(
  title: String,
  version: String,
  deprecationMessage: Option[String],
  documentationItems: List[DocumentationItem],
  resourceGroups: List[ResourceGroup],
  types: List[TypeDeclaration2]
)

object OurModel {
  def apply(raml: RAML): OurModel = {

    def title: String = raml.title.value

    def version: String = raml.version.value

    def deprecationMessage: Option[String] = Annotation.optional(raml, "(deprecationMessage)")

    def documentationItems: List[DocumentationItem] = raml.documentation.asScala.toList.map(item => DocumentationItem(item.title.value, item.content.value))

    def resourceGroups: List[ResourceGroup] = GroupedResources(raml.resources.asScala).toList

    def typeDeclaration2Converter(td: TypeDeclaration): TypeDeclaration2 = TypeDeclaration2(td.name, td.required, td.description, td.example, td.`type`)

    def types: List[TypeDeclaration2] = (raml.types.asScala.toList ++ raml.uses.asScala.flatMap(_.types.asScala)).map(typeDeclaration2Converter)

    OurModel(
      title,
      version,
      deprecationMessage,
      documentationItems,
      resourceGroups,
      types
    )
  }
}