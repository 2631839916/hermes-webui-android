package com.hermes.webui;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "HermesWebUI";

    private DrawerLayout drawerLayout;
    private RecyclerView messageList;
    private RecyclerView sessionList;
    private EditText inputMessage;
    private ImageButton btnSend;
    private ImageButton btnHamburger;
    private ImageButton btnNewChat;
    private ImageButton btnReload;
    private ImageButton fabNewSession;

    // 导航按钮
    private ImageButton navChat, navTasks, navKanban, navSkills, navMemory;
    private ImageButton navSpaces, navProfiles, navTodos, navInsights, navLogs, navSettings;

    // 面板
    private View panelChat, panelTasks, panelKanban, panelSkills, panelMemory, panelSettings;

    private MessageAdapter messageAdapter;
    private SessionAdapter sessionAdapter;
    private List<Message> messages = new ArrayList<>();
    private List<Session> sessions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate 开始");

        try {
            setContentView(R.layout.activity_main);
            Log.d(TAG, "setContentView 完成");

            initViews();
            Log.d(TAG, "initViews 完成");

            setupListeners();
            Log.d(TAG, "setupListeners 完成");

            setupRecyclerViews();
            Log.d(TAG, "setupRecyclerViews 完成");

            loadSampleData();
            Log.d(TAG, "loadSampleData 完成");

        } catch (Exception e) {
            Log.e(TAG, "启动错误: " + e.getMessage(), e);
            Toast.makeText(this, "启动错误: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
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

        // 导航按钮
        navChat = findViewById(R.id.navChat);
        navTasks = findViewById(R.id.navTasks);
        navKanban = findViewById(R.id.navKanban);
        navSkills = findViewById(R.id.navSkills);
        navMemory = findViewById(R.id.navMemory);
        navSpaces = findViewById(R.id.navSpaces);
        navProfiles = findViewById(R.id.navProfiles);
        navTodos = findViewById(R.id.navTodos);
        navInsights = findViewById(R.id.navInsights);
        navLogs = findViewById(R.id.navLogs);
        navSettings = findViewById(R.id.navSettings);

        // 面板
        panelChat = findViewById(R.id.panelChat);
        panelTasks = findViewById(R.id.panelTasks);
        panelKanban = findViewById(R.id.panelKanban);
        panelSkills = findViewById(R.id.panelSkills);
        panelMemory = findViewById(R.id.panelMemory);
        panelSettings = findViewById(R.id.panelSettings);
    }

    private void setupListeners() {
        // 汉堡菜单
        if (btnHamburger != null) {
            btnHamburger.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(Gravity.LEFT);
                }
            });
        }

        // 新建聊天
        if (btnNewChat != null) {
            btnNewChat.setOnClickListener(v -> createNewChat());
        }

        // 刷新
        if (btnReload != null) {
            btnReload.setOnClickListener(v -> refreshCurrentPanel());
        }

        // 发送消息
        if (btnSend != null) {
            btnSend.setOnClickListener(v -> sendMessage());
        }

        // 新建会话
        if (fabNewSession != null) {
            fabNewSession.setOnClickListener(v -> createNewSession());
        }

        // 导航按钮点击
        setupNavButton(navChat, "chat");
        setupNavButton(navTasks, "tasks");
        setupNavButton(navKanban, "kanban");
        setupNavButton(navSkills, "skills");
        setupNavButton(navMemory, "memory");
        setupNavButton(navSpaces, "workspaces");
        setupNavButton(navProfiles, "profiles");
        setupNavButton(navTodos, "todos");
        setupNavButton(navInsights, "insights");
        setupNavButton(navLogs, "logs");
        setupNavButton(navSettings, "settings");

        // 侧边栏关闭按钮
        View closeBtn = findViewById(R.id.btnCloseSidebar);
        if (closeBtn != null) {
            closeBtn.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.closeDrawer(Gravity.LEFT);
                }
            });
        }
    }

    private void setupNavButton(ImageButton button, String panel) {
        if (button != null) {
            button.setOnClickListener(v -> {
                switchPanel(panel);
                resetNavSelection();
                button.setSelected(true);
            });
        }
    }

    private void resetNavSelection() {
        if (navChat != null) navChat.setSelected(false);
        if (navTasks != null) navTasks.setSelected(false);
        if (navKanban != null) navKanban.setSelected(false);
        if (navSkills != null) navSkills.setSelected(false);
        if (navMemory != null) navMemory.setSelected(false);
        if (navSpaces != null) navSpaces.setSelected(false);
        if (navProfiles != null) navProfiles.setSelected(false);
        if (navTodos != null) navTodos.setSelected(false);
        if (navInsights != null) navInsights.setSelected(false);
        if (navLogs != null) navLogs.setSelected(false);
        if (navSettings != null) navSettings.setSelected(false);
    }

    private void switchPanel(String panel) {
        // 隐藏所有面板
        if (panelChat != null) panelChat.setVisibility(View.GONE);
        if (panelTasks != null) panelTasks.setVisibility(View.GONE);
        if (panelKanban != null) panelKanban.setVisibility(View.GONE);
        if (panelSkills != null) panelSkills.setVisibility(View.GONE);
        if (panelMemory != null) panelMemory.setVisibility(View.GONE);
        if (panelSettings != null) panelSettings.setVisibility(View.GONE);

        // 显示选中的面板
        switch (panel) {
            case "chat":
                if (panelChat != null) panelChat.setVisibility(View.VISIBLE);
                break;
            case "tasks":
                if (panelTasks != null) panelTasks.setVisibility(View.VISIBLE);
                break;
            case "kanban":
                if (panelKanban != null) panelKanban.setVisibility(View.VISIBLE);
                break;
            case "skills":
                if (panelSkills != null) panelSkills.setVisibility(View.VISIBLE);
                break;
            case "memory":
                if (panelMemory != null) panelMemory.setVisibility(View.VISIBLE);
                break;
            case "settings":
                if (panelSettings != null) panelSettings.setVisibility(View.VISIBLE);
                break;
            default:
                if (panelChat != null) panelChat.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void setupRecyclerViews() {
        // 消息列表
        messageAdapter = new MessageAdapter(messages);
        if (messageList != null) {
            messageList.setLayoutManager(new LinearLayoutManager(this));
            messageList.setAdapter(messageAdapter);
        }

        // 会话列表
        sessionAdapter = new SessionAdapter(sessions, session -> {
            loadSession(session);
            if (drawerLayout != null) {
                drawerLayout.closeDrawer(Gravity.LEFT);
            }
        });
        if (sessionList != null) {
            sessionList.setLayoutManager(new LinearLayoutManager(this));
            sessionList.setAdapter(sessionAdapter);
        }
    }

    private void loadSampleData() {
        // 添加示例消息
        messages.add(new Message("Hermes", "你好！我是 Hermes，你的 AI 助手。有什么可以帮助你的吗？", false));
        messages.add(new Message("User", "帮我写一个 Python 脚本", true));
        messages.add(new Message("Hermes", "好的！这是一个简单的 Python 脚本示例：\n\n```python\nprint('Hello, World!')\n```", false));

        messageAdapter.notifyDataSetChanged();

        // 添加示例会话
        sessions.add(new Session("会话 1", "这是第一个会话"));
        sessions.add(new Session("会话 2", "这是第二个会话"));
        sessions.add(new Session("会话 3", "这是第三个会话"));

        sessionAdapter.notifyDataSetChanged();
    }

    private void sendMessage() {
        if (inputMessage == null) return;

        String text = inputMessage.getText().toString().trim();
        if (!text.isEmpty()) {
            messages.add(new Message("User", text, true));
            messageAdapter.notifyItemInserted(messages.size() - 1);
            if (messageList != null) {
                messageList.scrollToPosition(messages.size() - 1);
            }
            inputMessage.setText("");
            simulateAiResponse(text);
        }
    }

    private void simulateAiResponse(String userMessage) {
        if (messageList == null) return;

        messageList.postDelayed(() -> {
            String response = generateResponse(userMessage);
            messages.add(new Message("Hermes", response, false));
            messageAdapter.notifyItemInserted(messages.size() - 1);
            messageList.scrollToPosition(messages.size() - 1);
        }, 1000);
    }

    private String generateResponse(String userMessage) {
        if (userMessage.contains("你好") || userMessage.contains("hi") || userMessage.contains("hello")) {
            return "你好！有什么可以帮助你的吗？";
        } else if (userMessage.contains("脚本") || userMessage.contains("代码")) {
            return "好的！这是一个示例代码：\n\n```python\ndef hello():\n    print('Hello, World!')\n\nhello()\n```";
        } else if (userMessage.contains("谢谢") || userMessage.contains("感谢")) {
            return "不客气！如果还有其他问题，随时告诉我。";
        } else {
            return "我理解你的问题。让我来帮你处理...";
        }
    }

    private void createNewChat() {
        messages.clear();
        messageAdapter.notifyDataSetChanged();
        Toast.makeText(this, "新会话已创建", Toast.LENGTH_SHORT).show();
    }

    private void createNewSession() {
        Session newSession = new Session("新会话 " + (sessions.size() + 1), "");
        sessions.add(newSession);
        sessionAdapter.notifyItemInserted(sessions.size() - 1);
        Toast.makeText(this, "新会话已创建", Toast.LENGTH_SHORT).show();
    }

    private void loadSession(Session session) {
        messages.clear();
        messages.add(new Message("Hermes", "这是 " + session.getTitle() + " 的内容", false));
        messageAdapter.notifyDataSetChanged();
        Toast.makeText(this, "已加载: " + session.getTitle(), Toast.LENGTH_SHORT).show();
    }

    private void refreshCurrentPanel() {
        Toast.makeText(this, "刷新中...", Toast.LENGTH_SHORT).show();
    }

    // 消息类
    public static class Message {
        private String sender;
        private String content;
        private boolean isUser;

        public Message(String sender, String content, boolean isUser) {
            this.sender = sender;
            this.content = content;
            this.isUser = isUser;
        }

        public String getSender() { return sender; }
        public String getContent() { return content; }
        public boolean isUser() { return isUser; }
    }

    // 会话类
    public static class Session {
        private String title;
        private String preview;

        public Session(String title, String preview) {
            this.title = title;
            this.preview = preview;
        }

        public String getTitle() { return title; }
        public String getPreview() { return preview; }
    }

    // 消息适配器
    public static class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
        private List<Message> messages;

        public MessageAdapter(List<Message> messages) {
            this.messages = messages;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Message message = messages.get(position);
            holder.bind(message);
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            private TextView senderText;
            private TextView contentText;
            private View messageContainer;

            ViewHolder(View itemView) {
                super(itemView);
                senderText = itemView.findViewById(R.id.senderText);
                contentText = itemView.findViewById(R.id.contentText);
                messageContainer = itemView.findViewById(R.id.messageContainer);
            }

            void bind(Message message) {
                senderText.setText(message.getSender());
                contentText.setText(message.getContent());

                FrameLayout.LayoutParams params =
                    (FrameLayout.LayoutParams) messageContainer.getLayoutParams();

                if (message.isUser()) {
                    messageContainer.setBackgroundResource(R.drawable.message_user_background);
                    params.gravity = Gravity.END;
                } else {
                    messageContainer.setBackgroundResource(R.drawable.message_ai_background);
                    params.gravity = Gravity.START;
                }
                messageContainer.setLayoutParams(params);
            }
        }
    }

    // 会话适配器
    public static class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.ViewHolder> {
        private List<Session> sessions;
        private OnSessionClickListener listener;

        public interface OnSessionClickListener {
            void onSessionClick(Session session);
        }

        public SessionAdapter(List<Session> sessions, OnSessionClickListener listener) {
            this.sessions = sessions;
            this.listener = listener;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_session, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Session session = sessions.get(position);
            holder.bind(session);
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSessionClick(session);
                }
            });
        }

        @Override
        public int getItemCount() {
            return sessions.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            private TextView titleText;
            private TextView previewText;

            ViewHolder(View itemView) {
                super(itemView);
                titleText = itemView.findViewById(R.id.sessionTitle);
                previewText = itemView.findViewById(R.id.sessionPreview);
            }

            void bind(Session session) {
                titleText.setText(session.getTitle());
                previewText.setText(session.getPreview());
            }
        }
    }
}
