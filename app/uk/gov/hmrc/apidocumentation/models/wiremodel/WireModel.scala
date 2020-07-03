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

package uk.gov.hmrc.apidocumentation.models.wiremodel

import org.raml.v2.api.model.v10.datamodel.{ExampleSpec, TypeDeclaration}
import org.raml.v2.api.model.v10.resources.Resource
import uk.gov.hmrc.apidocumentation.services.RAML
import uk.gov.hmrc.apidocumentation.views.helpers.{GroupedResources}

import scala.collection.JavaConverters._
import org.raml.v2.api.model.v10.methods.{Method => RamlMethod}
import uk.gov.hmrc.apidocumentation.views.helpers.FindProperty
import org.raml.v2.api.model.v10.common.{Annotable => RamlAnnotable}
import org.raml.v2.api.model.v10.datamodel.{
  TypeInstance => RamlTypeInstance,
  StringTypeDeclaration => RamlStringTypeDeclaration
}

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

  def apply(method: RamlMethod): HmrcMethod = {
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
          code = DefaultToEmptyValue(r.code()),
          body = r.body.asScala.toList.map(TypeDeclaration2.apply),
          headers = r.headers().asScala.toList.map(TypeDeclaration2.apply),
          description = Option(r.description()).map(_.value())
        )
      })
    }

    def sandboxData = Annotation.optional(method, "(sandboxData)")

    HmrcMethod(
      method.method,
      DefaultToEmptyValue(method.displayName),
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
  examples: List[HmrcExampleSpec],
  enumValues: List[String],
  pattern: Option[String]){
    val example : Option[HmrcExampleSpec] = examples.headOption
  }

object TypeDeclaration2 {
  def apply(td: TypeDeclaration): TypeDeclaration2 = {
    val examples =
      if(td.example != null)
        List(HmrcExampleSpec(td.example))
      else
        td.examples.asScala.toList.map(HmrcExampleSpec.apply)

    val enumValues = td match {
      case t: RamlStringTypeDeclaration => t.enumValues().asScala.toList
      case _                        => List()
    }

    val patterns = td match {
      case t: RamlStringTypeDeclaration => Some(t.pattern())
      case _                        => None
    }

    TypeDeclaration2(
      td.name,
      DefaultToEmptyValue(td.displayName),
      td.`type`,
      td.required,
      Option(td.description).map(_.value()),
      examples,
      enumValues,
      patterns
    )
  }
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

      SafeValue(FindProperty(example.structuredValue, "value"))
        .orElse(SafeValue(example))
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

object WireModel {
  def apply(raml: RAML) : WireModel = {

    def title: String = DefaultToEmptyValue(raml.title)

    def version: String = raml.version.value

    def deprecationMessage: Option[String] = Annotation.optional(raml, "(deprecationMessage)")

    def documentationItems: List[DocumentationItem] =
      raml.documentation.asScala.toList.map(item => DocumentationItem(
        DefaultToEmptyValue(item.title), DefaultToEmptyValue(item.content)
      ))

    def resources: List[HmrcResource] = raml.resources.asScala.toList.map(HmrcResource.recursiveResource)

    def resourceGroups: List[ResourceGroup2] = GroupedResources(resources).toList

    def types: List[TypeDeclaration2] = (raml.types.asScala.toList ++ raml.uses.asScala.flatMap(_.types.asScala)).map(TypeDeclaration2.apply)

    def isFieldOptionalityKnown: Boolean = !Annotation.exists(raml, "(fieldOptionalityUnknown)")

    WireModel(
      title,
      version,
      deprecationMessage,
      documentationItems,
      resourceGroups,
      types,
      isFieldOptionalityKnown
    )
  }
}

// TODO: Add some tests
object SafeValue {
  //Convert nulls and empty strings to Option.None
  def apply(v: String): Option[String] = apply(Option(v))
  def apply(v: {def value(): String}): Option[String] = apply(Option(v).map(_.value()))
  def apply(v: Option[String]): Option[String] = v.filter(_.nonEmpty)
}

object DefaultToEmptyValue {
  def apply(v: {def value(): String}): String = SafeValue(v.value()).getOrElse("")
}


