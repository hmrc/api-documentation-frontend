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


case class HmrcResource(resourcePath: String, methods: List[HmrcMethod], uriParameters: List[TypeDeclaration2], relativeUri: String, displayName: String, children: List[HmrcResource])

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

case class HmrcExampleSpec(
  description: Option[String],
  documentation: Option[String],
  code: Option[String],
  value: Option[String]
)

case class WireModel (
  title: String,
  version: String,
  deprecationMessage: Option[String],
  documentationItems: List[DocumentationItem],
  resourceGroups: List[ResourceGroup2],
  types: List[TypeDeclaration2],
  isFieldOptionalityKnown: Boolean
)

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

    implicit val hmrcResponseJF = Json.format[HmrcResponse]
    implicit val hmrcMethodJF = Json.format[HmrcMethod]
    implicit val hmrcResourceJF = Json.format[HmrcResource]

    implicit val documentationItemJF = Json.format[DocumentationItem]
    implicit val resourceGroup2JF = Json.format[ResourceGroup2]

    implicit val wireModelJF = Json.format[WireModel]
  }

case class ResourceGroup2(name: Option[String] = None, description: Option[String] = None, resources: List[HmrcResource] = Nil) {
  def +(resource: HmrcResource) = {
    // TODO not efficient
    ResourceGroup2(name, description, resources :+ resource)
  }
}
