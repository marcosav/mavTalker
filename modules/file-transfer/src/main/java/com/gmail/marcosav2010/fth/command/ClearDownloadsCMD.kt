package com.gmail.marcosav2010.fth.command

import com.gmail.marcosav2010.command.Command
import com.gmail.marcosav2010.common.Utils
import com.gmail.marcosav2010.fth.FileTransferHandler
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

internal class ClearDownloadsCMD : Command("cleardownloads", arrayOf("cd", "cleard", "cdownloads")) {

    override fun execute(arg: Array<String>, length: Int) {
        val p = Paths.get(FileTransferHandler.DOWNLOAD_FOLDER)
        if (p.toFile().exists()) try {
            val totalSize = Files.walk(p).sorted(Collections.reverseOrder()).mapToLong { sp -> sp.toFile().length() }
                    .sum()
            Files.walk(p).sorted(Collections.reverseOrder()).map { obj -> obj.toFile() }.forEach { obj -> obj.delete() }
            log.log("INFO: Successfully removed " + Utils.formatSize(totalSize) + " of downloaded files.")

        } catch (e: IOException) {
            log.log(e)
        } else log.log("INFO: There are currently no downloads.")
    }
}