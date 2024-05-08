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

package uk.gov.hmrc.apidocumentation.v2.models

import scala.io.Source
import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.{ApiCategory, ApiDefinition, ApiVersion}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApiVersionNbr
import uk.gov.hmrc.play.json.Union
import uk.gov.hmrc.apidocumentation.controllers.routes
import uk.gov.hmrc.apidocumentation.models.{DocumentationLabel, WrappedApiDefinition, XmlApiDocumentation}
import uk.gov.hmrc.apiplatform.modules.common.domain.services.SealedTraitJsonFormatting

import scala.collection.immutable.ListSet

case class DocumentIdentifier(value: String) extends AnyVal

object DocumentIdentifier {
  implicit val documentIdentifierFormat: Format[DocumentIdentifier] = Json.valueFormat[DocumentIdentifier]
}

sealed trait ApiDocumentation {
  val identifier: DocumentIdentifier
  val name: String
  val description: String
  val context: String
  val label: DocumentationLabel
  val categories: Seq[ApiCategory]

  def documentationUrl: String

}

object ApiDocumentation {

  implicit val apiDocumentationFormats: OFormat[ApiDocumentation] = Union.from[ApiDocumentation]("label")
    .and[RestDocumentation](DocumentationLabel.REST_API.toString)
    .and[XmlDocumentation](DocumentationLabel.XML_API.toString)
    .and[ServiceGuideDocumentation](DocumentationLabel.SERVICE_GUIDE.toString)
    .and[RoadMapDocumentation](DocumentationLabel.ROADMAP.toString)
    .and[RestDocumentation.TestSupportApiDocumentation](DocumentationLabel.TEST_SUPPORT_API.toString)
    .format
}

case class RestDocumentation(identifier: DocumentIdentifier, name: String, description: String, context: String, version: ApiVersionNbr, url: String, categories: Seq[ApiCategory])
    extends ApiDocumentation {

  val label: DocumentationLabel = DocumentationLabel.REST_API

  def documentationUrl: String = url

}

object RestDocumentation {

  case class TestSupportApiDocumentation(identifier: DocumentIdentifier, name: String, description: String, context: String, version: ApiVersionNbr, url: String, categories: Seq[ApiCategory])
    extends ApiDocumentation {

    val label: DocumentationLabel = DocumentationLabel.TEST_SUPPORT_API

    def documentationUrl: String = url
  }

  object TestSupportApiDocumentation {
    implicit val testSupportApiDocumentationFormats: OFormat[TestSupportApiDocumentation] = Json.format[TestSupportApiDocumentation]
  }

  def fromApiDefinition(definition: ApiDefinition, descriptionOverride: Option[RestApiDescriptionOverride]) = {

    val defaultVersion: ApiVersion = definition
      .versionsAsList
      .sorted(WrappedApiDefinition.statusVersionOrdering)
      .head

    val documentationUrl: String = routes.ApiDocumentationController.renderApiDocumentation(definition.serviceName, defaultVersion.versionNbr, None).url
    if(definition.isTestSupport){
      TestSupportApiDocumentation(
        DocumentIdentifier(definition.serviceName.value),
        definition.name,
        descriptionOverride.map(_.description).getOrElse(definition.description),
        definition.context.value,
        defaultVersion.versionNbr,
        documentationUrl,
        definition.categories
      )
    }else {
      RestDocumentation(
        DocumentIdentifier(definition.serviceName.value),
        definition.name,
        descriptionOverride.map(_.description).getOrElse(definition.description),
        definition.context.value,
        defaultVersion.versionNbr,
        documentationUrl,
        definition.categories
      )
    }
  }

  implicit val restDocumentFormats: OFormat[RestDocumentation] = Json.format[RestDocumentation]
}




case class XmlDocumentation(identifier: DocumentIdentifier, name: String, description: String, context: String, categories: Seq[ApiCategory]) extends ApiDocumentation {

  val label: DocumentationLabel = DocumentationLabel.XML_API

  def documentationUrl: String = routes.ApiDocumentationController.renderXmlApiDocumentation(identifier.value).url
}

object XmlDocumentation {

  def fromXmlDocumentation(api: XmlApiDocumentation) = {
    XmlDocumentation(DocumentIdentifier(api.name), api.name, api.description, api.context, api.categories.getOrElse(Seq.empty))
  }

  implicit val xmlDocumentFormats: OFormat[XmlDocumentation] = Json.format[XmlDocumentation]
}

case class ServiceGuideDocumentation(identifier: DocumentIdentifier, name: String, description: String, context: String, categories: Seq[ApiCategory]) extends ApiDocumentation {

  val label: DocumentationLabel = DocumentationLabel.SERVICE_GUIDE

  def documentationUrl: String = context
}

object ServiceGuideDocumentation {

  lazy val serviceGuides: Seq[ServiceGuideDocumentation] =
    Json.parse(Source.fromInputStream(getClass.getResourceAsStream("/service_guides.json")).mkString).as[Seq[ServiceGuideDocumentation]]

  implicit val serviceGuideDocumentFormats: OFormat[ServiceGuideDocumentation] = Json.format[ServiceGuideDocumentation]

}

case class RoadMapDocumentation(identifier: DocumentIdentifier, name: String, description: String, context: String, categories: Seq[ApiCategory]) extends ApiDocumentation {

  val label: DocumentationLabel = DocumentationLabel.ROADMAP

  def documentationUrl: String = context
}

object RoadMapDocumentation {

  lazy val roadMaps: Seq[RoadMapDocumentation] =
    Json.parse(Source.fromInputStream(getClass.getResourceAsStream("/roadmap.json")).mkString).as[Seq[RoadMapDocumentation]]

  implicit val roadMapDocumentationFormats: OFormat[RoadMapDocumentation] = Json.format[RoadMapDocumentation]
}

case class RestApiDescriptionOverride(identifier: DocumentIdentifier, description: String)

object RestApiDescriptionOverride {
  implicit val format: OFormat[RestApiDescriptionOverride] = Json.format[RestApiDescriptionOverride]

  def descriptionOverrides: Seq[RestApiDescriptionOverride] =
    Json.parse(Source.fromInputStream(getClass.getResourceAsStream("/api-description-overrides.json")).mkString).as[Seq[RestApiDescriptionOverride]]
}
