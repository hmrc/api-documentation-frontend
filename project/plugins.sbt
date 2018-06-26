credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

val hmrcRepoHost = java.lang.System.getProperty("hmrc.repo.host", "https://nexus-preview.tax.service.gov.uk")

resolvers ++= Seq(
  "hmrc-snapshots" at hmrcRepoHost + "/content/repositories/hmrc-snapshots",
  "hmrc-releases" at hmrcRepoHost + "/content/repositories/hmrc-releases",
  "typesafe-releases" at hmrcRepoHost + "/content/repositories/typesafe-releases",
  Resolver.url("hmrc-sbt-plugin-releases", url("https://dl.bintray.com/hmrc/sbt-plugin-releases"))
  (Resolver.ivyStylePatterns))

addSbtPlugin("uk.gov.hmrc" % "sbt-auto-build" % "1.8.0")
addSbtPlugin("uk.gov.hmrc" % "sbt-distributables" % "1.0.0")
addSbtPlugin("uk.gov.hmrc" % "sbt-git-versioning" % "0.10.0")

addSbtPlugin("org.irundaia.sbt" % "sbt-sassify" % "1.4.6")
addSbtPlugin("net.ground5hark.sbt" % "sbt-concat" % "0.1.9")
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.12")
addSbtPlugin("com.typesafe.sbt" % "sbt-uglify" % "1.0.3")
addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.1")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")
