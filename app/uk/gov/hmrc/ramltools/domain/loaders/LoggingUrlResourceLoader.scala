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

package uk.gov.hmrc.ramltools.loaders

import java.io.{BufferedInputStream, IOException, InputStream}
import java.net.{URISyntaxException, URL}

import javax.annotation.Nullable
import org.raml.v2.api.loader.{ResourceLoaderExtended, ResourceUriCallback}
import play.api.Logger

class LoggingUrlResourceLoader extends ResourceLoaderExtended {
  override def fetchResource(resourceName: String, callback: ResourceUriCallback): InputStream = {
    var inputStream: InputStream = null
    try {
      val url = new URL(resourceName)
      inputStream = new BufferedInputStream(url.openStream)
      if (callback != null) callback.onResourceFound(url.toURI)
    }
    catch {
      case e: IOException => Logger.error(s"Failed Loader ${e}", e)
      case e: URISyntaxException => Logger.error(s"Failed Loader ${e}", e)
    }
    inputStream
  }

  @Nullable override def fetchResource(resourceName: String): InputStream = fetchResource(resourceName, null)
}

