package xyz.ggos3.lythmServer.domain;

import lombok.Data;

@Data
public class Player {
    String socketId;
    Long score = 0L;
    String state = "NotReady";

    public Player(String socketId) {
        this.socketId = socketId;
    }
}
