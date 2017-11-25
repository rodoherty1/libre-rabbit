lazy val baseSettings = Seq(
  organization := "com.paddypowerbetfair.librerabbit",
  version := "1.0-SNAPSHOT",
  scalaVersion := "2.11.8",
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
//  specs2Dep,
  specs2CoreDep,
  junitDep,
  spec2ScalaCheckDep,
  specs2JunitDep
)

lazy val scalazVersion = "7.1.1"
lazy val scalazStreamVersion = "0.7a"
lazy val rabbitmqVersion = "3.3.4"
lazy val slf4jVersion = "1.7.5"
lazy val typesafeConfVersion = "1.3.1"
lazy val monocleVersion = "1.1.1"
lazy val specs2Version = "4.0.1"
lazy val scalaCheckVersion = "1.13.4"
lazy val junitVersion = "4.10"


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
lazy val scalaCheckDep     =  "org.scalacheck"              %%  "scalacheck"                 % scalaCheckVersion    % Test
//lazy val specs2Dep         =  "org.specs2"                  %%  "specs2"                     % specs2Version        % Test
lazy val spec2ScalaCheckDep= "org.specs2"                   %%  "specs2-scalacheck"          % specs2Version        % Test

lazy val specs2CoreDep     =  "org.specs2"                  %%  "specs2-core"                % specs2Version        % Test
lazy val specs2JunitDep    =  "org.specs2"                  %%  "specs2-junit"               % specs2Version        % Test
lazy val junitDep          =  "junit"                        %  "junit"                      % junitVersion         % Test


lazy val scalazStreamResolver = Seq(
  "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"
)
