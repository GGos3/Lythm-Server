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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomEventHandler {

    private final SocketIOServer server;
    private final Map<String, RoomInfo> createdRooms = new ConcurrentHashMap<>();

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
            code = createUniqueCode(client);
            log.info("Working: [createRoom] {} -> {}", client.getSessionId(), code);
        }

        code = fillZero(6, code);

        if (client.getAllRooms().contains(code)) {
            log.info("Fail [createRoom] {} -> {} is already created", sessionId, code);
            String finalCode = code;
            client.sendEvent("roomCreateError", new HashMap<String, Object>() {{put("date", new Date().getTime());put("code", finalCode);}});
        } else {
            log.info("Working: [createRoom] {} -> {}", sessionId, code);
        }

        RoomInfo roomInfo = createRoom(client, levelCode, sessionId, code);
        roomInfoUpdate(client, code, roomInfo);
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
        List<Player> playersOnRoom = getPlayerOnRoom(client, code);
        RoomInfo roomInfo = createdRooms.get(code);

        roomInfo.setCurPlayer(playersOnRoom.size());
        roomInfo.setPlayers(playersOnRoom);
        createdRooms.put(code, roomInfo);

        roomInfoUpdate(client, code, roomInfo);
    }

    @OnEvent(value = "leaveRoom")
    public void onLeaveRoom(SocketIOClient client, String code) {
        String sessionId = client.getSessionId().toString();

        if (code == null)
            log.error("Error: [leaveRoom] cannot leave room code is null {}", sessionId);

        log.info("Working: [leaveRoom] {} -> {}", sessionId, code);
        client.leaveRoom(code);
        client.sendEvent("leaveRoomSuccess", new HashMap<String, Object>() {{put("date", new Date().getTime());put("code", code);}});
        clientHasOwner(client, code, sessionId);
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
                    clientHasOwner(client, room, sessionId);
                });
    }

    private void clientHasOwner(SocketIOClient client, String code, String sessionId) {
        Collection<SocketIOClient> clients = client.getNamespace().getRoomOperations(code).getClients();

        if (clients.isEmpty())
            createdRooms.remove(code);
        else {
            List<Player> playerOnRoom = getPlayerOnRoom(client, code);
            RoomInfo roomInfo = createdRooms.get(code);

            roomInfo.setCurPlayer(playerOnRoom.size());
            roomInfo.setPlayers(playerOnRoom);

            if (roomInfo.getOwnerSocketId().toString().contains(sessionId))
                roomInfo.setOwnerSocketId(randomValueFromArray(roomInfo.getPlayers()).getSocketId());
            createdRooms.put(code, roomInfo);

            roomInfoUpdate(client, code, roomInfo);
        }
    }

    public RoomInfo createRoom(SocketIOClient client, String levelCode, UUID sessionId, String code) {
        List<Player> playersOnRoom = getPlayerOnRoom(client, code);

        RoomInfo roomInfo = new RoomInfo(levelCode, sessionId, playersOnRoom.size(),8, code, playersOnRoom);
        createdRooms.put(code, roomInfo);
        return roomInfo;
    }

    private List<Player> getPlayerOnRoom(SocketIOClient client, String code) {
        Collection<SocketIOClient> clients = client.getNamespace().getRoomOperations(code).getClients();
        List<Player> playersOnRoom = new ArrayList<>();

        client.joinRoom(code);
        clients.iterator().forEachRemaining(a -> playersOnRoom.add(new Player(a.getSessionId())));

        return playersOnRoom;
    }

    public String createUniqueCode(SocketIOClient client) {
        String code;
        do {
            int randNum = createRandNum(1, 99999);
            code = fillZero(6, String.valueOf(randNum));
        } while (client.getAllRooms().contains(code));
        return code;
    }

    public void roomInfoUpdate(SocketIOClient client, String code, RoomInfo roomInfo) {
        createdRooms.get(code).getPlayers().iterator().forEachRemaining(
                player -> {
                    SocketIOClient roomClient = server.getClient(player.getSocketId());
                    roomClient.sendEvent("roomUpdate", new HashMap<String, Object>() {{
                        put("date", new Date().getTime());
                        put("room", roomInfo);
                    }});
                }
        );
    }

    public Map<String, RoomInfo> getCreatedRooms() {
        return createdRooms;
    }

    private Player randomValueFromArray(List<Player> players) {
        Random rand = new Random();
        int randomIndex = rand.nextInt(players.size());
        return players.get(randomIndex);
    }

    private int createRandNum(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private String fillZero(int width, String str) {
        return String.format("%0" + width + "d", Integer.parseInt(str));
    }
}

