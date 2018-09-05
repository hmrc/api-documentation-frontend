/*
 * Copyright 2018 HM Revenue & Customs
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

package acceptance.uk.gov.hmrc.apidocumentation

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.openqa.selenium.WebDriver
import org.scalatest._
import org.scalatestplus.play.OneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Mode}

trait TestSpec extends FeatureSpec with BeforeAndAfterEach with BeforeAndAfterAll with Matchers with NavigationSugar {
  implicit val webDriver: WebDriver = Env.driver

  val stubHost = "localhost"
  val stubPort = sys.env.getOrElse("WIREMOCK_PORT", "11111").toInt
  val wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(stubPort))

  override def beforeAll() = {
    if (!wireMockServer.isRunning) {
      wireMockServer.start()
    }

    WireMock.configureFor(stubHost, stubPort)
  }

  override def afterAll() = {
    if (wireMockServer.isRunning) {
      wireMockServer.stop()
    }
  }
}

trait BaseSpec extends TestSpec with OneServerPerSuite {
  override lazy val port = 6001

  implicit override lazy val app: Application =
    GuiceApplicationBuilder().configure("run.mode" -> "Stub").in(Mode.Prod).build()
}
