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
  junitDep,
  specs2Dep
)

lazy val scalazVersion = "7.1.1"
lazy val scalazStreamVersion = "0.7a"
lazy val rabbitmqVersion = "3.3.4"
lazy val slf4jVersion = "1.7.5"
lazy val typesafeConfVersion = "1.3.1"
lazy val monocleVersion = "1.1.1"
lazy val scalacheckVersion = "1.12.5"
lazy val specs2Version = "3.3.1"
lazy val junitVersion = "4.10"

lazy val amqpClientDep     =  "com.rabbitmq"                 %  "amqp-client"                % rabbitmqVersion
lazy val scalazDep         =  "org.scalaz"                  %%  "scalaz-core"                % scalazVersion
lazy val scalazConcDep     =  "org.scalaz"                  %%  "scalaz-concurrent"          % scalazVersion
lazy val scalazEffectDep   =  "org.scalaz"                  %%  "scalaz-effect"              % scalazVersion
lazy val scalazStreamDep   =  "org.scalaz.stream"           %%  "scalaz-stream"              % scalazStreamVersion
lazy val slf4jDep          =  "org.slf4j"                    %  "slf4j-api"                  % slf4jVersion
lazy val typesafeConfDep   =  "com.typesafe"                 %  "config"                     % typesafeConfVersion
lazy val monocleCoreDep    =  "com.github.julien-truffaut"           %%  "monocle-core"               % monocleVersion
lazy val monocleGenericDep =  "com.github.julien-truffaut"           %%  "monocle-generic"            % monocleVersion
lazy val monocleMacroDep   =  "com.github.julien-truffaut"           %%  "monocle-macro"              % monocleVersion
lazy val scalaCheckDep     =  "org.scalacheck"              %%  "scalacheck"                 % scalacheckVersion    % "test"
lazy val junitDep          =  "junit"                        %  "junit"                      % junitVersion         % "test"
lazy val specs2Dep         =  "org.specs2"                  %%  "specs2"                     % specs2Version        % "test"

lazy val scalazStreamResolver = Seq(
  "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"
)