  object WireModelFormatters {
    import play.api.libs.json.Json

    implicit val hmrcExampleSpecJF = Json.format[HmrcExampleSpec]
    implicit val typeDeclaration2JF = Json.format[TypeDeclaration2]

    implicit val securitySchemeJF = Json.format[SecurityScheme]

    implicit val groupJF = Json.format[Group]
    implicit val hmrcResponseJF = Json.format[HmrcResponse]
    implicit val hmrcMethodJF = Json.format[HmrcMethod]
    implicit val hmrcResourceJF = Json.format[HmrcResource]

    implicit val documentationItemJF = Json.format[DocumentationItem]
    implicit val resourceGroup2JF = Json.format[ResourceGroup2]

    implicit val wireModelJF = Json.format[WireModel]
  }


  object Annotation {
  def apply(context: RamlAnnotable, names: String*): String = getAnnotation(context, names: _*).getOrElse("")

  def exists(context: RamlAnnotable, names: String*): Boolean = getAnnotation(context, names: _*).isDefined

  def optional(context: RamlAnnotable, names: String*): Option[String] = getAnnotation(context, names: _*).filterNot(_.isEmpty)

  def getAnnotation(context: RamlAnnotable, names: String*): Option[String] = {
    val matches = context.annotations.asScala.find { ann =>
      Option(ann.name).exists(stripNamespace(_) == names.head)
    }

    val out = for {
      m <- matches
      annotation = m.structuredValue
    } yield propertyForPath(annotation, names.tail.toList)

    out.flatten.map(_.toString)
  }

  private def stripNamespace(name: String): String = {
    name.replaceFirst("\\(.*\\.", "(")
  }

  private def propertyForPath(annotation: RamlTypeInstance, path: List[AnyRef]): Option[AnyRef] =
    if (annotation.isScalar) scalarValueOf(annotation, path)
    else complexValueOf(annotation, path)

  private def complexValueOf(annotation: RamlTypeInstance, path: List[AnyRef]): Option[AnyRef] =
    if (path.isEmpty) Option(annotation)
    else getProperty(annotation, path.head) match {
      case Some(ti: RamlTypeInstance) => propertyForPath(ti, path.tail)
      case other => other
    }

  private def scalarValueOf(annotation: RamlTypeInstance, path: List[AnyRef]): Option[AnyRef] =
    if (path.nonEmpty) throw new RuntimeException(s"Scalar annotations do not have properties")
    else Option(annotation.value())

  private def getProperty(annotation: RamlTypeInstance, property: AnyRef) =
    annotation
      .properties.asScala
      .find(prop => prop.name == property)
      .map(ti => transformScalars(ti.value))

  private def transformScalars(value: RamlTypeInstance) =
    if (value.isScalar) value.value() else value
}

case class ResourceGroup2(name: Option[String] = None, description: Option[String] = None, resources: List[HmrcResource] = Nil) {
  def +(resource: HmrcResource) = {
    // TODO not efficient
    ResourceGroup2(name, description, resources :+ resource)
  }
}

object ResourceGroup2 {
  def apply(resources: List[HmrcResource]): List[ResourceGroup2] = {
    def flatten(resources: List[HmrcResource], acc: List[HmrcResource]): List[HmrcResource] = {
      resources match {
        case Nil => acc
        case head :: tail =>
          // TODO - not efficient to right concat
          flatten(tail, flatten(head.children, head :: acc))
      }
    }

    def group(resources: List[HmrcResource], currentGroup: ResourceGroup2 = ResourceGroup2(), groups: List[ResourceGroup2] = Nil): List[ResourceGroup2] = {
      resources match {
        case head :: tail => {
          if (head.group.isDefined) {
            group(tail, ResourceGroup2(head.group.map(_.name), head.group.map(_.description), List(head)), groups :+ currentGroup)
          } else {
            group(tail, currentGroup + head, groups)
          }
        }
        case _ => groups :+ currentGroup
      }
    }

    group(flatten(resources, Nil).reverse).filterNot(_.resources.length < 1)
  }
}
