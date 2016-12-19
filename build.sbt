name := "SimpleChessOpeningsTrainer"

fork in run := true

version := "1.0"

scalaVersion := "2.12.1"

assemblyJarName in assembly := "SimpleChessOpeningsTrainer.jar"
mainClass in assembly := Some("com.loloof64.scala.simple_chess_openings_trainer.ApplicationEntry")

resolvers += "Clojars" at "http://clojars.org/repo/"

libraryDependencies += "com._0xab" % "chesspresso" % "0.9.2"
    
