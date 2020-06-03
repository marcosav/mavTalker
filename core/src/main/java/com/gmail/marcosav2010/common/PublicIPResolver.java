package com.gmail.marcosav2010.common;

import com.gmail.marcosav2010.logger.ILog;
import com.gmail.marcosav2010.logger.Logger;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PublicIPResolver {

    private static final String[] IP_PROVIDERS = new String[]{"http://checkip.amazonaws.com",
            "http://bot.whatismyipaddress.com/", "https://ident.me/", "https://ip.seeip.org/",
            "https://api.ipify.org"};
    private static final long IP_TIMEOUT = 5L;
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(1);

    private static PublicIPResolver instance;

    private final ILog log;

    @Getter
    private InetAddress publicAddress;

    public PublicIPResolver() {
        log = Logger.getGlobal();
        obtainPublicAddress();
    }

    public static PublicIPResolver getInstance() {
        if (instance == null)
            instance = new PublicIPResolver();
        return instance;
    }

    private void obtainPublicAddress() {
        log.log("Obtaining public address...", VerboseLevel.MEDIUM);
        try {
            long m = System.currentTimeMillis();
            publicAddress = obtainExternalAddress();
            log.log("Public address got in " + (System.currentTimeMillis() - m) + "ms: " + publicAddress.getHostName(),
                    VerboseLevel.MEDIUM);
        } catch (IOException e) {
            log.log(e, "There was an error while obtaining public address, shutting down...");
        }
    }

    private InetAddress obtainExternalAddress() throws IOException {
        String r;
        try {
            r = EXECUTOR.invokeAny(Stream.of(IP_PROVIDERS).map(this::readRawWebsite).collect(Collectors.toList()),
                    IP_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            return InetAddress.getLocalHost();
        }

        EXECUTOR.shutdownNow();

        return InetAddress.getByName(r);
    }

    private Callable<String> readRawWebsite(String str) {
        return () -> {
            URL ipUrl = new URL(str);
            try (BufferedReader in = new BufferedReader(new InputStreamReader(ipUrl.openStream()))) {
                return in.readLine();
            }
        };
    }
}