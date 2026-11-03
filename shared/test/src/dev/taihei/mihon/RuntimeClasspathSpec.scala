package dev.taihei.mihon

import zio.test.*

// What a user's build puts on the classpath for the published artifact (RuntimeModules, resolved
// by the build from the artifact's POM with Coursier, on the JVM and on Scala.js): no ZIO module,
// so a user who does not use ZIO gets none through Mihon, and jsoniter-scala-core at exactly the
// version Mihon declares, and Tsuji brings that version too, so neither is evicted by the other.
object RuntimeClasspathSpec extends ZIOSpecDefault:

  private def jsoniter(modules: List[(String, String, String)]) = modules.collect {
    case ("com.github.plokhotnyuk.jsoniter-scala", name, version) if name.startsWith("jsoniter-scala-core_") => version
  }

  private def zio(modules: List[(String, String, String)]) = modules.filter(_._1 == "dev.zio").toSet

  def spec = suite("RuntimeClasspathSpec")(
    test("the resolution is transitive and holds Mihon's own dependencies") {
      val names = RuntimeModules.all.map(m => (m._1, m._2.takeWhile(_ != '_')))
      assertTrue(
        names.contains(("dev.taihei", "taihei-tsuji")),
        names.contains(("com.github.plokhotnyuk.jsoniter-scala", "jsoniter-scala-core")),
        RuntimeModules.all.size > RuntimeModules.withoutTsuji.size,
      )
    },
    test("no dev.zio module is on the published runtime classpath") {
      assertTrue(
        zio(RuntimeModules.all).isEmpty,
        zio(RuntimeModules.tsujiAlone).isEmpty,
      )
    },
    // Coursier keeps the highest version, so the resolved one alone would not show a Tsuji that
    // pins a lower one: what Tsuji brings on its own is checked too.
    test("jsoniter-scala-core resolves to exactly the version Mihon declares, and Tsuji brings no other") {
      assertTrue(
        jsoniter(RuntimeModules.all) == List(RuntimeModules.declaredJsoniterScala),
        jsoniter(RuntimeModules.tsujiAlone) == List(RuntimeModules.declaredJsoniterScala),
        RuntimeModules.tsujiAlone.exists(_._2.startsWith("taihei-tsuji_")),
      )
    },
  )

end RuntimeClasspathSpec
