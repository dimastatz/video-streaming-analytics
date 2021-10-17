import sbt.util

version := "0.1"
scalaVersion := "2.12.12"
name := "flumenz"

val sparkVersion = "3.0.1"
val dptLibVersion = "3.3.9"
val legionLibVersion = "2.4.0"
lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")
lazy val testScalastyle = taskKey[Unit]("testScalastyle")

resolvers ++= Seq(
  Resolver.sbtPluginRepo("releases"),
  "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/",
  "Cloudera Repository" at "https://repository.cloudera.com/artifactory/cloudera-repos/",
  "Artima Maven Repository" at "https://repo.artima.com/releases",
  "MrPowers Packages Rep" at "https://repo1.maven.org/maven2/com/github",
  "Sonatype Repo" at "https://oss.sonatype.org/content/groups/public",
  "Maven Central" at "https://mvnrepository.com/artifact"
)

semanticdbEnabled := true
semanticdbVersion := scalafixSemanticdb.revision
Test / fork := true
Test / parallelExecution := true
javaOptions ++= Seq("-Xms10M", "-Xmx4G", "-XX:+CMSClassUnloadingEnabled")
Test / test := ((Test / test) dependsOn testScalastyle).value
Compile / compile := ((Compile / compile) dependsOn compileScalastyle).value
javaOptions += s"-Duser.dir=${(LocalRootProject / baseDirectory).value}"
updateSbtClassifiers / updateConfiguration := (updateSbtClassifiers / updateConfiguration).value
  .withMissingOk(true)

compileScalastyle := (Compile / scalastyle).toTask("").value
testScalastyle := (Test / scalastyle).toTask("").value
ThisBuild / scalafmtOnCompile := true
coverageMinimum := 90
coverageFailOnMinimum := true
coverageExcludedFiles := ".*Boot.*;.*KafkaExtensions.commitOffsets"

scalacOptions ++= Seq(
  "-deprecation",
  "-Ywarn-unused-import",
  "-feature",
  "-Ywarn-value-discard",
  "-Xfatal-warnings"
)

val awsJars = ExclusionRule(organization = "com.amazonaws")
val hadoopJars = ExclusionRule(organization = "org.apache.hadoop")

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.2",
  "com.typesafe.play" %% "play-json" % "2.9.2",
  "org.scalactic" %% "scalactic" % "3.2.6",
  "org.scalamock" %% "scalamock" % "5.1.0" % Test,
  "org.scalatest" %% "scalatest-funsuite" % "3.2.6" % Test,
  "org.scalatest" %% "scalatest" % "3.2.6" % "test",
  "com.amazonaws" % "aws-java-sdk-bundle" % "1.11.965",
  "org.apache.hadoop" % "hadoop-aws" % "3.2.0",
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  "org.apache.spark" %% "spark-streaming" % sparkVersion,
  "org.apache.spark" %% "spark-sql-kafka-0-10" % sparkVersion,
  "org.apache.spark" %% "spark-hadoop-cloud" % "3.0.1.3.0.7110.0-81",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % Test,
  "org.apache.logging.log4j" %% "log4j-api-scala" % "12.0",
  "org.apache.logging.log4j" % "log4j-core" % "2.13.0" % Runtime,
  "com.github.simplyscala" %% "scalatest-embedmongo" % "0.2.4" % Test
)

libraryDependencies ~= { _.map(_.exclude("org.slf4j", "slf4j-log4j12")) }

assembly / assemblyMergeStrategy := {
  case PathList("org", "aopalliance", xs @ _*)      => MergeStrategy.last
  case PathList("javax", "inject", xs @ _*)         => MergeStrategy.last
  case PathList("javax", "servlet", xs @ _*)        => MergeStrategy.last
  case PathList("javax", "activation", xs @ _*)     => MergeStrategy.last
  case PathList("com", "sun", xs @ _*)              => MergeStrategy.last
  case PathList("org", "apache", xs @ _*)           => MergeStrategy.last
  case PathList("org", "sparkproject", xs @ _*)     => MergeStrategy.last
  case PathList("com", "esotericsoftware", xs @ _*) => MergeStrategy.last
  case PathList("com", "codahale", xs @ _*)         => MergeStrategy.last
  case PathList("com", "yammer", xs @ _*)           => MergeStrategy.last
  case "about.html"                                 => MergeStrategy.rename
  case "git.properties"                             => MergeStrategy.last
  case "META-INF/mailcap"                           => MergeStrategy.last
  case "plugin.properties"                          => MergeStrategy.last
  case "log4j.properties"                           => MergeStrategy.last
  case "module-info.class"                          => MergeStrategy.last
  case "META-INF/ECLIPSEF.RSA"                      => MergeStrategy.last
  case "META-INF/mimetypes.default"                 => MergeStrategy.last
  case "mozilla/public-suffix-list.txt"             => MergeStrategy.last
  case "org/slf4j/impl/StaticMDCBinder.class"       => MergeStrategy.last
  case "org/slf4j/impl/StaticLoggerBinder.class"    => MergeStrategy.last
  case "org/slf4j/impl/StaticMarkerBinder.class"    => MergeStrategy.last
  case "META-INF/io.netty.versions.properties"      => MergeStrategy.last
  case "META-INF/native-image/io.netty/common/native-image.properties" =>
    MergeStrategy.last
  case "META-INF/native-image/io.netty/handler/native-image.properties" =>
    MergeStrategy.last
  case "META-INF/native-image/io.netty/buffer/native-image.properties" =>
    MergeStrategy.last
  case "META-INF/native-image/io.netty/transport/reflection-config.json" =>
    MergeStrategy.last
  case "META-INF/native-image/io.netty/transport/native-image.properties" =>
    MergeStrategy.last
  case "META-INF/native-image/io.netty/codec-http/native-image.properties" =>
    MergeStrategy.last
  case "META-INF/services/org.apache.spark.sql.sources.DataSourceRegister" =>
    MergeStrategy.concat
  case x => (assembly / assemblyMergeStrategy).value(x)
}

addCommandAlias(
  "testCoverage",
  "; clean; coverage; test; coverageReport; coverageOff"
)

logLevel := Level.Info

