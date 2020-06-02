package com.gmail.marcosav2010.command.base;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.gmail.marcosav2010.command.Command;
import com.gmail.marcosav2010.main.Main;
import com.gmail.marcosav2010.peer.Peer;

class NewCMD extends Command {

	NewCMD() {
		super("new", new String[] { "create" },
				"<peer [name] [port]/connection [peer] [address:port] (no address = localhost) [-k]>");
	}

	@Override
	public void execute(String[] arg, int args) {
		if (args < 1) {
			log.log("ERROR: Specify entity type.");
			return;
		}
		switch (arg[0].toLowerCase()) {
			case "peer": {
				Peer peer;
				if (args >= 3) {
					int port;
					try {
						port = Integer.valueOf(arg[2]);
					} catch (NumberFormatException ex) {
						log.log("ERROR: Invalid port.");
						return;
					}
					peer = Main.getInstance().getPeerManager().create(arg[1], port);
				} else if (args >= 2) {
					peer = Main.getInstance().getPeerManager().create(arg[1]);
				} else {
					peer = Main.getInstance().getPeerManager().create();
				}

				if (peer == null)
					return;
				peer.start();
				break;
			}
			case "c":
			case "conn":
			case "connection":
				boolean autoPeer = Main.getInstance().getPeerManager().count() == 1 && args == 2;

				if (autoPeer || args >= 3) {
					Peer peer;

					if (!autoPeer) {
						String peerName = arg[1];

						if (Main.getInstance().getPeerManager().exists(peerName)) {
							peer = Main.getInstance().getPeerManager().get(peerName);
						} else {
							log.log("ERROR: Peer \"" + peerName + "\" doesn't exists.");
							return;
						}
					} else
						peer = Main.getInstance().getPeerManager().getFirstPeer();

					boolean addressKeyConnection = arg[autoPeer ? 1 : 2].equalsIgnoreCase("-k");

					if (addressKeyConnection) {

						log.log("Enter remote Address Key: ");

						var read = System.console().readPassword();
						if (read == null || read.length == 0) {
							log.log("Please enter an Address Key.");
							return;
						}

						InetSocketAddress address;

						try {
							address = peer.getConnectionManager().getHandshakeAuthentificator()
									.parseAddressKey(read).getAddress();

						} catch (IllegalArgumentException e) {
							log.log(e.getMessage());
							return;
						} catch (UnknownHostException e) {
							log.log("This address key references an unknown host.");
							return;
						} catch (BadPaddingException | InvalidKeyException | NoSuchAlgorithmException
								| NoSuchPaddingException | IllegalBlockSizeException e) {
							log.log("There was an error reading the provided address key: " + e.getMessage() + ".");
							return;
						} catch (Exception e) {
							log.log(e, "There was an unknown error reading the provided address key.");
							return;
						}

						try {
							peer.connect(address);
						} catch (Exception e) {
							log.log(e);
						}

					} else {

						String[] rawAddress = arg[autoPeer ? 1 : 2].split(":");
						boolean local = rawAddress.length == 1;

						int port;
						try {
							port = Integer.valueOf(rawAddress[local ? 0 : 1]);
						} catch (NumberFormatException ex) {
							log.log("ERROR: Invalid address format.");
							return;
						}

						try {
							InetSocketAddress address = local
									? new InetSocketAddress(InetAddress.getLocalHost(), port)
									: new InetSocketAddress(rawAddress[0], port);

							if (peer.getConnectionManager().isConnectedTo(address)) {
								log.log("ERROR: \"" + peer.getName()
										+ "\" peer is already connected to this address.");
								return;
							}

							if (local)
								log.log("INFO: No hostname provided, connecting to localhost.");

							peer.connect(address);
						} catch (UnknownHostException e) {
							log.log("ERROR: Invalid address.");

						} catch (IOException | GeneralSecurityException e) {
							log.log(e);
						}
					}
				}
		}
	}
}