package com.blockchain

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest

fun main(args: Array<String>) {
    checkArgs(args)
    createImageFolder(Constants.BLOCKCHAIN_DIR, Constants.PARENT_FOLDER_DIR)
    when (args[0]) {
        "-r", "--read" -> {
            readBlock(
                getLastBlock("${Constants.PARENT_FOLDER_DIR}/${Constants.BLOCKCHAIN_DIR}")
            )
        }
        "-w", "--write" -> {
            writeBlock(
                getLastBlock("${Constants.PARENT_FOLDER_DIR}/${Constants.BLOCKCHAIN_DIR}"),
                args.toMutableList().also {
                    it.removeAt(0)
                }
            )
        }
    }
}

private fun readBlock(lastBlock: Int) {
    val listOfContents: List<String> = getFileContent(getFileName(0)).split("\n")
    val listOfHashes = mutableListOf<String>()
    var dataJson: Block?

    if (lastBlock > 1) {
        println("Check from blocks: ")
        for (it in 2..lastBlock) {
            dataJson = jacksonObjectMapper().readValue(String(File(getFileName(it)).readBytes()), Block::class.java)
            listOfHashes.add(getHash(getFileContent(getFileName(it - 1))))
            if (dataJson.hash == listOfHashes[it - 2]) {
                println("[Block: ${it - 1}] -> Readable")
            } else {
                println("[Block: ${it - 1}] -> Corrupted")
            }
        }
    }

    println()

    listOfHashes.add(getHash(getFileContent(getFileName(lastBlock))))

    listOfContents.forEachIndexed { index, element ->
        if (element == listOfHashes[index]) {
            println("[Block: ${index + 1}] -> Readable")
        } else {
            println("[Block: ${index + 1}] -> Corrupted")
        }
    }

}

private fun writeBlock(lastBlock: Int, args: MutableList<String>) {
    val newBlock = getFileName(lastBlock + 1)
    var hash = ""
    if (lastBlock > 0) {
        hash = getHash(getFileContent(getFileName(lastBlock)))
    }
    saveNewFile(
        newBlock, ObjectMapper().writeValueAsString(
            Block(
                from = args[0],
                to = args[1],
                sum = args[2],
                hash = hash
            )
        )
    )
    extendZeroBlock(getHash(getFileContent(newBlock)))
}

private fun extendZeroBlock(hash: String) {
    val zero = getFileName(0)

    if (!File(zero).exists()) {
        saveNewFile(zero, "")
    }

    val zeroFile = File(zero)

    val currentContent = String(zeroFile.readBytes())

    if (currentContent.isEmpty()) {
        updateFile(zeroFile, "$currentContent$hash")
    } else {
        updateFile(zeroFile, "$currentContent\n$hash")
    }
}

private fun getLastBlock(dir: String): Int {
    var max = 0

    File(dir).also {
        if (!it.exists()) throw Exception("File is not found")
    }.listFiles().forEach {
        val maxBlock = it.name[0].toString().toInt()
        if (maxBlock > max) {
            max = maxBlock
        }
    }

    return max
}

private fun checkArgs(args: Array<String>) {
    if (args.isEmpty())
        throw Exception("Введите аргументы")
    when (args[0]) {
        "-r", "--read" -> return
        "-w", "--write" -> {
            if (args.size != 4)
                throw Exception("Колличество аргументов при записименьше 4")
        }
        else -> throw Exception("Вы ввели неправильные ключи")
    }
}

private fun createImageFolder(imageFolderName: String, parentFolderName: String) =
    File(parentFolderName, imageFolderName).also {
        it.setReadable(true, false)
        it.setExecutable(true, false)
        it.setWritable(true, false)
        if (!it.exists()) {
            it.mkdirs()
        }
    }

private fun getFileContent(fileName: String): String =
    String(Files.readAllBytes(Paths.get(fileName)))

private fun getFileName(base: Int): String =
    "${Constants.PARENT_FOLDER_DIR}/${Constants.BLOCKCHAIN_DIR}/$base${Constants.EXTENSION}"

private fun getHash(json: String): String = bytesToHex(
    MessageDigest
        .getInstance("SHA-256")
        .digest(json.toByteArray(Charsets.UTF_8))
)

private fun saveNewFile(paths: String, json: String) {
    var outputStream: OutputStream? = null
    try {
        outputStream = FileOutputStream(paths)
        outputStream.write(json.toByteArray(Charsets.UTF_8))
    } catch (e: Exception) {
        e.printStackTrace()
        throw RuntimeException("trabl from file")
    } finally {
        try {
            outputStream?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

private fun updateFile(file: File, newContent: String) {
    var fileWriter: FileWriter? = null
    try {
        fileWriter = FileWriter(file)
        fileWriter.write(newContent)
    } catch (e: IOException) {
        e.printStackTrace()
        throw RuntimeException("trabl with file")
    } finally {
        try {
            fileWriter?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

private fun bytesToHex(hash: ByteArray): String {
    val hexString = StringBuffer()
    for (i in hash.indices) {
        val hex = Integer.toHexString(0xff and hash[i].toInt())
        if (hex.length == 1) hexString.append('0')
        hexString.append(hex)
    }
    return hexString.toString()
}

data class Block(
    val from: String,
    val to: String,
    val sum: String,
    val hash: String
)

object Constants {

    const val EXTENSION = ".json"

    const val BLOCKCHAIN_DIR = "Blockchain"

    const val PARENT_FOLDER_DIR = "/home/anvar"

}
