package dev.taihei.mihon

import java.security.SecureRandom
import java.time.*
import java.util.{Random, UUID}

import dev.taihei.mihon.data.{LanguageData, Pattern}
import dev.taihei.mihon.sources.*
import dev.taihei.tsuji.Language

/**
 * Fake data for tests, seed data and mock-ups, in one language: text, people, addresses, phone
 * numbers, dates, files and ids.
 *
 * A generator is bound to one of [[Mihon.languages]]. Its text is in that language's script and
 * punctuation, its people are named in that language's name order, and its addresses and phone
 * numbers are those of the language's default country. [[withLanguage]] gives a view in another
 * language that shares the same random generator and clock.
 *
 * A generator draws every value from its own random number generator and reads today's date from
 * its own clock; there is no global generator and no wall clock. Create one with [[Mihon.seeded]]
 * for values that repeat from run to run, or with [[Mihon.secure]] for values that do not.
 *
 * One instance may be shared across threads: it stays thread-safe, but which thread gets which
 * value then depends on scheduling, so the values are no longer reproducible.
 *
 * Mihon is for tests and mock-ups. Its data for all its languages adds megabytes to a Scala.js
 * bundle, so it does not belong in a production browser bundle.
 *
 * {{{
 * import dev.taihei.mihon.Mihon
 * import dev.taihei.tsuji.Language
 *
 * val en = Mihon.secure()
 * en.name     // a name such as "Linda Robinson"
 * en.email    // "linda.robinson@brightwavehub.example"
 *
 * val ja = en.withLanguage(Language("ja"))
 * ja.sentence // Japanese text, without spaces, ending in 。, ？ or ！
 * ja.person.fullName // family name first, without a space, such as "田中さちこ"
 *
 * Mihon.seeded(1L, Language("la")).sentence // Lorem Ipsum
 * }}}
 */
