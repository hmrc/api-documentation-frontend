package uk.gov.hmrc.apidocumentation

import uk.gov.hmrc.apidocumentation.specs.ComponentTestsSpec
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor1}

trait TableDrivenPropertyMocks extends TableDrivenPropertyChecks { cs: ComponentTestsSpec =>
  def helloWorldVersionsIsDeployed(versionTable:TableFor1[String]=Table("Versions", "1.0", "1.2")) {
    forAll(versionTable) { version =>
      And.helloWorldIsDeployed("api-example-microservice", version)
    }
  }
}
