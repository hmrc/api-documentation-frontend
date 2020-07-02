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
import uk.gov.hmrc.apidocumentation.services.RAML
import uk.gov.hmrc.apidocumentation.views.helpers.{Annotation, GroupedResources}

import scala.collection.JavaConverters._
import org.raml.v2.api.model.v10.methods.Method
import uk.gov.hmrc.apidocumentation.views.helpers.Val
import uk.gov.hmrc.apidocumentation.views.helpers.ResourceGroup2
import uk.gov.hmrc.apidocumentation.views.helpers.FindProperty

case class DocumentationItem(title: String, content: String)

case class SecurityScheme(`type`: String, scope: Option[String])

case class HmrcResponse(
  code: String,
  body: List[TypeDeclaration2],
  headers: List[TypeDeclaration2],
  description: Option[String])

case class HmrcMethod(
  method: String,
  displayName: String,
  body: List[TypeDeclaration2],
  headers: List[TypeDeclaration2],
  queryParameters: List[TypeDeclaration2],
  description: Option[String],
  securedBy: Option[SecurityScheme],
  responses: List[HmrcResponse],
  sandboxData: Option[String]
)

object HmrcMethod {

private val correctOrder = Map(
    "get" -> 0, "post" -> 1, "put" -> 2, "delete" -> 3,
    "head" -> 4, "patch" -> 5, "options" -> 6
  )

  def apply(method: Method): HmrcMethod = {
    val queryParameters = method.queryParameters.asScala.toList.map(TypeDeclaration2.apply)
    val headers = method.headers.asScala.toList.map(TypeDeclaration2.apply)
    val body = method.body.asScala.toList.map(TypeDeclaration2.apply)


    def fetchAuthorisation: Option[SecurityScheme] = {
      if (method.securedBy().asScala.nonEmpty) {
        method.securedBy.get(0).securityScheme.`type` match {
          case "OAuth 2.0" => Some(SecurityScheme("user", Some(Annotation(method, "(scope)"))))
          case _ => Some(SecurityScheme("application", None))
        }
      } else {
        None
      }
    }

    def responses: List[HmrcResponse] = {
      method.responses().asScala.toList.map(r => {
        HmrcResponse(
          code = r.code().value(),
          body = r.body.asScala.toList.map(TypeDeclaration2.apply),
          headers = r.headers().asScala.toList.map(TypeDeclaration2.apply),
          description = Option(r.description()).map(_.value())
        )
      })
    }

    object SafeValue {
      def apply(v: String): Option[String] = Option(v)
      def apply(v: {def value(): String}): Option[String] = Option(v).map(_.value())
    }

    def sandboxData = Annotation.optional(method, "(sandboxData)")

    HmrcMethod(
      method.method,
      method.displayName.value,
      body,
      headers,
      queryParameters,
      SafeValue(method.description),
      fetchAuthorisation,
      responses,
      sandboxData
    )
  }

  def apply(resource: Resource): List[HmrcMethod] =
    resource.methods.asScala.toList.sortWith { (left, right) =>
      (for {
        l <- correctOrder.get(left.method)
        r <- correctOrder.get(right.method)
      } yield l < r).getOrElse(false)
    }
    .map(m => HmrcMethod.apply(m))

  def apply(resource: HmrcResource): List[HmrcMethod] =
    resource.methods.sortWith { (left, right) =>
      (for {
        l <- correctOrder.get(left.method)
        r <- correctOrder.get(right.method)
      } yield l < r).getOrElse(false)
    }
}

case class Group(name: String, description: String)

case class HmrcResource(resourcePath: String, group: Option[Group], methods: List[HmrcMethod], uriParameters: List[TypeDeclaration2], relativeUri: String, displayName: String, children: List[HmrcResource])

object HmrcResource {
  def recursiveResource(resource: Resource): HmrcResource = {
    val children: List[HmrcResource] = resource.resources().asScala.toList.map(recursiveResource)

    val group = if (Annotation.exists(resource, "(group)")) {
        val groupName = Annotation(resource, "(group)", "name")
        val groupDesc = Annotation(resource, "(group)", "description")
        Some(Group(groupName, groupDesc))
    }
    else {
      None
    }

    HmrcResource(
      resourcePath = resource.resourcePath,
      methods = HmrcMethod(resource),
      group = group,
      relativeUri = resource.relativeUri.value,
      uriParameters = resource.uriParameters.asScala.toList.map(TypeDeclaration2.apply),
      displayName = resource.displayName.value,
      children = children
    )
  }
}