final class Mihon private (
  random: Random,
  clock: Clock,
  data: LanguageData):

  /**
   * The language whose data this generator uses: the one it was asked for, or the language that
   * serves it (`en` for `en-GB`, `zh-Hans` for `zh-CN`), see [[Mihon.resolve]].
   */
  def language: Language = data.language

  /**
   * A generator in another language that shares this one's random generator and clock: drawing
   * from either moves the same sequence, so the order of calls across the two is part of what a
   * seed repeats.
   *
   * @throws IllegalArgumentException
   *   when Mihon has no data for `language`, see [[Mihon.resolve]]
   */
  def withLanguage(language: Language): Mihon = new Mihon(random, clock, Mihon.dataFor(language))

  /** One of [[Mihon.languages]], each equally likely. */
  def anyLanguage: Language =
    val all = LanguageData.all
    all(random.nextInt(all.length)).language

  /**
   * A random int in `[1, bound]`, each value equally likely; never 0.
   *
   * @throws IllegalArgumentException
   *   when `bound` is not positive
   */
  def int(bound: Int): Int = 1 + random.nextInt(bound)

  /**
   * A random long in `[1, bound]`, each value equally likely; never 0. The bound is an `Int`, so
   * the result is at most `Int.MaxValue`.
   *
   * @throws IllegalArgumentException
   *   when `bound` is not positive
   */
  def long(bound: Int): Long = 1L + random.nextInt(bound)

  /**
   * A random double in `[0, bound)`.
   *
   * @throws IllegalArgumentException
   *   when `bound` is not positive and finite
   */
  def double(bound: Double): Double =
    if !(bound > 0.0 && bound < Double.PositiveInfinity) then
      throw new IllegalArgumentException(s"bound must be positive and finite: $bound")
    // nextDouble() alone: its algorithm is specified, so a seed gives the same value on every
    // platform; the bounded variant of newer JDKs is not.
    val r = random.nextDouble() * bound
    if r < bound then r else Math.nextDown(bound)

  /**
   * `f` applied to `1, 2, …, n` for a random `n` in `[1, bound]`: between one and `bound` items.
   *
   * {{{
   * mihon.times(10, _ => mihon.name) // one to ten names
   * }}}
   *
   * @throws IllegalArgumentException
   *   when `bound` is not positive
   */
  def times[A](bound: Int, f: Int => A): List[A] = (1 to int(bound)).map(f.apply).toList

  /** True or false, each equally likely. */
  def bool: Boolean = random.nextBoolean()

  // ---------------------------------------------------------------------------------------------
  // Text
  // ---------------------------------------------------------------------------------------------

  private def text = data.text

  /**
   * One word of the language, drawn by how often it is used: frequent words come up more often.
   * For Latin (`la`) the words of Lorem Ipsum.
   */
  def word: String = text.words.draw(random)

  /**
   * One to ten words, joined by the language's word separator: a space, or nothing in Japanese,
   * Chinese and Thai.
   */
  def words: String = times(10, _ => word).mkString(text.wordSeparator)

  /**
   * One sentence in the language: its first letter capitalised where the script has case, now and
   * then a comma or a quoted span in the language's quotation marks, and a full stop, question
   * mark or exclamation mark in the language's own form (`.` `。` `؟` `।` and the Greek `;`). Thai
   * writes no full stop.
   */
  def sentence: String =
    val t        = text
    val n        = t.minWords + random.nextInt(t.maxWords - t.minWords + 1)
    val drawn    = Vector.fill(n)(word)
    val ws       = drawn.updated(0, t.capitalize(drawn(0)))
    val quoted   = random.nextInt(5) == 0
    val quoteLen = if quoted then 1 + random.nextInt(math.min(3, n)) else 0
    val start    = if quoted then random.nextInt(n - quoteLen + 1) else -1
    val end      = start + quoteLen - 1
    val commas   = (0 until n - 1).map(_ => random.nextInt(8) == 0)
    val pieces   = ws.indices.map { i =>
      val open  = if i == start then t.quoteOpen else ""
      val close = if i == end then t.quoteClose else ""
      // A comma inside a quoted span would split it; one after its last word follows the mark.
      val comma = if i < n - 1 && commas(i) && (i < start || i >= end) then t.comma else ""
      open + ws(i) + close + comma
    }
    val stop     = random.nextInt(100)
    val body     = pieces.mkString(t.wordSeparator)
    if stop < 80 then body + t.fullStop
    else if stop < 95 then t.questionOpen + body + t.questionMark
    else t.exclamationOpen + body + t.exclamationMark

  end sentence

  /** One to five sentences, joined by the language's sentence separator. */
  def sentences: String = times(5, _ => sentence).mkString(text.sentenceSeparator)

  /** One to six sentences. */
  def paragraph: String = times(6, _ => sentence).mkString(text.sentenceSeparator)

  /** One to four paragraphs, joined by the language's sentence separator. */
  def paragraphs: String = times(4, _ => paragraph).mkString(text.sentenceSeparator)

  /** One to four paragraphs, each wrapped in `<p>…</p>`. */
  def htmlParagraphs: String = times(4, _ => s"<p>$paragraph</p>").mkString

  /**
   * One to five words as a title: every word capitalised in English, the first letter only in the
   * other languages whose script has case, unchanged where it has none.
   */
  def title: String =
    val t           = text
    val ws          = times(5, _ => word)
    val capitalised =
      if t.titleCase then ws.map(t.capitalize)
      else ws.updated(0, t.capitalize(ws.head))
    capitalised.mkString(t.wordSeparator)

  /**
   * Two to eleven words in the language's script, as a path: one to ten joined by `/`, with one
   * more appended to the last.
   */
  def slug: String = (1 to int(10)).map(_ => word).mkString("/") + word

  // ---------------------------------------------------------------------------------------------
  // People
  // ---------------------------------------------------------------------------------------------

  private def names = data.names

  private def drawGender: Gender = if bool then Gender.Female else Gender.Male

  /** A person of either gender, each equally likely; see [[person(gender:*]]. */
  def person: Person = person(drawGender)

  /**
   * A person with a given name from the language's list for `gender` and a family name in the
   * matching form.
   */
  def person(gender: Gender): Person =
    val n      = names
    val first  = (if gender == Gender.Female then n.female else n.male).draw(random)
    val family = n.family.draw(random)
    val form   = if gender == Gender.Female then family.female else family.male
    new Person(first, form, gender, n.familyFirst, n.separator)

  /** The [[Person.fullName]] of a new [[person]], in the language's name order. */
  def name: String = person.fullName

  /** A female or a male given name, each equally likely. */
  def givenName: String =
    val n = names
    (if bool then n.female else n.male).draw(random).name

  /** A family name, in its female or its male form, each equally likely. */
  def familyName: String =
    val female = bool
    val family = names.family.draw(random)
    (if female then family.female else family.male).name

  /**
   * The e-mail address of a new [[person]]: the romanised name ([[Person.latin]]) in ASCII, lower
   * case, its parts joined by dots, at a domain under the reserved `.example` top-level domain
   * (RFC 2606), so it is ASCII in every language and no mail reaches a real host.
   */
  def email: String =
    val local = person.ascii.map(c => if c >= 'A' && c <= 'Z' then (c + 32).toChar else c)
      .map(c => if c == ' ' then '.' else c)
      .filter(c =>
        (c >= 'a' && c <= 'z') ||
        (c >= '0' && c <= '9') || c == '.' || c == '-' || c == '_'
      )
    s"$local@${DomainNames(random)}"

  // ---------------------------------------------------------------------------------------------
  // Places and contact
  // ---------------------------------------------------------------------------------------------

  private def place = data.country

  /**
   * A street and a house number in the language's default country, made up from the language's
   * names and words: "1600 Wilson Street", "Kerkstraat 12", "ул. Пушкина, д. 5". In Japan a block
   * and lot, such as `千代田1-1-1`.
   */
  def street: String =
    val t = text
    val n = names
    Pattern.template(
      place.streets.draw(random),
      {
        case "family"       => n.family.draw(random).male.name
        case "familyFemale" => n.family.draw(random).female.name
        case "given"        => (if bool then n.female else n.male).draw(random).name
        case "word"         => word
        case "Word"         => t.capitalize(word)
        case "number"       => int(200).toString
        case "small"        => int(30).toString
        case other => throw new IllegalStateException(s"an unknown street placeholder: $other")
      },
    )

  end street

  /** A city of the language's default country, larger cities more often, named in the language. */
  def city: String = place.cities.draw(random).name

  /** A postal code in the pattern of the language's default country, with ASCII digits. */
  def postalCode: String = Pattern.code(place.postalCode, random)

  /** A country's name in the language, any of the world's countries, each equally likely. */
  def country: String =
    val all = data.countries
    all(random.nextInt(all.length))._2

  /**
   * A phone number of the language's default country: `+`, the calling code and the national
   * number, in ASCII digits. Where the country reserves numbers for fiction (the United States'
   * 555-0100 to 555-0199) it is one of those; elsewhere the number may be real, so never call or
   * text it.
   */
  def phone: String = Pattern.code(place.phone, random)

  /**
   * An address in the format of the language's default country: a street from [[street]], a real
   * city, a postal code in the country's pattern and, where the format has one, the state or
   * province.
   */
  def address: Address =
    val c      = place
    val city   = c.cities.draw(random)
    val region = city.region.orElse(c.regions.map(_.draw(random)))
    val postal = postalCode
    val str    = street
    val name   = data.countryNames.getOrElse(c.code, c.code)
    val lines  = c.lines
      .map { line =>
        Pattern.template(
          line,
          {
            case "street"     => str
            case "city"       => city.name
            case "region"     => region.getOrElse("")
            case "postalCode" => postal
            case "country"    => name
            case other => throw new IllegalStateException(s"an unknown address field: $other")
          },
        ).trim
      }
      .filter(_.nonEmpty)
    new Address(lines, c.separator, str, postal, city.name, region, name, c.code)

  end address

  // ---------------------------------------------------------------------------------------------
  // Language-neutral
  // ---------------------------------------------------------------------------------------------

  /** `https://` and a domain under the reserved `.example` top-level domain (RFC 2606). */
  def url: String = s"https://${DomainNames(random)}"

  /**
   * A random version-4 UUID with `_` instead of `-`, drawn from this generator, so a seeded one is
   * reproducible. Not unique across generators with the same seed.
   */
  def id: String =
    // Version 4 (random) and the IETF variant, as UUID.randomUUID sets them.
    val msb = (random.nextLong() & ~0xf000L) | 0x4000L
    val lsb = (random.nextLong() & 0x3fffffffffffffffL) | Long.MinValue
    new UUID(msb, lsb).toString.replace("-", "_")

  /** One printable ASCII character: a letter, a digit or one of `!@#$%^&*()_+-=[]{}|;:,.<>?`. */
  def character: String = Characters(random)

  /**
   * Thirty random [[character]]s. Fake data, not a credential: from a seeded generator it is
   * predictable.
   */
  def password: String = (1 to 30).map(_ => character).mkString

  /** A file extension with its leading dot, such as `.pdf`. */
  def fileExtension: String = FileExtensions(random)

  /** A file extension without the dot, such as `pdf`. */
  def fileExtensionWithoutDot: String = FileExtensions(random).filterNot(Set('.'))

  /** A MIME type, such as `application/pdf`. */
  def mimeType: String = MimeTypes(random)

  // ---------------------------------------------------------------------------------------------
  // Dates, as values: formatting them is the caller's, for instance with Taihei Tsuji
  // ---------------------------------------------------------------------------------------------

  /** A random date: year in `[1, 3000]`, any month, day of month in `[1, 28]`. */
  def localDate: LocalDate = LocalDate.of(int(3000), int(12), int(28))

  /** Today's date by this generator's clock, in the clock's time zone. Not random. */
  def today: LocalDate = LocalDate.now(clock)

  /** The day before [[today]]. Not random. */
  def yesterday: LocalDate = today.minusDays(1)

  /** The day after [[today]]. Not random. */
  def tomorrow: LocalDate = today.plusDays(1)

  /** [[today]] plus one to a thousand weeks. */
  def futureDate: LocalDate = today.plusWeeks(long(1000))

  /** [[today]] minus one to a thousand weeks. */
  def pastDate: LocalDate = today.minusWeeks(long(1000))

