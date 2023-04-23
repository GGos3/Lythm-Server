package xyz.ggos3.lythmServer.socket;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import io.netty.util.internal.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xyz.ggos3.lythmServer.domain.Player;
import xyz.ggos3.lythmServer.domain.RoomInfo;
import xyz.ggos3.lythmServer.service.RoomEventHandlerService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RoomEventHandler {

    private final RoomEventHandlerService service;
    private final Map<String, RoomInfo> createdRooms;

    public RoomEventHandler(RoomEventHandlerService service) {
        this.service = service;
        this.createdRooms = service.getCreatedRooms();
    }

    @OnConnect
    public void onConnect(SocketIOClient client) {
        String token = client.getHandshakeData().getSingleUrlParam("token");
        if (token != null && token.contains("UNITY")) {
            log.info("[connection] SessionID={}", client.getSessionId());
            client.sendEvent("connection", new HashMap<String, Object>() {{
                put("date", new Date().getTime());
                put("data", "Hello, Unity");
            }});
        } else {
            String errorMessage = "Authentication error: invalid token";
            client.sendEvent("error", errorMessage);
            client.disconnect();
        }
    }

    @OnEvent(value = "createRoom")
    public void onCreateRoom(SocketIOClient client, String code, String levelCode) {
        UUID sessionId = client.getSessionId();

        log.info("Request: [CreateRoom] {} -> {}", sessionId, code);

        if (code.contains("-1")) {
            code = service.createUniqueCode(client);
        }

        code = service.fillZero(6, code);

        if (client.getAllRooms().contains(code)) {
            log.info("Fail [createRoom] {} -> {} is already created", sessionId, code);
            String finalCode = code;
            client.sendEvent("roomCreateError", new HashMap<String, Object>() {{put("date", new Date().getTime());put("code", finalCode);}});
        } else {
            log.info("Working: [createRoom] {} -> {}", sessionId, code);
        }

        RoomInfo roomInfo = service.createRoom(client, levelCode, sessionId, code);
        service.roomInfoUpdate(client, code, roomInfo);
    }

    @OnEvent(value = "joinRoom")
    public void onJoinRoom(SocketIOClient client, String code) {
        String sessionId = client.getSessionId().toString();
        boolean hasRoom = createdRooms.containsKey(code);

        if (!hasRoom) {
            log.info("Fail: [joinRoom] {} -> There is no room match with code {}", sessionId, code);
            client.sendEvent("roomJoinError", new HashMap<String, Object>() {{put("date", new Date().getTime());put("code", code);}});
            return;
        }

        client.joinRoom(code);
        log.info("Working: [joinRooms] {} -> {}", sessionId, code);
        List<Player> playersOnRoom = service.getPlayerOnRoom(client, code);
        RoomInfo roomInfo = createdRooms.get(code);

        roomInfo.setCurPlayer(playersOnRoom.size());
        roomInfo.setPlayers(playersOnRoom);
        createdRooms.put(code, roomInfo);

        service.roomInfoUpdate(client, code, roomInfo);
    }

    @OnEvent(value = "leaveRoom")
    public void onLeaveRoom(SocketIOClient client, String code) {
        String sessionId = client.getSessionId().toString();

        if (code == null)
            log.error("Error: [leaveRoom] cannot leave room code is null {}", sessionId);

        log.info("Working: [leaveRoom] {} -> {}", sessionId, code);
        client.leaveRoom(code);
        client.sendEvent("leaveRoomSuccess", new HashMap<String, Object>() {{put("date", new Date().getTime());put("code", code);}});
        service.clientHasOwner(client, code, sessionId);
    }

    @OnEvent(value = "disconnecting")
    public void onDisconnecting(SocketIOClient client, String reason) {
        String sessionId = client.getSessionId().toString();

        log.info("[disconnect] {} reason {} ", sessionId, reason);

        client.getAllRooms().stream()
                .filter(room -> !Objects.equals(room, sessionId))
                .forEach(room -> {
                    log.info("Emit: [roomUserLeft] {} -> {}", sessionId, room);
                    client.leaveRoom(room);
                    service.clientHasOwner(client, room, sessionId);
                });
    }
}

