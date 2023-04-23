package xyz.ggos3.lythmServer.domain;

import lombok.Data;

import java.util.UUID;

@Data
public class Player {
    UUID socketId;
    Long score = 0L;
    String state = "NotReady";

    public Player(UUID socketId) {
        this.socketId = socketId;
    }
}
