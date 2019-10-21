/*
 * Copyright 2019 HM Revenue & Customs
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

package unit.uk.gov.hmrc.apidocumentation.config

import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.play.test.UnitSpec
class AppConfigSpec extends UnitSpec with GuiceOneAppPerTest with MockitoSugar {

  trait Setup {
    val appConfig = app.injector.instanceOf[ApplicationConfig]
  }

  override def fakeApplication: Application = {
    val prefix1 = "microservice.services.fake-service-sandbox"
    val prefix2 = "microservice.services.fake-service-production"

      new GuiceApplicationBuilder()
      .configure(
  s"$prefix1.use-proxy" -> true,
        s"$prefix1.bearer-token" -> "aFakeBearerToken",
        s"$prefix1.api-key" -> "aFakeKey",
        s"$prefix1.host" -> "aHost",
        s"$prefix1.port" -> "54321",
        s"$prefix1.context" -> "someContext",

        s"$prefix2.use-proxy" -> false,
        s"$prefix2.host" -> "anotherHost",
        s"$prefix2.port" -> "12345",

        "bob" -> "nothing"
      )
      .build()
  }

  "AppConfig" should {
    val proxiedService = "fake-service-sandbox"
    val directService = "fake-service-production"
    "handle useProxy" in new Setup {
      appConfig.useProxy(proxiedService) shouldBe true
      appConfig.useProxy(directService) shouldBe false
    }

    "handle bearerToken" in new Setup {
      appConfig.bearerToken(proxiedService) shouldBe "aFakeBearerToken"
      appConfig.bearerToken(directService) shouldBe ""
    }

    "handle apiKey" in new Setup {
      appConfig.apiKey(proxiedService) shouldBe "aFakeKey"
      appConfig.apiKey(directService) shouldBe ""
    }

    "handle serviceUrl for proxied service" in new Setup {
      val baseUrl = appConfig.baseUrl(proxiedService)
      val serviceUrl = appConfig.serviceUrl("fake-service")(proxiedService)
      serviceUrl should include("aHost:54321")
      serviceUrl should include("someContext")
    }

    "handle serviceUrl for non-proxied service" in new Setup {
      val baseUrl = appConfig.baseUrl(directService)
      val serviceUrl = appConfig.serviceUrl("fake-service")(directService)
      serviceUrl should include("anotherHost:12345")
      serviceUrl should not include("someContext")
    }
  }
}
