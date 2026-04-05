package com.edu.pe.vallegrande.genesis.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import javax.net.ssl.SSLException;
import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Value("${rapidapi.key}")
    private String rapidApiKey;

    @Value("${rapidapi.face-detection.host}")
    private String faceDetectionHost;

    @Value("${rapidapi.face-analyzer.host}")
    private String faceAnalyzerHost;

    private HttpClient buildHttpClient() throws SSLException {
        SslContext sslContext = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        ConnectionProvider provider = ConnectionProvider.builder("rapidapi")
                .maxConnections(50)
                .maxIdleTime(Duration.ofSeconds(20))
                .build();

        return HttpClient.create(provider)
                .secure(spec -> spec.sslContext(sslContext))
                .followRedirect(true)
                .responseTimeout(Duration.ofSeconds(60));
    }

    /** Cliente para descargar imágenes públicas con headers de browser para evitar bloqueos */
    @Bean("imageDownloaderClient")
    public WebClient imageDownloaderClient(WebClient.Builder builder) throws SSLException {
        SslContext sslContext = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        HttpClient httpClient = HttpClient.create()
                .secure(spec -> spec.sslContext(sslContext))
                .followRedirect(true)
                .responseTimeout(Duration.ofSeconds(30));

        return builder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .defaultHeader("Accept", "image/webp,image/apng,image/*,*/*;q=0.8")
                .defaultHeader("Accept-Language", "en-US,en;q=0.9")
                .defaultHeader("Referer", "https://www.google.com/")
                .build();
    }

    @Bean("faceDetectionClient")
    public WebClient faceDetectionClient(WebClient.Builder builder) throws SSLException {
        return builder
                .clientConnector(new ReactorClientHttpConnector(buildHttpClient()))
                .baseUrl("https://face-detection14.p.rapidapi.com")
                .defaultHeader("x-rapidapi-key", rapidApiKey)
                .defaultHeader("x-rapidapi-host", faceDetectionHost)
                .build();
    }

    @Bean("faceAnalyzerClient")
    public WebClient faceAnalyzerClient(WebClient.Builder builder) throws SSLException {
        return builder
                .clientConnector(new ReactorClientHttpConnector(buildHttpClient()))
                .baseUrl("https://face-detection-and-analysis.p.rapidapi.com")
                .defaultHeader("x-rapidapi-key", rapidApiKey)
                .defaultHeader("x-rapidapi-host", "face-detection-and-analysis.p.rapidapi.com")
                .build();
    }
}
