
import com.typesafe.sbt.GitVersioning

organization := "com.paddypowerbetfair"

name := "libre-rabbit"

version := "0.0.2-SNAPSHOT"

scalaVersion := "2.11.8"

lazy val baseSettings = Seq(
  logBuffered in Test := false,
  resolvers ++= scalazStreamResolver
)

lazy val scalacBaseOpts = Seq(
  "-Xlint",
  "-deprecation",
  "-feature",
  "-language:postfixOps",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-infer-any",
  "-Ywarn-unused-import"
)

lazy val scalacRigidOpts = Seq(
  "-Xfatal-warnings"
)

lazy val commonSettings = baseSettings ++ Seq(scalacOptions ++= (scalacBaseOpts ++ scalacRigidOpts))

lazy val librerabbit = (project in file("."))
  .settings(commonSettings)
  .settings(libraryDependencies ++= librerabbitDependencies ++ testDependencies)

lazy val librerabbitDependencies = Seq(
  amqpClientDep,
  scalazDep,
  scalazConcDep,
  scalazEffectDep,
  scalazStreamDep,
  slf4jDep,
  typesafeConfDep,
  monocleCoreDep,
  monocleGenericDep,
  monocleMacroDep
)

lazy val testDependencies = Seq(
  scalaCheckDep,
  logbackDep,
  scalaTestDep,
  scalaCheckDep,
  mockitoDep,
  scalazScalaTestDep
)

lazy val scalazVersion = "7.1.3"
lazy val scalazStreamVersion = "0.7a"
lazy val rabbitmqVersion = "3.3.4"
lazy val slf4jVersion = "1.7.5"
lazy val logbackVersion = "1.2.3"
lazy val typesafeConfVersion = "1.3.1"
lazy val monocleVersion = "1.1.1"
lazy val scalaCheckVersion = "1.14.0"
lazy val scalaTestVersion = "3.0.5"
lazy val mockitoVersion = "2.19.0"
lazy val scalazScalaTestVersion = "0.5.2"


lazy val amqpClientDep     =  "com.rabbitmq"                 %  "amqp-client"                % rabbitmqVersion
lazy val scalazDep         =  "org.scalaz"                  %%  "scalaz-core"                % scalazVersion
lazy val scalazConcDep     =  "org.scalaz"                  %%  "scalaz-concurrent"          % scalazVersion
lazy val scalazEffectDep   =  "org.scalaz"                  %%  "scalaz-effect"              % scalazVersion
lazy val scalazStreamDep   =  "org.scalaz.stream"           %%  "scalaz-stream"              % scalazStreamVersion
lazy val slf4jDep          =  "org.slf4j"                    %  "slf4j-api"                  % slf4jVersion
lazy val typesafeConfDep   =  "com.typesafe"                 %  "config"                     % typesafeConfVersion
lazy val monocleCoreDep    =  "com.github.julien-truffaut"  %%  "monocle-core"               % monocleVersion
lazy val monocleGenericDep =  "com.github.julien-truffaut"  %%  "monocle-generic"            % monocleVersion
lazy val monocleMacroDep   =  "com.github.julien-truffaut"  %%  "monocle-macro"              % monocleVersion


lazy val logbackDep        =  "ch.qos.logback"               %  "logback-classic"            % logbackVersion         % Test
lazy val scalaCheckDep     =  "org.scalacheck"              %%  "scalacheck"                 % scalaCheckVersion      % Test
lazy val scalaTestDep      =  "org.scalatest"               %%  "scalatest"                  % scalaTestVersion       % Test
lazy val mockitoDep        =  "org.mockito"                  %  "mockito-core"               % mockitoVersion         % Test
lazy val scalazScalaTestDep = "org.typelevel"               %%  "scalaz-scalatest"           % scalazScalaTestVersion % Test

lazy val scalazStreamResolver = Seq(
  "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"
)

addCommandAlias("ci-all",  ";+clean ;+compile ;+test ;+package")
addCommandAlias("release", ";+publishSigned ;sonatypeReleaseAll")


// For distribution to sonartype

useGpg := false
usePgpKeyHex("4BEF11849D8638711107EB75B76CCB046AAA0BF2")
pgpPublicRing := baseDirectory.value / "project" / ".gnupg" / "pubring.gpg"
pgpSecretRing := baseDirectory.value / "project" / ".gnupg" / "secring.gpg"
pgpPassphrase := sys.env.get("PGP_PASS").map(_.toArray)

credentials += Credentials(
  "Sonatype Nexus Repository Manager",
  "oss.sonatype.org",
  sys.env.getOrElse("SONATYPE_USER", ""),
  sys.env.getOrElse("SONATYPE_PASS", "")
)

isSnapshot := version.value endsWith "SNAPSHOT"

publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)

enablePlugins(GitVersioning)

/* The BaseVersion setting represents the in-development (upcoming) version,
 * as an alternative to SNAPSHOTS.
 */
git.baseVersion := "3.0.0"

val ReleaseTag = """^v([\d\.]+)$""".r
git.gitTagToVersionNumber := {
  case ReleaseTag(v) => Some(v)
  case _ => None
}

git.formattedShaVersion := {
  val suffix = git.makeUncommittedSignifierSuffix(git.gitUncommittedChanges.value, git.uncommittedSignifier.value)

  git.gitHeadCommit.value map { _.substring(0, 7) } map { sha =>
    git.baseVersion.value + "-" + sha + suffix
  }
}
