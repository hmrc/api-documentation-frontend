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
import org.raml.v2.api.model.v10.methods.Method
import uk.gov.hmrc.apidocumentation.views.helpers.Val
import uk.gov.hmrc.apidocumentation.views.helpers.ResourceGroup2

case class RamlAndSchemas(raml: RAML, schemas: Map[String, JsonSchema])

case class DocumentationItem(title: String, content: String)

case class SecurityScheme(`type`: String, scope: Option[String])

case class HmrcResponse(code: String,  body: List[TypeDeclaration2], headers: List[TypeDeclaration2], description: Option[String])

case class HmrcMethod(
  method: String,
  displayName: String,
  body: List[TypeDeclaration2],
  headers: List[TypeDeclaration2],
  queryParameters: List[TypeDeclaration2],
  description: String,
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

    def sandboxData = Annotation.optional(method, "(sandboxData)")

    HmrcMethod(
      method.method,
      method.displayName.value,
      body,
      headers,
      queryParameters,
      method.description.value,
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

// If we had some useful key we could use a Tree
// Could we use resourcePath ???

object HmrcResources {
  type Child = HmrcResource
  type MaybeParent = Option[HmrcResource]
}

import HmrcResources._

case class HmrcResources(resources: List[HmrcResource], relationships: Map[Child, MaybeParent]) {

  def combine(other: HmrcResources): HmrcResources = {
    HmrcResources(this.resources ++ other.resources, this.relationships ++ other.relationships)
  }

  def isRoot(resource: HmrcResource): Boolean = {
    relationships.get(resource).flatten.isEmpty
  }

  def roots: List[HmrcResource] = {
    this.resources.filter(isRoot)
  }

  def parentOf(child: HmrcResource): Option[HmrcResource] = {
    relationships.find(t => t._1 == child)map(_._1)
  }

  def depthFirstDescendants(resource: HmrcResource): List[HmrcResource] = {
    ???
  }

  def ancestorsOf(resource: HmrcResource): List[HmrcResource] = {
    def recursiveAncestor(lookAt: HmrcResource, ancestorsSoFar: List[HmrcResource]): List[HmrcResource] = {
      parentOf(lookAt) match {
        case None => ancestorsSoFar
        case Some(a) => recursiveAncestor(a, a :: ancestorsSoFar)
      }
    }
    recursiveAncestor(resource, List())
  }
}

//TODO: Change description from MarkDown and example from ExampleSpec
case class TypeDeclaration2(name: String, displayName: String, `type`: String, required: Boolean, description: Option[String], /* TODO */ example: ExampleSpec, examples: List[ExampleSpec])

object TypeDeclaration2 {
  def apply(td: TypeDeclaration): TypeDeclaration2 =
    TypeDeclaration2(td.name, Val(td.displayName), td.`type`, td.required, Option(td.description).map(_.value()), td.example, td.examples.asScala.toList)
}

case class OurModel(
  title: String,
  version: String,
  deprecationMessage: Option[String],
  documentationItems: List[DocumentationItem],
  resources: HmrcResources,
  resourceGroups: List[ResourceGroup2],
  types: List[TypeDeclaration2],
  isFieldOptionalityKnown: Boolean
)

object OurModel {
  def apply(raml: RAML): OurModel = {

    def asHmrcResources: HmrcResources = {

      def recurseHR(resource: Resource): HmrcResources = {
        val childTrees: List[HmrcResources] = resource.resources().asScala.toList.map(recurseHR)

        val immediateChildren: List[HmrcResource] = childTrees.flatMap(_.roots)

        val group = if (Annotation.exists(resource, "(group)")) {
            val groupName = Annotation(resource, "(group)", "name")
            val groupDesc = Annotation(resource, "(group)", "description")
            Some(Group(groupName, groupDesc))
        }
        else {
          None
        }

        val theResource = HmrcResource(
          resourcePath = resource.resourcePath,
          methods = HmrcMethod(resource),
          group = group,
          relativeUri = resource.relativeUri.value,
          uriParameters = resource.uriParameters.asScala.toList.map(TypeDeclaration2.apply),
          displayName = resource.displayName.value,
          children = immediateChildren
        )

        val currentRelationships = theResource.children.map(c => (c -> Some(theResource))).toMap

        val thisHR = HmrcResources(List(theResource), currentRelationships)

        childTrees.foldRight(thisHR)( (l,r) => l.combine(r))
      }

      val roots = raml.resources.asScala.toList.map(r => recurseHR(r))

      val finalHR = roots match {
        case Nil => HmrcResources(List(), Map())
        case head :: Nil => head
        case head :: tail => tail.foldRight(head)( (l,r) => l.combine(r) )
      }

      finalHR
    }

    def title: String = raml.title.value

    def version: String = raml.version.value

    def deprecationMessage: Option[String] = Annotation.optional(raml, "(deprecationMessage)")

    def documentationItems: List[DocumentationItem] = raml.documentation.asScala.toList.map(item => DocumentationItem(item.title.value, item.content.value))

    def resourceGroups: List[ResourceGroup2] = GroupedResources(asHmrcResources.resources).toList

    def typeDeclaration2Converter(td: TypeDeclaration): TypeDeclaration2 = TypeDeclaration2.apply(td)

    def types: List[TypeDeclaration2] = (raml.types.asScala.toList ++ raml.uses.asScala.flatMap(_.types.asScala)).map(typeDeclaration2Converter)

    def isFieldOptionalityKnown: Boolean = !Annotation.exists(raml, "(fieldOptionalityUnknown)")

    OurModel(
      title,
      version,
      deprecationMessage,
      documentationItems,
      resources = asHmrcResources,
      resourceGroups,
      types,
      isFieldOptionalityKnown
    )
  }
}
