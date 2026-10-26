package dev.taihei.mihon

import scala.util.Try

import zio.test.*

import dev.taihei.mihon.data.*
import dev.taihei.mihon.data.Files.given

import com.github.plokhotnyuk.jsoniter_scala.core.*

// The decoders of the data files, on both platforms: they read what the files hold, and refuse
// what the build's data check refuses, so a file that slipped past it fails loudly rather than
// load as other data.
object FilesSpec extends ZIOSpecDefault:

  private def read[A : JsonValueCodec](json: String): Either[String, A] =
    Try(readFromString[A](json)).toEither.left.map {
      case e: JsonReaderException => e.getMessage
      case other                  => s"not a JsonReaderException: $other"
    }

  private def refused[A : JsonValueCodec](json: String): Boolean =
    read[A](json).left.exists(!_.startsWith("not a JsonReaderException"))

  private val city = """{"name":"Gent","weight":3,"region":"Oost-Vlaanderen"}"""

  private val country =
    s"""{"code":"BE","lines":["{street}","{postalCode} {city}"],"separator":", ","postalCode":"####",
       |"regions":[["Oost-Vlaanderen",1]],"cities":[$city,{"name":"Brugge","weight":2}],
       |"streets":["{family}straat {number}"],"phone":"+32 9 ### ## ##","fictionalPhone":false}""".stripMargin

  // A country file whose first city is `json`: the city decoder is reached through the file.
  private def refusedCity(json: String): Boolean = refused[CountryFile](country.replace(city, json))

  private val names =
    """{"order":"family-given","separator":"",
      |"female":[{"name":"さくら","weight":2,"latin":"Sakura","reading":"さくら"}],
      |"male":[{"name":"Ivan","weight":1,"ascii":"Ivan"}],
      |"family":[{"name":"Novák","weight":5,"ascii":"Novak","female":{"name":"Nováková","ascii":"Novakova"}}]}""".stripMargin

  private val text =
    """{"wordSeparator":" ","sentenceSeparator":" ","fullStop":".","questionOpen":"¿","questionMark":"?",
      |"exclamationOpen":"¡","exclamationMark":"!","comma":",","quoteOpen":"«","quoteClose":"»",
      |"cased":true,"titleCase":false,"capitals":[["ij","IJ"],["é","É"]],"wordsPerSentence":[4,12],
      |"words":[["één",3],["woord",1]]}""".stripMargin

  def spec = suite("FilesSpec")(
    test("a country file reads to its fields, with an absent optional field as None") {
      assertTrue(
        read[CountryFile](country) == Right(
          CountryFile(
            code = "BE",
            lines = List("{street}", "{postalCode} {city}"),
            separator = ", ",
            postalCode = "####",
            regions = List("Oost-Vlaanderen" -> 1),
            cities = List(CityEntry("Gent", 3, Some("Oost-Vlaanderen")), CityEntry("Brugge", 2, None)),
            streets = List("{family}straat {number}"),
            phone = "+32 9 ### ## ##",
            fictionalPhone = false,
          )
        )
      )
    },
    test("a names file reads every form, the female family form included") {
      assertTrue(
        read[NamesFile](names) == Right(
          NamesFile(
            order = "family-given",
            separator = "",
            female = List(GivenEntry("さくら", 2, Some("Sakura"), None, Some("さくら"))),
            male = List(GivenEntry("Ivan", 1, None, Some("Ivan"), None)),
            family = List(
              FamilyEntry("Novák", 5, None, Some("Novak"), None, Some(FormEntry("Nováková", None, Some("Novakova"), None)))
            ),
          )
        )
      )
    },
    test("a text file reads its strings, flags, pairs and weighted words") {
      val file = read[TextFile](text)
      assertTrue(
        file.map(_.questionOpen) == Right("¿"),
        file.map(_.quoteClose) == Right("»"),
        file.map(f => (f.cased, f.titleCase)) == Right((true, false)),
        file.map(_.capitals) == Right(List("ij" -> "IJ", "é" -> "É")),
        file.map(_.wordsPerSentence) == Right((4, 12)),
        file.map(_.words) == Right(List("één" -> 3, "woord" -> 1)),
      )
    },
    test("the languages file and a countries file read, empty lists included") {
      assertTrue(
        read[LanguagesFile]("""{"languages":[{"tag":"sr-Cyrl","script":"Cyrl","country":"RS"}],"aliases":[]}""") ==
        Right(LanguagesFile(List(LanguageEntry("sr-Cyrl", "Cyrl", "RS")), Nil)),
        read[Files.CountriesFile]("""[["BE","België"],["NL","Nederland"]]""") ==
        Right(List("BE" -> "België", "NL" -> "Nederland")),
        read[Files.CountriesFile](" [ ] ") == Right(Nil),
      )
    },
    test("an unknown, a repeated or a missing field is refused") {
      assertTrue(
        country.contains(city),
        read[CountryFile](country).isRight,
        refusedCity("""{"name":"Gent","weight":3,"extra":1}"""),
        refusedCity("""{"name":"Gent","weight":3,"name":"Gent"}"""),
        refusedCity("""{"name":"Gent"}"""),
        // weight is a field of a given name, never of a female family form
        refused[NamesFile](names.replace(
          """"female":{"name":"Nováková",""",
          """"female":{"weight":1,"name":"Nováková",""",
        )),
        refused[CountryFile](country.replace(""","fictionalPhone":false""", "")),
        refused[TextFile](text.replace(""""comma":",",""", "")),
      )
    },
    test("a null or a value of the wrong type is refused") {
      assertTrue(
        refusedCity("""{"name":null,"weight":3}"""),
        refusedCity("""{"name":"Gent","weight":null}"""),
        refusedCity("""{"name":"Gent","weight":"3"}"""),
        refusedCity("""{"name":"Gent","weight":1.5}"""),
        refusedCity("""{"name":"Gent","weight":3,"region":null}"""),
        refused[CountryFile](country.replace(""""fictionalPhone":false""", """"fictionalPhone":"no"""")),
        refusedCity("""null"""),
        refusedCity("""["Gent",3]"""),
        refused[CountryFile]("null"),
      )
    },
    test("a pair is exactly two elements") {
      assertTrue(
        read[Files.CountriesFile]("""[["BE","België"]]""").isRight,
        refused[Files.CountriesFile]("""[["BE"]]"""),
        refused[Files.CountriesFile]("""[["BE","België","x"]]"""),
        refused[Files.CountriesFile]("""[{"BE":"België"}]"""),
      )
    },
    test("malformed or truncated JSON, and anything after the value, is refused") {
      assertTrue(
        refused[Files.CountriesFile]("""[["BE","België"]"""),
        refused[Files.CountriesFile]("""[["BE","België"],]"""),
        refused[Files.CountriesFile]("""[["BE","België"]] []"""),
        refused[CountryFile](country.dropRight(1)),
        refused[Files.CountriesFile](""),
      )
    },
    test("a malformed file is a packaging error naming its path, without a dump of the file") {
      val path    = "/dev/taihei/mihon/data/xx/countries.json"
      val failure = Try(LanguageData.parse[Files.CountriesFile](path, """[["BE","België"],"""))
      assertTrue(
        LanguageData.parse[Files.CountriesFile](path, """[["BE","België"]]""") == List("BE" -> "België"),
        failure.failed.toOption.exists(_.isInstanceOf[IllegalStateException]),
        failure.failed.toOption.exists(_.getMessage.startsWith(s"$path: ")),
        // jsoniter's default config does dump the input, so the check below can tell
        Try(readFromString[Files.CountriesFile]("""[["BE","België"],""")).failed.toOption
          .exists(_.getMessage.contains("+----------+")),
        failure.failed.toOption.exists(e => !e.getMessage.contains("+----------+")),
      )
    },
    test("every data file of every language decodes") {
      val all = LanguageData.all
      assertTrue(
        all.size == Mihon.languages.size,
        all.size > 20,
        all.forall(d => d.text.words.size > 0 && d.names.family.size > 0 && d.country.code == d.entry.country),
        all.forall(d => d.countryNames.contains(d.entry.country)),
      )
    },
  )

end FilesSpec
