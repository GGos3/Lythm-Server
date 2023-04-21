package xyz.ggos3.lythmServer.config;

import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class ServerCommendLineRunner implements CommandLineRunner {

    private final SocketIOServer server;

    @Override
    public void run(String... args) {
        server.start();
        server.addConnectListener(authorizationListener());
    }

    public ConnectListener authorizationListener() {
        return client -> {
            String token = client.getHandshakeData().getSingleUrlParam("token");
            if (token != null && token.contains("UNITY")) {
                log.info("[Connect]={}", client.getSessionId());
                client.sendEvent("connection", new HashMap<String, Object>() {{
                    put("date", new Date().getTime());
                    put("data", "Hello, Unity");
                }});
            } else {
                String errorMessage = "Authentication error: invalid token";
                client.sendEvent("error", errorMessage);
                client.disconnect();
            }
        };
    }
}
