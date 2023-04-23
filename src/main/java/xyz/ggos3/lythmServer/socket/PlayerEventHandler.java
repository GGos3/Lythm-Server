package xyz.ggos3.lythmServer.socket;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.annotation.OnEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xyz.ggos3.lythmServer.domain.RoomInfo;
import xyz.ggos3.lythmServer.service.RoomEventHandlerService;

import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

@Slf4j
@Component
public class PlayerEventHandler {
    private final RoomEventHandlerService service;
    private final Map<String, RoomInfo> createdRooms;

    public PlayerEventHandler(RoomEventHandlerService service) {
        this.service = service;
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
}
