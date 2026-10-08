package fr.reveil.musical.infrastructure.track;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class TrackHttpConfiguration {

    @Bean
    ObjectMapper trackObjectMapper() {
        return JsonMapper.builder().build();
    }

    @Bean
    HttpClient trackHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }
}
