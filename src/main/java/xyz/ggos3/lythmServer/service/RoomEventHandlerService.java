package xyz.ggos3.lythmServer.service;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import io.netty.util.internal.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import xyz.ggos3.lythmServer.domain.Player;
import xyz.ggos3.lythmServer.domain.RoomInfo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomEventHandlerService {

    private final SocketIOServer server;
    private final Map<String, RoomInfo> createdRooms = new ConcurrentHashMap<>();

    public void clientHasOwner(SocketIOClient client, String code, String sessionId) {
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
        client.joinRoom(code);
        List<Player> playersOnRoom = getPlayerOnRoom(client, code);
        Collection<SocketIOClient> clients = client.getNamespace().getRoomOperations(code).getClients();

        log.info("clients {}", clients);

        RoomInfo roomInfo = new RoomInfo(levelCode, sessionId, playersOnRoom.size(),8, code, playersOnRoom);
        createdRooms.put(code, roomInfo);

        return roomInfo;
    }

    public List<Player> getPlayerOnRoom(SocketIOClient client, String code) {
        Collection<SocketIOClient> clients = client.getNamespace().getRoomOperations(code).getClients();
        List<Player> playersOnRoom = new ArrayList<>();
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

    public Player randomValueFromArray(List<Player> players) {
        Random rand = new Random();
        int randomIndex = rand.nextInt(players.size());
        return players.get(randomIndex);
    }

    public int createRandNum(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public String fillZero(int width, String str) {
        return String.format("%0" + width + "d", Integer.parseInt(str));
    }
}
