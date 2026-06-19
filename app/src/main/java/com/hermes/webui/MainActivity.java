package com.hermes.webui;

import android.os.Bundle;
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

    // 所有导航按钮
    private ImageButton[] navButtons;
    private String[] navNames = {"chat", "tasks", "kanban", "skills", "memory", "workspaces", "profiles", "todos", "insights", "logs", "settings"};

    // 面板
    private View panelChat, panelTasks, panelKanban, panelSkills, panelMemory, panelSettings;

    private MsgAdapter msgAdapter;
    private SessionAdapter sessionAdapter;
    private List<String[]> messages = new ArrayList<>(); // [sender, content, isUser]
    private List<String> sessionNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        try {
            setContentView(R.layout.activity_main);
            initViews();
            setupMessageList();
            setupSessionList();
            setupAllListeners();
            loadSampleData();
            Log.d(TAG, "启动成功");
        } catch (Exception e) {
            Log.e(TAG, "启动错误", e);
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

        // 面板
        panelChat = findViewById(R.id.panelChat);
        panelTasks = findViewById(R.id.panelTasks);
        panelKanban = findViewById(R.id.panelKanban);
        panelSkills = findViewById(R.id.panelSkills);
        panelMemory = findViewById(R.id.panelMemory);
        panelSettings = findViewById(R.id.panelSettings);

        // 收集所有导航按钮
        navButtons = new ImageButton[] {
            findViewById(R.id.navChat),
            findViewById(R.id.navTasks),
            findViewById(R.id.navKanban),
            findViewById(R.id.navSkills),
            findViewById(R.id.navMemory),
            findViewById(R.id.navSpaces),
            findViewById(R.id.navProfiles),
            findViewById(R.id.navTodos),
            findViewById(R.id.navInsights),
            findViewById(R.id.navLogs),
            findViewById(R.id.navSettings)
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

    private void setupAllListeners() {
        // 发送
        btnSend.setOnClickListener(v -> sendMessage());

        // 汉堡菜单 -> 打开侧边栏
        btnHamburger.setOnClickListener(v -> drawerLayout.openDrawer(Gravity.LEFT));

        // 新建聊天
        btnNewChat.setOnClickListener(v -> {
            messages.clear();
            msgAdapter.notifyDataSetChanged();
            Toast.makeText(this, "新会话已创建", Toast.LENGTH_SHORT).show();
        });

        // 刷新
        btnReload.setOnClickListener(v -> {
            Toast.makeText(this, "已刷新", Toast.LENGTH_SHORT).show();
        });

        // 新建会话 (侧边栏)
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

        // 所有导航按钮
        for (int i = 0; i < navButtons.length; i++) {
            final int index = i;
            if (navButtons[i] != null) {
                navButtons[i].setOnClickListener(v -> {
                    Log.d(TAG, "点击: " + navNames[index]);
                    switchPanel(navNames[index]);
                    highlightNav(index);
                });
            }
        }

        // 默认选中 chat
        highlightNav(0);
    }

    private void switchPanel(String name) {
        // 隐藏所有面板
        if (panelChat != null) panelChat.setVisibility(View.GONE);
        if (panelTasks != null) panelTasks.setVisibility(View.GONE);
        if (panelKanban != null) panelKanban.setVisibility(View.GONE);
        if (panelSkills != null) panelSkills.setVisibility(View.GONE);
        if (panelMemory != null) panelMemory.setVisibility(View.GONE);
        if (panelSettings != null) panelSettings.setVisibility(View.GONE);

        // 显示对应面板
        switch (name) {
            case "chat": if (panelChat != null) panelChat.setVisibility(View.VISIBLE); break;
            case "tasks": if (panelTasks != null) panelTasks.setVisibility(View.VISIBLE); break;
            case "kanban": if (panelKanban != null) panelKanban.setVisibility(View.VISIBLE); break;
            case "skills": if (panelSkills != null) panelSkills.setVisibility(View.VISIBLE); break;
            case "memory": if (panelMemory != null) panelMemory.setVisibility(View.VISIBLE); break;
            case "settings": if (panelSettings != null) panelSettings.setVisibility(View.VISIBLE); break;
            default:
                // workspaces, profiles, todos, insights, logs -> 暂时显示 toast
                Toast.makeText(this, name + " 功能开发中...", Toast.LENGTH_SHORT).show();
                if (panelChat != null) panelChat.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void highlightNav(int selected) {
        for (int i = 0; i < navButtons.length; i++) {
            if (navButtons[i] != null) {
                navButtons[i].setSelected(i == selected);
            }
        }
    }

    private void loadSampleData() {
        messages.add(new String[]{"Hermes", "你好！我是 Hermes，你的 AI 助手。有什么可以帮助你的吗？", "false"});
        messages.add(new String[]{"User", "帮我写一个 Python 脚本", "true"});
        messages.add(new String[]{"Hermes", "好的！这是一个简单的 Python 脚本：\n\nprint('Hello, World!')", "false"});
        msgAdapter.notifyDataSetChanged();

        sessionNames.add("会话 1");
        sessionNames.add("会话 2");
        sessionNames.add("会话 3");
        sessionAdapter.notifyDataSetChanged();
    }

    private void sendMessage() {
        String text = inputMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        Log.d(TAG, "发送: " + text);

        // 添加用户消息
        messages.add(new String[]{"User", text, "true"});
        msgAdapter.notifyItemInserted(messages.size() - 1);
        messageList.scrollToPosition(messages.size() - 1);
        inputMessage.setText("");

        // 延迟回复
        final String userInput = text;
        messageList.postDelayed(() -> {
            String reply;
            if (userInput.contains("你好") || userInput.toLowerCase().contains("hi") || userInput.toLowerCase().contains("hello")) {
                reply = "你好！有什么可以帮助你的吗？";
            } else if (userInput.contains("代码") || userInput.contains("脚本")) {
                reply = "好的，这是一个示例：\n\nprint('Hello, World!')";
            } else if (userInput.contains("谢谢")) {
                reply = "不客气！还有其他问题吗？";
            } else {
                reply = "收到！让我来帮你处理: " + userInput;
            }

            messages.add(new String[]{"Hermes", reply, "false"});
            msgAdapter.notifyItemInserted(messages.size() - 1);
            messageList.scrollToPosition(messages.size() - 1);
            Log.d(TAG, "回复: " + reply);
        }, 800);
    }

    // ===== 消息适配器 =====
    class MsgAdapter extends RecyclerView.Adapter<MsgAdapter.VH> {
        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            String[] msg = messages.get(pos);
            h.senderText.setText(msg[0]);
            h.contentText.setText(msg[1]);

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) h.container.getLayoutParams();
            if ("true".equals(msg[2])) {
                h.container.setBackgroundResource(R.drawable.message_user_background);
                params.gravity = Gravity.END;
            } else {
                h.container.setBackgroundResource(R.drawable.message_ai_background);
                params.gravity = Gravity.START;
            }
            h.container.setLayoutParams(params);
        }

        @Override
        public int getItemCount() { return messages.size(); }

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

    // ===== 会话适配器 =====
    class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.VH> {
        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_session, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            String name = sessionNames.get(pos);
            h.titleText.setText(name);
            h.previewText.setText("点击加载此会话");
            h.itemView.setOnClickListener(v -> {
                messages.clear();
                messages.add(new String[]{"Hermes", "已加载: " + name, "false"});
                msgAdapter.notifyDataSetChanged();
                drawerLayout.closeDrawer(Gravity.LEFT);
                Toast.makeText(MainActivity.this, "已加载: " + name, Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public int getItemCount() { return sessionNames.size(); }

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
