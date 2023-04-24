package xyz.ggos3.lythmServer.config;

import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;


@Configuration
@ComponentScan
public class WebSocketConfig {
    @Value("${socket-server.host}")
    private String host;

    @Value("${socket-server.port}")
    private int port;

    @Value("${socket-server.ssl.enabled}")
    private boolean sslEnabled;

    @Value("${socket-server.ssl.key-store}")
    private String keyStorePath;

    @Value("${socket-server.ssl.key-store-password}")
    private String keyStorePassword;

    @Bean
    public SocketIOServer socketIOServer() {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname(host);
        config.setPort(port);
        config.setPingTimeout(5000); // 5초
        config.setOrigin("*");

        if (sslEnabled) {
            config.setKeyStorePassword(keyStorePassword);
            InputStream stream = getClass().getClassLoader().getResourceAsStream(keyStorePath);
            config.setKeyStore(stream);
        }

        return new SocketIOServer(config);
    }
}

