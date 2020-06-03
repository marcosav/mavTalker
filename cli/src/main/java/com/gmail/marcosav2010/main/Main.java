package com.gmail.marcosav2010.main;

import com.gmail.marcosav2010.command.CommandManager;
import com.gmail.marcosav2010.common.PublicIPResolver;
import com.gmail.marcosav2010.config.GeneralConfiguration;
import com.gmail.marcosav2010.logger.ILog;
import com.gmail.marcosav2010.logger.Logger;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;
import com.gmail.marcosav2010.module.CommandModuleLoader;
import com.gmail.marcosav2010.module.ModuleLoader;
import com.gmail.marcosav2010.module.ModuleManager;
import com.gmail.marcosav2010.module.ModuleScope;
import com.gmail.marcosav2010.peer.Peer;
import com.gmail.marcosav2010.peer.PeerManager;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;

public class Main implements ModuleScope {

    @Getter
    @Setter
    private static Main instance;

    @Getter
    private CommandManager commandManager;
    @Getter
    private GeneralConfiguration generalConfig;
    @Getter
    private PeerManager peerManager;
    @Getter
    private ModuleManager moduleManager;
    @Getter
    private ILog log;

    @Getter
    private boolean shuttingDown;

    void init() {
        log = Logger.getGlobal();
        generalConfig = new GeneralConfiguration();

        Logger.setVerboseLevel(generalConfig.getVerboseLevel());

        ModuleLoader.getInstance().load();
        CommandModuleLoader.getInstance().load();

        commandManager = new CommandManager();
        peerManager = new PeerManager();

        moduleManager = new ModuleManager(this);
        moduleManager.initializeModules();
    }

    public void main(String[] args) {
        PublicIPResolver.getInstance();

        log.log("Starting application...", VerboseLevel.MEDIUM);

        moduleManager.onEnable();

        log.log("Done");

        if (args.length == 2) {
            String name = args[0], port = args[1];
            log.log("Trying to create Peer \"" + name + "\" in localhost:" + port + "...", VerboseLevel.MEDIUM);
            run(name, Integer.parseInt(port));
        }
    }

    private void run(String name, int port) {
        Peer startPeer = peerManager.create(name, port);
        startPeer.start();
    }

    public void shutdown() {
        if (shuttingDown)
            return;

        shuttingDown = true;

        moduleManager.onDisable();

        log.log("Exiting application...");
        if (getPeerManager() != null)
            getPeerManager().shutdown();

        if (generalConfig != null)
            try {
                generalConfig.store();
            } catch (IOException e) {
                e.printStackTrace();
            }

        log.log("Bye :)");
    }
}