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

package uk.gov.hmrc.ramltools.domain.loaders

import java.io.{File, InputStream}

import org.raml.v2.api.loader.ClassPathResourceLoader

class AlternateClasspathResourceLoader(prefix: String = "") extends ClassPathResourceLoader {
  override def fetchResource(resourceName: String): InputStream = {
    val path: String = new File(prefix, resourceName).getPath.substring(1)
    super.fetchResource(path)
  }
}
