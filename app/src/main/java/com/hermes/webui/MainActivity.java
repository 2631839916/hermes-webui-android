package com.hermes.webui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "HermesWebUI";
    private static final String BASE_URL = "http://localhost:8787";

    private DrawerLayout drawerLayout;
    private RecyclerView messageList, sessionList;
    private EditText inputMessage;
    private ImageButton btnSend, btnHamburger, btnNewChat, btnReload, fabNewSession;

    private ImageButton[] navButtons;
    private String[] navNames = {"chat", "tasks", "kanban", "skills", "memory", "workspaces", "profiles", "todos", "insights", "logs", "settings"};
    private View panelChat, panelTasks, panelKanban, panelSkills, panelMemory, panelSettings;

    private MsgAdapter msgAdapter;
    private SessionAdapter sessionAdapter;
    private List<MsgItem> messages = new ArrayList<>();
    private List<SessionItem> sessions = new ArrayList<>();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String currentSessionId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupMessageList();
        setupSessionList();
        setupListeners();
        highlightNav(0);

        // 加载会话列表
        loadSessions();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        messageList = findViewById(R.id.messageList);
        sessionList = findViewById(R.id.sessionList);
        inputMessage = findViewById(R.id.inputMessage);
        btnSend = findViewById(R.id.btnSend);
        btnHamburger = findViewById(R.id.btnHamburger);
        btnNewChat = findViewById(R.id.btnNewChat);
        btnReload = findViewById(R.id.btnReload);
        fabNewSession = findViewById(R.id.fabNewSession);

        panelChat = findViewById(R.id.panelChat);
        panelTasks = findViewById(R.id.panelTasks);
        panelKanban = findViewById(R.id.panelKanban);
        panelSkills = findViewById(R.id.panelSkills);
        panelMemory = findViewById(R.id.panelMemory);
        panelSettings = findViewById(R.id.panelSettings);

        navButtons = new ImageButton[] {
            findViewById(R.id.navChat), findViewById(R.id.navTasks), findViewById(R.id.navKanban),
            findViewById(R.id.navSkills), findViewById(R.id.navMemory), findViewById(R.id.navSpaces),
            findViewById(R.id.navProfiles), findViewById(R.id.navTodos), findViewById(R.id.navInsights),
            findViewById(R.id.navLogs), findViewById(R.id.navSettings)
        };
    }

    private void setupMessageList() {
        msgAdapter = new MsgAdapter();
        messageList.setLayoutManager(new LinearLayoutManager(this));
        messageList.setAdapter(msgAdapter);
    }

    private void setupSessionList() {
        sessionAdapter = new SessionAdapter();
        sessionList.setLayoutManager(new LinearLayoutManager(this));
        sessionList.setAdapter(sessionAdapter);
    }

    private void setupListeners() {
        btnSend.setOnClickListener(v -> sendMessage());
        btnHamburger.setOnClickListener(v -> drawerLayout.openDrawer(Gravity.LEFT));
        btnNewChat.setOnClickListener(v -> newChat());
        btnReload.setOnClickListener(v -> loadSessions());
        fabNewSession.setOnClickListener(v -> newChat());

        View closeBtn = findViewById(R.id.btnCloseSidebar);
        if (closeBtn != null) closeBtn.setOnClickListener(v -> drawerLayout.closeDrawer(Gravity.LEFT));

        for (int i = 0; i < navButtons.length; i++) {
            final int idx = i;
            if (navButtons[i] != null) {
                navButtons[i].setOnClickListener(v -> {
                    switchPanel(navNames[idx]);
                    highlightNav(idx);
                });
            }
        }
    }

    private void switchPanel(String name) {
        if (panelChat != null) panelChat.setVisibility(View.GONE);
        if (panelTasks != null) panelTasks.setVisibility(View.GONE);
        if (panelKanban != null) panelKanban.setVisibility(View.GONE);
        if (panelSkills != null) panelSkills.setVisibility(View.GONE);
        if (panelMemory != null) panelMemory.setVisibility(View.GONE);
        if (panelSettings != null) panelSettings.setVisibility(View.GONE);

        switch (name) {
            case "chat": if (panelChat != null) panelChat.setVisibility(View.VISIBLE); break;
            case "tasks": if (panelTasks != null) panelTasks.setVisibility(View.VISIBLE); break;
            case "kanban": if (panelKanban != null) panelKanban.setVisibility(View.VISIBLE); break;
            case "skills": if (panelSkills != null) panelSkills.setVisibility(View.VISIBLE); break;
            case "memory": if (panelMemory != null) panelMemory.setVisibility(View.VISIBLE); break;
            case "settings": if (panelSettings != null) panelSettings.setVisibility(View.VISIBLE); break;
            default:
                Toast.makeText(this, name + " - 开发中", Toast.LENGTH_SHORT).show();
                if (panelChat != null) panelChat.setVisibility(View.VISIBLE);
                highlightNav(0);
                break;
        }
    }

    private void highlightNav(int selected) {
        for (int i = 0; i < navButtons.length; i++) {
            if (navButtons[i] != null) navButtons[i].setSelected(i == selected);
        }
    }

    // ===== 会话列表 =====
    private void loadSessions() {
        executor.execute(() -> {
            try {
                String resp = httpGet(BASE_URL + "/api/sessions");
                JSONObject json = new JSONObject(resp);
                JSONArray arr = json.getJSONArray("sessions");

                List<SessionItem> list = new ArrayList<>();
                for (int i = 0; i < arr.length() && i < 50; i++) {
                    JSONObject s = arr.getJSONObject(i);
                    String id = s.optString("session_id", s.optString("id", ""));
                    String title = s.optString("title", "Untitled");
                    String preview = s.optString("preview", s.optString("last_message", ""));
                    list.add(new SessionItem(id, title, preview));
                }

                mainHandler.post(() -> {
                    sessions.clear();
                    sessions.addAll(list);
                    sessionAdapter.notifyDataSetChanged();
                    if (!sessions.isEmpty() && currentSessionId == null) {
                        loadSessionMessages(sessions.get(0));
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "加载会话失败", e);
                mainHandler.post(() -> {
                    Toast.makeText(this, "连接失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // 添加演示数据
                    sessions.clear();
                    sessions.add(new SessionItem("demo", "示例会话", "点击发送消息开始对话"));
                    sessionAdapter.notifyDataSetChanged();
                });
            }
        });
    }

    private void loadSessionMessages(SessionItem session) {
        currentSessionId = session.id;
        drawerLayout.closeDrawer(Gravity.LEFT);

        executor.execute(() -> {
            try {
                String resp = httpGet(BASE_URL + "/api/session?session_id=" + session.id);
                JSONObject json = new JSONObject(resp);
                JSONArray msgs = json.optJSONArray("messages");

                List<MsgItem> list = new ArrayList<>();
                if (msgs != null) {
                    for (int i = 0; i < msgs.length(); i++) {
                        JSONObject m = msgs.getJSONObject(i);
                        String role = m.optString("role", "user");
                        String content = m.optString("content", "");
                        if (!content.isEmpty()) {
                            boolean isUser = "user".equals(role);
                            list.add(new MsgItem(isUser ? "User" : "Hermes", content, isUser));
                        }
                    }
                }

                mainHandler.post(() -> {
                    messages.clear();
                    messages.addAll(list);
                    msgAdapter.notifyDataSetChanged();
                    if (!messages.isEmpty()) {
                        messageList.scrollToPosition(messages.size() - 1);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "加载消息失败", e);
            }
        });
    }

    // ===== 发送消息 =====
    private void sendMessage() {
        String text = inputMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        messages.add(new MsgItem("User", text, true));
        msgAdapter.notifyItemInserted(messages.size() - 1);
        messageList.scrollToPosition(messages.size() - 1);
        inputMessage.setText("");

        // 添加临时 AI 消息
        MsgItem aiMsg = new MsgItem("Hermes", "思考中...", false);
        messages.add(aiMsg);
        int aiPos = messages.size() - 1;
        msgAdapter.notifyItemInserted(aiPos);
        messageList.scrollToPosition(aiPos);

        final String userText = text;
        executor.execute(() -> {
            try {
                // 先获取或创建 session
                if (currentSessionId == null) {
                    try {
                        String createResp = httpPost(BASE_URL + "/api/chat/start", "{\"message\":\"" + escapeJson(userText) + "\"}");
                        JSONObject json = new JSONObject(createResp);
                        currentSessionId = json.optString("session_id", null);
                    } catch (Exception e) {
                        Log.e(TAG, "创建session失败", e);
                    }
                }

                // 发送消息
                String body;
                if (currentSessionId != null) {
                    body = "{\"session_id\":\"" + escapeJson(currentSessionId) + "\",\"message\":\"" + escapeJson(userText) + "\"}";
                } else {
                    body = "{\"message\":\"" + escapeJson(userText) + "\"}";
                }

                String resp = httpPost(BASE_URL + "/api/chat/start", body);
                JSONObject json = new JSONObject(resp);

                if (currentSessionId == null) {
                    currentSessionId = json.optString("session_id", null);
                }

                // 获取回复
                String reply = json.optString("response", json.optString("message", ""));
                if (reply.isEmpty()) {
                    // 尝试从 stream 获取
                    String streamId = json.optString("stream_id", "");
                    if (!streamId.isEmpty()) {
                        reply = waitForStreamResponse(streamId);
                    }
                }

                if (reply.isEmpty()) {
                    reply = "已收到消息，正在处理...";
                }

                final String finalReply = reply;
                mainHandler.post(() -> {
                    aiMsg.content = finalReply;
                    msgAdapter.notifyItemChanged(aiPos);
                    messageList.scrollToPosition(messages.size() - 1);
                });

            } catch (Exception e) {
                Log.e(TAG, "发送失败", e);
                mainHandler.post(() -> {
                    aiMsg.content = "发送失败: " + e.getMessage();
                    msgAdapter.notifyItemChanged(aiPos);
                });
            }
        });
    }

    private String waitForStreamResponse(String streamId) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(BASE_URL + "/api/chat/stream?stream_id=" + streamId).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6).trim();
                    if (!data.isEmpty() && !"[DONE]".equals(data)) {
                        try {
                            JSONObject obj = new JSONObject(data);
                            String content = obj.optString("content", obj.optString("text", ""));
                            if (!content.isEmpty()) {
                                result.append(content);
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
            reader.close();
            return result.toString();
        } catch (Exception e) {
            Log.e(TAG, "Stream 失败", e);
            return "";
        }
    }

    private void newChat() {
        currentSessionId = null;
        messages.clear();
        msgAdapter.notifyDataSetChanged();
        Toast.makeText(this, "新会话", Toast.LENGTH_SHORT).show();
    }

    // ===== HTTP 工具 =====
    private String httpGet(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
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
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    // ===== 数据类 =====
    static class MsgItem {
        String sender, content;
        boolean isUser;
        MsgItem(String s, String c, boolean u) { sender = s; content = c; isUser = u; }
    }

    static class SessionItem {
        String id, title, preview;
        SessionItem(String i, String t, String p) { id = i; title = t; preview = p; }
    }

    // ===== 消息适配器 =====
    class MsgAdapter extends RecyclerView.Adapter<MsgAdapter.VH> {
        @Override public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false));
        }
        @Override public void onBindViewHolder(VH h, int pos) {
            MsgItem msg = messages.get(pos);
            h.senderText.setText(msg.sender);
            h.contentText.setText(msg.content);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) h.container.getLayoutParams();
            if (msg.isUser) {
                h.container.setBackgroundResource(R.drawable.message_user_background);
                params.gravity = Gravity.END;
            } else {
                h.container.setBackgroundResource(R.drawable.message_ai_background);
                params.gravity = Gravity.START;
            }
            h.container.setLayoutParams(params);
        }
        @Override public int getItemCount() { return messages.size(); }
        class VH extends RecyclerView.ViewHolder {
            TextView senderText, contentText; View container;
            VH(View v) { super(v); senderText = v.findViewById(R.id.senderText); contentText = v.findViewById(R.id.contentText); container = v.findViewById(R.id.messageContainer); }
        }
    }

    // ===== 会话适配器 =====
    class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.VH> {
        @Override public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_session, parent, false));
        }
        @Override public void onBindViewHolder(VH h, int pos) {
            SessionItem s = sessions.get(pos);
            h.titleText.setText(s.title);
            h.previewText.setText(s.preview);
            h.itemView.setOnClickListener(v -> loadSessionMessages(s));
        }
        @Override public int getItemCount() { return sessions.size(); }
        class VH extends RecyclerView.ViewHolder {
            TextView titleText, previewText;
            VH(View v) { super(v); titleText = v.findViewById(R.id.sessionTitle); previewText = v.findViewById(R.id.sessionPreview); }
        }
    }
}
