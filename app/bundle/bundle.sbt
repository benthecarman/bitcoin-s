name := "bitcoin-s-bundle"

mainClass := Some("org.bitcoins.bundle.gui.BundleGUI")

publish / skip := true

// Fork a new JVM for 'run' and 'test:run' to avoid JavaFX double initialization problems
fork := true

enablePlugins(JDKPackagerPlugin)

jdkPackagerType := "msi"

jdkAppIcon := Some(new File("/app/gui/src/main/resources/icons/bitcoin-s.ico"))

assembly / mainClass := Some("org.bitcoins.bundle.gui.BundleGUI")

assembly / assemblyJarName := s"${name.value}.jar"

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", _ @_*)       => MergeStrategy.discard
  case PathList("reference.conf", _ @_*) => MergeStrategy.concat
  case _                                 => MergeStrategy.first
}
