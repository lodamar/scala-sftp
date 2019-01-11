lazy val root = (project in file(".")).settings(
  name := "scala-sftp",
  version := "0.0.1",
  scalaVersion := "2.12.8",
  libraryDependencies ++= Seq(
    "com.jcraft" % "jsch" % "0.1.55"
  )
)
