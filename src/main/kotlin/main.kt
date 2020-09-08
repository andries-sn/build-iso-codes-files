@file:JvmName("Main")

import java.io.File
import kotlin.system.exitProcess

fun main(vararg args: String) {

    val blackListedCodes = setOf("peo", "qaa-qtz", "dum", "grc", "ina", "mga", "sga", "ota", "zxx", "ang")
    val openLangTable = { File("iso_language_code.md").bufferedReader() }

    when (val which = args.firstOrNull()) {
        "proto" -> ProtoLangEnumGenerator(blackListedCodes, openLangTable).generate(System.out)
        "enum" -> InternalLangEnumGenerator(blackListedCodes, openLangTable).generate(System.out)
        else -> {
            System.err.println("Unsupported : $which")
            exitProcess(1)
        }
    }

}