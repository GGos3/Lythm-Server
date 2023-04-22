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
    public void createRoom(SocketIOClient client, String roomCode, String levelCode) {
        String sessionId = client.getSessionId().toString();
        log.info("Request: [CreateRoom] {} -> {}", sessionId, roomCode);
        String code = roomCode;
        if ("-1".equals(code)) {
            do {
                int randNum = createRandNum(1, 99999);
                code = fillZero(6, String.valueOf(randNum));
            } while (client.getAllRooms().contains(code));

            log.info("Working: [createRoom] {} -> {}", client.getSessionId(), roomCode);

            RoomInfo roomInfo = new RoomInfo("", levelCode, "", 8, code);

            client.joinRoom(code);
            Collection<SocketIOClient> clients = client.getNamespace().getRoomOperations(code).getClients();
            List<Player> playersOnRoom = new ArrayList<>();
            for (SocketIOClient socketIOClient : clients) {
                playersOnRoom.add(new Player(socketIOClient.getSessionId().toString()));
            }

            roomInfo.setOwnerSocketId(sessionId);
            roomInfo.setCurPlayer(playersOnRoom.size());
            roomInfo.setPlayers(playersOnRoom);
            createdRooms.put(code, roomInfo);
            roomInfoUpdate(client, code, roomInfo);
        } else {
            code = fillZero(6, code);
            if (client.getAllRooms().contains(code)) {
                log.info("Fail [createRoom] {} -> {} is already created", sessionId, code);
                String finalCode = code;
                client.sendEvent("roomCreateError", new HashMap<String, Object>() {{
                    put("date", new Date().getTime());
                    put("code", finalCode);
                }});
            } else {
                System.out.println("Working: [createRoom] " + sessionId + " -> " + code);
                client.joinRoom(code);
                Collection<SocketIOClient> clients = client.getNamespace().getRoomOperations(code).getClients();
                List<Player> playersOnRoom = new ArrayList<>();
                for (SocketIOClient socketIOClient : clients) {
                    playersOnRoom.add(new Player(socketIOClient.getSessionId().toString()));
                }
                RoomInfo roomInfo = new RoomInfo("", levelCode, "", 8, code);
                roomInfo.setOwnerSocketId(sessionId);
                roomInfo.setCurPlayer(playersOnRoom.size());
                roomInfo.setPlayers(playersOnRoom);
                createdRooms.put(code, roomInfo);

                roomInfoUpdate(client, code, roomInfo);
            }
        }

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
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < width) {
            sb.insert(0, '0');
        }
        return sb.toString();
    }
}

