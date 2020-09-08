import org.intellij.lang.annotations.Language
import java.io.BufferedReader
import java.io.PrintStream
import java.util.stream.Stream
import kotlin.streams.asSequence

class InternalLangEnumGenerator(
    private val blackedListedCodes: Set<String>,
    private val openLangTable: () -> BufferedReader
) {

    private val placeholder = "//%insert-here%"

    @Language("kotlin")
    private val template = """
        @Suppress("unused", "SpellCheckingInspection", "MemberVisibilityCanBePrivate")
        enum class IsoLangCode(
            val isoCode639_1: String, 
            val isoCode639_2: String, 
            val description: String) {
        
        //%insert-here%
        ;
            companion object {
            
                private val byIsoCode639_1 = values()
                    .filterNot { it.isoCode639_1.isBlank() }
                    .map { it.isoCode639_1 to it }.toMap()
                
                private val byIsoCode639_2 = values()
                    .filterNot { it.isoCode639_2.isBlank() }
                    .map { it.isoCode639_2 to it }.toMap()
                
                fun fromIsoCode(code: String): IsoLangCode? = byIsoCode639_2[code] ?: byIsoCode639_1[code]
            }
       }
    """.trimIndent()

    fun generate(output: PrintStream) {

        fun Stream<String>.skipHeader() = skip(2).asSequence()
        fun Sequence<String>.toLangCodeStream(): List<IsoLanguageCodes> {
            return map { line ->
                line.splitToSequence("|").drop(1).take(3).map { it.trim() }.toList().let { record ->
                    IsoLanguageCodes(
                        iso639_2 = record[0].asSanitisedIsoCode(),
                        iso639_1 = record[1].asSanitisedIsoCode(),
                        description = record[2].trim()
                    )
                }
            }.filterNot {
                (it.iso639_1 in blackedListedCodes) || (it.iso639_2 in blackedListedCodes)
            }.toSet().toList().sortedBy(IsoLanguageCodes::iso639_2)
        }


        fun IsoLanguageCodes.toEnumSourceFragment(enumName: String): String {
            return """$enumName("$iso639_1","$iso639_2", "${this.description}")"""
        }

        fun List<IsoLanguageCodes>.toEnumMapped(): List<Pair<String, IsoLanguageCodes>> {
            return map {
                val enumName = it.description.split(";").first().normalizeAsEnumName()
                enumName to it
            }
        }

        val fragments = openLangTable().use { reader ->
            reader.lines()
                .skipHeader()
                .toLangCodeStream()
                .toEnumMapped()
                .filterNot { (name, _) -> name.isBlank() }
                .map { (enumName, isoCodes) -> isoCodes.toEnumSourceFragment(enumName) }
        }

        val before = template.substringBefore(placeholder)
        val after = template.substringAfter(placeholder)

        output.println(before)
        fragments.forEachIndexed { i, fragment ->
            if (i > 0) output.print(",\n")
            output.print("        $fragment")
        }
        output.println()
        output.println(after)
    }


    companion object {
        private fun String.normalizeAsEnumName(): String {
            return replace("\\s+".toRegex(), "_")
                .replace("\\W+".toRegex(), "_")
                .replace("\\(B\\)".toRegex(), "")
                .replace("_+".toRegex(), "_")
                .trim()
                .toUpperCase()
        }
    }

}


