import java.io.File
import java.io.PrintStream
import java.util.stream.Stream
import kotlin.streams.asSequence

class ProtoLangEnumGenerator {

    private val blackedListedIsoCodes = setOf("peo", "qaa-qtz", "dum", "grc", "ina", "mga", "sga", "ota")

    data class IsoLanguageCode(
        val iso639_1: String,
        val iso639_2: String,
        val description: String
    )

    fun generate(markDownDataFile: File, output: PrintStream) {

        val stripHeader = fun Stream<String>.() = skip(2).asSequence()
        val extractIsoCodes =
            fun Sequence<String>.() = run { map { line -> line.splitToSequence("|").take(4).toList() } }

        val buildIsoLangData = fun Sequence<List<String>>.() = map { r ->
            IsoLanguageCode(
                iso639_2 = r[1].asSanitisedIsoCode(),
                iso639_1 = r[2].asSanitisedIsoCode(),
                description = r[3].split(";").map(String::trim).first()
            )
        }

        val generateIsoLangProtoEnumEntry = fun(isoLanguageCode: IsoLanguageCode, seq: Int) = buildString {
            val finalCode = isoLanguageCode.iso639_2
            append("  ")
            append("LANGUAGE_")
            append(finalCode.toUpperCase())
            append(" = ")
            append(seq)
            append("; ")
            append("// ")
            append(isoLanguageCode.description)
        }

        val skipGarbageIsoCodes = fun Sequence<IsoLanguageCode>.() = filter {
            it.description.isNotBlank()
                    && (it.iso639_2 !in blackedListedIsoCodes)
        }

        val unknownLanguageCode = IsoLanguageCode("", "UNKNOWN", "Language is not known.")

        val ensureEnglishLangCodeContractIsNotBroken = fun Sequence<IsoLanguageCode>.(): List<IsoLanguageCode> {
            val languages = toSortedSet(compareBy(IsoLanguageCode::description)).toMutableList()
            val givenPositionOfEnglishCode = languages.indexOfFirst { it.iso639_2 == "eng" }
            val expectedEnglishToBeFirst = languages.removeAt(givenPositionOfEnglishCode).copy(iso639_2 = "en")
            languages.add(0, unknownLanguageCode)
            languages.add(1, expectedEnglishToBeFirst)
            return languages.toList()
        }

        val languageCodes = markDownDataFile.bufferedReader().use {
            it.lines()
                .stripHeader()
                .extractIsoCodes()
                .buildIsoLangData()
                .skipGarbageIsoCodes()
                .ensureEnglishLangCodeContractIsNotBroken()
        }


        output.println()
        output.println("// ISO Language codes. ")
        output.println("// NOTE: Using the 3 letter variant by default, with the exception of english which is")
        output.println("// is us already using the two letter variant.")
        output.println("//")
        output.println("enum Language {")
        languageCodes.withIndex().forEach { (seq, code) ->
            output.println("     ${generateIsoLangProtoEnumEntry(code, seq)}")
        }
        output.println("}")
        output.println()

    }


}