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
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiCategory

object APICategoryFilters {
  import ApiCategory._

  val filterMap = Map[String, ApiCategory](
    ("example"                      -> EXAMPLE),
    ("agents"                       -> AGENTS),
    ("business-rates"               -> BUSINESS_RATES),
    ("charities"                    -> CHARITIES),
    ("construction-industry-scheme" -> CONSTRUCTION_INDUSTRY_SCHEME),
    ("corporation-tax"              -> CORPORATION_TAX),
    ("customs"                      -> CUSTOMS),
    ("estates"                      -> ESTATES),
    ("help-to-save"                 -> HELP_TO_SAVE),
    ("income-tax"                   -> INCOME_TAX_MTD),
    ("lifetime-isa"                 -> LIFETIME_ISA),
    ("marriage-allowance"           -> MARRIAGE_ALLOWANCE),
    ("national-insurance"           -> NATIONAL_INSURANCE),
    ("paye"                         -> PAYE),
    ("pensions"                     -> PENSIONS),
    ("private-government"           -> PRIVATE_GOVERNMENT),
    ("relief-at-source"             -> RELIEF_AT_SOURCE),
    ("self-assessment"              -> SELF_ASSESSMENT),
    ("stamp-duty"                   -> STAMP_DUTY),
    ("trusts"                       -> TRUSTS),
    ("vat"                          -> VAT_MTD),
    ("vat"                          -> VAT),
    ("other"                        -> OTHER)
  )

  def fromFilter(filter: String): Option[ApiCategory] = filterMap.get(filter)

  lazy val categoryMap: Map[String, Seq[ApiCategory]] =
    Json.parse(Source.fromInputStream(getClass.getResourceAsStream("/categories.json")).mkString).as[Map[String, Seq[ApiCategory]]]
}
