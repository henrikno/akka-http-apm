import sbtassembly.AssemblyKeys.assembly

lazy val akkaHttpVersion   = "10.1.12"
lazy val akkaVersion       = "2.6.6"
lazy val elasticApmVersion = "1.17.0"

resolvers += Resolver.mavenLocal

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "com.example",
      scalaVersion    := "2.13.2"
    )),
    name := "akka-http-apm",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"                % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json"     % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"              % akkaVersion,
      "ch.qos.logback"    % "logback-classic"           % "1.2.3",
      "co.elastic.apm"    % "apm-agent-attach"          % elasticApmVersion,
      "co.elastic.apm"    % "apm-agent-api"             % elasticApmVersion,
      "co.elastic.apm"    % "elastic-apm-agent"         % elasticApmVersion % Provided,

      "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"                % "3.0.8"         % Test
    )
  )

assembly / test  := {}