package com.hermes.webui.api;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HermesApi {
    private static final String TAG = "HermesApi";
    private String baseUrl = "http://localhost:8787";
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public void setBaseUrl(String url) {
        this.baseUrl = url;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    // ===== 会话管理 =====
    public void getSessions(ApiCallback<JSONArray> callback) {
        executor.execute(() -> {
            try {
                String resp = httpGet(baseUrl + "/api/sessions");
                JSONObject json = new JSONObject(resp);
                JSONArray sessions = json.getJSONArray("sessions");
                mainHandler.post(() -> callback.onSuccess(sessions));
            } catch (Exception e) {
                Log.e(TAG, "getSessions 失败", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void getSession(String sessionId, ApiCallback<JSONObject> callback) {
        executor.execute(() -> {
            try {
                String resp = httpGet(baseUrl + "/api/session?session_id=" + sessionId);
                JSONObject json = new JSONObject(resp);
                mainHandler.post(() -> callback.onSuccess(json));
            } catch (Exception e) {
                Log.e(TAG, "getSession 失败", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void deleteSession(String sessionId, ApiCallback<JSONObject> callback) {
        executor.execute(() -> {
            try {
                String resp = httpPost(baseUrl + "/api/session/delete", "{\"session_id\":\"" + sessionId + "\"}");
                mainHandler.post(() -> callback.onSuccess(new JSONObject(resp)));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ===== 聊天 =====
    public void sendChatMessage(String sessionId, String message, ApiCallback<JSONObject> callback) {
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                if (sessionId != null) body.put("session_id", sessionId);
                body.put("message", message);
                String resp = httpPost(baseUrl + "/api/chat/start", body.toString());
                JSONObject json = new JSONObject(resp);
                mainHandler.post(() -> callback.onSuccess(json));
            } catch (Exception e) {
                Log.e(TAG, "sendChatMessage 失败", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void cancelChat(String streamId, ApiCallback<JSONObject> callback) {
        executor.execute(() -> {
            try {
                String resp = httpPost(baseUrl + "/api/chat/cancel", "{\"stream_id\":\"" + streamId + "\"}");
                mainHandler.post(() -> callback.onSuccess(new JSONObject(resp)));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // SSE 流式响应
    public interface SseCallback {
        void onData(String data);
        void onComplete();
        void onError(String error);
    }

    public void streamChat(String streamId, SseCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(baseUrl + "/api/chat/stream?stream_id=" + streamId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(120000);
                conn.setRequestProperty("Accept", "text/event-stream");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                StringBuilder buffer = new StringBuilder();

                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) {
                            mainHandler.post(callback::onComplete);
                            break;
                        }
                        mainHandler.post(() -> callback.onData(data));
                    }
                }
                reader.close();
                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "streamChat 失败", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ===== Cron 任务 =====
    public void getCrons(ApiCallback<JSONArray> callback) {
        executor.execute(() -> {
            try {
                String resp = httpGet(baseUrl + "/api/crons");
                JSONObject json = new JSONObject(resp);
                JSONArray crons = json.optJSONArray("crons", new JSONArray());
                mainHandler.post(() -> callback.onSuccess(crons));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void createCron(JSONObject cronData, ApiCallback<JSONObject> callback) {
        executor.execute(() -> {
            try {
                String resp = httpPost(baseUrl + "/api/crons/create", cronData.toString());
                mainHandler.post(() -> callback.onSuccess(new JSONObject(resp)));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void deleteCron(String jobId, ApiCallback<JSONObject> callback) {
        executor.execute(() -> {
            try {
                String resp = httpPost(baseUrl + "/api/crons/delete", "{\"job_id\":\"" + jobId + "\"}");
                mainHandler.post(() -> callback.onSuccess(new JSONObject(resp)));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ===== Kanban =====
    public void getKanban(ApiCallback<JSONObject> callback) {
        executor.execute(() -> {
            try {
                String resp = httpGet(baseUrl + "/api/kanban");
                mainHandler.post(() -> callback.onSuccess(new JSONObject(resp)));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void createKanbanTask(String title, ApiCallback<JSONObject> callback) {
        executor.execute(() -> {
            try {
                String resp = httpPost(baseUrl + "/api/kanban/task", "{\"title\":\"" + escapeJson(title) + "\"}");
                mainHandler.post(() -> callback.onSuccess(new JSONObject(resp)));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void updateKanbanTask(String taskId, JSONObject data, ApiCallback<JSONObject> callback) {
        executor.execute(() -> {
            try {
                data.put("task_id", taskId);
                String resp = httpPost(baseUrl + "/api/kanban/task/update", data.toString());
                mainHandler.post(() -> callback.onSuccess(new JSONObject(resp)));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ===== Skills =====
    public void getSkills(ApiCallback<JSONArray> callback) {
        executor.execute(() -> {
            try {
                String resp = httpGet(baseUrl + "/api/skills");
                JSONObject json = new JSONObject(resp);
                mainHandler.post(() -> callback.onSuccess(json.optJSONArray("skills", new JSONArray())));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void getSkillDetail(String name, ApiCallback<JSONObject> callback) {
        executor.execute(() -> {
            try {
                String resp = httpGet(baseUrl + "/api/skills/detail?name=" + name);
                mainHandler.post(() -> callback.onSuccess(new JSONObject(resp)));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ===== Memory =====
    public void getMemory(ApiCallback<JSONArray> callback) {
        executor.execute(() -> {
            try {
                String resp = httpGet(baseUrl + "/api/memory");
                JSONObject json = new JSONObject(resp);
                mainHandler.post(() -> callback.onSuccess(json.optJSONArray("memories", new JSONArray())));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ===== Todos =====
    public void getTodos(ApiCallback<JSONArray> callback) {
        executor.execute(() -> {
            try {
                String resp = httpGet(baseUrl + "/api/todos");
                JSONObject json = new JSONObject(resp);
                mainHandler.post(() -> callback.onSuccess(json.optJSONArray("todos", new JSONArray())));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ===== Workspaces =====
    public void getWorkspaces(ApiCallback<JSONArray> callback) {
        executor.execute(() -> {
            try {
                String resp = httpGet(baseUrl + "/api/workspaces");
                JSONObject json = new JSONObject(resp);
                mainHandler.post(() -> callback.onSuccess(json.optJSONArray("workspaces", new JSONArray())));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ===== Profiles =====
    public void getProfiles(ApiCallback<JSONArray> callback) {
        executor.execute(() -> {
            try {
                String resp = httpGet(baseUrl + "/api/profiles");
                JSONObject json = new JSONObject(resp);
                mainHandler.post(() -> callback.onSuccess(json.optJSONArray("profiles", new JSONArray())));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ===== Insights =====
    public void getInsights(String period, ApiCallback<JSONObject> callback) {
        executor.execute(() -> {
            try {
                String resp = httpGet(baseUrl + "/api/insights?period=" + (period != null ? period : "7d"));
                mainHandler.post(() -> callback.onSuccess(new JSONObject(resp)));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ===== Logs =====
    public void getLogs(ApiCallback<JSONObject> callback) {
        executor.execute(() -> {
            try {
                String resp = httpGet(baseUrl + "/api/logs");
                mainHandler.post(() -> callback.onSuccess(new JSONObject(resp)));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ===== Settings =====
    public void getSettings(ApiCallback<JSONObject> callback) {
        executor.execute(() -> {
            try {
                String resp = httpGet(baseUrl + "/api/settings");
                mainHandler.post(() -> callback.onSuccess(new JSONObject(resp)));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void updateSettings(JSONObject settings, ApiCallback<JSONObject> callback) {
        executor.execute(() -> {
            try {
                String resp = httpPost(baseUrl + "/api/settings", settings.toString());
                mainHandler.post(() -> callback.onSuccess(new JSONObject(resp)));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ===== Models =====
    public void getModels(ApiCallback<JSONObject> callback) {
        executor.execute(() -> {
            try {
                String resp = httpGet(baseUrl + "/api/models");
                mainHandler.post(() -> callback.onSuccess(new JSONObject(resp)));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ===== 健康检查 =====
    public void healthCheck(ApiCallback<JSONObject> callback) {
        executor.execute(() -> {
            try {
                String resp = httpGet(baseUrl + "/health");
                mainHandler.post(() -> callback.onSuccess(new JSONObject(resp)));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ===== HTTP 工具 =====
    private String httpGet(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("Accept", "application/json");

        int code = conn.getResponseCode();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
            code >= 400 ? conn.getErrorStream() : conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();

        if (code >= 400) throw new Exception("HTTP " + code + ": " + sb.toString());
        return sb.toString();
    }

    private String httpPost(String urlStr, String body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);

        OutputStream os = conn.getOutputStream();
        os.write(body.getBytes(StandardCharsets.UTF_8));
        os.flush();
        os.close();

        int code = conn.getResponseCode();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
            code >= 400 ? conn.getErrorStream() : conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();

        if (code >= 400) throw new Exception("HTTP " + code + ": " + sb.toString());
        return sb.toString();
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    public void shutdown() {
        executor.shutdown();
    }
}
