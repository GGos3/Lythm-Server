package xyz.ggos3.lythmServer.domain;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class RoomInfo {
    String name;
    String levelCode;
    UUID ownerSocketId;
    int maxPlayer;
    int curPlayer;
    List<Player> players;
    String roomCode;

    public RoomInfo(String levelCode, UUID ownerSocketId, int curPlayer, int maxPlayer, String roomCode, List<Player> players) {
        this.levelCode = levelCode;
        this.ownerSocketId = ownerSocketId;
        this.curPlayer = curPlayer;
        this.maxPlayer = maxPlayer;
        this.roomCode = roomCode;
        this.players = players;
    }
}