end Mihon

/** Creates [[Mihon]] generators and tells which languages they serve. */
object Mihon:

  // 2026-01-01T00:00:00Z as seconds since the epoch. Lazy, and not parsed: once an Instant
  // exists, Scala.js links its formatting (Instant.toString) into the bundle, which a program that
  // never asks for a date should not pay for. MihonSpec checks the value.
  /**
   * The instant a [[seeded]] generator's clock stands at unless it is given another: midnight
   * at the start of 1 January 2026, UTC. It never changes within a major version, because every
   * seeded date depends on it.
   */
  lazy val SeededInstant: Instant = Instant.ofEpochSecond(1_767_225_600L)

  // seeded's default clock: Clock.fixed(SeededInstant, ZoneOffset.UTC), except that it creates
  // no Instant until a date is asked for, for the reason above.
  private object SeededClock extends Clock:
    def getZone: ZoneId                        = ZoneOffset.UTC
    override def withZone(zone: ZoneId): Clock = Clock.fixed(SeededInstant, zone)
    def instant(): Instant                     = SeededInstant
    override def toString: String              = "SeededClock[2026-01-01T00:00:00Z,Z]"

  /**
   * A generator whose values repeat. The same seed, language, clock and Mihon version give the
   * same values in the same order, on the JVM and on Scala.js, whatever the day: unless it is
   * given a clock, its [[Mihon.today]] is the date of [[SeededInstant]], so its dates repeat too.
   * Any other Mihon version, a patch included, may give other values; the release notes say so
   * whenever they do. Pin the Mihon version to assert on a seed's values.
   *
   * {{{
   * import java.time.{Clock, Instant, ZoneOffset}
   *
   * Mihon.seeded(42L).today // 2026-01-01, on any day
   *
   * val clock = Clock.fixed(Instant.parse("2026-10-24T12:00:00Z"), ZoneOffset.UTC)
   * Mihon.seeded(42L, clock = clock).today // 2026-10-24
   *
   * Mihon.seeded(42L, clock = Clock.systemUTC()).today // the real date, in UTC
   *
   * Mihon.seeded(42L, Language("la")).paragraph // Lorem Ipsum
   * }}}
   *
   * @param seed
   *   the seed of the generator's `java.util.Random`
   * @param language
   *   the language of the data, English by default; resolved as [[resolve]] says
   * @param clock
   *   where [[Mihon.today]] and the dates relative to it come from; by default a clock fixed at
   *   [[SeededInstant]] in UTC, so that dates repeat with the seed. Pass `Clock.systemUTC()` for
   *   dates relative to the real date.
   * @throws IllegalArgumentException
   *   when Mihon has no data for `language`
   */
  def seeded(
    seed: Long,
    language: Language = Language.English,
    clock: Clock = SeededClock,
  ): Mihon =
    new Mihon(new Random(seed), clock, dataFor(language))

  /**
   * A generator whose values do not repeat, drawn from a `java.security.SecureRandom`.
   *
   * @param language
   *   the language of the data, English by default; resolved as [[resolve]] says
   * @param clock
   *   where [[Mihon.today]] and the dates relative to it come from; the system clock in UTC by
   *   default
   * @throws IllegalArgumentException
   *   when Mihon has no data for `language`
   */
  def secure(language: Language = Language.English, clock: Clock = Clock.systemUTC()): Mihon =
    new Mihon(new SecureRandom(), clock, dataFor(language))

  /** The languages Mihon has data for, in the order of their tags. */
  def languages: List[Language] = LanguageData.all.map(_.language).toList

  /**
   * The language a factory would use for `language`, or `None` when Mihon has none for it. The tag
   * is looked up as it is, then as an alias (`zh`, `zh-CN` and `zh-SG` are `zh-Hans`; `zh-TW`,
   * `zh-HK` and `zh-MO` are `zh-Hant`; `sr` and `sr-RS` are `sr-Cyrl`), then with subtags stripped
   * from the right (`en-GB` is `en`, `ja-JP` is `ja`). A tag that names another
   * script than the one found is refused (`ru-Latn`). An alias decides: `zh-TW` is served by
   * `zh-Hant`, never by `zh-Hans` through `zh`. A later minor release may serve a
   * tag that is refused today, or serve it by a closer language (`nl-BE` once it has its own
   * data). Pin the version to rely on `language`. Code with input from outside asks first:
   *
   * {{{
   * val mihon = Mihon.secure(Mihon.resolve(requested).getOrElse(Language.English))
   * }}}
   */
  def resolve(language: Language): Option[Language] = LanguageData.resolve(language).map(_.language)

  private def dataFor(language: Language): LanguageData =
    LanguageData.resolve(language).getOrElse {
      throw new IllegalArgumentException(
        s"Mihon has no data for the language ${printable(language.tag)}; it has ${languages.map(_.tag).mkString(", ")}"
      )
    }

  // A tag as it may safely go into a message: anything but printable ASCII as a \uXXXX escape.
  private def printable(s: String): String =
    s.flatMap(c => if c >= ' ' && c <= '~' then c.toString else "\\u%04x".format(c.toInt))

end Mihon
