/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.apidocumentation.controllers.binders

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.{ApiCategory, ServiceName}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApiVersionNbr

import uk.gov.hmrc.apidocumentation.models.DocumentationTypeFilter

object RouteModels {
  type SimpleServiceName             = String
  type SimpleApiVersionNbr           = String
  type SimpleApiCategory             = String
  type SimpleDocumentationTypeFilter = String

  object Conversions {

    given Conversion[SimpleServiceName, ServiceName] with
      def apply(x: SimpleServiceName): ServiceName = ServiceName(x)

    given Conversion[SimpleApiVersionNbr, ApiVersionNbr] with
      def apply(x: SimpleApiVersionNbr): ApiVersionNbr = ApiVersionNbr(x)

    given Conversion[Option[SimpleApiVersionNbr], Option[ApiVersionNbr]] with
      def apply(x: Option[SimpleApiVersionNbr]): Option[ApiVersionNbr] = x.map(ApiVersionNbr.apply)

    given sac: Conversion[List[SimpleApiCategory], List[ApiCategory]] with
      def apply(x: List[SimpleApiCategory]): List[ApiCategory] = x.map(ApiCategory.unsafeApply)

    given sdtf: Conversion[List[SimpleDocumentationTypeFilter], List[DocumentationTypeFilter]] with
      def apply(x: List[SimpleDocumentationTypeFilter]): List[DocumentationTypeFilter] = x.map(DocumentationTypeFilter.unsafeApply)
  }

  object ViewConversions {

    given Conversion[ServiceName, SimpleServiceName] with
      def apply(x: ServiceName): SimpleServiceName = x.toString

    given Conversion[ApiVersionNbr, SimpleApiVersionNbr] with
      def apply(x: ApiVersionNbr): SimpleApiVersionNbr = x.toString

    given Conversion[Option[ApiVersionNbr], Option[SimpleApiVersionNbr]] with
      def apply(x: Option[ApiVersionNbr]): Option[SimpleApiVersionNbr] = x.map(_.toString)

    given sac: Conversion[List[ApiCategory], List[SimpleApiCategory]] with
      def apply(x: List[ApiCategory]): List[SimpleApiCategory] = x.map(_.toString)

    given sdtf: Conversion[List[DocumentationTypeFilter], List[SimpleDocumentationTypeFilter]] with
      def apply(x: List[DocumentationTypeFilter]): List[SimpleDocumentationTypeFilter] = x.map(_.toString)
  }
}
