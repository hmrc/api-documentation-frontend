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

package uk.gov.hmrc.apidocumentation.views.templates

import javax.inject.{Inject, Singleton}

import views.html.helper

import play.api.Configuration
import play.api.mvc.RequestHeader

@Singleton
class FooterConfig @Inject() (config: Configuration) {

  private lazy val urlFooterConfig = config.underlying.getConfig("urls.footer")

  lazy val cookies: String             = urlFooterConfig.getString("cookies")
  lazy val privacy: String             = urlFooterConfig.getString("privacy")
  lazy val termsConditions: String     = urlFooterConfig.getString("termsConditions")
  lazy val govukHelp: String           = urlFooterConfig.getString("govukHelp")
  lazy val serviceAvailability: String = urlFooterConfig.getString("serviceAvailability")

  def accessibility(implicit requestHeader: RequestHeader): String =
    s"${urlFooterConfig.getString("accessibility")}/hmrc-developer-hub?referrerUrl=${helper.urlEncode(requestHeader.uri)}"
}
