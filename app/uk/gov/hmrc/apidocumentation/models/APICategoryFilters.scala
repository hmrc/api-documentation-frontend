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

import play.api.libs.json.*
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiCategory

object APICategoryFilters {

  val filterMap = Map[String, ApiCategory](
    ("example"                      -> ApiCategory.Example),
    ("agents"                       -> ApiCategory.Agents),
    ("business-rates"               -> ApiCategory.BusinessRates),
    ("charities"                    -> ApiCategory.Charities),
    ("construction-industry-scheme" -> ApiCategory.ConstructionIndustryScheme),
    ("corporation-tax"              -> ApiCategory.CorporationTax),
    ("customs"                      -> ApiCategory.Customs),
    ("estates"                      -> ApiCategory.Estates),
    ("help-to-save"                 -> ApiCategory.HelpToSave),
    ("income-tax"                   -> ApiCategory.IncomeTaxMtd),
    ("lifetime-isa"                 -> ApiCategory.LifetimeIsa),
    ("marriage-allowance"           -> ApiCategory.MarriageAllowance),
    ("national-insurance"           -> ApiCategory.NationalInsurance),
    ("paye"                         -> ApiCategory.Paye),
    ("pensions"                     -> ApiCategory.Pensions),
    ("private-government"           -> ApiCategory.PrivateGovernment),
    ("relief-at-source"             -> ApiCategory.ReliefAtSource),
    ("self-assessment"              -> ApiCategory.SelfAssessment),
    ("stamp-duty"                   -> ApiCategory.StampDuty),
    ("trusts"                       -> ApiCategory.Trusts),
    ("vat"                          -> ApiCategory.VatMtd),
    ("vat"                          -> ApiCategory.Vat),
    ("other"                        -> ApiCategory.Other)
  )

  def fromFilter(filter: String): Option[ApiCategory] = filterMap.get(filter)

  lazy val categoryMap: Map[String, Seq[ApiCategory]] =
    Json.parse(Source.fromInputStream(getClass.getResourceAsStream("/categories.json")).mkString).as[Map[String, Seq[ApiCategory]]]
}
