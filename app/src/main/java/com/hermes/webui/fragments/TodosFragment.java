package com.hermes.webui.fragments;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hermes.webui.MainActivity;
import com.hermes.webui.api.HermesApi;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TodosFragment extends Fragment {
    private static final String TAG = "TodosFragment";

    private static final int COLOR_BG = Color.parseColor("#FEFCF7");
    private static final int COLOR_BORDER = Color.parseColor("#E0D8C8");
    private static final int COLOR_TEXT = Color.parseColor("#1A1610");
    private static final int COLOR_MUTED = Color.parseColor("#5C5344");
    private static final int COLOR_ACCENT = Color.parseColor("#B8860B");

    private RecyclerView recyclerView;
    private TextView emptyView;
    private TextView countView;
    private HermesApi api;

    private final List<TodoItem> todos = new ArrayList<>();
    private TodoAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setPadding(dp(20), dp(16), dp(20), dp(16));

        // Header
        LinearLayout header = new LinearLayout(requireContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headerParams.setMargins(0, 0, 0, dp(12));
        header.setLayoutParams(headerParams);

        TextView title = new TextView(requireContext());
        title.setText("Todos");
        title.setTextSize(22);
        title.setTextColor(COLOR_TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(titleParams);
        header.addView(title);

        countView = new TextView(requireContext());
        countView.setTextSize(14);
        countView.setTextColor(COLOR_MUTED);
        header.addView(countView);

        TextView btnRefresh = new TextView(requireContext());
        btnRefresh.setText("Refresh");
        btnRefresh.setTextColor(COLOR_ACCENT);
        btnRefresh.setTextSize(14);
        btnRefresh.setTypeface(Typeface.DEFAULT_BOLD);
        btnRefresh.setPadding(dp(12), dp(6), 0, dp(6));
        btnRefresh.setOnClickListener(v -> loadTodos());
        header.addView(btnRefresh);

        root.addView(header);

        // Filter tabs
        LinearLayout filterRow = new LinearLayout(requireContext());
        filterRow.setOrientation(LinearLayout.HORIZONTAL);
        filterRow.setPadding(0, 0, 0, dp(12));
        String[] filters = {"All", "Pending", "In Progress", "Completed", "Cancelled"};
        TextView[] filterButtons = new TextView[filters.length];
        for (int i = 0; i < filters.length; i++) {
            TextView btn = new TextView(requireContext());
            btn.setText(filters[i]);
            btn.setTextSize(13);
            btn.setTextColor(i == 0 ? Color.WHITE : COLOR_MUTED);
            btn.setPadding(dp(12), dp(6), dp(12), dp(6));
            btn.setTypeface(Typeface.DEFAULT_BOLD);

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(16));
            bg.setColor(i == 0 ? COLOR_ACCENT : Color.TRANSPARENT);
            if (i != 0) bg.setStroke(dp(1), COLOR_BORDER);
            btn.setBackground(bg);

            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            btnParams.setMargins(0, 0, dp(8), 0);
            btn.setLayoutParams(btnParams);

            final int filterIdx = i;
            btn.setOnClickListener(v -> {
                for (int j = 0; j < filterButtons.length; j++) {
                    GradientDrawable bg2 = new GradientDrawable();
                    bg2.setCornerRadius(dp(16));
                    if (j == filterIdx) {
                        bg2.setColor(COLOR_ACCENT);
                        filterButtons[j].setTextColor(Color.WHITE);
                    } else {
                        bg2.setColor(Color.TRANSPARENT);
                        bg2.setStroke(dp(1), COLOR_BORDER);
                        filterButtons[j].setTextColor(COLOR_MUTED);
                    }
                    filterButtons[j].setBackground(bg2);
                }
                applyFilter(filterIdx);
            });
            filterButtons[i] = btn;
            filterRow.addView(btn);
        }
        root.addView(filterRow);

        // Divider
        View divider = new View(requireContext());
        divider.setBackgroundColor(COLOR_BORDER);
        divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        root.addView(divider);

        // Empty view
        emptyView = new TextView(requireContext());
        emptyView.setText("No todos found.");
        emptyView.setTextSize(16);
        emptyView.setTextColor(COLOR_MUTED);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setPadding(dp(20), dp(60), dp(20), dp(20));
        emptyView.setVisibility(View.GONE);
        root.addView(emptyView);

        // RecyclerView
        recyclerView = new RecyclerView(requireContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TodoAdapter();
        recyclerView.setAdapter(adapter);
        LinearLayout.LayoutParams recyclerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        recyclerParams.setMargins(0, dp(8), 0, 0);
        recyclerView.setLayoutParams(recyclerParams);
        root.addView(recyclerView);

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
        loadTodos();
    }

    private void loadTodos() {
        if (api == null) return;
        api.getTodos(new HermesApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject result) {
                if (!isAdded()) return;
                todos.clear();
                JSONArray arr = result.optJSONArray("todos");
                if (arr == null) arr = new JSONArray();
                for (int i = 0; i < arr.length(); i++) {
                    try {
                        JSONObject obj = arr.getJSONObject(i);
                        String id = obj.optString("todo_id", obj.optString("id", ""));
                        String content = obj.optString("content", obj.optString("text", obj.optString("title", "")));
                        String status = obj.optString("status", "pending");
                        String priority = obj.optString("priority", "");
                        String dueDate = obj.optString("due_date", obj.optString("dueDate", ""));
                        if (!content.isEmpty()) {
                            todos.add(new TodoItem(id, content, status, priority, dueDate));
                        }
                    } catch (Exception ignored) {}
                }
                // Sort: in_progress first, then pending, then completed, then cancelled
                todos.sort((a, b) -> {
                    int orderA = getStatusOrder(a.status);
                    int orderB = getStatusOrder(b.status);
                    return Integer.compare(orderA, orderB);
                });
                adapter.notifyDataSetChanged();
                updateCount();
                updateEmpty();
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Failed to load todos: " + error, Toast.LENGTH_SHORT).show();
                updateEmpty();
            }
        });
    }

    private int getStatusOrder(String status) {
        if (status == null) return 0;
        switch (status.toLowerCase()) {
            case "in_progress": return 0;
            case "pending": return 1;
            case "completed": return 2;
            case "cancelled": return 3;
            default: return 1;
        }
    }

    private int currentFilter = 0; // 0=All, 1=Pending, 2=In Progress, 3=Completed, 4=Cancelled

    private void applyFilter(int filterIndex) {
        currentFilter = filterIndex;
        adapter.notifyDataSetChanged();
        updateCount();
        updateEmpty();
    }

    private List<TodoItem> getFilteredTodos() {
        if (currentFilter == 0) return todos;
        List<TodoItem> filtered = new ArrayList<>();
        for (TodoItem todo : todos) {
            switch (currentFilter) {
                case 1: if ("pending".equals(todo.status)) filtered.add(todo); break;
                case 2: if ("in_progress".equals(todo.status)) filtered.add(todo); break;
                case 3: if ("completed".equals(todo.status)) filtered.add(todo); break;
                case 4: if ("cancelled".equals(todo.status)) filtered.add(todo); break;
            }
        }
        return filtered;
    }

    private void updateEmpty() {
        if (emptyView != null && recyclerView != null) {
            List<TodoItem> filtered = getFilteredTodos();
            emptyView.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    private void updateCount() {
        if (countView != null) {
            List<TodoItem> filtered = getFilteredTodos();
            int total = todos.size();
            int pending = 0, inProgress = 0, completed = 0;
            for (TodoItem t : todos) {
                switch (t.status) {
                    case "pending": pending++; break;
                    case "in_progress": inProgress++; break;
                    case "completed": completed++; break;
                }
            }
            countView.setText(filtered.size() + " shown | " + pending + " pending, " +
                    inProgress + " active, " + completed + " done");
        }
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics());
    }

    // Data class
    private static class TodoItem {
        String id, content, status, priority, dueDate;
        TodoItem(String id, String content, String status, String priority, String dueDate) {
            this.id = id;
            this.content = content;
            this.status = status;
            this.priority = priority;
            this.dueDate = dueDate;
        }
    }

    // Adapter
    private class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout item = new LinearLayout(requireContext());
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setBackgroundColor(Color.WHITE);
            item.setPadding(dp(14), dp(12), dp(14), dp(12));
            item.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, dp(6));
            item.setLayoutParams(params);

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(8));
            bg.setColor(Color.WHITE);
            bg.setStroke(dp(1), COLOR_BORDER);
            item.setBackground(bg);

            // Checkbox
            CheckBox checkBox = new CheckBox(requireContext());
            checkBox.setId(android.R.id.checkbox);
            checkBox.setButtonTintList(android.content.res.ColorStateList.valueOf(COLOR_ACCENT));
            LinearLayout.LayoutParams cbParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cbParams.setMargins(0, 0, dp(8), 0);
            checkBox.setLayoutParams(cbParams);
            item.addView(checkBox);

            // Content column
            LinearLayout contentCol = new LinearLayout(requireContext());
            contentCol.setOrientation(LinearLayout.VERTICAL);
            contentCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            // Content text
            TextView contentView = new TextView(requireContext());
            contentView.setTextSize(15);
            contentView.setTextColor(COLOR_TEXT);
            contentView.setMaxLines(3);
            contentView.setId(android.R.id.text1);
            contentCol.addView(contentView);

            // Meta row (status + priority + due date)
            LinearLayout metaRow = new LinearLayout(requireContext());
            metaRow.setOrientation(LinearLayout.HORIZONTAL);
            metaRow.setGravity(Gravity.CENTER_VERTICAL);
            metaRow.setPadding(0, dp(3), 0, 0);

            TextView statusView = new TextView(requireContext());
            statusView.setTextSize(11);
            statusView.setTypeface(Typeface.DEFAULT_BOLD);
            statusView.setPadding(dp(6), dp(2), dp(6), dp(2));
            statusView.setId(android.R.id.text2);
            metaRow.addView(statusView);

            TextView priorityView = new TextView(requireContext());
            priorityView.setTextSize(11);
            priorityView.setTextColor(COLOR_MUTED);
            priorityView.setId(android.R.id.summary);
            LinearLayout.LayoutParams prioParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            prioParams.setMargins(dp(8), 0, 0, 0);
            priorityView.setLayoutParams(prioParams);
            metaRow.addView(priorityView);

            TextView dueView = new TextView(requireContext());
            dueView.setTextSize(11);
            dueView.setTextColor(COLOR_MUTED);
            dueView.setId(android.R.id.content);
            LinearLayout.LayoutParams dueParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            dueParams.setMargins(dp(8), 0, 0, 0);
            dueView.setLayoutParams(dueParams);
            metaRow.addView(dueView);

            contentCol.addView(metaRow);
            item.addView(contentCol);

            return new VH(item);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            List<TodoItem> filtered = getFilteredTodos();
            if (position >= filtered.size()) return;
            TodoItem todo = filtered.get(position);

            // Content
            holder.contentView.setText(todo.content);

            boolean isCompleted = "completed".equals(todo.status);
            boolean isCancelled = "cancelled".equals(todo.status);

            // Strikethrough for completed/cancelled
            if (isCompleted || isCancelled) {
                holder.contentView.setPaintFlags(holder.contentView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                holder.contentView.setTextColor(COLOR_MUTED);
            } else {
                holder.contentView.setPaintFlags(holder.contentView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                holder.contentView.setTextColor(COLOR_TEXT);
            }

            // Checkbox
            holder.checkBox.setOnCheckedChangeListener(null);
            holder.checkBox.setChecked(isCompleted);
            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // Visual feedback only - actual status update would need API call
                if (isChecked) {
                    holder.contentView.setPaintFlags(holder.contentView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    holder.contentView.setTextColor(COLOR_MUTED);
                } else {
                    holder.contentView.setPaintFlags(holder.contentView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                    holder.contentView.setTextColor(COLOR_TEXT);
                }
            });

            // Status badge
            holder.statusView.setText(formatStatus(todo.status));
            int statusColor = getStatusColor(todo.status);
            holder.statusView.setTextColor(statusColor);
            GradientDrawable statusBg = new GradientDrawable();
            statusBg.setCornerRadius(dp(4));
            statusBg.setColor(Color.argb(30, Color.red(statusColor), Color.green(statusColor), Color.blue(statusColor)));
            holder.statusView.setBackground(statusBg);

            // Priority
            if (todo.priority != null && !todo.priority.isEmpty()) {
                holder.priorityView.setText("Priority: " + todo.priority);
                holder.priorityView.setVisibility(View.VISIBLE);
            } else {
                holder.priorityView.setVisibility(View.GONE);
            }

            // Due date
            if (todo.dueDate != null && !todo.dueDate.isEmpty()) {
                holder.dueView.setText("Due: " + todo.dueDate);
                holder.dueView.setVisibility(View.VISIBLE);
            } else {
                holder.dueView.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return getFilteredTodos().size();
        }

        private String formatStatus(String status) {
            if (status == null) return "Pending";
            switch (status.toLowerCase()) {
                case "in_progress": return "In Progress";
                case "completed": return "Completed";
                case "cancelled": return "Cancelled";
                default: return "Pending";
            }
        }

        private int getStatusColor(String status) {
            if (status == null) return COLOR_MUTED;
            switch (status.toLowerCase()) {
                case "in_progress": return Color.parseColor("#E65100");
                case "completed": return Color.parseColor("#2E7D32");
                case "cancelled": return Color.parseColor("#C62828");
                default: return COLOR_ACCENT;
            }
        }

        class VH extends RecyclerView.ViewHolder {
            CheckBox checkBox;
            TextView contentView, statusView, priorityView, dueView;
            VH(View v) {
                super(v);
                checkBox = v.findViewById(android.R.id.checkbox);
                contentView = v.findViewById(android.R.id.text1);
                statusView = v.findViewById(android.R.id.text2);
                priorityView = v.findViewById(android.R.id.summary);
                dueView = v.findViewById(android.R.id.content);
            }
        }
    }
}
