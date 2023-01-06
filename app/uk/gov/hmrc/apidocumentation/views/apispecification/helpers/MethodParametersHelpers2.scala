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

package uk.gov.hmrc.apidocumentation.views.apispecification.helpers

import uk.gov.hmrc.apidocumentation.models.apispecification._
import uk.gov.hmrc.apidocumentation.models.ViewModel

case class MethodParameter2(
    name: String,
    typeName: String,
    baseTypeName: String,
    required: Boolean,
    description: String,
    example: Option[ExampleSpec],
    pattern: Option[String] = None,
    enumValues: List[String] = List.empty
  )

case object Parameters {

  def fromTypeDeclaration(td: TypeDeclaration) = {
    val typeName = td.`type` match {
      case "date-only" => "date"
      case other       => other
    }
    MethodParameter2(td.name, typeName, typeName, td.required, td.description.getOrElse(""), td.examples.headOption)
  }

  def resolveTypes(parameters: Seq[TypeDeclaration], viewModel: ViewModel): Seq[MethodParameter2] = {

    def findType(param: TypeDeclaration) = {
      def findInTypes(types: List[TypeDeclaration]) = types.find(_.name == param.`type`)

      findInTypes(viewModel.types)
    }

    parameters.map { p =>
      findType(p).fold(fromTypeDeclaration(p))(t =>
        fromTypeDeclaration(p).copy(
          baseTypeName = t.`type`,
          pattern = t.pattern,
          enumValues = t.enumValues,
          example = Option(p.example).getOrElse(t.example)
        )
      )
    }
  }

  def uriParams(resource: Option[Resource], viewModel: ViewModel): Seq[MethodParameter2] = {
    resource.fold(Seq.empty[MethodParameter2]) { res =>
      uriParams(viewModel.relationships.get(res).flatten, viewModel) ++ resolveTypes(res.uriParameters, viewModel)
    }
  }

  def queryParams(method: Option[Method], viewModel: ViewModel): Seq[MethodParameter2] = {
    method.fold(Seq.empty[MethodParameter2]) { meth =>
      resolveTypes(meth.queryParameters, viewModel)
    }
  }
}
