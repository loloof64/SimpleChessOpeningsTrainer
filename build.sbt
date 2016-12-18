name := "SimpleChessOpeningsTrainer"

fork in run := true

version := "1.0"

scalaVersion := "2.12.1"

resolvers += "Clojars" at "http://clojars.org/repo/"
resolvers += "Maven repositories" at "http://search.maven.org"

libraryDependencies += "org.scalafx" % "scalafx_2.12" % "8.0.102-R11"
libraryDependencies += "com._0xab" % "chesspresso" % "0.9.2"
    