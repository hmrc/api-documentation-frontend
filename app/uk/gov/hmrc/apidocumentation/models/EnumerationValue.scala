package uk.gov.hmrc.apidocumentation.models

import play.api.libs.json._

case class EnumerationValue(value: String)

object EnumerationValue {

  implicit val format: Format[EnumerationValue] = new Format[EnumerationValue] {
    override def writes(o: EnumerationValue): JsValue = Json.writes[EnumerationValue].writes(o)

    override def reads(json: JsValue): JsResult[EnumerationValue] = json match {
      case JsNumber(value)  => JsSuccess(EnumerationValue(value.toString))
      case JsBoolean(value) => JsSuccess(EnumerationValue(value.toString))
      case JsString(value)  => JsSuccess(EnumerationValue(value))
      case JsObject(_)      => JsError("Unsupported enum format (Json object): use NUMBER, BOOLEAN OR STRING")
      case JsArray(_)       => JsError("Unsupported enum format (Json array): use NUMBER, BOOLEAN OR STRING")
      case JsNull           => JsError("Unsupported enum format (Json null): use NUMBER, BOOLEAN OR STRING")
    }
  }
}
