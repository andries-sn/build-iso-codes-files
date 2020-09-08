import java.io.BufferedReader
import java.io.File
import kotlin.streams.asSequence

fun main(args: Array<String>) {


    val tab = "    "
    val markdownDataFile = File("./iso_language_code.md")
    val statements = {
        markdownDataFile.bufferedReader().use { reader ->
            reader.readIsoLanguages()
                ?.map { it.toEnumStatement() }
                ?.joinToString(",\n") { "${tab.repeat(4)}$it" }
                ?: ""
        }
    }

    println(
        """
        ISOLanguage(
            val iso6392: String, 
            val iso6391: String, 
            val alternativeNames:Set<String>) {
            
            ${statements()}
            
            ;
            companion object {
            }
        }        
        """.trimIndent()
    )
}


data class Language(
    val enumName: String,
    val iso6392: String,
    val iso6391: String,
    val alternativeNames: List<String>
)

fun Language.toEnumStatement(): String {

    return """$enumName("$iso6392", "$iso6391", listOf(${alternativeNames.joinToString(",", transform = { "\"$it\"" })}))"""
}

fun String.normalizeAsEnumName(): String {
    return replace("\\s+".toRegex(), "_")
        .replace("\\W+".toRegex(), "_")
        .replace("\\(B\\)".toRegex(), "")
        .replace("_+".toRegex(), "_")
        .trim()
        .toUpperCase()
}

fun parseLanguage(line: String): Language {
    return line.splitToSequence("|")
        .map { it.trim() }
        .take(4).toList()
        .let { r ->
            val names = r[3].split(";").toList()
            Language(
                enumName = names.first().normalizeAsEnumName(),
                iso6391 = r[0].asSanitisedIsoCode(),
                iso6392 = r[1].asSanitisedIsoCode(),
                alternativeNames = names
            )
        }
}

fun BufferedReader.readIsoLanguages(): Sequence<Language>? {
    return buffered().lines().skip(2).map { parseLanguage(it) }.filter { it.enumName.isNotEmpty() }.asSequence()
}

