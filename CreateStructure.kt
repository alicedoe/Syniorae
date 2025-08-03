import java.io.File

fun main() {
    val basePath = "com/syniorae"

    val structure = listOf(
        "SyniOraeApplication.kt",
        "core/base",
        "core/constants",
        "core/extensions",
        "core/utils",
        "core/navigation",
        "data/local",
        "data/remote",
        "data/repository",
        "domain/models",
        "domain/usecases",
        "domain/exceptions",
        "presentation/activities",
        "presentation/fragments",
        "presentation/common",
        "services"
    )

    val baseDir = File(basePath)

    if (!baseDir.exists()) {
        baseDir.mkdirs()
        println("Création du dossier racine : $basePath")
    }

    structure.forEach { path ->
        val fileOrDir = File(baseDir, path)
        if (path.endsWith(".kt")) {
            if (!fileOrDir.exists()) {
                fileOrDir.createNewFile()
                println("Fichier créé : ${fileOrDir.path}")
            }
        } else {
            if (!fileOrDir.exists()) {
                fileOrDir.mkdirs()
                println("Dossier créé : ${fileOrDir.path}")
            }
        }
    }
}
