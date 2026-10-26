package dev.taihei.mihon.data

import dev.taihei.mihon.data.Files.given
import dev.taihei.mihon.sources.ResourceLoader
import dev.taihei.tsuji.Language

import com.github.plokhotnyuk.jsoniter_scala.core.*

// A name in one form: its own script, romanised, in ASCII, and its reading where the language
// has one (Japanese kana).
final private[mihon] case class Form(
  name: String,
  latin: String,
  ascii: String,
  reading: Option[String])

private[mihon] object Form:

  def of(
    name: String,
    latin: Option[String],
    ascii: Option[String],
    reading: Option[String],
  ): Form =
    val l = latin.getOrElse(name)
    Form(name, l, ascii.getOrElse(l), reading)

// A family name with its female form; the two are the same where the language does not inflect.
final private[mihon] case class FamilyName(male: Form, female: Form)

final private[mihon] class TextData(file: TextFile):
  val wordSeparator: String     = file.wordSeparator
  val sentenceSeparator: String = file.sentenceSeparator
  val fullStop: String          = file.fullStop
  val questionOpen: String      = file.questionOpen
  val questionMark: String      = file.questionMark
  val exclamationOpen: String   = file.exclamationOpen
  val exclamationMark: String   = file.exclamationMark
  val comma: String             = file.comma
  val quoteOpen: String         = file.quoteOpen
  val quoteClose: String        = file.quoteClose
  val cased: Boolean            = file.cased
  val titleCase: Boolean        = file.titleCase
  val minWords: Int             = file.wordsPerSentence._1
  val maxWords: Int             = file.wordsPerSentence._2
  val words: Weighted[String]   = Weighted(file.words)

  // The capital of each initial the data uses, computed when the data was built, so that no
  // platform case mapping (which differs between the JVM and JavaScript, and by locale) is
  // involved. Keys are one code point, or two letters where the language capitalises a digraph
  // (Dutch ij).
  private val capitals: Map[String, String] = file.capitals.toMap

  /** The word with its first letter capitalised, where the script has case. */
  def capitalize(word: String): String =
    if !cased || word.isEmpty then word
    else
      val two = if word.length >= 2 then capitals.get(word.substring(0, 2)) else None
      two match
        case Some(capital) => capital + word.substring(2)
        case None          =>
          val first = word.offsetByCodePoints(0, 1)
          capitals.get(word.substring(0, first)) match
            case Some(capital) => capital + word.substring(first)
            case None          => word

end TextData

final private[mihon] class NamesData(file: NamesFile):
  val familyFirst: Boolean = file.order == "family-given"
  val separator: String    = file.separator

  private def givenName(e: GivenEntry) = Form.of(e.name, e.latin, e.ascii, e.reading) -> e.weight

  val female: Weighted[Form] = Weighted(file.female.map(givenName))
  val male: Weighted[Form]   = Weighted(file.male.map(givenName))

  val family: Weighted[FamilyName] = Weighted(file.family.map { e =>
    val male   = Form.of(e.name, e.latin, e.ascii, e.reading)
    val female = e.female.fold(male)(f => Form.of(f.name, f.latin, f.ascii, f.reading))
    FamilyName(male, female) -> e.weight
  })

final private[mihon] case class City(name: String, region: Option[String])

final private[mihon] class CountryData(file: CountryFile):
  val code: String        = file.code
  val lines: List[String] = file.lines
  val separator: String   = file.separator
  val postalCode: String  = file.postalCode

  val regions: Option[Weighted[String]] =
    if file.regions.isEmpty then None else Some(Weighted(file.regions))

  val cities: Weighted[City]    = Weighted(file.cities.map(c => City(c.name, c.region) -> c.weight))
  val streets: Weighted[String] = Weighted.uniform(file.streets)
  val phone: String             = file.phone
  val fictionalPhone: Boolean   = file.fictionalPhone

// One language's data, each file parsed on first use.
final private[mihon] class LanguageData(val entry: LanguageEntry):
  val language: Language = Language(entry.tag)

  lazy val text: TextData = new TextData(LanguageData.read[TextFile](s"${entry.tag}/text.json"))
  lazy val names: NamesData = new NamesData(LanguageData.read[NamesFile](s"${entry.tag}/names.json"))

  lazy val country: CountryData =
    new CountryData(LanguageData.read[CountryFile](s"${entry.tag}/country-${entry.country}.json"))

  // Country names in this language, by ISO 3166-1 alpha-2 code, in code order.
  lazy val countries: IndexedSeq[(String, String)] =
    LanguageData.read[Files.CountriesFile](s"${entry.tag}/countries.json").toIndexedSeq

  lazy val countryNames: Map[String, String] = countries.toMap

private[mihon] object LanguageData:

  private val root = "/dev/taihei/mihon/data"

  // Throws on a missing or malformed file: the build checks every file, so this is a packaging
  // bug to fix, not an input to handle.
  private[data] def read[A : JsonValueCodec](file: String): A =
    parse[A](s"$root/$file", ResourceLoader.load(s"$root/$file"))

  // The decoding of one file's text, named by its path in any error. Reentrant, with a reader of
  // its own: a file is read on first use, which may be inside a caller's jsoniter decode on this
  // thread, and readFromString would reuse that decode's pooled reader. Each file is read once.
  private[mihon] def parse[A : JsonValueCodec](path: String, json: String): A =
    try readFromStringReentrant[A](json, config)
    catch
      case e: JsonReaderException => throw new IllegalStateException(s"$path: ${e.getMessage}", e)

  // jsoniter's defaults but no hex dump of the file in an error: the path and offset say enough.
  private val config = ReaderConfig.withAppendHexDumpToParseException(false)

  private lazy val catalog: LanguagesFile = read[LanguagesFile]("languages.json")

  /** Every language with data, in tag order. */
  lazy val all: IndexedSeq[LanguageData] =
    catalog.languages.sortBy(_.tag).map(new LanguageData(_)).toIndexedSeq

  private lazy val byTag: Map[String, LanguageData] = all.map(d => d.entry.tag -> d).toMap
  private lazy val aliases: Map[String, String]     = catalog.aliases.toMap

  /**
   * The language whose data serves `language`: its tag, then an alias from languages.json, then
   * the tag with subtags stripped from the right (RFC 4647 lookup: `nl-BE` is `nl`); the first of
   * these that is a language or an alias decides, so an alias of a language without data
   * refuses. A tag that names a script other than the one found is refused (`en-Cyrl` is not
   * served by `en`), so a caller never gets text in a script it did not ask for.
   */
  def resolve(language: Language): Option[LanguageData] =
    val subtags = language.tag.split('-').toList
    // The script subtag, if any: four letters, before any extension or private use.
    val script  =
      subtags.drop(1).takeWhile(_.length > 1).find(s => s.length == 4 && s.forall(_.isLetter))
    val chain   = (subtags.length to 1 by -1).map(n => subtags.take(n).mkString("-"))
    // The first tag of the chain that is a language with data or an alias decides: an alias
    // whose language has no data refuses, rather than fall through to the base tag (zh-TW names
    // Traditional Chinese, so it must not be served by zh, which is Simplified).
    chain.iterator
      .flatMap(t => aliases.get(t).map(Left(_)).orElse(byTag.get(t).map(Right(_))))
      .nextOption()
      .flatMap {
        case Left(target) => byTag.get(target)
        case Right(data)  => Some(data)
      }
      .filter(d => script.forall(_ == d.entry.script))

  end resolve

end LanguageData
