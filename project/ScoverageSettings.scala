import scoverage.ScoverageKeys._

object ScoverageSettings {
  def apply() = Seq(
    coverageMinimumStmtTotal := 76.00,    // TO BE RETURNED TO 80 - when ReDoc is no longer a POC - OVERDUE
    coverageFailOnMinimum := true,
    coverageHighlighting := true,
    coverageExcludedPackages := List(
      "<empty>",
      "definition.*",
      "sandbox.*",
      "live.*",
      "prod.*",
      "testOnlyDoNotUseInAppConf.*",
      "uk\\.gov\\.hmrc\\.config.*",
      "app.Routes",
      "app.RoutesPrefix",
      "controllers.javascript",
      ".*Reverse.*",
      "uk\\.gov\\.hmrc\\.controllers\\.Reverse.*",
    ).mkString(";")
  )
}