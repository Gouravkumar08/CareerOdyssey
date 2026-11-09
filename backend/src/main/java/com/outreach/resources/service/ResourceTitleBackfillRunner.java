package com.outreach.resources.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.outreach.resources.model.ResourceItem;
import com.outreach.resources.repository.ResourceItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Runs on every startup — cheap no-op once every item has a real name. Some of the seeded
 * Free Resources rows (see {@link ResourceSeedRunner}) only ever had a generic placeholder
 * name like "YouTube video" instead of the actual video title (the source link list never
 * carried titles). This backfills those in place by looking the title up via YouTube's public
 * oEmbed endpoint — no API key required. Any item whose name doesn't match a known placeholder
 * is left untouched, and a lookup failure (deleted video, no network, etc.) just leaves that
 * one row as-is rather than failing startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(2)
public class ResourceTitleBackfillRunner implements ApplicationRunner {

    private static final Set<String> PLACEHOLDER_NAMES = Set.of("youtube video", "youtube playlist");
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(6);

    private final ResourceItemRepository itemRepository;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void run(ApplicationArguments args) {
        List<ResourceItem> placeholders = itemRepository.findAll().stream()
                .filter(i -> PLACEHOLDER_NAMES.contains(i.getName().trim().toLowerCase()))
                .toList();

        if (placeholders.isEmpty()) {
            return;
        }

        log.info("Backfilling real titles for {} Free Resources item(s)...", placeholders.size());
        int updated = 0;
        for (ResourceItem item : placeholders) {
            String title = fetchOEmbedTitle(item.getLink());
            if (StringUtils.hasText(title)) {
                item.setName(title.trim());
                itemRepository.save(item);
                updated++;
            }
        }
        log.info("Free Resources title backfill: {} of {} updated.", updated, placeholders.size());
    }

    private String fetchOEmbedTitle(String videoUrl) {
        try {
            String oEmbedUrl = "https://www.youtube.com/oembed?format=json&url="
                    + URLEncoder.encode(videoUrl, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder(URI.create(oEmbedUrl))
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("oEmbed lookup failed for {}: HTTP {}", videoUrl, response.statusCode());
                return null;
            }
            JsonNode node = objectMapper.readTree(response.body());
            return node.hasNonNull("title") ? node.get("title").asText() : null;
        } catch (Exception ex) {
            log.warn("oEmbed lookup failed for {}: {}", videoUrl, ex.getMessage());
            return null;
        }
    }
}
