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

//pomExtra := (
//  <url>https://www.paddypowerbetfair.com</url>
//    <licenses>
//      <license>
//        <name>PPB</name>
//        <url>https://github.com/PaddyPowerBetfair/Standards/blob/master/LICENCE.md</url>
//        <distribution>repo</distribution>
//      </license>
//    </licenses>
//    <scm>
//      <url>https://github.com/PaddyPowerBetfair/libre-rabbit</url>
//      <connection>scm:git@github.com:PaddyPowerBetfair/libre-rabbit.git</connection>
//    </scm>
//    <developers>
//      <developer>
//        <id>rodoherty1</id>
//        <name>Rob O'Doherty</name>
//        <url>https://www.paddypowerbetfair.com</url>
//      </developer>
//    </developers>)