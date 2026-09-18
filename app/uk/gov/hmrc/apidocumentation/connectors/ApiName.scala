package uk.gov.hmrc.apidocumentation.connectors

opaque type ApiName <: String = String

object ApiName {
  def apply(text: String): ApiName = text
}
