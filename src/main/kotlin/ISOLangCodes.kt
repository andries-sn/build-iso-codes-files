fun String.asSanitisedIsoCode():String {
    return replace("(B)","")
        .replace("(T)", "")
        .replace("\\s+".toRegex(), "").trim()
}