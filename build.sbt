name := "board"

version := "1.0.0"

scalaVersion := "2.11.4"

assemblyJarName in assembly := {
  s"${name.value}-${version.value}.jar"
}

resolvers ++= Seq()

libraryDependencies ++= Seq(
  "com.tristanhunt" %% "knockoff" % "0.8.3"
)

lazy val root = project.in(file(".")).dependsOn(simpleHttpFramework)

lazy val simpleHttpFramework = uri("git://github.com/amaya-382/SimpleHttpFramework.git")
