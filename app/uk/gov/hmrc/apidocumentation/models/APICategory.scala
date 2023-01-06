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

package uk.gov.hmrc.apidocumentation.models

import scala.io.Source

import play.api.libs.json._

import uk.gov.hmrc.apidocumentation.models.JsonFormatters._

object APICategory extends Enumeration {
  type APICategory = Value

  protected case class Val(displayName: String, filter: String) extends super.Val
  implicit def valueToAPICategoryVal(x: Value): Val = x.asInstanceOf[Val]

  val EXAMPLE                      = Val("Example", "example")
  val AGENTS                       = Val("Agents", "agents")
  val BUSINESS_RATES               = Val("Business Rates", "business-rates")
  val CHARITIES                    = Val("Charities", "charities")
  val CONSTRUCTION_INDUSTRY_SCHEME = Val("Construction Industry Scheme", "construction-industry-scheme")
  val CORPORATION_TAX              = Val("Corporation Tax", "corporation-tax")
  val CUSTOMS                      = Val("Customs", "customs")
  val ESTATES                      = Val("Estates", "estates")
  val HELP_TO_SAVE                 = Val("Help to Save", "help-to-save")
  val INCOME_TAX_MTD               = Val("Income Tax (Making Tax Digital)", "income-tax")
  val LIFETIME_ISA                 = Val("Lifetime ISA", "lifetime-isa")
  val MARRIAGE_ALLOWANCE           = Val("Marriage Allowance", "marriage-allowance")
  val NATIONAL_INSURANCE           = Val("National Insurance", "national-insurance")
  val PAYE                         = Val("PAYE", "paye")
  val PENSIONS                     = Val("Pensions", "pensions")
  val PRIVATE_GOVERNMENT           = Val("Private Government", "private-government")
  val RELIEF_AT_SOURCE             = Val("Relief at Source", "relief-at-source")
  val SELF_ASSESSMENT              = Val("Self Assessment", "self-assessment")
  val STAMP_DUTY                   = Val("Stamp Duty", "stamp-duty")
  val TRUSTS                       = Val("Trusts", "trusts")
  val VAT_MTD                      = Val("VAT (Making Tax Digital)", "vat")
  val VAT                          = Val("VAT", "vat")

  val OTHER = Val("Other", "other")

  def fromFilter(filter: String) = values.find(_.filter == filter)

  lazy val categoryMap: Map[String, Seq[APICategory]] =
    Json.parse(Source.fromInputStream(getClass.getResourceAsStream("/categories.json")).mkString).as[Map[String, Seq[APICategory]]]
}
