import sbt.url

sonatypeProfileName := "com.paddypowerbetfair"

// To sync with Maven central, you need to supply the following information:
publishMavenStyle := false

licenses := Seq("PPB" -> url("https://github.com/PaddyPowerBetfair/Standards/blob/master/LICENCE.md"))
homepage := Some(url("https://github.com/PaddyPowerBetfair/libre-rabbit"))

scmInfo := Some(
  ScmInfo(
    url("https://github.com/PaddyPowerBetfair/libre-rabbit"),
    "scm:git@github.com:PaddyPowerBetfair/libre-rabbit.git"
  ))

developers := List(
  Developer(
    id="rodoherty1",
    name="Rob O'Doherty",
    email="opensource@paddypowerbetfair.com",
    url=url("https://www.paddypowerbetfair.com")
  )
)
