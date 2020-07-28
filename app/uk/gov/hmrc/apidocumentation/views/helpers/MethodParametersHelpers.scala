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

package uk.gov.hmrc.apidocumentation.views.helpers

import org.raml.v2.api.model.v10.datamodel.{ExampleSpec => RamlExampleSpec, StringTypeDeclaration => RamlStringTypeDeclaration, TypeDeclaration => RamlTypeDeclaration}
import org.raml.v2.api.model.v10.methods.{Method => RamlMethod}
import org.raml.v2.api.model.v10.resources.{Resource => RamlResource}
import org.raml.v2.api.model.v10.system.types.{MarkdownString => RamlMarkdownString}
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.apidocumentation.services._

import scala.collection.JavaConverters._

case class MethodParameter(name: String, typeName: String, baseTypeName: String, required: Boolean, description: RamlMarkdownString,
                           example: RamlExampleSpec, pattern: Option[String] = None, enumValues: Seq[String] = Seq.empty)

case object MethodParameter {
  def fromTypeDeclaration(td: RamlTypeDeclaration) = {
    val typeName = td.`type` match {
      case "date-only" => "date"
      case other => other
    }
    MethodParameter(td.name, typeName, typeName, td.required, td.description, td.example)
  }

  def resolveTypes(parameters: Seq[RamlTypeDeclaration], raml: RAML): Seq[MethodParameter] = {

    def findType(param: RamlTypeDeclaration) = {
      def findInTypes(types: Seq[RamlTypeDeclaration]) = types.find(_.name == param.`type`)

      findInTypes(raml.types.asScala).orElse(findInTypes(raml.uses.asScala.flatMap(_.types.asScala)))
    }

    parameters.map { p =>

      findType(p).fold(MethodParameter.fromTypeDeclaration(p)) {
        case t: RamlStringTypeDeclaration => {
          MethodParameter.fromTypeDeclaration(p).copy(baseTypeName = t.`type`, pattern = Option(t.pattern),
            enumValues = t.enumValues.asScala, example = Option(p.example).getOrElse(t.example))
        }
        case t => {
          MethodParameter.fromTypeDeclaration(p).copy(baseTypeName = t.`type`,
            example = Option(p.example).getOrElse(t.example))
        }
      }
    }
  }

}

object UriParams {
  def apply(resource: RamlResource, raml: RAML): Seq[MethodParameter] = {
    Option(resource).fold(Seq.empty[MethodParameter]) { res =>
      apply(res.parentResource, raml) ++ MethodParameter.resolveTypes(res.uriParameters.asScala, raml)
    }
  }
}


object QueryParams {
  def apply(method: RamlMethod, raml: RAML): Seq[MethodParameter] = {
    Option(method).fold(Seq.empty[MethodParameter]) { meth =>
      MethodParameter.resolveTypes(meth.queryParameters.asScala, raml)
    }
  }
}
