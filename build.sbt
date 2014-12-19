name := "board"

version := "0.1.0"

scalaVersion := "2.11.4"

assemblyJarName in assembly := { s"${name.value}-${version.value}.jar" }

resolvers ++= Seq()

libraryDependencies ++= Seq()

lazy val root = project.in(file(".")).dependsOn(simpleHttpServer)

lazy val simpleHttpServer = uri("git://github.com/amaya-382/SimpleHttpServer.git")
