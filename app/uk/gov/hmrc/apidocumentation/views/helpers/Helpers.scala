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

package uk.gov.hmrc.apidocumentation.views.helpers

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneOffset}
import scala.language.reflectiveCalls

import play.twirl.api.Html
import uk.gov.hmrc.apiplatform.modules.apis.domain.models._

import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.models._

object Slugify {
  def apply(text: String): String = makeSlug(text)

  // scalastyle:off structural.type
  def apply(obj: { def value(): String }): String = Option(obj).fold("")(obj => makeSlug(obj.value()))
  // scalastyle:on structural.type

  private def makeSlug(text: String) = Option(text).fold("") { obj =>
    obj.replaceAll("[^\\w\\s]", "").replaceAll("\\s+", "-").toLowerCase
  }
}

object Markdown {

  def apply(text: String): Html = Html(process(text))

  def apply(text: Option[String]): Html = apply(text.getOrElse(""))

  // scalastyle:off structural.type
  def apply(obj: { def value(): String }): Html = Option(obj).fold(emptyHtml)(node => apply(node.value()))
  // scalastyle:on structural.type

  import com.github.rjeschke.txtmark.{Configuration, Processor}
  import org.markdown4j._

  private val emptyHtml = Html("")

  private def configuration =
    Configuration.builder
      .forceExtentedProfile
      .registerPlugins(new YumlPlugin, new WebSequencePlugin, new IncludePlugin)
      .setDecorator(new ExtDecorator()
        .addStyleClass("list list-bullet", "ul")
        .addStyleClass("list list-number", "ol")
        .addStyleClass("code--slim", "code")
        .addStyleClass("heading-large", "h1")
        .addStyleClass("heading-medium", "h2")
        .addStyleClass("heading-small", "h3")
        .addStyleClass("heading-small", "h4"))
      .setCodeBlockEmitter(new CodeBlockEmitter)

  private def process(text: String) = Processor.process(text, configuration.build)
}

object AvailabilityPhrase {
  val yes             = "Yes"
  val yesPrivateTrial = "Yes - private trial"
  val no              = "No"
}

object EndpointsAvailable {

  def apply(availability: Option[ApiAvailability]): String = availability match {
    case Some(ApiAvailability(endpointsEnabled, access, _, authorised)) if endpointsEnabled =>
      access match {
        case ApiAccess.PUBLIC                       => AvailabilityPhrase.yes
        case ApiAccess.Private(true)                => AvailabilityPhrase.yesPrivateTrial
        case ApiAccess.Private(false) if authorised => AvailabilityPhrase.yes
        case _                                      => AvailabilityPhrase.no
      }
    case _                                                                                  => AvailabilityPhrase.no
  }
}

object ShowBaseURL {

  def apply(availability: Option[ApiAvailability]) = EndpointsAvailable(availability) match {
    case AvailabilityPhrase.yes | AvailabilityPhrase.yesPrivateTrial => true
    case _                                                           => false
  }
}

object VersionDocsVisible {

  def apply(availability: Option[VersionVisibility]): DocsVisibility = availability match {
    case Some(VersionVisibility(ApiAccessType.PUBLIC, _, _, _))         => DocsVisibility.VISIBLE       // PUBLIC
    case Some(VersionVisibility(ApiAccessType.PRIVATE, true, true, _))  => DocsVisibility.VISIBLE       // PRIVATE, logged in, whitelisted (authorised)
    case Some(VersionVisibility(ApiAccessType.PRIVATE, _, false, true)) => DocsVisibility.OVERVIEW_ONLY // PRIVATE, trial, either not logged in or not whitelisted (authorised)
    case _                                                              => DocsVisibility.NOT_VISIBLE
  }

  def apply(version: ExtendedApiVersion): DocsVisibility = VersionVisibility(version) match {
    case Some(VersionVisibility(ApiAccessType.PUBLIC, _, _, _))         => DocsVisibility.VISIBLE       // PUBLIC
    case Some(VersionVisibility(ApiAccessType.PRIVATE, true, true, _))  => DocsVisibility.VISIBLE       // PRIVATE, logged in, whitelisted (authorised)
    case Some(VersionVisibility(ApiAccessType.PRIVATE, _, false, true)) => DocsVisibility.OVERVIEW_ONLY // PRIVATE, trial, either not logged in or not whitelisted (authorised)
    case _                                                              => DocsVisibility.NOT_VISIBLE
  }
}

object ShowAvailabilityInEnvironment {

  def apply(sandboxAvailability: String, productionAvailability: String, applicationConfig: ApplicationConfig): String =
    (sandboxAvailability, productionAvailability) match {
      case ((AvailabilityPhrase.yes | AvailabilityPhrase.yesPrivateTrial), (AvailabilityPhrase.yes | AvailabilityPhrase.yesPrivateTrial)) =>
        s"${applicationConfig.nameOfSubordinateEnvironment} and ${applicationConfig.nameOfPrincipalEnvironment}"
      case (AvailabilityPhrase.no, (AvailabilityPhrase.yes | AvailabilityPhrase.yesPrivateTrial))                                         => applicationConfig.nameOfPrincipalEnvironment
      case ((AvailabilityPhrase.yes | AvailabilityPhrase.yesPrivateTrial), AvailabilityPhrase.no)                                         => applicationConfig.nameOfSubordinateEnvironment
      case (AvailabilityPhrase.no, AvailabilityPhrase.no)                                                                                 => "Not applicable"
      case (_, _)                                                                                                                         => "Not applicable"
    }
}

object DateFormatter {

  val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")

  def getFormattedDate(date: Option[Instant]): String = {
    date match {
      case None             => ""
      case Some(d: Instant) => dateFormatter.format(d.atOffset(ZoneOffset.UTC).toLocalDateTime)
    }
  }
}
