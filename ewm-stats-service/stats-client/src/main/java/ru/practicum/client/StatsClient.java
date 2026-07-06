package ru.practicum.client;

import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class StatsClient {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 5_000;
    private static final int DEFAULT_READ_TIMEOUT_MS    = 10_000;

    private final RestTemplate restTemplate;
    private final String serverUrl;

    /**
     * Primary constructor. Callers are responsible for providing a {@link RestTemplate}
     * that is already configured with appropriate timeouts and error handlers.
     * Use {@link #create(String)} for a sensible set of defaults.
     */
    public StatsClient(String serverUrl, RestTemplate restTemplate) {
        if (serverUrl == null || serverUrl.isBlank()) {
            throw new IllegalArgumentException("serverUrl must not be blank");
        }
        if (restTemplate == null) {
            throw new IllegalArgumentException("restTemplate must not be null");
        }
        this.restTemplate = restTemplate;
        this.serverUrl = serverUrl;
    }

    /**
     * Convenience factory that creates a client backed by a {@link RestTemplate}
     * with {@value DEFAULT_CONNECT_TIMEOUT_MS} ms connect timeout and
     * {@value DEFAULT_READ_TIMEOUT_MS} ms read timeout.
     */
    public static StatsClient create(String serverUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(DEFAULT_READ_TIMEOUT_MS);
        return new StatsClient(serverUrl, new RestTemplate(factory));
    }

    public ResponseEntity<Object> saveHit(EndpointHitDto hitDto) {
        return restTemplate.postForEntity(serverUrl + "/hit", hitDto, Object.class);
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                       List<String> uris, boolean unique) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(serverUrl + "/stats")
                .queryParam("start", start.format(FORMATTER))
                .queryParam("end", end.format(FORMATTER))
                .queryParam("unique", unique);

        if (uris != null && !uris.isEmpty()) {
            builder.queryParam("uris", uris.toArray());
        }

        ViewStatsDto[] response = restTemplate.getForObject(builder.build(false).toUri(), ViewStatsDto[].class);
        return response != null ? Arrays.asList(response) : List.of();
    }
}
