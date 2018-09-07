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
import uk.gov.hmrc.apidocumentation.models.{DocsVisibility, ExtendedAPIVersion}
import uk.gov.hmrc.apidocumentation.views.helpers.VersionDocsVisible

import scala.collection.JavaConversions._

package object services {

  type RAML = Api

  implicit class RicherRAML(val x: Api) {

    def documentationForVersion(version: Option[ExtendedAPIVersion]): Seq[DocumentationItem] = versionVisibility(version) match {
      case DocsVisibility.VISIBLE => x.documentation.toSeq
      case DocsVisibility.OVERVIEW_ONLY => x.documentation.filter(_.title.value == "Overview")
      case _ => Seq.empty
    }

    private def versionVisibility(version: Option[ExtendedAPIVersion]): DocsVisibility.Value = version match {
      case Some(v) => VersionDocsVisible(v.visibility)
      case _ => DocsVisibility.VISIBLE
    }
  }

}
