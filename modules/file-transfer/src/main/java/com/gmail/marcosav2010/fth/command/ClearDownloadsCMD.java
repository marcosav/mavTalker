package com.gmail.marcosav2010.fth.command;

import com.gmail.marcosav2010.command.Command;
import com.gmail.marcosav2010.common.Utils;
import com.gmail.marcosav2010.fth.FileTransferHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

class ClearDownloadsCMD extends Command {

    ClearDownloadsCMD() {
        super("cleardownloads", new String[]{"cd", "cleard", "cdownloads"});
    }

    @Override
    public void execute(String[] arg, int args) {
        Path p = Paths.get(FileTransferHandler.DOWNLOAD_FOLDER);
        if (p.toFile().exists())
            try {
                long totalSize = Files.walk(p).sorted(Collections.reverseOrder()).mapToLong(sp -> sp.toFile().length())
                        .sum();
                Files.walk(p).sorted(Collections.reverseOrder()).map(Path::toFile).forEach(File::delete);
                log.log("INFO: Successfully removed " + Utils.formatSize(totalSize) + " of downloaded files.");

            } catch (IOException e) {
                log.log(e);
            }
        else
            log.log("INFO: There are currently no downloads.");
    }
}