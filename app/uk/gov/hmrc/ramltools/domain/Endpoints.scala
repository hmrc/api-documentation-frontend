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

package uk.gov.hmrc.ramltools.domain

import org.raml.v2.api.model.v10.methods.Method
import org.raml.v2.api.model.v10.resources.Resource
import uk.gov.hmrc.ramltools.Implicits.RichRAML
import uk.gov.hmrc.ramltools.RAML

import scala.collection.JavaConversions._
import scala.util.matching.Regex

case class QueryParam(name: String, required: Boolean)

case class Endpoint(uriPattern: String, endpointName: String, method: String,
                    authType: String, throttlingTier: String, scope: Option[String],
                    queryParameters: Option[Seq[QueryParam]])

object Endpoints {

  def apply(raml: RAML, context: Option[String]): Seq[Endpoint] = {
    for {
      endpoint <- raml.flattenedResources()
      method <- endpoint.methods
    } yield {
      Endpoint(
        getUriPattern(context, endpoint),
        method.displayName.value,
        method.method.toUpperCase,
        getAuthType(method),
        getThrottlingTier(method),
        getScope(method),
        getQueryParams(method)
      )
    }
  }

  private def getAuthType(method: Method): String = {
    method.securedBy.toList.map(_.securityScheme.name) match {
      case Seq() => "NONE"
      case "oauth_2_0" :: _ => "USER"
      case "x-application" :: _ => "APPLICATION"
    }
  }

  private def getScope(method: Method): Option[String] = {
    method.annotations.toList.filter(_.name.matches("\\((.*\\.)?scope\\)")).map(a =>
      a.structuredValue.value.toString
    ).headOption
  }

  private def getUriPattern(context: Option[String], endpoint: Resource): String = {
    endpoint.resourcePath.replaceFirst(getPrefix(context), "")
  }

  private def getPrefix(context: Option[String]): String = {
    context.map(cx => s"^/${Regex.quote(cx)}").getOrElse("")
  }

  private def getThrottlingTier(method: Method): String = {
    method.annotations.toList.filter(_.name.matches("\\((.*\\.)?throttlingTier\\)")) match {
      case List(tier) => tier.structuredValue().value().toString
      case _ => "UNLIMITED"
    }
  }

  private def getQueryParams(method: Method): Option[Seq[QueryParam]] = {
    val qps = method.queryParameters().toList.map(param => QueryParam(param.name(), param.required().booleanValue()))

    if (qps.isEmpty) None else Some(qps)
  }
}
