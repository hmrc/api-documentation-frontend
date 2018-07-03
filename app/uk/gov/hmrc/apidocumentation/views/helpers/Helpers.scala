/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.apidocumentation.views.helpers


import uk.gov.hmrc.apidocumentation.models.JsonFormatters._
import uk.gov.hmrc.apidocumentation.models.{APIAvailability, ErrorResponse}
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.raml.v2.api.model.v10.bodies.Response
import org.raml.v2.api.model.v10.common.Annotable
import org.raml.v2.api.model.v10.datamodel._
import org.raml.v2.api.model.v10.methods.Method
import org.raml.v2.api.model.v10.resources.Resource
import play.api.libs.json.Json
import play.libs.XML
import play.twirl.api.Html

import scala.collection.JavaConversions._
import scala.language.reflectiveCalls
import scala.util.Try

object Slugify {
  def apply(text: String): String = makeSlug(text)

  def apply(obj: {def value(): String}): String = Option(obj).fold("")(obj => makeSlug(obj.value()))

  private def makeSlug(text: String) = Option(text).fold("") { obj =>
    obj.replaceAll("[^\\w\\s]", "").replaceAll("\\s+", "-").toLowerCase
  }
}


object Val {
  def apply(obj: String): String = Option(obj).getOrElse("")

  def apply(obj: {def value(): String}): String = Option(obj).fold("")(_.value())
}

object HeaderVal {
  def apply(header: TypeDeclaration, version: String): String = {
    def replace(example: String) = {
      example.replace("application/vnd.hmrc.1.0", "application/vnd.hmrc." + version)
    }
    val example = Val(header.example)
    Val(header.displayName) match {
      case "Accept"=> replace(example)
      case "Content-Type" => replace(example)
      case _  => example
    }
  }
}

object FindProperty {
  def apply(typeInstance: TypeInstance, names: String*): Option[String] = {
    names match {
      case head +: Nil => {
        typeInstance.properties.find(_.name == head).map(scalarValue)
      }
      case head +: tail => {
        typeInstance.properties.find(_.name == head) match {
          case Some(property) => FindProperty(property.value, tail: _*)
          case _ => None
        }
      }
    }
  }

  private def scalarValue(property: TypeInstanceProperty): String = {
    if (!property.isArray && property.value.isScalar) property.value.value.toString else ""
  }
}

object Annotation {
  def apply(context: Annotable, names: String*): String = getAnnotation(context, names: _*).getOrElse("")

  def exists(context: Annotable, names: String*): Boolean = getAnnotation(context, names: _*).isDefined

