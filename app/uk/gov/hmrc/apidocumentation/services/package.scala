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

package uk.gov.hmrc.apidocumentation

import org.raml.v2.api.model.v10.api.{Api, DocumentationItem}
import org.raml.v2.api.model.v10.resources.Resource
import uk.gov.hmrc.apidocumentation.models.{APIAccessType, DocsVisibility, ExtendedAPIVersion, VersionVisibility}
import uk.gov.hmrc.apidocumentation.views.helpers.VersionDocsVisible

import scala.collection.JavaConversions._

package object services {
  type RAML = Api

  implicit class RichRAML(val x: Api) {
    def flattenedResources() = {
      flatten(x.resources().toList)
    }

    def documentationForVersion(version: Option[ExtendedAPIVersion]): Seq[DocumentationItem] = versionVisibility(version) match {
      case DocsVisibility.VISIBLE => x.documentation.toSeq
      case DocsVisibility.OVERVIEW_ONLY => x.documentation.filter(_.title.value == "Overview")
      case _ => Seq.empty
    }

    private def flatten(resources: List[Resource], acc: List[Resource]=Nil): List[Resource] = resources match {
      case head :: tail => flatten(head.resources.toList ++ tail, acc :+ head)
      case _ => acc
    }

    private def versionVisibility(version: Option[ExtendedAPIVersion]): DocsVisibility.Value = version match {
      case Some(v) => VersionDocsVisible(v.visibility)
      case _ => DocsVisibility.VISIBLE
    }
  }
}
