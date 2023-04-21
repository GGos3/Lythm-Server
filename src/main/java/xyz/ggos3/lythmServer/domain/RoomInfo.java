package xyz.ggos3.lythmServer.domain;

import lombok.Data;

import java.util.List;

@Data
public class RoomInfo {
    String name;
    String levelCode;
    String ownerSocketId;
    String maxPlayer;
    String curPlayer;
    List<Player> players;
    String roomCode;

    public RoomInfo(String name, String levelCode, String ownerSocketId, String maxPlayer, String roomCode) {
        this.name = name;
        this.levelCode = levelCode;
        this.ownerSocketId = ownerSocketId;
        this.maxPlayer = maxPlayer;
        this.roomCode = roomCode;
    }
}


