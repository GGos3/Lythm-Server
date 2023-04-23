package xyz.ggos3.lythmServer.config;

import com.corundumstudio.socketio.SocketIOServer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import xyz.ggos3.lythmServer.socket.PlayerEventHandler;
import xyz.ggos3.lythmServer.socket.RoomEventHandler;

@Component
@RequiredArgsConstructor
public class Runner implements CommandLineRunner {

    private final SocketIOServer server;
    private final RoomEventHandler roomEventHandler;
    private final PlayerEventHandler playerEventHandler;

    @Override
    public void run(String... args) throws Exception {
        server.addListeners(roomEventHandler);
        server.addListeners(playerEventHandler);
        server.start();
    }
}
