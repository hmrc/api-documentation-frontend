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

package uk.gov.hmrc.apidocumentation.views.helpers

import play.twirl.api.{Html, HtmlFormat}

object FieldHelpers {

  def build(
             fields: List[RequestResponseField],
             fieldOptionalityKnown: Boolean,
             key: Option[String] = None,
             built: List[Html] = Nil
           ): Html = {
    fields match {
      case Nil          =>  HtmlFormat.fill(built)
      case head :: tail =>
        val b: List[Html] = uk.gov.hmrc.apidocumentation.views.html.raml.field(head, fieldOptionalityKnown, key) +: head.oneOf.map {
          case (k, values) =>
            build(
              fields = values.toList,
              fieldOptionalityKnown = fieldOptionalityKnown,
              key = appendKey(key, k.toLowerCase.replaceAll(" ", "-")),
              built = List(uk.gov.hmrc.apidocumentation.views.html.raml.one_of_field(k, head.depth, fieldOptionalityKnown, key)))
        }.toList

       build(tail, fieldOptionalityKnown, key, built ::: b)
    }
  }

  private def appendKey(key: Option[String], k: String): Option[String] = {
    Some(key.fold(k)(_ + s" $k"))
  }
}
