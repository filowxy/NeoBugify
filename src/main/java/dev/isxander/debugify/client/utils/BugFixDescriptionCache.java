/*
 * SPDX-License-Identifier: LGPL-3.0-only
 */
package dev.isxander.debugify.client.utils;

import dev.isxander.debugify.Debugify;
import dev.isxander.debugify.fixes.BugFixData;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class BugFixDescriptionCache {
    private static final Path file = Debugify.getConfigDir().resolve("neobugify-descriptions.json");
    private static final Gson gson = new Gson();
    private final Map<String, String> descriptionHolder = new HashMap<>();
    private final String url = "https://bugs.mojang.com/rest/api/2/issue/%s";

    public boolean loadDescriptions() {
        if (Files.notExists(file)) {
            return false;
        }

        try {
            String json = Files.readString(file);
            gson.fromJson(json, JsonObject.class)
                    .entrySet()
                    .forEach(entry ->
                            descriptionHolder.put(entry.getKey(), entry.getValue().getAsString()));

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void cacheMissingDescriptions() {
        ExecutorService executor = Executors.newFixedThreadPool(4, r -> new Thread(r, "NeoBugify Description Cache"));
        HttpClient client = HttpClient.newHttpClient();

        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (BugFixData bugFix : Debugify.CONFIG.getBugFixes().keySet()) {
            String id = bugFix.bugId();
            if (descriptionHolder.containsKey(id)) continue;

            Debugify.LOGGER.info("Caching description for bug {}", id);

            futures.add(CompletableFuture.runAsync(() -> cacheBugDescription(client, id), executor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    save();
                    executor.shutdown();
                });
    }

    private void cacheBugDescription(HttpClient client, String bugId) {
        try {
            HttpRequest request = HttpRequest.newBuilder(new URI(String.format(url, bugId)))
                    .setHeader("User-Agent", "NeoBugify/%s (Mojang bug tracker description cache)".formatted(Debugify.getVersion()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                Debugify.LOGGER.error("Description Cache fail: HTTP status {} - {}", response.statusCode(), response.body());
                return;
            }

            JsonObject json = gson.fromJson(response.body(), JsonObject.class);
            JsonObject fields = json.getAsJsonObject("fields");
            String summary = fields.get("summary").getAsString();

            descriptionHolder.put(bugId, summary);
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void save() {
        try {
            Files.deleteIfExists(file);

            JsonObject json = new JsonObject();
            descriptionHolder.forEach(json::addProperty);

            Files.createFile(file);
            Files.writeString(file, gson.toJson(json));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String get(String id) {
        return descriptionHolder.get(id);
    }

    public boolean has(String id) {
        return descriptionHolder.containsKey(id);
    }
}
