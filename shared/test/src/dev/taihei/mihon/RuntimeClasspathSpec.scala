package dev.taihei.mihon

import zio.test.*

// What a user's build puts on the classpath for the published artifact (RuntimeModules, resolved
// by the build from the artifact's POM with Coursier, on the JVM and on Scala.js): no ZIO module,
// so a user who does not use ZIO gets none through Mihon, and jsoniter-scala-core at exactly the
// version Mihon declares, and any Tsuji that brings jsoniter-scala-core brings that version too
// (Tsuji 6's pin; Tsuji 5 has none), so neither is evicted by the other.
object RuntimeClasspathSpec extends ZIOSpecDefault:

  private def jsoniter(modules: List[(String, String, String)]) = modules.collect {
    case ("com.github.plokhotnyuk.jsoniter-scala", name, version) if name.startsWith("jsoniter-scala-core_") => version
  }

  private def zio(modules: List[(String, String, String)]) = modules.filter(_._1 == "dev.zio").toSet

  // TEMPORARY, until the pin on Taihei Tsuji 6.0.0. Tsuji 5 depends on zio-json, which brings ZIO,
  // so while Mihon pins Tsuji 5 the ZIO modules that come only through Tsuji are allowed. The
  // commit that moves the pin deletes this allowance and the first assertion below, which fails
  // on any Tsuji but 5 so that the allowance cannot outlive the pin; the test then holds Mihon to
  // no dev.zio module at all.
  private val throughTsuji5: Set[(String, String, String)] =
    zio(RuntimeModules.all) -- zio(RuntimeModules.withoutTsuji)

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
        RuntimeModules.declaredTsuji.startsWith("5."),
        zio(RuntimeModules.withoutTsuji).isEmpty,
        // the allowance is still needed: Tsuji 5 does bring ZIO
        throughTsuji5.nonEmpty,
      )
    },
    // Coursier keeps the highest version, so the resolved one alone would not show a Tsuji that
    // pins a lower one: what Tsuji brings on its own is checked too.
    test("jsoniter-scala-core resolves to exactly the version Mihon declares, and Tsuji brings no other") {
      assertTrue(
        jsoniter(RuntimeModules.all) == List(RuntimeModules.declaredJsoniterScala),
        jsoniter(RuntimeModules.tsujiAlone).forall(_ == RuntimeModules.declaredJsoniterScala),
        RuntimeModules.tsujiAlone.exists(_._2.startsWith("taihei-tsuji_")),
      )
    },
  )

end RuntimeClasspathSpec
