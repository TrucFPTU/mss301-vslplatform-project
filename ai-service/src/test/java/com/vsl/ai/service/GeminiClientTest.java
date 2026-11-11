package com.vsl.ai.service;

import com.sun.net.httpserver.HttpServer;
import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** GeminiClient với server giả (trả mã HTTP theo từng key + model): đổi model, đổi key dự phòng. */
class GeminiClientTest {

    private HttpServer server;
    /** "key|model" -> mã HTTP trả về (mặc định 200). */
    private final Map<String, Integer> statusByKeyModel = new ConcurrentHashMap<>();
    /** Thứ tự các lần gọi: "key|model". */
    private final List<String> calls = new CopyOnWriteArrayList<>();

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1beta/models/", ex -> {
            String path = ex.getRequestURI().getPath();
            String model = path.substring("/v1beta/models/".length(), path.indexOf(':'));
            String id = ex.getRequestHeaders().getFirst("x-goog-api-key") + "|" + model;
            calls.add(id);
            int code = statusByKeyModel.getOrDefault(id, 200);
            byte[] body = (code == 200 ? "{\"ok\":\"" + id + "\"}" : "{\"error\":" + code + "}").getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(code, body.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private GeminiClient client(String backupKey) {
        return new GeminiClient("main", backupKey, "http://127.0.0.1:" + server.getAddress().getPort(),
                "m1", List.of("m2"), true);
    }

    private String call(GeminiClient c) {
        return c.generateJson("hi", 0, ErrorCode.MODERATION_ERROR);
    }

    @Test
    void mainKeyWorks_backupNeverUsed() {
        assertThat(call(client("backup"))).contains("main|m1");
        assertThat(calls).containsExactly("main|m1");
    }

    @Test
    void modelOverloaded_triesFallbackModel_sameKey() {
        statusByKeyModel.put("main|m1", 503);

        assertThat(call(client("backup"))).contains("main|m2");
        assertThat(calls).containsExactly("main|m1", "main|m2");
    }

    @Test
    void mainKeyOutOfQuota_switchesToBackupKey() {
        statusByKeyModel.put("main|m1", 429);
        statusByKeyModel.put("main|m2", 429);

        assertThat(call(client("backup"))).contains("backup|m1");
        assertThat(calls).containsExactly("main|m1", "main|m2", "backup|m1");
    }

    @Test
    void mainKeyRevoked_switchesToBackupImmediately() {
        statusByKeyModel.put("main|m1", 403);

        assertThat(call(client("backup"))).contains("backup|m1");
        // Key sai thì đổi model cũng không khỏi -> không thử m2 với key chính.
        assertThat(calls).containsExactly("main|m1", "backup|m1");
    }

    @Test
    void mainKeyOverloadedForEveryModel_switchesToBackupKey() {
        statusByKeyModel.put("main|m1", 503);
        statusByKeyModel.put("main|m2", 503);

        assertThat(call(client("backup"))).contains("backup|m1");
        assertThat(calls).containsExactly("main|m1", "main|m2", "backup|m1");
    }

    @Test
    void mainKeyMixedErrors_switchesToBackupKey_andBackupFallsBackToo() {
        statusByKeyModel.put("main|m1", 503);
        statusByKeyModel.put("main|m2", 429);
        statusByKeyModel.put("backup|m1", 503);

        assertThat(call(client("backup"))).contains("backup|m2");
        assertThat(calls).containsExactly("main|m1", "main|m2", "backup|m1", "backup|m2");
    }

    @Test
    void everythingOverloaded_fails() {
        statusByKeyModel.put("main|m1", 503);
        statusByKeyModel.put("main|m2", 503);
        statusByKeyModel.put("backup|m1", 503);
        statusByKeyModel.put("backup|m2", 503);

        assertThatThrownBy(() -> call(client("backup"))).isInstanceOf(AppException.class);
        assertThat(calls).hasSize(4);
    }

    @Test
    void bothKeysOutOfQuota_fails() {
        statusByKeyModel.put("main|m1", 429);
        statusByKeyModel.put("main|m2", 429);
        statusByKeyModel.put("backup|m1", 429);
        statusByKeyModel.put("backup|m2", 429);

        assertThatThrownBy(() -> call(client("backup"))).isInstanceOf(AppException.class);
        assertThat(calls).hasSize(4);
    }

    @Test
    void noBackupKey_quotaError_fails() {
        statusByKeyModel.put("main|m1", 401);

        assertThatThrownBy(() -> call(client(""))).isInstanceOf(AppException.class);
        assertThat(calls).containsExactly("main|m1");
    }

    @Test
    void onlyBackupKeySet_isEnabledAndUsesIt() {
        GeminiClient c = new GeminiClient("", "backup", "http://127.0.0.1:" + server.getAddress().getPort(),
                "m1", List.of(), true);

        assertThat(c.isEnabled()).isTrue();
        assertThat(call(c)).contains("backup|m1");
    }

    @Test
    void sameKeyTwice_triedOnce() {
        statusByKeyModel.put("main|m1", 429);
        statusByKeyModel.put("main|m2", 429);

        assertThatThrownBy(() -> call(client("main"))).isInstanceOf(AppException.class);
        assertThat(calls).containsExactly("main|m1", "main|m2");
    }
}
