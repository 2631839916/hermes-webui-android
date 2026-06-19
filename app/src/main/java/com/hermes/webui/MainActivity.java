package com.hermes.webui;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
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
    private SimpleSessionAdapter sessionAdapter;
    private List<Message> messages = new ArrayList<>();
    private List<String> sessionNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate 开始");

        try {
            setContentView(R.layout.activity_main);
            Log.d(TAG, "setContentView 成功");

            initViews();
            Log.d(TAG, "initViews 成功");

            setupMessageList();
            Log.d(TAG, "setupMessageList 成功");

            setupSessionList();
            Log.d(TAG, "setupSessionList 成功");

            setupClickListeners();
            Log.d(TAG, "setupClickListeners 成功");

            loadSampleData();
            Log.d(TAG, "loadSampleData 成功");

            Log.d(TAG, "启动完成！");

        } catch (Exception e) {
            Log.e(TAG, "启动错误: " + e.getMessage(), e);
            Toast.makeText(this, "错误: " + e.getMessage(), Toast.LENGTH_LONG).show();
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

        Log.d(TAG, "控件初始化: drawer=" + (drawerLayout!=null) +
              " msgList=" + (messageList!=null) +
              " input=" + (inputMessage!=null) +
              " send=" + (btnSend!=null));
    }

    private void setupMessageList() {
        messageAdapter = new MessageAdapter();
        messageList.setLayoutManager(new LinearLayoutManager(this));
        messageList.setAdapter(messageAdapter);
    }

    private void setupSessionList() {
        sessionAdapter = new SimpleSessionAdapter();
        sessionList.setLayoutManager(new LinearLayoutManager(this));
        sessionList.setAdapter(sessionAdapter);
    }

    private void setupClickListeners() {
        // 发送按钮
        btnSend.setOnClickListener(v -> sendMessage());

        // 汉堡菜单
        btnHamburger.setOnClickListener(v -> drawerLayout.openDrawer(Gravity.LEFT));

        // 新建聊天
        btnNewChat.setOnClickListener(v -> {
            messages.clear();
            messageAdapter.notifyDataSetChanged();
            Toast.makeText(this, "新会话", Toast.LENGTH_SHORT).show();
        });

        // 刷新
        btnReload.setOnClickListener(v -> Toast.makeText(this, "刷新中...", Toast.LENGTH_SHORT).show());

        // 新建会话
        fabNewSession.setOnClickListener(v -> {
            sessionNames.add("会话 " + (sessionNames.size() + 1));
            sessionAdapter.notifyDataSetChanged();
            Toast.makeText(this, "新会话已创建", Toast.LENGTH_SHORT).show();
        });

        // 关闭侧边栏
        View closeBtn = findViewById(R.id.btnCloseSidebar);
        if (closeBtn != null) {
            closeBtn.setOnClickListener(v -> drawerLayout.closeDrawer(Gravity.LEFT));
        }

        // 导航按钮
        setupNav(navChat, "chat", panelChat);
        setupNav(navTasks, "tasks", panelTasks);
        setupNav(navKanban, "kanban", panelKanban);
        setupNav(navSkills, "skills", panelSkills);
        setupNav(navMemory, "memory", panelMemory);
        setupNav(navSettings, "settings", panelSettings);

        // 默认选中 chat
        if (navChat != null) navChat.setSelected(true);
    }

    private void setupNav(ImageButton btn, String name, View panel) {
        if (btn == null) return;
        btn.setOnClickListener(v -> {
            Log.d(TAG, "导航点击: " + name);
            // 隐藏所有面板
            hideAllPanels();
            // 显示目标面板
            if (panel != null) panel.setVisibility(View.VISIBLE);
            // 更新选中状态
            resetNavSelection();
            btn.setSelected(true);
        });
    }

    private void hideAllPanels() {
        if (panelChat != null) panelChat.setVisibility(View.GONE);
        if (panelTasks != null) panelTasks.setVisibility(View.GONE);
        if (panelKanban != null) panelKanban.setVisibility(View.GONE);
        if (panelSkills != null) panelSkills.setVisibility(View.GONE);
        if (panelMemory != null) panelMemory.setVisibility(View.GONE);
        if (panelSettings != null) panelSettings.setVisibility(View.GONE);
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

    private void loadSampleData() {
        messages.add(new Message("Hermes", "你好！我是 Hermes，你的 AI 助手。有什么可以帮助你的吗？", false));
        messages.add(new Message("User", "帮我写一个 Python 脚本", true));
        messages.add(new Message("Hermes", "好的！这是一个简单的 Python 脚本：\n\nprint('Hello, World!')", false));
        messageAdapter.notifyDataSetChanged();

        sessionNames.add("会话 1");
        sessionNames.add("会话 2");
        sessionNames.add("会话 3");
        sessionAdapter.notifyDataSetChanged();
    }

    private void sendMessage() {
        String text = inputMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        Log.d(TAG, "发送消息: " + text);

        // 添加用户消息
        messages.add(new Message("User", text, true));
        messageAdapter.notifyDataSetChanged();
        messageList.scrollToPosition(messages.size() - 1);
        inputMessage.setText("");

        // 模拟 AI 回复
        String finalText = text;
        messageList.postDelayed(() -> {
            String reply = generateReply(finalText);
            messages.add(new Message("Hermes", reply, false));
            messageAdapter.notifyDataSetChanged();
            messageList.scrollToPosition(messages.size() - 1);
            Log.d(TAG, "AI 回复: " + reply);
        }, 800);
    }

    private String generateReply(String input) {
        if (input.contains("你好") || input.contains("hi") || input.contains("hello")) {
            return "你好！有什么可以帮助你的吗？";
        } else if (input.contains("代码") || input.contains("脚本")) {
            return "好的，这是一个示例：\n\nprint('Hello, World!')";
        } else if (input.contains("谢谢")) {
            return "不客气！还有其他问题吗？";
        }
        return "收到！让我来帮你处理...";
    }

    // ========== 消息类 ==========
    static class Message {
        String sender;
        String content;
        boolean isUser;

        Message(String sender, String content, boolean isUser) {
            this.sender = sender;
            this.content = content;
            this.isUser = isUser;
        }
    }

    // ========== 消息适配器 ==========
    class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.VH> {

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int pos) {
            Message msg = messages.get(pos);
            holder.senderText.setText(msg.sender);
            holder.contentText.setText(msg.content);

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.container.getLayoutParams();
            if (msg.isUser) {
                holder.container.setBackgroundResource(R.drawable.message_user_background);
                params.gravity = Gravity.END;
            } else {
                holder.container.setBackgroundResource(R.drawable.message_ai_background);
                params.gravity = Gravity.START;
            }
            holder.container.setLayoutParams(params);
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView senderText, contentText;
            View container;

            VH(View v) {
                super(v);
                senderText = v.findViewById(R.id.senderText);
                contentText = v.findViewById(R.id.contentText);
                container = v.findViewById(R.id.messageContainer);
            }
        }
    }

    // ========== 会话适配器 ==========
    class SimpleSessionAdapter extends RecyclerView.Adapter<SimpleSessionAdapter.VH> {

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_session, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int pos) {
            String name = sessionNames.get(pos);
            holder.titleText.setText(name);
            holder.previewText.setText("点击加载此会话");
            holder.itemView.setOnClickListener(v -> {
                messages.clear();
                messages.add(new Message("Hermes", "这是 " + name + " 的内容", false));
                messageAdapter.notifyDataSetChanged();
                drawerLayout.closeDrawer(Gravity.LEFT);
                Toast.makeText(MainActivity.this, "已加载: " + name, Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public int getItemCount() {
            return sessionNames.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView titleText, previewText;
            VH(View v) {
                super(v);
                titleText = v.findViewById(R.id.sessionTitle);
                previewText = v.findViewById(R.id.sessionPreview);
            }
        }
    }
}
