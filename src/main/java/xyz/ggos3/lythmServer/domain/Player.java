package xyz.ggos3.lythmServer.domain;

import lombok.Data;

@Data
public class Player {
    String socketID;
    Long score = 0L;
    String state = "NotReady";
}
