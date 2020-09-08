data class IsoLanguageCodes(
    val iso639_1: String,
    val iso639_2: String,
    val description: String
)

fun String.asSanitisedIsoCode():String {
    return replace("(B)","")
        .replace("(T)", "")
        .replace("\\s+".toRegex(), "").trim()
}