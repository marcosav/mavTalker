package com.gmail.marcosav2010.fth

import java.nio.file.Path

class FileSendInfo internal constructor(
        val path: Path,
        val size: Int = 0,
        val blocks: Int = 0,
        val blockSize: Int = 0
) {

    var fileID = -1
        private set

    val fileName: String
        get() = path.fileName.toString()

    fun setFileID(fileID: Int) {
        if (this.fileID == -1)
            this.fileID = fileID
        else
            throw IllegalStateException("ID is already set.")
    }
}