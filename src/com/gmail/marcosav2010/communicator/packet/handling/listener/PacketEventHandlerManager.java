package com.gmail.marcosav2010.communicator.packet.handling.listener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.gmail.marcosav2010.communicator.packet.Packet;
import com.gmail.marcosav2010.connection.Connection;
import com.gmail.marcosav2010.logger.ILog;
import com.gmail.marcosav2010.logger.Log;
import com.gmail.marcosav2010.peer.ConnectedPeer;

/**
 * This class handles packets when are received.
 * 
 * @author Marcos
 *
 */
public class PacketEventHandlerManager {

	private final ILog log;

	private final Map<Method, ? extends PacketListener> methodListener = new HashMap<>();
	private final Map<Class<? extends Packet>, Set<Method>> packetMethods = new HashMap<>();

	public PacketEventHandlerManager(Connection connection) {
		log = new Log(connection, "PEH");
	}

	public int getListenerCount() {
		return new HashSet<>(methodListener.values()).size();
	}

	public long getHandlerCount() {
		return packetMethods.values().stream().map(m -> m).count();
	}

	@SuppressWarnings("unchecked")
	private <T extends PacketListener> void put(Map<Method, ? extends PacketListener> set, Method m, T listener) {
		((Map<Method, T>) set).put(m, listener);
	}

	@SuppressWarnings("unchecked")
	public void registerListeners(Collection<PacketListener> packetListeners) {
		packetListeners.forEach(l -> {
			Stream.of(l.getClass().getMethods()).forEach(m -> {
				if (m.isAnnotationPresent(PacketEventHandler.class) && m.getParameters().length == 2
						&& Packet.class.isAssignableFrom(m.getParameterTypes()[0])
						&& ConnectedPeer.class.isAssignableFrom(m.getParameterTypes()[1])) {
					Class<? extends Packet> packetType = (Class<? extends Packet>) m.getParameters()[0].getType();
					if (!packetMethods.containsKey(packetType)) {
						packetMethods.put(packetType, Set.of(m));
					} else {
						Set<Method> mList = packetMethods.get(packetType);
						mList.add(m);
						packetMethods.put(packetType, mList);
					}
					put(methodListener, m, l);
				}
			});
		});
	}

	public void unregisterEvents() {
		packetMethods.clear();
		methodListener.clear();
	}

	public void handlePacket(Packet packet, ConnectedPeer peer) {
		Set<Method> pClass = packetMethods.get(packet.getClass());
		if (pClass == null)
			return;

		pClass.forEach(me -> {
			try {
				me.invoke(methodListener.get(me), packet, peer);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				log.log(e,
						"There was an error while handling event:\n\tMethod: " + me.getName() + "\n\tClass: "
								+ me.getDeclaringClass().getName() + "\n\tPacket: " + packet.getClass().getSimpleName()
								+ "\n\tStacktrace: ");
			}
		});
	}
}