  def getAnnotation(context: Annotable, names: String*): Option[String] = {
    val matches = context.annotations.find { ann =>
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

  private def propertyForPath(annotation: TypeInstance, path: List[AnyRef]): Option[AnyRef] =
    if (annotation.isScalar) scalarValueOf(annotation, path)
    else complexValueOf(annotation, path)

  private def complexValueOf(annotation: TypeInstance, path: List[AnyRef]): Option[AnyRef] =
    if (path.isEmpty) Option(annotation)
    else getProperty(annotation, path.head) match {
      case Some(ti: TypeInstance) => propertyForPath(ti, path.tail)
      case other => other
    }

  private def scalarValueOf(annotation: TypeInstance, path: List[AnyRef]): Option[AnyRef] =
    if (path.nonEmpty) throw new RuntimeException(s"Scalar annotations do not have properties")
    else Option(annotation.value())

  private def getProperty(annotation: TypeInstance, property: AnyRef) =
    annotation
      .properties
      .find(prop => prop.name == property)
      .map(ti => transformScalars(ti.value))

  private def transformScalars(value: TypeInstance) =
    if (value.isScalar) value.value() else value
}


object Markdown {

  def apply(text: String): Html = Html(process(text))

  def apply(obj: {def value(): String}): Html = Option(obj).fold(emptyHtml)(node => apply(node.value()))

  import com.github.rjeschke.txtmark.{Configuration, Processor}
  import org.markdown4j._

  private val emptyHtml = Html("")

  private def configuration =
    Configuration.builder
      .forceExtentedProfile
      .registerPlugins(new YumlPlugin, new WebSequencePlugin, new IncludePlugin)
      .setDecorator(new ExtDecorator()
        .addStyleClass("list list-bullet", "ul")
        .addStyleClass("list list-number", "ol")
        .addStyleClass("code", "code")
        .addStyleClass("heading-xlarge", "h1")
        .addStyleClass("heading-large", "h2")
        .addStyleClass("heading-medium", "h3")
        .addStyleClass("heading-small", "h4"))
      .setCodeBlockEmitter(new CodeBlockEmitter)

  private def process(text: String) = Processor.process(text, configuration.build)
}

case class ResourceGroup(name: Option[String] = None, description: Option[String] = None, resources: Seq[Resource] = Nil) {
  def +(resource: Resource) = {
    ResourceGroup(name, description, resources :+ resource)
  }
}


object GroupedResources {
  def apply(resources: Seq[Resource]): Seq[ResourceGroup] = {
    group(flatten(resources)).filterNot(_.resources.length < 1)
  }

  private def group(resources: Seq[Resource], currentGroup: ResourceGroup = ResourceGroup(), groups: Seq[ResourceGroup] = Nil): Seq[ResourceGroup] = {
    resources match {
      case head +: tail => {
        if (Annotation.exists(head, "(group)")) {
          val groupName = Annotation(head, "(group)", "name")
          val groupDesc = Annotation(head, "(group)", "description")
          group(tail, ResourceGroup(Some(groupName), Some(groupDesc), Seq(head)), groups :+ currentGroup)
        } else {
          group(tail, currentGroup + head, groups)
        }
      }
      case _ => groups :+ currentGroup
    }
  }

  private def flatten(resources: Seq[Resource], acc: Seq[Resource] = Nil): Seq[Resource] = {
    resources match {
      case head +: tail => {
        flatten(tail, flatten(head.resources, acc :+ head))
      }
      case _ => acc
    }
  }
}

object Methods {
  private val correctOrder = Map(
    "get" -> 0, "post" -> 1, "put" -> 2, "delete" -> 3,
    "head" -> 4, "patch" -> 5, "options" -> 6
  )

  def apply(resource: Resource): List[Method] =
    resource.methods.toList.sortWith { (left, right) =>
      (for {
        l <- correctOrder.get(left.method)
        r <- correctOrder.get(right.method)
      } yield l < r).getOrElse(false)
    }
}

object Authorisation {
  def apply(method: Method): (String, Option[String]) = fetchAuthorisation(method)

  private def fetchAuthorisation(method: Method): (String, Option[String]) = {
    if (method.securedBy().nonEmpty) {
      method.securedBy.get(0).securityScheme.`type` match {
        case "OAuth 2.0" => ("user", Some(Annotation(method, "(scope)")))
        case _ => ("application", None)
      }
    } else {
      ("none", None)
    }
  }
}



object Responses {
  def success(method: Method) = method.responses.filter(isSuccessResponse)

  def error(method: Method) = method.responses.filter(isErrorResponse)

  private def isSuccessResponse(response: Response) = {
    val code = Val(response.code)
    code.startsWith("2") || code.startsWith("3")
  }

  private def isErrorResponse(response: Response) = {
    val code = Val(response.code)
    code.startsWith("4") || code.startsWith("5")
  }
}


object ErrorScenarios {
  def apply(method: Method): Seq[Map[String, String]] = {
    val errorScenarios = for {
      response <- Responses.error(method)
      body <- response.body
      example <- BodyExamples(body)
      scenarioDescription <- scenarioDescription(body, example)
      errorResponse <- errorResponse(example)
    } yield {
      errorResponse.code.map(code =>
        Map("scenario" -> scenarioDescription,
          "code" -> code,
          "httpStatus" -> response.code.value))
    }

    errorScenarios.flatten
  }

  private def errorResponse(bodyExample: BodyExample): Option[ErrorResponse] = {
    FindProperty(bodyExample.example.structuredValue, "value", "code")
      .orElse(FindProperty(bodyExample.example.structuredValue, "code"))
      .fold(responseFromBody(bodyExample))(code => Some(ErrorResponse(code = Some(code))))
  }

  private def scenarioDescription(body: TypeDeclaration, example: BodyExample): Option[String] = {
    example.description()
      .orElse(Option(body.description).map(_.value))
  }
  private def responseFromBody(example: BodyExample): Option[ErrorResponse] = {
    responseFromJson(example).orElse(responseFromXML(example))
  }

  private def responseFromJson(example: BodyExample): Option[ErrorResponse] = {
    example.value.flatMap(v => Try(Json.parse(v).as[ErrorResponse]).toOption)
  }

  private def responseFromXML(example: BodyExample): Option[ErrorResponse] = {
    for {
      v <- example.value
      codes <- Try(XML.fromString(v).getElementsByTagName("code")).toOption
      first <- Option(codes.item(0))
    } yield {
      ErrorResponse(Some(first.getTextContent))
    }
  }
}

case class BodyExample(example: ExampleSpec) {
  def description(): Option[String] = {
    FindProperty(example.structuredValue, "description", "value")
  }

  def documentation(): Option[String] = {
    if (Annotation.exists(example, "(documentation)")) {
      Option(Annotation(example, "(documentation)"))
    } else {
      None
    }
  }

  def code(): Option[String] = {
    FindProperty(example.structuredValue, "value", "code")
      .orElse(FindProperty(example.structuredValue, "code"))
  }

  def value() = {
    FindProperty(example.structuredValue, "value")
      .orElse(Some(example.value))
  }
}

object BodyExamples {
  def apply(body: TypeDeclaration): Seq[BodyExample] = {
    if (body.examples.size > 0) body.examples.toSeq.map(ex => BodyExample(ex)) else Seq(BodyExample(body.example))
  }
}

object HttpStatus {

  def apply(statusCode: String): String = apply(statusCode.toInt)

  def apply(statusCode: Int): String = {
    val responseStatus = HttpResponseStatus.valueOf(statusCode)
    s"$statusCode (${responseStatus.getReasonPhrase})"
  }
}

object EndpointsAvailable {
  def apply(availability: Option[APIAvailability]): String = {
    if (availability.fold(false)(a => a.endpointsEnabled && a.authorised)) "Yes"
    else "No"
  }
}