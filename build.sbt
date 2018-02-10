organization  := "com.github.bhop"
name          := "auth-store"
version       := "1.0.0"
scalaVersion  := "2.12.4"

resolvers     += Resolver.sonatypeRepo("snapshots")
resolvers     += Resolver.bintrayRepo("jmcardon", "tsec")

val CatsVersion       = "1.0.1"
val CatsEffectVersion = "0.8"
val Http4sVersion     = "0.18.0-M9"
val TsecVersion       = "0.0.1-M7"
val CirceVersion      = "0.9.1"
val DoobieVersion     = "0.5.0-RC2"
val FlywayVersion     = "5.0.5"
val MariaDbVersion    = "2.2.1"
val PureConfigVersion = "0.9.0"
val LogbackVersion    = "1.2.3"
val ScalaTestVersion  = "3.0.4"

libraryDependencies ++= Seq(
  "org.typelevel"         %% "cats-core"            % CatsVersion,
  "org.typelevel"         %% "cats-effect"          % CatsEffectVersion,

  "org.http4s"            %% "http4s-blaze-server"  % Http4sVersion,
  "org.http4s"            %% "http4s-circe"         % Http4sVersion,
  "org.http4s"            %% "http4s-dsl"           % Http4sVersion,

  "io.github.jmcardon"    %% "tsec-jwt-mac"         % TsecVersion,

  "io.circe"              %% "circe-core"           % CirceVersion,
  "io.circe"              %% "circe-generic"        % CirceVersion,

  "org.tpolecat"          %% "doobie-core"          % DoobieVersion,
  "org.tpolecat"          %% "doobie-hikari"        % DoobieVersion,
  "org.tpolecat"          %% "doobie-h2"            % DoobieVersion       % "test,it",

  "org.flywaydb"          %  "flyway-core"          % FlywayVersion,

  "org.mariadb.jdbc"      %  "mariadb-java-client"  % MariaDbVersion,

  "com.github.pureconfig" %% "pureconfig"           % PureConfigVersion,

  "ch.qos.logback"        %  "logback-classic"      % LogbackVersion,

  "org.scalatest"         %% "scalatest"            % ScalaTestVersion    % "test,it"
)

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-Ywarn-dead-code",
  "-Ywarn-infer-any",
  "-Ywarn-unused-import",
  "-Ypartial-unification",
  "-Xfatal-warnings",
  "-Xlint"
)

Defaults.itSettings

configs(IntegrationTest)

enablePlugins(ScalafmtPlugin, JavaAppPackaging)