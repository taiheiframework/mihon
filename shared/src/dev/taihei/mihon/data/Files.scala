package dev.taihei.mihon.data

import scala.collection.mutable.ListBuffer

import com.github.plokhotnyuk.jsoniter_scala.core.*

// The JSON files under dev/taihei/mihon/data/, as they are written. build.mill checks the same
// shape when it builds (EmbeddedData.check), so a decoding failure here is a packaging bug.
// The fields are described in docs/skill/reference/internals.md and scripts/data/README.md.

// languages.json
final private[mihon] case class LanguageEntry(
  tag: String,
  script: String,
  country: String)

final private[mihon] case class LanguagesFile(
  languages: List[LanguageEntry],
  aliases: List[(String, String)])

// <tag>/text.json
final private[mihon] case class TextFile(
  wordSeparator: String,
  sentenceSeparator: String,
  fullStop: String,
  questionOpen: String,
  questionMark: String,
  exclamationOpen: String,
  exclamationMark: String,
  comma: String,
  quoteOpen: String,
  quoteClose: String,
  cased: Boolean,
  titleCase: Boolean,
  capitals: List[(String, String)],
  wordsPerSentence: (Int, Int),
  words: List[(String, Int)])

// <tag>/names.json: a name in its own script, romanised (`latin`, absent when the name is in
// Latin script), reduced to ASCII for e-mail addresses (`ascii`, absent when it equals
// `latin`), and its kana reading (`reading`, Japanese only).
final private[mihon] case class FormEntry(
  name: String,
  latin: Option[String],
  ascii: Option[String],
  reading: Option[String])

final private[mihon] case class GivenEntry(
  name: String,
  weight: Int,
  latin: Option[String],
  ascii: Option[String],
  reading: Option[String])

final private[mihon] case class FamilyEntry(
  name: String,
  weight: Int,
  latin: Option[String],
  ascii: Option[String],
  reading: Option[String],
  female: Option[FormEntry])

final private[mihon] case class NamesFile(
  order: String,
  separator: String,
  female: List[GivenEntry],
  male: List[GivenEntry],
  family: List[FamilyEntry])

// <tag>/country-<CC>.json
final private[mihon] case class CityEntry(
  name: String,
  weight: Int,
  region: Option[String])

final private[mihon] case class CountryFile(
  code: String,
  lines: List[String],
  separator: String,
  postalCode: String,
  regions: List[(String, Int)],
  cities: List[CityEntry],
  streets: List[String],
  phone: String,
  fictionalPhone: Boolean)

