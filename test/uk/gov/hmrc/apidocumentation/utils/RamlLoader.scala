/*
 * Copyright 2020 HM Revenue & Customs
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

package unit.uk.gov.hmrc.apidocumentation.utils

import java.io.File

import org.raml.v2.api.RamlModelBuilder
import org.raml.v2.api.loader.FileResourceLoader
import uk.gov.hmrc.ramltools.loaders.RamlLoader

class StringRamlLoader extends RamlLoader {
  override def load(content: String) = {
    val builder = new RamlModelBuilder()
    val api = builder.buildApi(content, "")
    verify(api)
  }
}

class FileRamlLoader extends RamlLoader {
  override def load(filepath: String) = {
    val file = new File(filepath)
    val ramlRoot = file.getParentFile
    val filename = file.getName
    val builder = new RamlModelBuilder(new FileResourceLoader(ramlRoot))
    verify(builder.buildApi(filename))
  }
}

