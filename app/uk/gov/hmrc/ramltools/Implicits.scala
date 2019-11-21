/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.ramltools

import org.raml.v2.api.model.v10.api.Api
import org.raml.v2.api.model.v10.resources.Resource
import scala.collection.JavaConversions._

object Implicits {

  implicit class RichRAML(val x: Api) {

    def flattenedResources(): List[Resource] = {
      flatten(x.resources().toList)
    }

    private def flatten(resources: List[Resource], acc: List[Resource]=Nil): List[Resource] = resources match {
      case head :: tail => flatten(head.resources.toList ++ tail, acc :+ head)
      case _ => acc
    }

  }

}
