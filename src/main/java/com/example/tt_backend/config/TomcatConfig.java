package com.example.tt_backend.config;

import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector -> {
            // ✅ S1905 — Cast supprimé, connector est déjà de type Connector
            connector.setMaxParameterCount(500);

            if (connector.getProtocolHandler() instanceof Http11NioProtocol proto) {
                proto.setMaxKeepAliveRequests(500);
            }
        });
    }
}