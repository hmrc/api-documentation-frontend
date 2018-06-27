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

import java.util.logging.{Level, Logger}

import cucumber.api.scala.{EN, ScalaDsl}
import org.openqa.selenium._
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxProfile}
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.openqa.selenium.remote.DesiredCapabilities
import org.scalatest.Matchers

import scala.util.Try

trait Env extends ScalaDsl with EN with Matchers {

  val port = 9680
  var host: String = _

  val hostIs = System.getProperty("env", "local").toLowerCase
  hostIs match {
    case "local" => host = s"http://localhost:$port"
  }

  val webDriverConfig = Option(System.getenv("test_driver")).getOrElse("firefox")
  val driver = if (webDriverConfig == "firefox") {
    val driver: WebDriver with HasCapabilities = {
      val profile = new FirefoxProfile
      profile.setAcceptUntrustedCertificates(true)
      new FirefoxDriver(profile)
    }
    driver
  } else if (webDriverConfig == "chrome"){
    val driver: WebDriver = {
      val driver = new ChromeDriver()
      driver.manage().deleteAllCookies()
      driver.manage().window().fullscreen()
      driver
    }
    driver
  } else {
    val driver: WebDriver = {
      val capabilities = DesiredCapabilities.htmlUnit()
      capabilities.setJavascriptEnabled(true)
      Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF)
      Logger.getLogger("com.gargoylesoftware.htmlunit.javascript.StrictErrorReporter").setLevel(Level.OFF)
      new HtmlUnitDriver(capabilities)
    }
    driver
  }

  sys addShutdownHook {
    Try(driver.quit())
  }
}

object Env extends Env
