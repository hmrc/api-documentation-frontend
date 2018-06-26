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

package uk.gov.hmrc.apidocumentation.services

import org.raml.v2.api.loader._
import org.raml.v2.api.{RamlModelBuilder, RamlModelResult}

import scala.collection.JavaConversions._
import scala.util.{Failure, Success, Try}

case class RamlParseException(msg: String) extends RuntimeException(msg)
case class RamlNotFoundException(msg: String) extends RuntimeException(msg)
case class RamlUnsupportedVersionException(msg: String) extends RuntimeException(msg)

trait RamlLoader {

  private val RamlDoesNotExist = "Raml does not exist at:"
  private val unsupportedSpecVersion: Try[RAML] = Failure(RamlUnsupportedVersionException("Only RAML1.0 is supported"))

  def load(resource: String): Try[RAML]

  protected def verify(result: RamlModelResult): Try[RAML] = {
    result.getValidationResults.toSeq match {
      case Nil => Option(result.getApiV10).fold(unsupportedSpecVersion) { api => Success(api) }
      case errors => {
        val msg = errors.map(e => transformError(e.toString)).mkString("; ")
        if (msg.contains(RamlDoesNotExist)) Failure(RamlNotFoundException(msg))
        else Failure(RamlParseException(msg))
      }
    }
  }

  protected def transformError(msg: String) = msg
}

class UrlRewritingRamlLoader(urlRewriter: UrlRewriter) extends RamlLoader {

  override def load(url: String) = {
    val builder = new RamlModelBuilder(new UrlRewritingResourceLoader(urlRewriter))
    verify(builder.buildApi(url))
  }

  override def transformError(msg: String) = urlRewriter.rewriteUrl(msg)
}

class UrlRewritingResourceLoader(urlRewriter: UrlRewriter) extends UrlResourceLoader {
  override def fetchResource(resourceName: String, callback: ResourceUriCallback) = {
    super.fetchResource(urlRewriter.rewriteUrl(resourceName), callback)
  }
}

trait UrlRewriter {
  val rewrites: Map[String,String]

  def rewriteUrl(url: String) = {
    rewrites.foldLeft(url)((currentUrl, rewrite) => currentUrl.replaceAll(rewrite._1, rewrite._2))
  }
}
