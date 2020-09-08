import java.io.File

fun main(args: Array<String>) {
    val markdownDataFile = File("./iso_language_code.md")
    ProtoLangEnumGenerator().generate(
        markdownDataFile,
        output = System.out
    )
}

