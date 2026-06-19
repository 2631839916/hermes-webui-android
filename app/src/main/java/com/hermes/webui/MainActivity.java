package com.hermes.webui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.hermes.webui.api.HermesApi;
import com.hermes.webui.fragments.*;

public class MainActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private HermesApi api;
    private Fragment currentFragment;
    private ImageButton[] navButtons;
    private int selectedNav = 0;

    public HermesApi getApi() { return api; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        api = new HermesApi();
        initViews();
        showFragment(new ChatFragment());
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawerLayout);

        ImageButton btnHamburger = findViewById(R.id.btnHamburger);
        ImageButton btnNewChat = findViewById(R.id.btnNewChat);
        ImageButton btnReload = findViewById(R.id.btnReload);
        ImageButton fabNewSession = findViewById(R.id.fabNewSession);
        View btnClose = findViewById(R.id.btnCloseSidebar);

        if (btnHamburger != null) btnHamburger.setOnClickListener(v -> drawerLayout.openDrawer(Gravity.LEFT));
        if (btnNewChat != null) btnNewChat.setOnClickListener(v -> { showFragment(new ChatFragment()); highlightNav(0); });
        if (btnReload != null) btnReload.setOnClickListener(v -> recreate());
        if (fabNewSession != null) fabNewSession.setOnClickListener(v -> { showFragment(new ChatFragment()); drawerLayout.closeDrawer(Gravity.LEFT); highlightNav(0); });
        if (btnClose != null) btnClose.setOnClickListener(v -> drawerLayout.closeDrawer(Gravity.LEFT));

        navButtons = new ImageButton[] {
            findViewById(R.id.navChat), findViewById(R.id.navTasks), findViewById(R.id.navKanban),
            findViewById(R.id.navSkills), findViewById(R.id.navMemory), findViewById(R.id.navSpaces),
            findViewById(R.id.navProfiles), findViewById(R.id.navTodos), findViewById(R.id.navInsights),
            findViewById(R.id.navLogs), findViewById(R.id.navSettings)
        };

        Fragment[] fragments = {
            new ChatFragment(), new TasksFragment(), new KanbanFragment(),
            new SkillsFragment(), new MemoryFragment(), new WorkspacesFragment(),
            new ProfilesFragment(), new TodosFragment(), new InsightsFragment(),
            new LogsFragment(), new SettingsFragment()
        };

        for (int i = 0; i < navButtons.length; i++) {
            final int idx = i;
            if (navButtons[i] != null) {
                navButtons[i].setOnClickListener(v -> {
                    showFragment(fragments[idx]);
                    highlightNav(idx);
                    drawerLayout.closeDrawer(Gravity.LEFT);
                });
            }
        }
        highlightNav(0);
    }

    private void showFragment(Fragment f) {
        currentFragment = f;
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (api != null) api.shutdown();
    }
}
