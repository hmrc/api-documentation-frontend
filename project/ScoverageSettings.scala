import scoverage.ScoverageKeys._

object ScoverageSettings {
  def apply() = Seq(
    coverageMinimum := 80,
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
      "com\\.kenshoo\\.play\\.metrics.*",
      ".*Reverse.*",
      "uk\\.gov\\.hmrc\\.controllers\\.Reverse.*",
    ).mkString(";")
  )
}