package xyz.ggos3.lythmServer.socket;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xyz.ggos3.lythmServer.domain.RoomInfo;
import xyz.ggos3.lythmServer.service.RoomEventHandlerService;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

@Slf4j
@Component
public class PlayerEventHandler {
    private final RoomEventHandlerService service;
    private final SocketIOServer server;
    private final Map<String, RoomInfo> createdRooms;

    public PlayerEventHandler(RoomEventHandlerService service, SocketIOServer server) {
        this.service = service;
        this.server = server;
        createdRooms = service.getCreatedRooms();
    }

    @OnEvent("roomSelectedLevel")
    public void onRoomSelectedLevel(SocketIOClient client, String code,  String levelCode) {
        UUID sessionId = client.getSessionId();

        if (code == null) {
            log.info("Error: [roomSelectedLevel] can not send to other levelCode {} {} -> {}", levelCode, sessionId, code);
            return;
        }

        log.info("Working: [roomSelectedLevel] levelCode [{}] {} -> {}", levelCode, sessionId, code);

        RoomInfo roomInfo = createdRooms.get(code);
        roomInfo.setLevelCode(levelCode);

        service.roomInfoUpdate(client, code, roomInfo);
    }

    @OnEvent("roomPlayerReady")
    public void onRoomPlayerReady(SocketIOClient client, String code) {
        UUID sessionId = client.getSessionId();

        if (code == null){
            log.info("Error: [roomPlayerReady] cannot Player Ready {} -> {}", sessionId, code);
            return;
        }

        log.info("Working: [roomPlayerReady] cannot Player Ready {} -> {}", sessionId, code);
        updatePlayerState(client, code, "Ready");
    }

    @OnEvent("roomPlayerReadyCancel")
    public void onRoomPlayerReadyCancel(SocketIOClient client, String code) {
        UUID sessionId = client.getSessionId();
        if (code == null) {
            log.info("Error: [roomPlayerReadyCancel] cannot Cancel Player Ready Code is Null {}", sessionId);
            return;
        }

        log.info("Working: [roomPlayerReadyCancel] cannot Player Ready {} -> {}", sessionId, code);
        updatePlayerState(client, code, "NotReady");
    }

    @OnEvent("roomChangeOwner")
    public void onRoomChangeOwner(SocketIOClient client, String code, UUID newOwner) {
        UUID sessionId = client.getSessionId();

        if (newOwner == null) {
            log.info("Error: [roomChangeOwner] cannot ChangeOwner {} -> {}", sessionId, code);
            return;
        }

        log.info("Working: [roomChangeOwner] {} -> {}", sessionId, code);

        updateOwnerSocket(client, code, newOwner);
    }

    @OnEvent("roomStartGame")
    public void onRoomStartGame(SocketIOClient client, String code) {
        UUID sessionId = client.getSessionId();

        if (code == null) {
            log.info("Errors: [roomStartGame] cannot Start Game Code is Null {}", sessionId);
            return;
        }

        log.info("Working: [roomStartGame] {} -> {}", sessionId, code);
        updateStateAndSendRoomUserToRoomStart(client, code);
    }

    @OnEvent("roomStartGamePlayerReady")
    public void onRoomStartGamePlayerReady(SocketIOClient client, String code) {
        UUID sessionId = client.getSessionId();

        if (code == null) {
            log.info("Error: [roomStartGamePlayerReady] {} -> {}", sessionId, code);
            return;
        }

        log.info("Working: [roomStartGamePlayerReady] {} -> {}", sessionId, code);
        updatePlayerState(client, code, "Playing");
    }

    @OnEvent("roomPlayerState")
    public void onRoomPlayerState(SocketIOClient client, String code, String state) {
        UUID sessionId = client.getSessionId();

        if (code == null) {
            log.info("Error: [roomPlayerState] cannot set state Code is Null {}", sessionId);
        }

        log.info("Working: [roomPlayerState] {} {} -> {}", state, sessionId, code);
        updatePlayerState(client, code, state);
    }

    @OnEvent("roomPlayerScoreUpdate")
    public void onRoomPlayerScoreUpdate(SocketIOClient client, String roomCode, String score) {
        UUID sessionId = client.getSessionId();

        if (roomCode == null) {
            log.info("Error: [roomPlayerScoreUpdate] cannot update score {} -> {}", score, sessionId);
            return;
        }

        updatePlayerScore(client, roomCode, score, sessionId);
    }

    private void updatePlayerScore(SocketIOClient client, String roomCode, String score, UUID sessionId) {
        RoomInfo roomInfo = createdRooms.get(roomCode);
        if (roomInfo == null)
            return;

        roomInfo.getPlayers().stream()
                .filter(player -> player.getSocketId().equals(sessionId))
                .findFirst()
                .ifPresent(player -> player.setScore(Long.parseLong(score)));

        createdRooms.put(roomCode, roomInfo);
        service.roomInfoUpdate(client, roomCode, roomInfo);
    }

    @OnEvent("roomPlayerGameResult")
    public void onRoomPlayerGameResult(SocketIOClient client, String rooCode, String result) {
        UUID sessionId = client.getSessionId();
        if (rooCode == null) {
            log.info("Error: [roomPlayerGameResult] cannot get result {}", sessionId);
            return;
        }
        sendRoomUserToResult(rooCode, result);
    }

    private void sendRoomUserToResult(String rooCode, String result) {
        RoomInfo roomInfo = createdRooms.get(rooCode);
        if (roomInfo == null)
            return;

        server.getRoomOperations(rooCode).getClients()
                .forEach(a -> a.sendEvent("roomPlayerResult", new HashMap<String, Object>(){{
                    put("date", new Date().getTime());
                    put("resultJson", result);
                }}));
    }

    private void updateStateAndSendRoomUserToRoomStart(SocketIOClient client, String code) {
        RoomInfo roomInfo = createdRooms.get(code);
        roomInfo.getPlayers().stream()
                .filter(p -> p.getState().equals("Ready"))
                .findFirst()
                .ifPresent(p -> p.setState("Loading"));

        createdRooms.put(code, roomInfo);
        service.roomInfoUpdate(client, code, roomInfo);
        server.getRoomOperations(code).getClients()
                .forEach(a -> a.sendEvent("roomStartGame", new HashMap<String, Object>() {{
                    put("date", new Date().getTime());
                    put("room", createdRooms.get(code));
                }}));
    }

    private void updatePlayerState(SocketIOClient client, String code, String state) {
        RoomInfo roomInfo = createdRooms.get(code);
        int index = IntStream.range(0, roomInfo.getPlayers().size())
                .filter(i -> roomInfo.getPlayers().get(i).getSocketId().equals(client.getSessionId()))
                .findFirst()
                .orElse(-1);

        roomInfo.getPlayers().get(index).setState(state);

        createdRooms.put(code, roomInfo);
        service.roomInfoUpdate(client, code, roomInfo);
    }

    private void updateOwnerSocket(SocketIOClient client, String code, UUID newOwner) {
        RoomInfo roomInfo = createdRooms.get(code);
        roomInfo.setOwnerSocketId(newOwner);
        createdRooms.put(code, roomInfo);

        service.roomInfoUpdate(client, code, roomInfo);
    }
}
