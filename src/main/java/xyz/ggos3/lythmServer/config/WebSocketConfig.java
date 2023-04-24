package xyz.ggos3.lythmServer.config;

import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


@Configuration
@ComponentScan
public class WebSocketConfig {
    @Value("${socket-server.host}")
    private String host;

    @Value("${socket-server.port}")
    private int port;

    @Bean
    public SocketIOServer socketIOServer() {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname(host);
        config.setPort(port);
        config.setPingTimeout(5000); // 5초
        config.setOrigin("*");

        return new SocketIOServer(config);
    }
}

