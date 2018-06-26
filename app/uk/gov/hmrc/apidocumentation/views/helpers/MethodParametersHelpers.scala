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

import org.raml.v2.api.model.v10.datamodel.{ExampleSpec, StringTypeDeclaration, TypeDeclaration}
import org.raml.v2.api.model.v10.methods.Method
import org.raml.v2.api.model.v10.resources.Resource
import org.raml.v2.api.model.v10.system.types.MarkdownString
import uk.gov.hmrc.apidocumentation.services._

import scala.collection.JavaConversions._

case class MethodParameter(name: String, typeName: String, baseTypeName: String, required: Boolean, description: MarkdownString,
                           example: ExampleSpec, pattern: Option[String] = None, enumValues: Seq[String] = Seq.empty)

case object MethodParameter {
  def fromTypeDeclaration(td: TypeDeclaration) = {
    val typeName = td.`type` match {
      case "date-only" => "date"
      case other => other
    }
    MethodParameter(td.name, typeName, typeName, td.required, td.description, td.example)
  }
}

trait MethodParameters {

  def resolveTypes(parameters: Seq[TypeDeclaration], raml: RAML): Seq[MethodParameter] = {

    def findType(param: TypeDeclaration) = {
      def findInTypes(types: Seq[TypeDeclaration]) = types.find(_.name == param.`type`)

      findInTypes(raml.types).orElse(findInTypes(raml.uses.flatMap(_.types)))
    }

    parameters.map { p =>

      findType(p).fold(MethodParameter.fromTypeDeclaration(p)) {
        case t: StringTypeDeclaration => {
          MethodParameter.fromTypeDeclaration(p).copy(baseTypeName = t.`type`, pattern = Option(t.pattern),
            enumValues = t.enumValues, example = Option(p.example).getOrElse(t.example))
        }
        case t => {
          MethodParameter.fromTypeDeclaration(p).copy(baseTypeName = t.`type`,
            example = Option(p.example).getOrElse(t.example))
        }
      }
    }
  }
}

object UriParams extends MethodParameters {
  def apply(resource: Resource, raml: RAML): Seq[MethodParameter] = {
    Option(resource).fold(Seq.empty[MethodParameter]) { res =>
      apply(res.parentResource, raml) ++ resolveTypes(res.uriParameters, raml)
    }
  }
}

object QueryParams extends MethodParameters {
  def apply(method: Method, raml: RAML): Seq[MethodParameter] = {
    Option(method).fold(Seq.empty[MethodParameter]) { meth =>
      resolveTypes(meth.queryParameters, raml)
    }
  }
}

