package com.hermes.webui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hermes.webui.api.HermesApi;
import com.hermes.webui.fragments.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private RecyclerView sessionList;
    private HermesApi api;
    private ImageButton[] navButtons;
    private int selectedNav = 0;
    private String currentSessionId;
    private Fragment[] fragmentCache = new Fragment[11];
    private final List<SessionItem> sessions = new ArrayList<>();
    private SessionAdapter sessionAdapter;

    public HermesApi getApi() { return api; }
    public String getCurrentSessionId() { return currentSessionId; }
    public void setCurrentSessionId(String id) { this.currentSessionId = id; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        api = new HermesApi();
        initViews();
        showFragment(new ChatFragment());
        loadSessions();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        sessionList = findViewById(R.id.sessionList);

        // Session list
        if (sessionList != null) {
            sessionAdapter = new SessionAdapter();
            sessionList.setLayoutManager(new LinearLayoutManager(this));
            sessionList.setAdapter(sessionAdapter);
        }

        ImageButton btnHamburger = findViewById(R.id.btnHamburger);
        ImageButton btnNewChat = findViewById(R.id.btnNewChat);
        ImageButton btnReload = findViewById(R.id.btnReload);
        ImageButton fabNewSession = findViewById(R.id.fabNewSession);
        View btnClose = findViewById(R.id.btnCloseSidebar);

        if (btnHamburger != null) btnHamburger.setOnClickListener(v -> drawerLayout.openDrawer(Gravity.LEFT));
        if (btnNewChat != null) btnNewChat.setOnClickListener(v -> { fragmentCache[0] = new ChatFragment(); showFragment(fragmentCache[0]); highlightNav(0); });
        if (btnReload != null) btnReload.setOnClickListener(v -> loadSessions());
        if (fabNewSession != null) fabNewSession.setOnClickListener(v -> {
            fragmentCache[0] = new ChatFragment();
            showFragment(fragmentCache[0]);
            drawerLayout.closeDrawer(Gravity.LEFT);
            highlightNav(0);
        });
        if (btnClose != null) btnClose.setOnClickListener(v -> drawerLayout.closeDrawer(Gravity.LEFT));

        navButtons = new ImageButton[] {
            findViewById(R.id.navChat), findViewById(R.id.navTasks), findViewById(R.id.navKanban),
            findViewById(R.id.navSkills), findViewById(R.id.navMemory), findViewById(R.id.navSpaces),
            findViewById(R.id.navProfiles), findViewById(R.id.navTodos), findViewById(R.id.navInsights),
            findViewById(R.id.navLogs), findViewById(R.id.navSettings)
        };

        // 初始化 fragment 缓存
        fragmentCache[0] = new ChatFragment();
        fragmentCache[1] = new TasksFragment();
        fragmentCache[2] = new KanbanFragment();
        fragmentCache[3] = new SkillsFragment();
        fragmentCache[4] = new MemoryFragment();
        fragmentCache[5] = new WorkspacesFragment();
        fragmentCache[6] = new ProfilesFragment();
        fragmentCache[7] = new TodosFragment();
        fragmentCache[8] = new InsightsFragment();
        fragmentCache[9] = new LogsFragment();
        fragmentCache[10] = new SettingsFragment();

        for (int i = 0; i < navButtons.length; i++) {
            final int idx = i;
            if (navButtons[i] != null) {
                navButtons[i].setOnClickListener(v -> {
                    showFragment(fragmentCache[idx]);
                    highlightNav(idx);
                    drawerLayout.closeDrawer(Gravity.LEFT);
                });
            }
        }
        highlightNav(0);
    }

    private void showFragment(Fragment f) {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.contentFrame, f)
            .commit();
    }

    private void highlightNav(int selected) {
        selectedNav = selected;
        for (int i = 0; i < navButtons.length; i++) {
            if (navButtons[i] != null) navButtons[i].setSelected(i == selected);
        }
    }

    private void loadSessions() {
        if (api == null) return;
        api.getSessions(new HermesApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject result) {
                JSONArray arr = result.optJSONArray("sessions");
                if (arr == null) arr = new JSONArray();
                sessions.clear();
                for (int i = 0; i < arr.length() && i < 50; i++) {
                    try {
                        JSONObject s = arr.getJSONObject(i);
                        String id = s.optString("session_id", s.optString("id", ""));
                        String title = s.optString("title", "Untitled");
                        String preview = s.optString("preview", s.optString("last_message", ""));
                        sessions.add(new SessionItem(id, title, preview));
                    } catch (Exception ignored) {}
                }
                if (sessionAdapter != null) sessionAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, "加载会话失败: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void loadSession(String sessionId) {
        drawerLayout.closeDrawer(Gravity.LEFT);
        currentSessionId = sessionId;
        fragmentCache[0] = new ChatFragment();
        Bundle args = new Bundle();
        args.putString("session_id", sessionId);
        fragmentCache[0].setArguments(args);
        showFragment(fragmentCache[0]);
        highlightNav(0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (api != null) api.shutdown();
    }

    // Session data class
    static class SessionItem {
        String id, title, preview;
        SessionItem(String i, String t, String p) { id = i; title = t; preview = p; }
    }

    // Session adapter
    class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.VH> {
        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_session, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            SessionItem s = sessions.get(pos);
            h.titleText.setText(s.title);
            h.previewText.setText(s.preview != null ? s.preview : "");
            h.itemView.setOnClickListener(v -> loadSession(s.id));
        }

        @Override
        public int getItemCount() { return sessions.size(); }

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
