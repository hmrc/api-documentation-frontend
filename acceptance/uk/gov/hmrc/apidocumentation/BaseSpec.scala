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

package uk.gov.hmrc.apidocumentation

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.openqa.selenium.WebDriver

import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Mode}
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.BeforeAndAfterEach
import org.scalatest.featurespec.AnyFeatureSpec
import play.api.test.RunningServer
import uk.gov.hmrc.apidocumentation.common.MyTestServerFactory
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

trait BaseSpec extends AnyFeatureSpec with BeforeAndAfterEach with BeforeAndAfterAll with Matchers with NavigationSugar
  with GuiceOneServerPerSuite {

  val stubPort = 11111
  val stubHost = "localhost"

  override protected implicit lazy val runningServer: RunningServer = MyTestServerFactory.start(app)

  implicit lazy val webDriver: WebDriver = Env.driver

  var wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))

  override def fakeApplication(): Application = {
    GuiceApplicationBuilder()
      .configure(
        "microservice.services.developer-frontend.host" -> stubHost,
        "microservice.services.developer-frontend.port" -> stubPort,
        "microservice.services.api-platform-microservice.host" -> stubHost,
        "microservice.services.api-platform-microservice.port" -> stubPort,
        "microservice.services.third-party-developer.host" -> stubHost,
        "microservice.services.third-party-developer.port" -> stubPort,
        "microservice.services.api-platform-xml-services.host" -> stubHost,
        "microservice.services.api-platform-xml-services.port" -> stubPort
      )
      .in(Mode.Prod)
      .build()
  }

  override def beforeAll() = {
    wireMockServer.start()
    WireMock.configureFor(stubHost, stubPort)
  }

  override def afterAll() = {
    wireMockServer.stop()
  }

  override def beforeEach() = {
    webDriver.manage().deleteAllCookies()
    WireMock.reset()
  }
}
