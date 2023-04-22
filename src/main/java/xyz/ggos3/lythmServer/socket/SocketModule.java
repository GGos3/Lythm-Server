package xyz.ggos3.lythmServer.socket;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
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
public class SocketModule {

    private final SocketIOServer server;
    private final Map<String, RoomInfo> createdRooms = new ConcurrentHashMap<>();

    @OnEvent("createRoom")
    public void onCreateRoom(SocketIOClient client, String roomCode, String levelCode) {
        String sessionId = client.getSessionId().toString();
        String code = roomCode;

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

    public RoomInfo createRoom(SocketIOClient client, String levelCode, String sessionId, String code) {
        Collection<SocketIOClient> clients = client.getNamespace().getRoomOperations(code).getClients();
        List<Player> playersOnRoom = new ArrayList<>();

        client.joinRoom(code);
        clients.iterator().forEachRemaining(a -> playersOnRoom.add(new Player(a.getSessionId().toString())));

        RoomInfo roomInfo = new RoomInfo(levelCode, sessionId, playersOnRoom.size(),8, code, playersOnRoom);
        createdRooms.put(code, roomInfo);
        return roomInfo;
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
        client.sendEvent("roomUpdate", new HashMap<String, Object>() {{
            put("date", new Date().getTime());
            put("room", roomInfo);
        }});
    }


    public int createRandNum(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public String fillZero(int width, String str) {
        return String.format("%0" + width + "d", Integer.parseInt(str));
    }
}

