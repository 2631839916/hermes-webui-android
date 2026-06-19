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

public class MemoryFragment extends Fragment {
    private static final String TAG = "MemoryFragment";

    private static final int COLOR_BG = Color.parseColor("#FEFCF7");
    private static final int COLOR_BORDER = Color.parseColor("#E0D8C8");
    private static final int COLOR_TEXT = Color.parseColor("#1A1610");
    private static final int COLOR_MUTED = Color.parseColor("#5C5344");
    private static final int COLOR_ACCENT = Color.parseColor("#B8860B");

    private RecyclerView recyclerView;
    private TextView emptyView;
    private TextView countView;
    private HermesApi api;

    private final List<MemoryItem> memories = new ArrayList<>();
    private MemoryAdapter adapter;

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
        title.setText(getString(R.string.panel_memory));
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
        btnRefresh.setText(getString(R.string.refresh));
        btnRefresh.setTextColor(COLOR_ACCENT);
        btnRefresh.setTextSize(14);
        btnRefresh.setTypeface(Typeface.DEFAULT_BOLD);
        btnRefresh.setPadding(dp(12), dp(6), 0, dp(6));
        btnRefresh.setOnClickListener(v -> loadMemory());
        header.addView(btnRefresh);

        root.addView(header);

        // Description
        TextView desc = new TextView(requireContext());
        desc.setText("Hermes memory entries: facts, preferences, and context stored across sessions.");
        desc.setTextSize(13);
        desc.setTextColor(COLOR_MUTED);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        descParams.setMargins(0, 0, 0, dp(12));
        desc.setLayoutParams(descParams);
        root.addView(desc);

        // Divider
        View divider = new View(requireContext());
        divider.setBackgroundColor(COLOR_BORDER);
        divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        root.addView(divider);

        // Empty view
        emptyView = new TextView(requireContext());
        emptyView.setText("No memory entries found.\nMemories are created as Hermes interacts with you.");
        emptyView.setTextSize(16);
        emptyView.setTextColor(COLOR_MUTED);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setPadding(dp(20), dp(60), dp(20), dp(20));
        emptyView.setVisibility(View.GONE);
        root.addView(emptyView);

        // RecyclerView
        recyclerView = new RecyclerView(requireContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MemoryAdapter();
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
        loadMemory();
    }