//TODO: Change description from MarkDown and example from ExampleSpec
case class TypeDeclaration2(
  name: String,
  displayName: String,
  `type`: String,
  required: Boolean,
  description: Option[String],
  examples: List[HmrcExampleSpec]){
    val example : Option[HmrcExampleSpec] = examples.headOption
  }

object TypeDeclaration2 {
  def apply(td: TypeDeclaration): TypeDeclaration2 =
    TypeDeclaration2(
      td.name,
      Val(td.displayName),
      td.`type`,
      td.required,
      Option(td.description).map(_.value()),
      td.examples.asScala.toList.map(HmrcExampleSpec.apply) // TODO: Single example?
    )
}

case class HmrcExampleSpec(
  description: Option[String],
  documentation: Option[String],
  code: Option[String],
  value: Option[String]
)

object HmrcExampleSpec {
  def apply(example : ExampleSpec) : HmrcExampleSpec = {

    val description: Option[String] = {
      FindProperty(example.structuredValue, "description", "value")
    }

    val documentation: Option[String] = {
      if (Annotation.exists(example, "(documentation)")) {
        Option(Annotation(example, "(documentation)"))
      } else {
        None
      }
    }

    val code: Option[String] = {
      FindProperty(example.structuredValue, "value", "code")
        .orElse(FindProperty(example.structuredValue, "code"))
    }

    val value = {
      FindProperty(example.structuredValue, "value")
        .orElse(Some(example.value))
    }

    HmrcExampleSpec(description, documentation, code, value)
  }
}

case class WireModel (
  title: String,
  version: String,
  deprecationMessage: Option[String],
  documentationItems: List[DocumentationItem],
  resourceGroups: List[ResourceGroup2],
  types: List[TypeDeclaration2],
  isFieldOptionalityKnown: Boolean
)

case class OurModel(
  title: String,
  version: String,
  deprecationMessage: Option[String],
  documentationItems: List[DocumentationItem],
  resourceGroups: List[ResourceGroup2],
  types: List[TypeDeclaration2],
  isFieldOptionalityKnown: Boolean,
  relationships: Map[HmrcResource, Option[HmrcResource]]
)

object OurModel {
  def apply(raml: RAML): (OurModel,WireModel) = {

    def title: String = raml.title.value

    def version: String = raml.version.value

    def deprecationMessage: Option[String] = Annotation.optional(raml, "(deprecationMessage)")

    def documentationItems: List[DocumentationItem] = raml.documentation.asScala.toList.map(item => DocumentationItem(item.title.value, item.content.value))

    def resources: List[HmrcResource] = raml.resources.asScala.toList.map(HmrcResource.recursiveResource)

    def resourceGroups: List[ResourceGroup2] = GroupedResources(resources).toList

    def typeDeclaration2Converter(td: TypeDeclaration): TypeDeclaration2 = TypeDeclaration2.apply(td)

    def types: List[TypeDeclaration2] = (raml.types.asScala.toList ++ raml.uses.asScala.flatMap(_.types.asScala)).map(typeDeclaration2Converter)

    def isFieldOptionalityKnown: Boolean = !Annotation.exists(raml, "(fieldOptionalityUnknown)")

    val wm = WireModel(
      title,
      version,
      deprecationMessage,
      documentationItems,
      resourceGroups,
      types,
      isFieldOptionalityKnown
    )

    val allResources: List[HmrcResource] = resourceGroups.flatMap(_.resources)

    val parentChildRelationships: Map[HmrcResource, Option[HmrcResource]] = {
      val startWithoutParents: Map[HmrcResource, Option[HmrcResource]] = allResources.map(r => (r->None)).toMap
      allResources.map { p =>
        p.children.map { c =>
          (c -> Option(p))
        }.toMap
      }
      .foldLeft(startWithoutParents)( (m1, m2) => m1 ++ m2)
    }

    val om = OurModel(wm.title, wm.version, wm.deprecationMessage, wm.documentationItems, wm.resourceGroups, wm.types, wm.isFieldOptionalityKnown, relationships = parentChildRelationships)

    (om,wm)
  }
}
