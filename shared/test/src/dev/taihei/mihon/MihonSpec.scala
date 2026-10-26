package dev.taihei.mihon

import java.time.*
import java.util.UUID

import zio.test.*

import dev.taihei.tsuji.Language

// The members that do not depend on a language's conventions, on an English and a Latin
// generator. The conventions of each language are LanguagesSpec's.
object MihonSpec extends ZIOSpecDefault:

  private val noon  = Instant.parse("2026-10-24T12:00:00Z")
  private val clock = Clock.fixed(noon, ZoneOffset.UTC)
  private val en    = Mihon.seeded(11L, clock = clock)

  private def many[A](n: Int)(f: => A): IndexedSeq[A] = (1 to n).map(_ => f)

  def spec = suite("MihonSpec")(
    suite("numbers")(
      test("int reaches every value in [1, bound], bound included, never 0") {
        assertTrue(many(600)(en.int(3)).toSet == Set(1, 2, 3), (1 to 50).forall(_ => en.int(1) == 1))
      },
      test("int(2) returns 1 and 2 about equally often") {
        val ones = many(2000)(en.int(2)).count(_ == 1)
        assertTrue(ones > 800 && ones < 1200)
      },
      test("long reaches every value in [1, bound], bound included") {
        assertTrue(many(600)(en.long(3)).toSet == Set(1L, 2L, 3L))
      },
      test("double stays below its bound and refuses one that is not positive and finite") {
        def refused(bound: Double) = scala.util.Try(en.double(bound)).isFailure
        assertTrue(
          many(1000)(en.double(0.5)).forall(d => d >= 0.0 && d < 0.5),
          refused(0.0),
          refused(-1.0),
          refused(Double.NaN),
          refused(Double.PositiveInfinity),
        )
      },
      test("times returns between one and bound items, applied to 1 to n") {
        val runs = many(600)(en.times(3, identity))
        assertTrue(runs.map(_.length).toSet == Set(1, 2, 3), runs.forall(r => r == (1 to r.length).toList))
      },
      test("bool gives both values") {
        assertTrue(many(50)(en.bool).toSet == Set(true, false))
      },
    ),
    suite("text")(
      test("Latin text is Lorem Ipsum") {
        val la    = Mihon.seeded(1L, Language("la"))
        val words = many(3000)(la.word).toSet
        assertTrue(words.contains("lorem"), words.contains("ipsum"), words.contains("dolor"), words.size > 400)
      },
      test("an English sentence starts with a capital and ends with . ? or !") {
        val sentences = many(300)(en.sentence)
        assertTrue(
          sentences.forall(s => s.head.isUpper || s.head == '“'),
          sentences.forall(s => Set('.', '?', '!').contains(s.last)),
        )
      },
      test("words has one to ten words, joined by spaces") {
        val counts = many(500)(en.words.split(' ').length)
        assertTrue(counts.min == 1, counts.max == 10)
      },
      test("an English title capitalises every word") {
        val titles = many(100)(en.title)
        assertTrue(titles.forall(_.split(' ').forall(_.head.isUpper)))
      },
      test("slug is two to eleven words joined by /") {
        val slugs = many(300)(en.slug)
        assertTrue(
          slugs.forall(s => !s.startsWith("/") && !s.endsWith("/")),
          slugs.map(_.count(_ == '/')).max == 9,
        )
      },
      test("htmlParagraphs wraps one to four paragraphs in <p>") {
        val html = many(100)(en.htmlParagraphs)
        assertTrue(
          html.forall(h => h.startsWith("<p>") && h.endsWith("</p>")),
          html.map(_.split("<p>").length - 1).max == 4,
        )
      },
      test("paragraph, paragraphs and sentences are not empty") {
        assertTrue(en.paragraph.nonEmpty, en.paragraphs.nonEmpty, en.sentences.nonEmpty)
      },
    ),
    suite("people and places")(
      test("an English name is a given name, a space and a family name") {
        val p = en.person
        assertTrue(p.fullName == s"${p.givenName} ${p.familyName}", p.latin == p.fullName, p.reading.isEmpty)
      },
      test("person(gender) draws from that gender's list, and person draws both") {
        val women = many(50)(en.person(Gender.Female))
        assertTrue(
          women.forall(_.gender == Gender.Female),
          many(100)(en.person.gender).toSet == Gender.values.toSet,
        )
      },
      test("email is ASCII, lower case, at a reserved .example domain") {
        val emails = many(200)(en.email)
        assertTrue(emails.forall(_.matches("[a-z0-9_-]+\\.[a-z0-9._-]+@[a-z0-9-]+\\.example")))
      },
      test("url uses only the reserved .example top-level domain") {
        assertTrue(many(100)(en.url).forall(_.matches("https://[a-z0-9-]+\\.example")))
      },
      test("an American address has a street, city, state and ZIP code, and the country") {
        val a = en.address
        assertTrue(
          a.countryCode == "US",
          a.country == "United States",
          a.region.nonEmpty,
          a.postalCode.matches("[0-9]{5}"),
          a.lines == List(a.street, s"${a.city}, ${a.region.get} ${a.postalCode}", "United States"),
          a.singleLine == a.lines.mkString(", "),
          a.toString == a.singleLine,
        )
      },
      test("an American phone number is in the range reserved for fiction") {
        assertTrue(many(200)(en.phone).forall(_.matches("\\+1 [2-9][0-9]{2} 555-01[0-9]{2}")))
      },
      test("postal codes and phone numbers use every digit, 0 and 9 included") {
        val digits = many(200)(en.postalCode + en.phone).mkString.filter(_.isDigit).toSet
        assertTrue(digits == ('0' to '9').toSet)
      },
      test("country names a country in the language, any of them") {
        val countries = many(3000)(en.country).toSet
        assertTrue(countries.contains("Belgium"), countries.contains("Japan"), countries.size > 200)
      },
      test("American cities include the five largest, Los Angeles among them") {
        val cities = many(3000)(en.city).toSet
        assertTrue(Set("New York City", "Los Angeles", "Chicago", "Houston", "Phoenix").subsetOf(cities))
      },
      test("people and addresses are values: one seed gives equal ones, with equal hash codes") {
        def draw(seed: Long) =
          val m = Mihon.seeded(seed, Language("ja")); (m.person, m.address)
        val (p1, a1)         = draw(4L)
        val (p2, a2)         = draw(4L)
        val (p3, a3)         = draw(5L)
        val people           = List(p1, p2, p3)
        assertTrue(
          p1 == p2,
          p1.hashCode == p2.hashCode,
          a1 == a2,
          a1.hashCode == a2.hashCode,
          p1 != p3,
          a1 != a3,
          people.distinct == List(p1, p3),
          Set(a1, a2, a3).size == 2,
          !p1.equals(p1.fullName),
          !a1.equals(a1.singleLine),
        )
      },
      test("city, street and postalCode are not empty") {
        assertTrue(en.city.nonEmpty, en.street.nonEmpty, en.postalCode.nonEmpty)
      },
    ),
    suite("language-neutral")(
      test("id is a version-4 UUID of the IETF variant, with _ for -") {
        val uuids = many(200)(UUID.fromString(en.id.replace("_", "-")))
        assertTrue(uuids.forall(u => u.version == 4 && u.variant == 2), uuids.toSet.size == 200)
      },
      test("password is thirty printable ASCII characters") {
        assertTrue(many(20)(en.password).forall(p => p.length == 30 && p.forall(c => c > ' ' && c <= '~')))
      },
      test("character is one character") {
        assertTrue(many(50)(en.character).forall(_.length == 1))
      },
      test("file extensions and MIME types") {
        assertTrue(
          en.fileExtension.startsWith("."),
          !en.fileExtensionWithoutDot.contains('.'),
          en.mimeType.matches("[a-z]+/[a-zA-Z0-9.+-]+"),
        )
      },
    ),
    suite("dates")(
      test("today is the clock's date in the clock's time zone") {
        // 23:30 UTC is already the next day two hours east.
        val late = Instant.parse("2026-10-24T23:30:00Z")
        val utc  = Mihon.seeded(1L, clock = Clock.fixed(late, ZoneOffset.UTC))
        val east = Mihon.secure(clock = Clock.fixed(late, ZoneId.of("+02:00")))
        assertTrue(
          utc.today == LocalDate.of(2026, 10, 24),
          utc.yesterday == LocalDate.of(2026, 10, 23),
          utc.tomorrow == LocalDate.of(2026, 10, 25),
          east.today == LocalDate.of(2026, 10, 25),
        )
      },
      test("a seeded generator without a clock stands at SeededInstant, so its dates repeat") {
        val one = Mihon.seeded(42L)
        val two = Mihon.seeded(42L)
        assertTrue(
          Mihon.SeededInstant == Instant.parse("2026-01-01T00:00:00Z"),
          one.today == LocalDate.of(2026, 1, 1),
          one.yesterday == LocalDate.of(2025, 12, 31),
          one.futureDate == LocalDate.of(2028, 7, 6),
          one.pastDate == LocalDate.of(2011, 5, 12),
          two.futureDate == LocalDate.of(2028, 7, 6),
          two.pastDate == LocalDate.of(2011, 5, 12),
        )
      },
      test("future and past dates are one to a thousand weeks from today") {
        val today = LocalDate.of(2026, 10, 24)
        val weeks = many(500)(
          Seq(
            java.time.temporal.ChronoUnit.WEEKS.between(today, en.futureDate),
            java.time.temporal.ChronoUnit.WEEKS.between(en.pastDate, today),
          )
        ).flatten
        assertTrue(weeks.nonEmpty, weeks.forall(w => w >= 1 && w <= 1000))
      },
      test("localDate reaches December and the 28th, and no day after it") {
        val dates = many(3000)(en.localDate)
        assertTrue(
          dates.exists(_.getMonthValue == 12),
          dates.exists(_.getDayOfMonth == 28),
          dates.forall(_.getDayOfMonth <= 28),
        )
      },
    ),
  )

end MihonSpec