    private void loadMemory() {
        if (api == null) return;
        api.getMemory(new HermesApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject result) {
                if (!isAdded()) return;
                memories.clear();
                JSONArray arr = result.optJSONArray("memories");
                if (arr == null) arr = new JSONArray();
                for (int i = 0; i < arr.length(); i++) {
                    try {
                        JSONObject obj = arr.getJSONObject(i);
                        String content = obj.optString("content", obj.optString("text", ""));
                        String target = obj.optString("target", obj.optString("scope", "memory"));
                        String key = obj.optString("key", obj.optString("name", ""));
                        String category = obj.optString("category", obj.optString("type", ""));
                        String timestamp = obj.optString("timestamp", obj.optString("created_at", ""));
                        if (!content.isEmpty()) {
                            memories.add(new MemoryItem(content, target, key, category, timestamp));
                        }
                    } catch (Exception ignored) {}
                }
                adapter.notifyDataSetChanged();
                updateCount();
                updateEmpty();
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Failed to load memory: " + error, Toast.LENGTH_SHORT).show();
                updateEmpty();
            }
        });
    }

    private void showMemoryDetail(MemoryItem memory) {
        if (!isAdded()) return;
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle(memory.key.isEmpty() ? "Memory Entry" : memory.key);

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(24), dp(16), dp(24), dp(16));

        // Target
        addDetailRow(layout, "Target", memory.target.isEmpty() ? "memory" : memory.target);

        // Category
        if (!memory.category.isEmpty()) {
            addDetailRow(layout, "Category", memory.category);
        }

        // Timestamp
        if (!memory.timestamp.isEmpty()) {
            addDetailRow(layout, getString(R.string.created), memory.timestamp);
        }

        // Content
        TextView contentLabel = new TextView(requireContext());
        contentLabel.setText("Content");
        contentLabel.setTextSize(13);
        contentLabel.setTextColor(COLOR_MUTED);
        contentLabel.setTypeface(Typeface.DEFAULT_BOLD);
        contentLabel.setPadding(0, dp(12), 0, dp(4));
        layout.addView(contentLabel);

        TextView contentView = new TextView(requireContext());
        contentView.setText(memory.content);
        contentView.setTextSize(14);
        contentView.setTextColor(COLOR_TEXT);
        contentView.setLineSpacing(dp(2), 1.1f);
        layout.addView(contentView);

        builder.setView(layout);
        builder.setPositiveButton("Close", null);
        builder.show();
    }

    private void addDetailRow(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, dp(4));

        TextView labelView = new TextView(requireContext());
        labelView.setText(label + ": ");
        labelView.setTextSize(14);
        labelView.setTextColor(COLOR_MUTED);
        labelView.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(labelView);

        TextView valueView = new TextView(requireContext());
        valueView.setText(value);
        valueView.setTextSize(14);
        valueView.setTextColor(COLOR_TEXT);
        row.addView(valueView);

        parent.addView(row);
    }

    private void updateEmpty() {
        if (emptyView != null && recyclerView != null) {
            emptyView.setVisibility(memories.isEmpty() ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(memories.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    private void updateCount() {
        if (countView != null) {
            countView.setText(memories.size() + " entries");
        }
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics());
    }

    // Data class
    private static class MemoryItem {
        String content, target, key, category, timestamp;
        MemoryItem(String content, String target, String key, String category, String timestamp) {
            this.content = content;
            this.target = target;
            this.key = key;
            this.category = category;
            this.timestamp = timestamp;
        }
    }

    // Adapter
    private class MemoryAdapter extends RecyclerView.Adapter<MemoryAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout item = new LinearLayout(requireContext());
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setBackgroundColor(Color.WHITE);
            item.setPadding(dp(16), dp(14), dp(16), dp(14));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, dp(8));
            item.setLayoutParams(params);

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(8));
            bg.setColor(Color.WHITE);
            bg.setStroke(dp(1), COLOR_BORDER);
            item.setBackground(bg);

            // Target badge column
            LinearLayout badgeCol = new LinearLayout(requireContext());
            badgeCol.setOrientation(LinearLayout.VERTICAL);
            badgeCol.setGravity(Gravity.CENTER);
            badgeCol.setPadding(0, 0, dp(12), 0);

            TextView targetBadge = new TextView(requireContext());
            targetBadge.setTextSize(11);
            targetBadge.setTextColor(Color.WHITE);
            targetBadge.setTypeface(Typeface.DEFAULT_BOLD);
            targetBadge.setGravity(Gravity.CENTER);
            targetBadge.setPadding(dp(6), dp(3), dp(6), dp(3));
            targetBadge.setId(android.R.id.icon);
            badgeCol.addView(targetBadge);

            item.addView(badgeCol);

            // Content column
            LinearLayout contentCol = new LinearLayout(requireContext());
            contentCol.setOrientation(LinearLayout.VERTICAL);
            contentCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            // Key/title
            TextView keyView = new TextView(requireContext());
            keyView.setTextSize(14);
            keyView.setTextColor(COLOR_TEXT);
            keyView.setTypeface(Typeface.DEFAULT_BOLD);
            keyView.setMaxLines(1);
            keyView.setId(android.R.id.text1);
            contentCol.addView(keyView);

            // Content preview
            TextView contentView = new TextView(requireContext());
            contentView.setTextSize(13);
            contentView.setTextColor(COLOR_MUTED);
            contentView.setMaxLines(2);
            contentView.setPadding(0, dp(2), 0, 0);
            contentView.setId(android.R.id.text2);
            contentCol.addView(contentView);

            // Category + timestamp row
            LinearLayout metaRow = new LinearLayout(requireContext());
            metaRow.setOrientation(LinearLayout.HORIZONTAL);
            metaRow.setPadding(0, dp(4), 0, 0);

            TextView categoryView = new TextView(requireContext());
            categoryView.setTextSize(11);
            categoryView.setTextColor(COLOR_ACCENT);
            categoryView.setId(android.R.id.summary);
            metaRow.addView(categoryView);

            TextView timestampView = new TextView(requireContext());
            timestampView.setTextSize(11);
            timestampView.setTextColor(COLOR_MUTED);
            timestampView.setId(android.R.id.content);
            LinearLayout.LayoutParams tsParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            tsParams.setMargins(dp(8), 0, 0, 0);
            timestampView.setLayoutParams(tsParams);
            metaRow.addView(timestampView);

            contentCol.addView(metaRow);
            item.addView(contentCol);

            return new VH(item);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            MemoryItem memory = memories.get(position);

            // Target badge
            String target = memory.target.isEmpty() ? "memory" : memory.target;
            holder.targetBadge.setText(target.substring(0, Math.min(3, target.length())).toUpperCase());
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setCornerRadius(dp(4));
            badgeBg.setColor(target.equalsIgnoreCase("user") ?
                    Color.parseColor("#1565C0") : Color.parseColor("#6A1B9A"));
            holder.targetBadge.setBackground(badgeBg);

            // Key
            holder.keyView.setText(memory.key.isEmpty() ? "Memory #" + (position + 1) : memory.key);

            // Content preview
            String preview = memory.content.length() > 120 ?
                    memory.content.substring(0, 120) + "..." : memory.content;
            holder.contentView.setText(preview);

            // Category
            holder.categoryView.setText(memory.category.isEmpty() ? "" : memory.category);

            // Timestamp
            String ts = memory.timestamp;
            if (ts.length() > 19) ts = ts.substring(0, 19);
            holder.timestampView.setText(ts);

            holder.itemView.setOnClickListener(v -> showMemoryDetail(memory));
        }

        @Override
        public int getItemCount() {
            return memories.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView targetBadge, keyView, contentView, categoryView, timestampView;
            VH(View v) {
                super(v);
                targetBadge = v.findViewById(android.R.id.icon);
                keyView = v.findViewById(android.R.id.text1);
                contentView = v.findViewById(android.R.id.text2);
                categoryView = v.findViewById(android.R.id.summary);
                timestampView = v.findViewById(android.R.id.content);
            }
        }
    }
}
