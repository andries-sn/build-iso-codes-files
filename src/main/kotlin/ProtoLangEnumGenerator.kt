import java.io.BufferedReader
import java.io.File
import java.io.PrintStream
import java.util.stream.Stream
import kotlin.streams.asSequence

class ProtoLangEnumGenerator(
    private val blackedListedIsoCodes: Set<String>,
    private val openTableData: () -> BufferedReader
) {

    fun generate(output: PrintStream) {

        val stripHeader = fun Stream<String>.() = skip(2).asSequence()
        val extractIsoCodes =
            fun Sequence<String>.() = run { map { line -> line.splitToSequence("|").take(4).toList() } }

        val buildIsoLangData = fun Sequence<List<String>>.() = map { r ->
            IsoLanguageCodes(
                iso639_2 = r[1].asSanitisedIsoCode(),
                iso639_1 = r[2].asSanitisedIsoCode(),
                description = r[3].split(";").map(String::trim).first()
            )
        }

        val generateIsoLangProtoEnumEntry = fun(isoLanguageCodes: IsoLanguageCodes, seq: Int) = buildString {
            val finalCode = isoLanguageCodes.iso639_2
            append("  ")
            append("LANGUAGE_")
            append(finalCode.toUpperCase())
            append(" = ")
            append(seq)
            append("; ")
            append("// ")
            append(isoLanguageCodes.description)
        }

        val skipGarbageIsoCodes = fun Sequence<IsoLanguageCodes>.() = filter {
            it.description.isNotBlank()
                    && (it.iso639_2 !in blackedListedIsoCodes)
        }

        val unknownLanguageCode = IsoLanguageCodes("", "UNKNOWN", "Language is not known.")

        val ensureEnglishLangCodeContractIsNotBroken = fun Sequence<IsoLanguageCodes>.(): List<IsoLanguageCodes> {
            val languages = toSortedSet(compareBy(IsoLanguageCodes::description)).toMutableList()
            val givenPositionOfEnglishCode = languages.indexOfFirst { it.iso639_2 == "eng" }
            val expectedEnglishToBeFirst = languages.removeAt(givenPositionOfEnglishCode).copy(iso639_2 = "en")
            languages.add(0, unknownLanguageCode)
            languages.add(1, expectedEnglishToBeFirst)
            return languages.toList()
        }

        val languageCodes = openTableData().use {
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