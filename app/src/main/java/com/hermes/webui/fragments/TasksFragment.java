package com.hermes.webui.fragments;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hermes.webui.MainActivity;
import com.hermes.webui.api.HermesApi;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TasksFragment extends Fragment {
    private static final String TAG = "TasksFragment";

    private static final int COLOR_BG = Color.parseColor("#FEFCF7");
    private static final int COLOR_SIDEBAR = Color.parseColor("#FAF7F0");
    private static final int COLOR_BORDER = Color.parseColor("#E0D8C8");
    private static final int COLOR_TEXT = Color.parseColor("#1A1610");
    private static final int COLOR_MUTED = Color.parseColor("#5C5344");
    private static final int COLOR_ACCENT = Color.parseColor("#B8860B");
    private static final int COLOR_GREEN = Color.parseColor("#2E7D32");
    private static final int COLOR_RED = Color.parseColor("#C62828");

    private RecyclerView recyclerView;
    private TextView emptyView;
    private HermesApi api;
    private final List<CronItem> cronJobs = new ArrayList<>();
    private CronAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FrameLayout root = new FrameLayout(requireContext());
        root.setBackgroundColor(COLOR_BG);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout mainLayout = new LinearLayout(requireContext());
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(dp(20), dp(16), dp(20), dp(16));
        FrameLayout.LayoutParams mainParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mainLayout.setLayoutParams(mainParams);

        // Header
        LinearLayout header = new LinearLayout(requireContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headerParams.setMargins(0, 0, 0, dp(16));
        header.setLayoutParams(headerParams);

        TextView title = new TextView(requireContext());
        title.setText("Scheduled Tasks (Cron)");
        title.setTextSize(22);
        title.setTextColor(COLOR_TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(titleParams);
        header.addView(title);

        // Refresh button
        TextView btnRefresh = new TextView(requireContext());
        btnRefresh.setText("Refresh");
        btnRefresh.setTextColor(COLOR_ACCENT);
        btnRefresh.setTextSize(14);
        btnRefresh.setTypeface(Typeface.DEFAULT_BOLD);
        btnRefresh.setPadding(dp(12), dp(6), dp(12), dp(6));
        btnRefresh.setOnClickListener(v -> loadCrons());
        header.addView(btnRefresh);

        mainLayout.addView(header);

        // Divider
        View divider = new View(requireContext());
        divider.setBackgroundColor(COLOR_BORDER);
        divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        mainLayout.addView(divider);

        // Empty view
        emptyView = new TextView(requireContext());
        emptyView.setText("No scheduled tasks.\nTap + to create one.");
        emptyView.setTextSize(16);
        emptyView.setTextColor(COLOR_MUTED);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setPadding(dp(20), dp(60), dp(20), dp(20));
        emptyView.setVisibility(View.GONE);
        mainLayout.addView(emptyView);

        // RecyclerView
        recyclerView = new RecyclerView(requireContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CronAdapter();
        recyclerView.setAdapter(adapter);
        LinearLayout.LayoutParams recyclerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        recyclerParams.setMargins(0, dp(8), 0, 0);
        recyclerView.setLayoutParams(recyclerParams);
        mainLayout.addView(recyclerView);

        root.addView(mainLayout);

        // FAB
        TextView fab = new TextView(requireContext());
        fab.setText("+");
        fab.setTextSize(28);
        fab.setTextColor(Color.WHITE);
        fab.setGravity(Gravity.CENTER);
        fab.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable fabBg = new GradientDrawable();
        fabBg.setShape(GradientDrawable.OVAL);
        fabBg.setColor(COLOR_ACCENT);
        fab.setBackground(fabBg);
        fab.setElevation(dp(6));
        FrameLayout.LayoutParams fabParams = new FrameLayout.LayoutParams(dp(56), dp(56));
        fabParams.gravity = Gravity.BOTTOM | Gravity.END;
        fabParams.setMargins(0, 0, dp(24), dp(24));
        fab.setLayoutParams(fabParams);
        fab.setOnClickListener(v -> showCreateDialog());
        root.addView(fab);

        // Swipe to delete
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
                int pos = vh.getAdapterPosition();
                if (pos >= 0 && pos < cronJobs.size()) {
                    deleteCron(pos);
                }
            }
        }).attachToRecyclerView(recyclerView);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() instanceof MainActivity) {
            api = ((MainActivity) getActivity()).getApi();
        }
        if (api == null) {
            Toast.makeText(requireContext(), "API not available", Toast.LENGTH_SHORT).show();
            return;
        }
        loadCrons();
    }

    private void loadCrons() {
        if (api == null) return;
        api.getCrons(new HermesApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject result) {
                if (!isAdded()) return;
                cronJobs.clear();
                JSONArray arr = result.optJSONArray("crons");
                if (arr == null) arr = new JSONArray();
                for (int i = 0; i < arr.length(); i++) {
                    try {
                        JSONObject obj = arr.getJSONObject(i);
                        cronJobs.add(new CronItem(
                                obj.optString("job_id", obj.optString("id", "")),
                                obj.optString("title", obj.optString("name", "Untitled")),
                                obj.optString("schedule", obj.optString("cron", "")),
                                obj.optString("status", "active"),
                                obj.optString("last_run", obj.optString("lastRun", "Never"))
                        ));
                    } catch (Exception ignored) {}
                }
                adapter.notifyDataSetChanged();
                updateEmpty();
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Failed to load tasks: " + error, Toast.LENGTH_SHORT).show();
                updateEmpty();
            }
        });
    }

    private void deleteCron(int position) {
        if (api == null || position < 0 || position >= cronJobs.size()) return;
        CronItem item = cronJobs.get(position);
        api.deleteCron(item.jobId, new HermesApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject result) {
                if (!isAdded()) return;
                if (position < cronJobs.size()) {
                    cronJobs.remove(position);
                    adapter.notifyItemRemoved(position);
                    updateEmpty();
                    Toast.makeText(requireContext(), "Task deleted", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                adapter.notifyItemChanged(position);
                Toast.makeText(requireContext(), "Delete failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCreateDialog() {
        if (!isAdded()) return;
        // Simple input dialog using programmatic AlertDialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("New Cron Job");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(24), dp(16), dp(24), dp(8));

        EditText titleInput = new EditText(requireContext());
        titleInput.setHint("Job title");
        titleInput.setTextColor(COLOR_TEXT);
        layout.addView(titleInput);

        EditText scheduleInput = new EditText(requireContext());
        scheduleInput.setHint("Cron schedule (e.g. 0 * * * *)");
        scheduleInput.setTextColor(COLOR_TEXT);
        LinearLayout.LayoutParams schedParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        schedParams.setMargins(0, dp(8), 0, 0);
        scheduleInput.setLayoutParams(schedParams);
        layout.addView(scheduleInput);

        EditText messageInput = new EditText(requireContext());
        messageInput.setHint("Task message/prompt");
        messageInput.setTextColor(COLOR_TEXT);
        LinearLayout.LayoutParams msgParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        msgParams.setMargins(0, dp(8), 0, 0);
        messageInput.setLayoutParams(msgParams);
        layout.addView(messageInput);

        builder.setView(layout);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String title = titleInput.getText().toString().trim();
            String schedule = scheduleInput.getText().toString().trim();
            String message = messageInput.getText().toString().trim();

            if (title.isEmpty() || schedule.isEmpty()) {
                Toast.makeText(requireContext(), "Title and schedule required", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                JSONObject data = new JSONObject();
                data.put("title", title);
                data.put("schedule", schedule);
                if (!message.isEmpty()) data.put("message", message);

                api.createCron(data, new HermesApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject result) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Task created", Toast.LENGTH_SHORT).show();
                        loadCrons();
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Create failed: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Error: " + e, Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateEmpty() {
        if (emptyView != null && recyclerView != null) {
            emptyView.setVisibility(cronJobs.isEmpty() ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(cronJobs.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics());
    }

    // Data class
    private static class CronItem {
        String jobId, title, schedule, status, lastRun;
        CronItem(String jobId, String title, String schedule, String status, String lastRun) {
            this.jobId = jobId;
            this.title = title;
            this.schedule = schedule;
            this.status = status;
            this.lastRun = lastRun;
        }
    }

    // Adapter
    private class CronAdapter extends RecyclerView.Adapter<CronAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout item = new LinearLayout(requireContext());
            item.setOrientation(LinearLayout.VERTICAL);
            item.setBackgroundColor(Color.WHITE);
            item.setPadding(dp(16), dp(14), dp(16), dp(14));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, dp(8));
            item.setLayoutParams(params);

            // Rounded corners
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(8));
            bg.setColor(Color.WHITE);
            bg.setStroke(dp(1), COLOR_BORDER);
            item.setBackground(bg);

            // Row 1: title + status
            LinearLayout row1 = new LinearLayout(requireContext());
            row1.setOrientation(LinearLayout.HORIZONTAL);
            row1.setGravity(Gravity.CENTER_VERTICAL);

            TextView titleView = new TextView(requireContext());
            titleView.setTextSize(16);
            titleView.setTextColor(COLOR_TEXT);
            titleView.setTypeface(Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            titleView.setLayoutParams(titleParams);
            titleView.setId(android.R.id.text1);
            row1.addView(titleView);

            TextView statusView = new TextView(requireContext());
            statusView.setTextSize(12);
            statusView.setTypeface(Typeface.DEFAULT_BOLD);
            statusView.setPadding(dp(8), dp(3), dp(8), dp(3));
            statusView.setId(android.R.id.text2);
            row1.addView(statusView);

            item.addView(row1);

            // Row 2: schedule
            TextView scheduleView = new TextView(requireContext());
            scheduleView.setTextSize(13);
            scheduleView.setTextColor(COLOR_MUTED);
            scheduleView.setPadding(0, dp(4), 0, 0);
            scheduleView.setId(android.R.id.summary);
            item.addView(scheduleView);

            // Row 3: last run
            TextView lastRunView = new TextView(requireContext());
            lastRunView.setTextSize(12);
            lastRunView.setTextColor(COLOR_MUTED);
            lastRunView.setPadding(0, dp(2), 0, 0);
            lastRunView.setId(android.R.id.content);
            item.addView(lastRunView);

            return new VH(item);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            CronItem cron = cronJobs.get(position);
            holder.titleView.setText(cron.title);
            holder.scheduleView.setText("Schedule: " + cron.schedule);
            holder.lastRunView.setText("Last run: " + cron.lastRun);

            boolean isActive = "active".equalsIgnoreCase(cron.status);
            holder.statusView.setText(isActive ? "Active" : "Paused");
            holder.statusView.setTextColor(isActive ? COLOR_GREEN : COLOR_RED);
            holder.statusView.setBackgroundColor(isActive ?
                    Color.parseColor("#E8F5E9") : Color.parseColor("#FFEBEE"));

            holder.itemView.setOnClickListener(v -> {
                Toast.makeText(requireContext(),
                        cron.title + "\n" + cron.schedule + "\nStatus: " + cron.status,
                        Toast.LENGTH_LONG).show();
            });
        }

        @Override
        public int getItemCount() {
            return cronJobs.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView titleView, statusView, scheduleView, lastRunView;
            VH(View v) {
                super(v);
                titleView = v.findViewById(android.R.id.text1);
                statusView = v.findViewById(android.R.id.text2);
                scheduleView = v.findViewById(android.R.id.summary);
                lastRunView = v.findViewById(android.R.id.content);
            }
        }
    }
}
