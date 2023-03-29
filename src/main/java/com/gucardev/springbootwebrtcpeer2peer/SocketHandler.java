package com.gucardev.springbootwebrtcpeer2peer;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SocketHandler {

  private final SocketIOServer server;
  private static final Map<String, String> users = new HashMap<>();
  private static final Map<String, String> rooms = new HashMap<>();

  public SocketHandler(SocketIOServer server) {
    this.server = server;
    server.addListeners(this);
    server.start();
  }

  @OnConnect
  public void onConnect(SocketIOClient client) {
    System.out.println("Client connected: " + client.getSessionId());
    String clientId = client.getSessionId().toString();
    users.put(clientId, null);
  }

  @OnDisconnect
  public void onDisconnect(SocketIOClient client) {
    String clientId = client.getSessionId().toString();
    String room = users.get(clientId);
    if (!Objects.isNull(room)) {
      System.out.println(String.format("Client disconnected: %s from : %s", clientId, room));
      users.remove(clientId);
      client.getNamespace().getRoomOperations(room).sendEvent("userDisconnected", clientId);
    }
    printLog("onDisconnect", client, room);
  }

  @OnEvent("joinRoom")
  public void onJoinRoom(SocketIOClient client, String room) {
    int connectedClients = server.getRoomOperations(room).getClients().size();
    if (connectedClients == 0) {
      client.joinRoom(room);
      client.sendEvent("created", room);
      users.put(client.getSessionId().toString(), room);
      rooms.put(room, client.getSessionId().toString());
    } else if (connectedClients == 1) {
      client.joinRoom(room);
      client.sendEvent("joined", room);
      users.put(client.getSessionId().toString(), room);
      client.sendEvent("setCaller", rooms.get(room));
    } else {
      client.sendEvent("full", room);
    }
    printLog("onReady", client, room);
  }

  @OnEvent("ready")
  public void onReady(SocketIOClient client, String room, AckRequest ackRequest) {
    client.getNamespace().getBroadcastOperations().sendEvent("ready", room);
    printLog("onReady", client, room);
  }

  @OnEvent("candidate")
  public void onCandidate(SocketIOClient client, Map<String, Object> payload) {
    String room = (String) payload.get("room");
    client.getNamespace().getRoomOperations(room).sendEvent("candidate", payload);
    printLog("onCandidate", client, room);
  }

  @OnEvent("offer")
  public void onOffer(SocketIOClient client, Map<String, Object> payload) {
    String room = (String) payload.get("room");
    Object sdp = payload.get("sdp");
    client.getNamespace().getRoomOperations(room).sendEvent("offer", sdp);
    printLog("onOffer", client, room);
  }

  @OnEvent("answer")
  public void onAnswer(SocketIOClient client, Map<String, Object> payload) {
    String room = (String) payload.get("room");
    Object sdp = payload.get("sdp");
    client.getNamespace().getRoomOperations(room).sendEvent("answer", sdp);
    printLog("onAnswer", client, room);
  }

  @OnEvent("leaveRoom")
  public void onLeaveRoom(SocketIOClient client, String room) {
    client.leaveRoom(room);
    printLog("onLeaveRoom", client, room);
  }

  private static void printLog(String header, SocketIOClient client, String room) {
    if (room == null) return;
    int size = 0;
    try {
      size = client.getNamespace().getRoomOperations(room).getClients().size();
    } catch (Exception e) {
      log.error("error ", e);
    }
    log.info("#ConncetedClients - {} => room: {}, count: {}", header, room, size);
  }
}
