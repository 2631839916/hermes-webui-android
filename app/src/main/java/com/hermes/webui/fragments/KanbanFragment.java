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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hermes.webui.MainActivity;
import com.hermes.webui.R;
import com.hermes.webui.api.HermesApi;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class KanbanFragment extends Fragment {
    private static final String TAG = "KanbanFragment";

    private static final int COLOR_BG = Color.parseColor("#FEFCF7");
    private static final int COLOR_BORDER = Color.parseColor("#E0D8C8");
    private static final int COLOR_TEXT = Color.parseColor("#1A1610");
    private static final int COLOR_MUTED = Color.parseColor("#5C5344");
    private static final int COLOR_ACCENT = Color.parseColor("#B8860B");

    private static final String[] COLUMN_NAMES = {"待处理", "进行中", "已阻塞", "已完成"};
    private static final String[] COLUMN_STATUSES = {"ready", "in_progress", "blocked", "done"};
    private static final int[] COLUMN_COLORS = {
            Color.parseColor("#E3F2FD"), // blue
            Color.parseColor("#FFF3E0"), // orange
            Color.parseColor("#FFEBEE"), // red
            Color.parseColor("#E8F5E9")  // green
    };
    private static final int[] COLUMN_HEADER_COLORS = {
            Color.parseColor("#1565C0"),
            Color.parseColor("#E65100"),
            Color.parseColor("#C62828"),
            Color.parseColor("#2E7D32")
    };

    private final LinearLayout[] columnContainers = new LinearLayout[4];
    private final List<TaskItem>[] columns = new List[4];
    private HermesApi api;

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        for (int i = 0; i < 4; i++) {
            columns[i] = new ArrayList<>();
        }

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setPadding(dp(12), dp(12), dp(12), dp(12));

        // Header with title + add button
        LinearLayout header = new LinearLayout(requireContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headerParams.setMargins(0, 0, 0, dp(12));
        header.setLayoutParams(headerParams);

        TextView title = new TextView(requireContext());
        title.setText("Kanban Board");
        title.setTextSize(22);
        title.setTextColor(COLOR_TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(titleParams);
        header.addView(title);

        TextView btnAdd = new TextView(requireContext());
        btnAdd.setText("+ Add Task");
        btnAdd.setTextColor(Color.WHITE);
        btnAdd.setBackgroundColor(COLOR_ACCENT);
        btnAdd.setPadding(dp(14), dp(8), dp(14), dp(8));
        btnAdd.setTypeface(Typeface.DEFAULT_BOLD);
        btnAdd.setTextSize(14);
        GradientDrawable addBg = new GradientDrawable();
        addBg.setCornerRadius(dp(6));
        addBg.setColor(COLOR_ACCENT);
        btnAdd.setBackground(addBg);
        btnAdd.setOnClickListener(v -> showCreateDialog());
        header.addView(btnAdd);

        root.addView(header);

        // HorizontalScrollView for columns
        android.widget.HorizontalScrollView hScroll = new android.widget.HorizontalScrollView(requireContext());
        hScroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout columnsRow = new LinearLayout(requireContext());
        columnsRow.setOrientation(LinearLayout.HORIZONTAL);
        columnsRow.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        for (int i = 0; i < 4; i++) {
            LinearLayout column = createColumn(i);
            LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(dp(260), ViewGroup.LayoutParams.MATCH_PARENT);
            colParams.setMargins(0, 0, dp(10), 0);
            column.setLayoutParams(colParams);
            columnsRow.addView(column);
            columnContainers[i] = column;
        }

        hScroll.addView(columnsRow);
        root.addView(hScroll);

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
        loadKanban();
    }

    private LinearLayout createColumn(int index) {
        LinearLayout column = new LinearLayout(requireContext());
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(8), dp(8), dp(8), dp(8));

        GradientDrawable colBg = new GradientDrawable();
        colBg.setCornerRadius(dp(10));
        colBg.setColor(COLUMN_COLORS[index]);
        colBg.setStroke(dp(1), COLOR_BORDER);
        column.setBackground(colBg);

        // Column header
        TextView headerText = new TextView(requireContext());
        headerText.setText(COLUMN_NAMES[index]);
        headerText.setTextSize(16);
        headerText.setTextColor(COLUMN_HEADER_COLORS[index]);
        headerText.setTypeface(Typeface.DEFAULT_BOLD);
        headerText.setPadding(dp(4), dp(4), dp(4), dp(8));
        column.addView(headerText);

        // Task list container
        LinearLayout taskList = new LinearLayout(requireContext());
        taskList.setOrientation(LinearLayout.VERTICAL);
        taskList.setTag("tasklist_" + index);
        column.addView(taskList, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        return column;
    }

    private void loadKanban() {
        if (api == null) return;
        api.getKanban(new HermesApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject result) {
                if (!isAdded()) return;
                for (int i = 0; i < 4; i++) columns[i].clear();

                // Parse tasks from result - could be array or object with columns
                JSONArray tasks = result.optJSONArray("tasks");
                if (tasks == null) {
                    // Try object with column keys
                    for (int i = 0; i < 4; i++) {
                        JSONArray colTasks = result.optJSONArray(COLUMN_STATUSES[i]);
                        if (colTasks != null) {
                            for (int j = 0; j < colTasks.length(); j++) {
                                try {
                                    JSONObject task = colTasks.getJSONObject(j);
                                    columns[i].add(parseTask(task, COLUMN_STATUSES[i]));
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                } else {
                    for (int i = 0; i < tasks.length(); i++) {
                        try {
                            JSONObject task = tasks.getJSONObject(i);
                            String status = task.optString("status", "ready");
                            int colIdx = getColumnIndex(status);
                            columns[colIdx].add(parseTask(task, status));
                        } catch (Exception ignored) {}
                    }
                }
                refreshColumns();
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Failed to load kanban: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private TaskItem parseTask(JSONObject obj, String status) {
        return new TaskItem(
                obj.optString("task_id", obj.optString("id", "")),
                obj.optString("title", obj.optString("name", "Untitled")),
                status,
                obj.optString("assignee", obj.optString("assigned_to", ""))
        );
    }

    private int getColumnIndex(String status) {
        if (status == null) return 0;
        switch (status.toLowerCase()) {
            case "in_progress": return 1;
            case "blocked": return 2;
            case "done": return 3;
            default: return 0;
        }
    }

    private void refreshColumns() {
        for (int i = 0; i < 4; i++) {
            LinearLayout taskList = columnContainers[i].findViewWithTag("tasklist_" + i);
            if (taskList == null) continue;
            taskList.removeAllViews();

            for (TaskItem task : columns[i]) {
                View card = createTaskCard(task, i);
                taskList.addView(card);
            }

            if (columns[i].isEmpty()) {
                TextView empty = new TextView(requireContext());
                empty.setText(getString(R.string.no_data));
                empty.setTextSize(13);
                empty.setTextColor(COLOR_MUTED);
                empty.setGravity(Gravity.CENTER);
                empty.setPadding(dp(8), dp(16), dp(8), dp(16));
                taskList.addView(empty);
            }
        }
    }

    private View createTaskCard(TaskItem task, int columnIndex) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(6));
        card.setLayoutParams(params);

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setCornerRadius(dp(6));
        cardBg.setColor(Color.WHITE);
        cardBg.setStroke(dp(1), COLOR_BORDER);
        card.setBackground(cardBg);
        card.setElevation(dp(2));

        // Title
        TextView titleView = new TextView(requireContext());
        titleView.setText(task.title);
        titleView.setTextSize(14);
        titleView.setTextColor(COLOR_TEXT);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setMaxLines(2);
        card.addView(titleView);

        // Assignee
        if (task.assignee != null && !task.assignee.isEmpty()) {
            TextView assigneeView = new TextView(requireContext());
            assigneeView.setText("Assigned: " + task.assignee);
            assigneeView.setTextSize(12);
            assigneeView.setTextColor(COLOR_MUTED);
            assigneeView.setPadding(0, dp(4), 0, 0);
            card.addView(assigneeView);
        }

        // Move buttons row
        LinearLayout moveRow = new LinearLayout(requireContext());
        moveRow.setOrientation(LinearLayout.HORIZONTAL);
        moveRow.setGravity(Gravity.END);
        moveRow.setPadding(0, dp(6), 0, 0);

        String[] moveLabels = {"←", "→"};
        int[] moveTargets = {columnIndex - 1, columnIndex + 1};
        for (int m = 0; m < 2; m++) {
            if (moveTargets[m] >= 0 && moveTargets[m] < 4) {
                TextView btn = new TextView(requireContext());
                btn.setText(moveLabels[m] + " " + COLUMN_NAMES[moveTargets[m]]);
                btn.setTextSize(11);
                btn.setTextColor(COLOR_ACCENT);
                btn.setPadding(dp(6), dp(3), dp(6), dp(3));
                final int targetIdx = moveTargets[m];
                final String targetStatus = COLUMN_STATUSES[targetIdx];
                btn.setOnClickListener(v -> moveTask(task, targetStatus));
                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                btnParams.setMargins(dp(4), 0, 0, 0);
                btn.setLayoutParams(btnParams);
                moveRow.addView(btn);
            }
        }
        card.addView(moveRow);

        return card;
    }

    private void moveTask(TaskItem task, String newStatus) {
        if (api == null) return;
        try {
            JSONObject data = new JSONObject();
            data.put("status", newStatus);
            api.updateKanbanTask(task.taskId, data, new HermesApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject result) {
                    if (!isAdded()) return;
                    loadKanban();
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Move failed: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(requireContext(), getString(R.string.error) + ": " + e, Toast.LENGTH_SHORT).show();
        }
    }

    private void showCreateDialog() {
        if (!isAdded()) return;
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("New Task");

        EditText input = new EditText(requireContext());
        input.setHint("Task title");
        input.setTextColor(COLOR_TEXT);
        input.setPadding(dp(16), dp(12), dp(16), dp(12));
        builder.setView(input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String title = input.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "Title required", Toast.LENGTH_SHORT).show();
                return;
            }
            api.createKanbanTask(title, new HermesApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject result) {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Task created", Toast.LENGTH_SHORT).show();
                    loadKanban();
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Create failed: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.show();
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics());
    }

    // Data class
    private static class TaskItem {
        String taskId, title, status, assignee;
        TaskItem(String taskId, String title, String status, String assignee) {
            this.taskId = taskId;
            this.title = title;
            this.status = status;
            this.assignee = assignee;
        }
    }
}
