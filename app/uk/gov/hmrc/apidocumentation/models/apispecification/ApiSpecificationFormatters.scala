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

package uk.gov.hmrc.apidocumentation.models.apispecification

object ApiSpecificationFormatters {
  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  implicit val hmrcExampleSpecJF = Json.format[ExampleSpec]

  implicit val typeDeclarationWrites: OWrites[TypeDeclaration] = (
    (__ \ "name").write[String] and
      (__ \ "displayName").write[String] and
      (__ \ "type").write[String] and
      (__ \ "required").write[Boolean] and
      (__ \ "description").writeNullable[String] and
      (__ \ "examples").writeNullable[Seq[ExampleSpec]].contramap[Seq[ExampleSpec]](notIfEmpty) and
      (__ \ "enumValues").writeNullable[Seq[String]].contramap[Seq[String]](notIfEmpty) and
      (__ \ "pattern").writeNullable[String]
  )(unlift(TypeDeclaration.unapply))

  implicit val typeDeclarationReads: Reads[TypeDeclaration] = (
    (__ \ "name").read[String] and
      (__ \ "displayName").read[String] and
      (__ \ "type").read[String] and
      (__ \ "required").read[Boolean] and
      (__ \ "description").readNullable[String] and
      (__ \ "examples").readNullable[List[ExampleSpec]].map(_.getOrElse(List())) and
      (__ \ "enumValues").readNullable[List[String]].map(_.getOrElse(List())) and
      (__ \ "pattern").readNullable[String]
  )(TypeDeclaration.apply _)

  implicit val securitySchemeJF = Json.format[SecurityScheme]

  implicit val groupJF = Json.format[Group]

  implicit val responseWrites: OWrites[Response] = (
    (__ \ "code").write[String] and
      (__ \ "body").writeNullable[Seq[TypeDeclaration]].contramap[Seq[TypeDeclaration]](notIfEmpty) and
      (__ \ "headers").writeNullable[Seq[TypeDeclaration]].contramap[Seq[TypeDeclaration]](notIfEmpty) and
      (__ \ "description").writeNullable[String]
  )(unlift(Response.unapply))

  implicit val reponseReads: Reads[Response] = (
    (__ \ "code").read[String] and
      (__ \ "body").readNullable[List[TypeDeclaration]].map(_.getOrElse(List())) and
      (__ \ "headers").readNullable[List[TypeDeclaration]].map(_.getOrElse(List())) and
      (__ \ "description").readNullable[String]
  )(Response.apply _)

  implicit val hmrcMethodJF   = Json.format[Method]
  implicit val hmrcResourceJF = Json.format[Resource]

  implicit val documentationItemJF = Json.format[DocumentationItem]
  implicit val hmrcResourceGroupJF = Json.format[ResourceGroup]

  implicit val apiSpecificationJF = Json.format[ApiSpecification]

  def notIfEmpty[A](seq: Seq[A]): Option[Seq[A]] = if (seq.isEmpty) None else Some(seq)

}