// The decoders, written by hand on jsoniter-scala's token reader rather than derived: the files
// are few and small, their shape is fixed, and no macro or reflection is needed on either
// platform. They are as strict as the build's check: an unknown, repeated or missing field, a
// null, or a value of the wrong type is refused.
private[mihon] object Files:

  // Countries: [code, name] pairs, sorted by code.
  type CountriesFile = List[(String, String)]

  // A reader for one value, the reader positioned before it.
  private type Read[A] = JsonReader => A

  // A codec for a whole file. Mihon only reads its data, so the writing side is never called.
  private def file[A](read: Read[A]): JsonValueCodec[A] = new JsonValueCodec[A]:
    def decodeValue(in: JsonReader, default: A): A = read(in)
    def encodeValue(x: A, out: JsonWriter): Unit   =
      out.encodeError("Mihon's data files are never written")
    // What jsoniter hands decodeValue as its default; never used, since every reader refuses null.
    def nullValue: A                               = null.asInstanceOf[A]

  // readString answers its argument for a JSON null, which is how a null is told apart here.
  private val string: Read[String] = in =>
    val s = in.readString(null)
    if s == null then in.decodeError("expected a string") else s

  // Both refuse a null and any other token: jsoniter reads no default for a primitive.
  private val int: Read[Int]         = _.readInt()
  private val boolean: Read[Boolean] = _.readBoolean()

  private def list[A](item: Read[A]): Read[List[A]] = in =>
    if !in.isNextToken('[') then in.decodeError("expected an array")
    else if in.isNextToken(']') then Nil
    else
      in.rollbackToken()
      val items = ListBuffer.empty[A]
      while
        items += item(in)
        in.isNextToken(',')
      do ()
      if !in.isCurrentToken(']') then in.arrayEndOrCommaError()
      items.toList

  // A two-element array, as the files write a pair.
  private def pair[A, B](first: Read[A], second: Read[B]): Read[(A, B)] = in =>
    if !in.isNextToken('[') then in.decodeError("expected a pair")
    else
      val a = first(in)
      if !in.isNextToken(',') then in.decodeError("expected a pair")
      val b = second(in)
      if !in.isNextToken(']') then in.decodeError("expected a pair")
      (a, b)

  // The fields of one object, read as they come: `field` reads the value of a key it knows and
  // answers false for any other key, which is refused. A key that comes twice is refused too.
  private def fields(in: JsonReader)(field: String => Boolean): Unit =
    if !in.isNextToken('{') then in.decodeError("expected an object")
    else if !in.isNextToken('}') then
      in.rollbackToken()
      val seen = scala.collection.mutable.Set.empty[String]
      while
        val key = in.readKeyAsString()
        if !seen.add(key) then in.decodeError(s"a repeated field $key")
        if !field(key) then in.decodeError(s"an unknown field $key")
        in.isNextToken(',')
      do ()
      if !in.isCurrentToken('}') then in.objectEndOrCommaError()

  // A field that must be there, once the object is read.
  private def required[A](
    in: JsonReader,
    name: String,
    value: Option[A],
  ): A =
    value.getOrElse(in.decodeError(s"missing field $name"))

  private val languageEntry: Read[LanguageEntry] = in =>
    var tag, script, country = Option.empty[String]
    fields(in) {
      case "tag"     => tag = Some(string(in)); true
      case "script"  => script = Some(string(in)); true
      case "country" => country = Some(string(in)); true
      case _         => false
    }
    LanguageEntry(
      required(in, "tag", tag),
      required(in, "script", script),
      required(in, "country", country),
    )

  given JsonValueCodec[LanguagesFile] = file { in =>
    var languages = Option.empty[List[LanguageEntry]]
    var aliases   = Option.empty[List[(String, String)]]
    fields(in) {
      case "languages" => languages = Some(list(languageEntry)(in)); true
      case "aliases"   => aliases = Some(list(pair(string, string))(in)); true
      case _           => false
    }
    LanguagesFile(required(in, "languages", languages), required(in, "aliases", aliases))
  }

  private val textStrings = Seq(
    "wordSeparator",
    "sentenceSeparator",
    "fullStop",
    "questionOpen",
    "questionMark",
    "exclamationOpen",
    "exclamationMark",
    "comma",
    "quoteOpen",
    "quoteClose",
  )

  given JsonValueCodec[TextFile] = file { in =>
    val strings          = scala.collection.mutable.Map.empty[String, String]
    var cased, titleCase = Option.empty[Boolean]
    var capitals         = Option.empty[List[(String, String)]]
    var wordsPerSentence = Option.empty[(Int, Int)]
    var words            = Option.empty[List[(String, Int)]]
    fields(in) {
      case k if textStrings.contains(k) => strings(k) = string(in); true
      case "cased"                      => cased = Some(boolean(in)); true
      case "titleCase"                  => titleCase = Some(boolean(in)); true
      case "capitals"                   => capitals = Some(list(pair(string, string))(in)); true
      case "wordsPerSentence"           => wordsPerSentence = Some(pair(int, int)(in)); true
      case "words"                      => words = Some(list(pair(string, int))(in)); true
      case _                            => false
    }
    def s(name: String)  = required(in, name, strings.get(name))
    TextFile(
      wordSeparator = s("wordSeparator"),
      sentenceSeparator = s("sentenceSeparator"),
      fullStop = s("fullStop"),
      questionOpen = s("questionOpen"),
      questionMark = s("questionMark"),
      exclamationOpen = s("exclamationOpen"),
      exclamationMark = s("exclamationMark"),
      comma = s("comma"),
      quoteOpen = s("quoteOpen"),
      quoteClose = s("quoteClose"),
      cased = required(in, "cased", cased),
      titleCase = required(in, "titleCase", titleCase),
      capitals = required(in, "capitals", capitals),
      wordsPerSentence = required(in, "wordsPerSentence", wordsPerSentence),
      words = required(in, "words", words),
    )
  }

  // The fields every name form has; `weight` and `female` only where the caller allows them.
  final private class NameFields:
    var name, latin, ascii, reading = Option.empty[String]
    var weight                      = Option.empty[Int]
    var female                      = Option.empty[FormEntry]

    def read(
      in: JsonReader,
      withWeight: Boolean,
      withFemale: Boolean,
    ): Unit = fields(in) {
      case "name"                 => name = Some(string(in)); true
      case "latin"                => latin = Some(string(in)); true
      case "ascii"                => ascii = Some(string(in)); true
      case "reading"              => reading = Some(string(in)); true
      case "weight" if withWeight => weight = Some(int(in)); true
      case "female" if withFemale => female = Some(formEntry(in)); true
      case _                      => false
    }

  end NameFields

  private val formEntry: Read[FormEntry] = in =>
    val f = NameFields()
    f.read(in, withWeight = false, withFemale = false)
    FormEntry(required(in, "name", f.name), f.latin, f.ascii, f.reading)

  private val givenEntry: Read[GivenEntry] = in =>
    val f = NameFields()
    f.read(in, withWeight = true, withFemale = false)
    GivenEntry(
      required(in, "name", f.name),
      required(in, "weight", f.weight),
      f.latin,
      f.ascii,
      f.reading,
    )

  private val familyEntry: Read[FamilyEntry] = in =>
    val f = NameFields()
    f.read(in, withWeight = true, withFemale = true)
    FamilyEntry(
      required(in, "name", f.name),
      required(in, "weight", f.weight),
      f.latin,
      f.ascii,
      f.reading,
      f.female,
    )

  given JsonValueCodec[NamesFile] = file { in =>
    var order, separator = Option.empty[String]
    var female, male     = Option.empty[List[GivenEntry]]
    var family           = Option.empty[List[FamilyEntry]]
    fields(in) {
      case "order"     => order = Some(string(in)); true
      case "separator" => separator = Some(string(in)); true
      case "female"    => female = Some(list(givenEntry)(in)); true
      case "male"      => male = Some(list(givenEntry)(in)); true
      case "family"    => family = Some(list(familyEntry)(in)); true
      case _           => false
    }
    NamesFile(
      required(in, "order", order),
      required(in, "separator", separator),
      required(in, "female", female),
      required(in, "male", male),
      required(in, "family", family),
    )
  }

  private val cityEntry: Read[CityEntry] = in =>
    var name, region = Option.empty[String]
    var weight       = Option.empty[Int]
    fields(in) {
      case "name"   => name = Some(string(in)); true
      case "weight" => weight = Some(int(in)); true
      case "region" => region = Some(string(in)); true
      case _        => false
    }
    CityEntry(required(in, "name", name), required(in, "weight", weight), region)

  given JsonValueCodec[CountryFile] = file { in =>
    var code, separator, postalCode, phone = Option.empty[String]
    var lines, streets                     = Option.empty[List[String]]
    var regions                            = Option.empty[List[(String, Int)]]
    var cities                             = Option.empty[List[CityEntry]]
    var fictionalPhone                     = Option.empty[Boolean]
    fields(in) {
      case "code"           => code = Some(string(in)); true
      case "lines"          => lines = Some(list(string)(in)); true
      case "separator"      => separator = Some(string(in)); true
      case "postalCode"     => postalCode = Some(string(in)); true
      case "regions"        => regions = Some(list(pair(string, int))(in)); true
      case "cities"         => cities = Some(list(cityEntry)(in)); true
      case "streets"        => streets = Some(list(string)(in)); true
      case "phone"          => phone = Some(string(in)); true
      case "fictionalPhone" => fictionalPhone = Some(boolean(in)); true
      case _                => false
    }
    CountryFile(
      code = required(in, "code", code),
      lines = required(in, "lines", lines),
      separator = required(in, "separator", separator),
      postalCode = required(in, "postalCode", postalCode),
      regions = required(in, "regions", regions),
      cities = required(in, "cities", cities),
      streets = required(in, "streets", streets),
      phone = required(in, "phone", phone),
      fictionalPhone = required(in, "fictionalPhone", fictionalPhone),
    )
  }

  given JsonValueCodec[CountriesFile] = file(list(pair(string, string)))

end Files
