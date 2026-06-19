package com.hermes.webui.api;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
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

    public interface ApiCallback {
        void onSuccess(JSONObject result);
        void onError(String error);
    }

    public void setBaseUrl(String url) { this.baseUrl = url; }
    public String getBaseUrl() { return baseUrl; }

    private JSONObject wrapArray(JSONArray arr, String key) {
        JSONObject obj = new JSONObject();
        try { obj.put(key, arr); } catch (JSONException ignored) {}
        return obj;
    }

    private JSONObject safeObj(String r) {
        try { return new JSONObject(r); } catch (JSONException e) { return new JSONObject(); }
    }

    private JSONArray safeArr(JSONObject j, String k) {
        JSONArray a = j.optJSONArray(k);
        return a != null ? a : new JSONArray();
    }

    public void getSessions(ApiCallback cb) {
        executor.execute(() -> {
            try {
                String r = httpGet(baseUrl + "/api/sessions");
                JSONObject j = new JSONObject(r);
                mainHandler.post(() -> cb.onSuccess(j));
            } catch (Exception e) { mainHandler.post(() -> cb.onError(e.getMessage())); }
        });
    }

    public void getSession(String sid, ApiCallback cb) {
        executor.execute(() -> {
            try {
                String r = httpGet(baseUrl + "/api/session?session_id=" + sid);
                JSONObject j = new JSONObject(r);
                mainHandler.post(() -> cb.onSuccess(j));
            } catch (Exception e) { mainHandler.post(() -> cb.onError(e.getMessage())); }
        });
    }

    public void deleteSession(String sid, ApiCallback cb) {
        executor.execute(() -> {
            try {
                String r = httpPost(baseUrl + "/api/session/delete", "{\"session_id\":\"" + sid + "\"}");
                mainHandler.post(() -> cb.onSuccess(safeObj(r)));
            } catch (Exception e) { mainHandler.post(() -> cb.onError(e.getMessage())); }
        });
    }

    public void createSession(ApiCallback cb) {
        executor.execute(() -> {
            try {
                String r = httpPost(baseUrl + "/api/session/new", "{}");
                mainHandler.post(() -> cb.onSuccess(safeObj(r)));
            } catch (Exception e) { mainHandler.post(() -> cb.onError(e.getMessage())); }
        });
    }

    public void sendChatMessage(String sid, String msg, ApiCallback cb) {
        executor.execute(() -> {
            try {
                String body = "{\"message\":\"" + esc(msg) + "\"" + (sid != null ? ",\"session_id\":\"" + sid + "\"" : "") + "}";
                String r = httpPost(baseUrl + "/api/chat/start", body);
                mainHandler.post(() -> cb.onSuccess(safeObj(r)));
            } catch (Exception e) { mainHandler.post(() -> cb.onError(e.getMessage())); }
        });
    }

    public void cancelChat(String streamId, ApiCallback cb) {
        executor.execute(() -> {
            try {
                String r = httpPost(baseUrl + "/api/chat/cancel", "{\"stream_id\":\"" + streamId + "\"}");
                mainHandler.post(() -> cb.onSuccess(safeObj(r)));
            } catch (Exception e) { mainHandler.post(() -> cb.onError(e.getMessage())); }
        });
    }

    public interface SseCallback {
        void onData(String data);
        void onComplete();
        void onError(String error);
    }

    public void streamChat(String streamId, SseCallback cb) {
        executor.execute(() -> {
            boolean completed = false;
            try {
                URL url = new URL(baseUrl + "/api/chat/stream?stream_id=" + streamId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(180000);
                conn.setRequestProperty("Accept", "text/event-stream");
                conn.setRequestProperty("Cache-Control", "no-cache");
                int responseCode = conn.getResponseCode();
                Log.d(TAG, "SSE connect: code=" + responseCode + " stream=" + streamId);
                if (responseCode >= 400) {
                    BufferedReader errReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    StringBuilder errSb = new StringBuilder();
                    String errLine;
                    while ((errLine = errReader.readLine()) != null) errSb.append(errLine);
                    errReader.close();
                    mainHandler.post(() -> cb.onError("HTTP " + responseCode + ": " + errSb.toString()));
                    conn.disconnect();
                    return;
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                String currentEvent = "";
                int tokenCount = 0;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("event: ")) {
                        currentEvent = line.substring(7).trim();
                    } else if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        // 完成信号：[DONE]、stream_end、done
                        if ("[DONE]".equals(data) || "stream_end".equals(currentEvent) || "done".equals(currentEvent)) {
                            Log.d(TAG, "SSE completed via: " + currentEvent);
                            completed = true;
                            mainHandler.post(cb::onComplete);
                            break;
                        }
                        // 处理 token 事件（实际回复内容）
                        if ("token".equals(currentEvent)) {
                            tokenCount++;
                            mainHandler.post(() -> cb.onData(data));
                        }
                    } else if (line.isEmpty()) {
                        currentEvent = "";
                    }
                }
                Log.d(TAG, "SSE stream ended, tokens=" + tokenCount + " completed=" + completed);
                reader.close();
                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "SSE error: " + e.getMessage());
                mainHandler.post(() -> cb.onError(e.getMessage()));
            } finally {
                // 确保 onComplete 一定会被调用
                if (!completed) {
                    Log.w(TAG, "SSE stream ended without completion signal, calling onComplete anyway");
                    mainHandler.post(cb::onComplete);
                }
            }
        });
    }

    public void getCrons(ApiCallback cb) {
        executor.execute(() -> {
            try {
                String r = httpGet(baseUrl + "/api/crons");
                JSONObject j = new JSONObject(r);
                mainHandler.post(() -> cb.onSuccess(j));
            } catch (Exception e) { mainHandler.post(() -> cb.onError(e.getMessage())); }
        });
    }

    public void createCron(JSONObject data, ApiCallback cb) {
        executor.execute(() -> {
            try {
                String r = httpPost(baseUrl + "/api/crons/create", data.toString());
                mainHandler.post(() -> cb.onSuccess(safeObj(r)));
            } catch (Exception e) { mainHandler.post(() -> cb.onError(e.getMessage())); }
        });
    }

    public void deleteCron(String jobId, ApiCallback cb) {
        executor.execute(() -> {
            try {
                String r = httpPost(baseUrl + "/api/crons/delete", "{\"job_id\":\"" + jobId + "\"}");
                mainHandler.post(() -> cb.onSuccess(safeObj(r)));
            } catch (Exception e) { mainHandler.post(() -> cb.onError(e.getMessage())); }
        });
    }

    public void getKanban(ApiCallback cb) {
        executor.execute(() -> {
            try {
                String r = httpGet(baseUrl + "/api/kanban/boards");
                JSONObject j = new JSONObject(r);
                mainHandler.post(() -> cb.onSuccess(j));
            } catch (Exception e) { mainHandler.post(() -> cb.onError(e.getMessage())); }
        });
    }

    public void createKanbanTask(String title, ApiCallback cb) {
        executor.execute(() -> {
            try {
                String r = httpPost(baseUrl + "/api/kanban/task", "{\"title\":\"" + esc(title) + "\"}");
                mainHandler.post(() -> cb.onSuccess(safeObj(r)));
            } catch (Exception e) { mainHandler.post(() -> cb.onError(e.getMessage())); }
        });
    }

    public void updateKanbanTask(String taskId, JSONObject data, ApiCallback cb) {
        executor.execute(() -> {
            try {
                data.put("task_id", taskId);
                String r = httpPost(baseUrl + "/api/kanban/task/update", data.toString());
                mainHandler.post(() -> cb.onSuccess(safeObj(r)));
            } catch (Exception e) { mainHandler.post(() -> cb.onError(e.getMessage())); }
        });
    }

    public void getSkills(ApiCallback cb) {
        executor.execute(() -> {
            try {
                String r = httpGet(baseUrl + "/api/skills");
                JSONObject j = new JSONObject(r);
                mainHandler.post(() -> cb.onSuccess(j));
            } catch (Exception e) { mainHandler.post(() -> cb.onError(e.getMessage())); }
        });
    }

    public void getSkillDetail(String name, ApiCallback cb) {
        executor.execute(() -> {
            try {
                String r = httpGet(baseUrl + "/api/skills/detail?name=" + name);
                mainHandler.post(() -> cb.onSuccess(safeObj(r)));
            } catch (Exception e) { mainHandler.post(() -> cb.onError(e.getMessage())); }
        });
    }

    public void getMemory(ApiCallback cb) {
        executor.execute(() -> {
            try {
                String r = httpGet(baseUrl + "/api/memory");
                JSONObject j = new JSONObject(r);
                mainHandler.post(() -> cb.onSuccess(j));
            } catch (Exception e) { mainHandler.post(() -> cb.onError(e.getMessage())); }
        });
    }

    public void getTodos(ApiCallback cb) {
        executor.execute(() -> {
            try {
                String r = httpGet(baseUrl + "/api/todos");
                JSONObject j = new JSONObject(r);
                mainHandler.post(() -> cb.onSuccess(j));
            } catch (Exception e) { mainHandler.post(() -> cb.onError(e.getMessage())); }
        });
    }

    public void getWorkspaces(ApiCallback cb) {
        executor.execute(() -> {
            try {
                String r = httpGet(baseUrl + "/api/workspaces");
                JSONObject j = new JSONObject(r);
                mainHandler.post(() -> cb.onSuccess(j));
            } catch (Exception e) { mainHandler.post(() -> cb.onError(e.getMessage())); }
        });
    }

    public void getProfiles(ApiCallback cb) {
        executor.execute(() -> {
            try {
                String r = httpGet(baseUrl + "/api/profiles");
                JSONObject j = new JSONObject(r);
                mainHandler.post(() -> cb.onSuccess(j));
            } catch (Exception e) { mainHandler.post(() -> cb.onError(e.getMessage())); }
        });
    }

    public void getInsights(String period, ApiCallback cb) {
        executor.execute(() -> {
            try {
                String r = httpGet(baseUrl + "/api/insights?period=" + (period != null ? period : "7d"));
                mainHandler.post(() -> cb.onSuccess(safeObj(r)));
            } catch (Exception e) { mainHandler.post(() -> cb.onError(e.getMessage())); }
        });
    }

    public void getLogs(ApiCallback cb) {
        executor.execute(() -> {
            try {
                String r = httpGet(baseUrl + "/api/logs");
                mainHandler.post(() -> cb.onSuccess(safeObj(r)));
            } catch (Exception e) { mainHandler.post(() -> cb.onError(e.getMessage())); }
        });
    }

    public void getSettings(ApiCallback cb) {
        executor.execute(() -> {
            try {
                String r = httpGet(baseUrl + "/api/settings");
                mainHandler.post(() -> cb.onSuccess(safeObj(r)));
            } catch (Exception e) { mainHandler.post(() -> cb.onError(e.getMessage())); }
        });
    }

    public void updateSettings(JSONObject s, ApiCallback cb) {
        executor.execute(() -> {
            try {
                String r = httpPost(baseUrl + "/api/settings", s.toString());
                mainHandler.post(() -> cb.onSuccess(safeObj(r)));
            } catch (Exception e) { mainHandler.post(() -> cb.onError(e.getMessage())); }
        });
    }

    public void getModels(ApiCallback cb) {
        executor.execute(() -> {
            try {
                String r = httpGet(baseUrl + "/api/models");
                mainHandler.post(() -> cb.onSuccess(safeObj(r)));
            } catch (Exception e) { mainHandler.post(() -> cb.onError(e.getMessage())); }
        });
    }

    public void healthCheck(ApiCallback cb) {
        executor.execute(() -> {
            try {
                String r = httpGet(baseUrl + "/health");
                mainHandler.post(() -> cb.onSuccess(safeObj(r)));
            } catch (Exception e) { mainHandler.post(() -> cb.onError(e.getMessage())); }
        });
    }

    private String httpGet(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("Accept", "application/json");
        int code = conn.getResponseCode();
        BufferedReader reader = new BufferedReader(new InputStreamReader(code >= 400 ? conn.getErrorStream() : conn.getInputStream()));
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
        BufferedReader reader = new BufferedReader(new InputStreamReader(code >= 400 ? conn.getErrorStream() : conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        if (code >= 400) throw new Exception("HTTP " + code + ": " + sb.toString());
        return sb.toString();
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    public void shutdown() { executor.shutdown(); }
}
