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

package unit.uk.gov.hmrc.apidocumentation.services

import uk.gov.hmrc.apidocumentation.models.JsonSchema
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json
import uk.gov.hmrc.apidocumentation.services.RAML
import uk.gov.hmrc.apidocumentation.services.SchemaService
import unit.uk.gov.hmrc.apidocumentation.utils.FileRamlLoader

import scala.io.Source
import scala.util.Success

class TestSchemaService extends SchemaService {
  override def fetchPlainTextSchema(uri: String): String = {
    Source.fromFile(uri).mkString
  }
}

class SchemaServiceSpec extends WordSpec with Matchers {

  def loader = new TestSchemaService()

  "The SchemaLoader" should {

    "load a schema that has no refs" in {
      val raml = loadRaml("test/resources/schemaloader/input/norefs.raml")
      val expectedPlainText = loadPlainText("test/resources/schemaloader/input/schemas/norefs-schema.json")
      val expectedSchema = loadSchema("test/resources/schemaloader/expected/expected-norefs-schema.json")

      val actual = loader.loadSchemas("test/resources/schemaloader/input/schemas", raml)

      actual should have size 1
      actual shouldBe Map(expectedPlainText -> expectedSchema)
    }

    "load a schema that has internal refs" in {
      val raml = loadRaml("test/resources/schemaloader/input/internalrefs.raml")
      val expectedPlainText = loadPlainText("test/resources/schemaloader/input/schemas/internalrefs-schema.json")
      val expectedSchema = loadSchema("test/resources/schemaloader/expected/expected-internalrefs-schema.json")

      val actual = loader.loadSchemas("test/resources/schemaloader/input/schemas", raml)

      actual should have size 1
      actual shouldBe Map(expectedPlainText -> expectedSchema)
    }

    "load a schema that has nested internal refs" in {
      val raml = loadRaml("test/resources/schemaloader/input/nestedinternalrefs.raml")
      val expectedPlainText = loadPlainText("test/resources/schemaloader/input/schemas/nestedinternalrefs-schema.json")
      val expectedSchema = loadSchema("test/resources/schemaloader/expected/expected-nestedinternalrefs-schema.json")

      val actual = loader.loadSchemas("test/resources/schemaloader/input/schemas", raml)

      actual should have size 1
      actual shouldBe Map(expectedPlainText -> expectedSchema)
    }

    "load a schema that has external refs" in {
      val raml = loadRaml("test/resources/schemaloader/input/externalrefs.raml")
      val expectedPlainText = loadPlainText("test/resources/schemaloader/input/schemas/externalrefs-schema.json")
      val expectedSchema = loadSchema("test/resources/schemaloader/expected/expected-externalrefs-schema.json")

      val actual = loader.loadSchemas("test/resources/schemaloader/input/schemas", raml)

      actual should have size 1
      actual shouldBe Map(expectedPlainText -> expectedSchema)
    }

    "load a schema that has a chain of external refs" in {
      val raml = loadRaml("test/resources/schemaloader/input/complexrefs.raml")
      val expectedPlainText = loadPlainText("test/resources/schemaloader/input/schemas/complexrefs-schema.json")
      val expectedSchema = loadSchema("test/resources/schemaloader/expected/expected-complexrefs-schema.json")

      val actual = loader.loadSchemas("test/resources/schemaloader/input/schemas", raml)

      actual should have size 1
      actual shouldBe Map(expectedPlainText -> expectedSchema)
    }

  }

  private def loadSchema(file: String): JsonSchema = {
    Json.parse(Source.fromFile(file).mkString).as[JsonSchema]
  }

  private def loadPlainText(file: String): String = {
    Source.fromFile(file).mkString
  }

  private def loadRaml(path: String): RAML = {
    new FileRamlLoader().load(path) match {
      case Success(raml) => raml
      case _ => throw new IllegalStateException("Could not load RAML")
    }
  }
}
