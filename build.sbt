name := "SimpleChessOpeningsTrainer"

fork in run := true

version := "1.0"

scalaVersion := "2.12.1"

resolvers += "Clojars" at "http://clojars.org/repo/"

libraryDependencies += "com._0xab" % "chesspresso" % "0.9.2"
    