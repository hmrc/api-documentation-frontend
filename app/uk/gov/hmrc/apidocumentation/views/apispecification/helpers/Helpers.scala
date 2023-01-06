/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.apidocumentation.views.apispecification.helpers

import scala.util.Try

import play.api.libs.json.Json
import play.libs.XML

import uk.gov.hmrc.apidocumentation.models.ErrorResponse
import uk.gov.hmrc.apidocumentation.models.apispecification._

object BodyExamples {

  def apply(body: TypeDeclaration): Seq[ExampleSpec] = {
    if (body.examples.size > 0) body.examples
    else {
      body.example match {
        case Some(e) => Seq(e)
        case None    => Seq.empty
      }
    }
  }
}

object Responses {
  def success(method: Method) = method.responses.filter(isSuccessResponse)

  def error(method: Method) = method.responses.filter(isErrorResponse)

  def hasResponseExample(response: Response) = {
    response.body.exists { responseBody =>
      responseBody.example.exists { example =>
        example
          .value
          .filter(_.trim.nonEmpty)
          .isDefined
      }
    }
  }

  private def isSuccessResponse(response: Response) = {
    response.code.startsWith("2") || response.code.startsWith("3")
  }

  private def isErrorResponse(response: Response) = {
    response.code.startsWith("4") || response.code.startsWith("5")
  }
}

object ErrorScenarios {

  private def errorResponse2(example: uk.gov.hmrc.apidocumentation.models.apispecification.ExampleSpec): Option[ErrorResponse] = {
    example.code.fold(responseFromBody2(example))(code => Some(ErrorResponse(code = Some(code))))
  }

  private def responseFromBody2(example: ExampleSpec): Option[ErrorResponse] = {
    responseFromJson2(example).orElse(responseFromXML2(example))
  }

  private def responseFromJson2(example: ExampleSpec): Option[ErrorResponse] = {
    import uk.gov.hmrc.apidocumentation.models.jsonFormatters._
    example.value.flatMap(v => Try(Json.parse(v).as[ErrorResponse]).toOption)
  }

  private def responseFromXML2(example: ExampleSpec): Option[ErrorResponse]  = {
    for {
      v     <- example.value
      codes <- Try(XML.fromString(v).getElementsByTagName("code")).toOption
      first <- Option(codes.item(0))
    } yield {
      ErrorResponse(Some(first.getTextContent))
    }
  }

  def apply(method: Method): Seq[Map[String, String]] = {

    val errorScenarios = for {
      response            <- Responses.error(method)
      body                <- response.body
      example             <- BodyExamples(body)
      scenarioDescription <- scenarioDescription(body, example)
      errorResponse       <- errorResponse2(example)
    } yield {
      errorResponse.code.fold(
        Map("scenario" -> scenarioDescription, "code" -> "", "httpStatus" -> response.code)
      )(code =>
        Map("scenario" -> scenarioDescription, "code" -> code, "httpStatus" -> response.code)
      )
    }
    errorScenarios
  }

  private def scenarioDescription(body: TypeDeclaration, example: ExampleSpec): Option[String] = {
    example.description.orElse(body.description)
  }
}
