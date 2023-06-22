package io.github.colintimbarndt.chat_emotes_util.serial

enum class FileType(val extension: String = "") {
    Folder,
    Json(".json"),
    Zip(".zip"),
}