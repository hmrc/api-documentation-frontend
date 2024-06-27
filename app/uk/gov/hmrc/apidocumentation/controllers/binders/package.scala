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

package uk.gov.hmrc.apidocumentation.controllers

import play.api.mvc.{PathBindable, QueryStringBindable}
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.{ApiCategory, ServiceName}

import uk.gov.hmrc.apidocumentation.v2.models.DocumentationTypeFilter

package object binders {

  implicit def serviceNamePathBinder(implicit textBinder: PathBindable[String]): PathBindable[ServiceName] = new PathBindable[ServiceName] {

    override def bind(key: String, value: String): Either[String, ServiceName] = {
      textBinder.bind(key, value).map(ServiceName(_))
    }

    override def unbind(key: String, serviceName: ServiceName): String = {
      serviceName.value
    }
  }

  implicit def apiCategoryQueryStringBinder(implicit textBinder: QueryStringBindable[String]): QueryStringBindable[ApiCategory] = new QueryStringBindable[ApiCategory] {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ApiCategory]] = {
      for {
        version <- textBinder.bind("categoryFilters", params)
      } yield {
        version match {
          case Right(version) => Right(ApiCategory.unsafeApply(version))
          case _              => Left("Unable to bind an api category")
        }
      }
    }

    override def unbind(key: String, category: ApiCategory): String = {
      textBinder.unbind("categoryFilters", category.toString)
    }
  }

  implicit def documentationTypeQueryStringBinder(implicit textBinder: QueryStringBindable[String]): QueryStringBindable[DocumentationTypeFilter] =
    new QueryStringBindable[DocumentationTypeFilter] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, DocumentationTypeFilter]] = {
        for {
          result <- textBinder.bind("docTypeFilters", params)
        } yield {
          result match {
            case Right(filter) => Right(DocumentationTypeFilter.unsafeApply(filter))
            case _             => Left("Unable to bind an api version")
          }
        }
      }

      override def unbind(key: String, filter: DocumentationTypeFilter): String = {
        textBinder.unbind("docTypeFilters", filter.toString)
      }
    }
}
