package com.hermes.webui.fragments;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.hermes.webui.api.HermesApi;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SkillsFragment extends Fragment {
    private static final String TAG = "SkillsFragment";

    private static final int COLOR_BG = Color.parseColor("#FEFCF7");
    private static final int COLOR_SIDEBAR = Color.parseColor("#FAF7F0");
    private static final int COLOR_BORDER = Color.parseColor("#E0D8C8");
    private static final int COLOR_TEXT = Color.parseColor("#1A1610");
    private static final int COLOR_MUTED = Color.parseColor("#5C5344");
    private static final int COLOR_ACCENT = Color.parseColor("#B8860B");

    private RecyclerView recyclerView;
    private EditText searchInput;
    private TextView emptyView;
    private TextView countView;
    private HermesApi api;

    private final List<SkillItem> allSkills = new ArrayList<>();
    private final List<SkillItem> filteredSkills = new ArrayList<>();
    private SkillAdapter adapter;

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
        title.setText("Skills");
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
        btnRefresh.setOnClickListener(v -> loadSkills());
        header.addView(btnRefresh);

        root.addView(header);

        // Search bar
        searchInput = new EditText(requireContext());
        searchInput.setHint("Search skills...");
        searchInput.setTextColor(COLOR_TEXT);
        searchInput.setHintTextColor(COLOR_MUTED);
        searchInput.setBackgroundColor(Color.WHITE);
        searchInput.setPadding(dp(14), dp(10), dp(14), dp(10));
        searchInput.setTextSize(15);
        searchInput.setSingleLine(true);
        GradientDrawable searchBg = new GradientDrawable();
        searchBg.setCornerRadius(dp(8));
        searchBg.setColor(Color.WHITE);
        searchBg.setStroke(dp(1), COLOR_BORDER);
        searchInput.setBackground(searchBg);
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchParams.setMargins(0, 0, 0, dp(12));
        searchInput.setLayoutParams(searchParams);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                filterSkills(s.toString());
            }
        });
        root.addView(searchInput);

        // Divider
        View divider = new View(requireContext());
        divider.setBackgroundColor(COLOR_BORDER);
        divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        root.addView(divider);

        // Empty view
        emptyView = new TextView(requireContext());
        emptyView.setText("No skills found.");
        emptyView.setTextSize(16);
        emptyView.setTextColor(COLOR_MUTED);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setPadding(dp(20), dp(60), dp(20), dp(20));
        emptyView.setVisibility(View.GONE);
        root.addView(emptyView);

        // RecyclerView
        recyclerView = new RecyclerView(requireContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SkillAdapter();
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
        loadSkills();
    }

    private void loadSkills() {
        if (api == null) return;
        api.getSkills(new HermesApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject result) {
                if (!isAdded()) return;
                allSkills.clear();
                JSONArray arr = result.optJSONArray("skills");
                if (arr == null) arr = new JSONArray();
                for (int i = 0; i < arr.length(); i++) {
                    try {
                        JSONObject obj = arr.getJSONObject(i);
                        allSkills.add(new SkillItem(
                                obj.optString("name", ""),
                                obj.optString("category", obj.optString("type", "")),
                                obj.optString("description", obj.optString("desc", "")),
                                obj.optString("version", ""),
                                obj.optBoolean("enabled", true)
                        ));
                    } catch (Exception ignored) {}
                }
                filterSkills(searchInput != null ? searchInput.getText().toString() : "");
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Failed to load skills: " + error, Toast.LENGTH_SHORT).show();
                updateEmpty();
            }
        });
    }

    private void filterSkills(String query) {
        filteredSkills.clear();
        if (query.isEmpty()) {
            filteredSkills.addAll(allSkills);
        } else {
            String lower = query.toLowerCase();
            for (SkillItem skill : allSkills) {
                if (skill.name.toLowerCase().contains(lower) ||
                        skill.category.toLowerCase().contains(lower) ||
                        skill.description.toLowerCase().contains(lower)) {
                    filteredSkills.add(skill);
                }
            }
        }
        adapter.notifyDataSetChanged();
        updateCount();
        updateEmpty();
    }

    private void showSkillDetail(SkillItem skill) {
        if (!isAdded()) return;
        if (api == null) return;

        api.getSkillDetail(skill.name, new HermesApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject result) {
                if (!isAdded()) return;
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
                builder.setTitle(skill.name);

                LinearLayout layout = new LinearLayout(requireContext());
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(dp(24), dp(16), dp(24), dp(16));

                // Category
                if (!skill.category.isEmpty()) {
                    addDetailRow(layout, "Category", skill.category);
                }

                // Version
                if (!skill.version.isEmpty()) {
                    addDetailRow(layout, "Version", skill.version);
                }

                // Description
                String desc = result.optString("description", skill.description);
                if (!desc.isEmpty()) {
                    TextView descLabel = new TextView(requireContext());
                    descLabel.setText("Description");
                    descLabel.setTextSize(13);
                    descLabel.setTextColor(COLOR_MUTED);
                    descLabel.setTypeface(Typeface.DEFAULT_BOLD);
                    descLabel.setPadding(0, dp(12), 0, dp(4));
                    layout.addView(descLabel);

                    TextView descView = new TextView(requireContext());
                    descView.setText(desc);
                    descView.setTextSize(14);
                    descView.setTextColor(COLOR_TEXT);
                    layout.addView(descView);
                }

                // Instructions/content if available
                String instructions = result.optString("instructions", result.optString("content", ""));
                if (!instructions.isEmpty()) {
                    TextView instrLabel = new TextView(requireContext());
                    instrLabel.setText("Instructions");
                    instrLabel.setTextSize(13);
                    instrLabel.setTextColor(COLOR_MUTED);
                    instrLabel.setTypeface(Typeface.DEFAULT_BOLD);
                    instrLabel.setPadding(0, dp(12), 0, dp(4));
                    layout.addView(instrLabel);

                    TextView instrView = new TextView(requireContext());
                    instrView.setText(instructions);
                    instrView.setTextSize(13);
                    instrView.setTextColor(COLOR_TEXT);
                    layout.addView(instrView);
                }

                builder.setView(layout);
                builder.setPositiveButton("Close", null);
                builder.show();
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                // Show basic info even if detail fails
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
                builder.setTitle(skill.name);
                builder.setMessage(skill.description.isEmpty() ? "No description available." : skill.description);
                builder.setPositiveButton("Close", null);
                builder.show();
            }
        });
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
            emptyView.setVisibility(filteredSkills.isEmpty() ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(filteredSkills.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    private void updateCount() {
        if (countView != null) {
            countView.setText(filteredSkills.size() + " of " + allSkills.size() + " skills");
        }
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics());
    }

    // Data class
    private static class SkillItem {
        String name, category, description, version;
        boolean enabled;
        SkillItem(String name, String category, String description, String version, boolean enabled) {
            this.name = name;
            this.category = category;
            this.description = description;
            this.version = version;
            this.enabled = enabled;
        }
    }

    // Adapter
    private class SkillAdapter extends RecyclerView.Adapter<SkillAdapter.VH> {
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

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(8));
            bg.setColor(Color.WHITE);
            bg.setStroke(dp(1), COLOR_BORDER);
            item.setBackground(bg);

            // Row 1: name + category badge
            LinearLayout row1 = new LinearLayout(requireContext());
            row1.setOrientation(LinearLayout.HORIZONTAL);
            row1.setGravity(Gravity.CENTER_VERTICAL);

            TextView nameView = new TextView(requireContext());
            nameView.setTextSize(16);
            nameView.setTextColor(COLOR_TEXT);
            nameView.setTypeface(Typeface.DEFAULT_BOLD);
            nameView.setMaxLines(1);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            nameView.setLayoutParams(nameParams);
            nameView.setId(android.R.id.text1);
            row1.addView(nameView);

            TextView categoryView = new TextView(requireContext());
            categoryView.setTextSize(11);
            categoryView.setTextColor(COLOR_ACCENT);
            categoryView.setTypeface(Typeface.DEFAULT_BOLD);
            categoryView.setPadding(dp(8), dp(3), dp(8), dp(3));
            categoryView.setId(android.R.id.text2);
            row1.addView(categoryView);

            item.addView(row1);

            // Description
            TextView descView = new TextView(requireContext());
            descView.setTextSize(13);
            descView.setTextColor(COLOR_MUTED);
            descView.setMaxLines(2);
            descView.setPadding(0, dp(4), 0, 0);
            descView.setId(android.R.id.summary);
            item.addView(descView);

            // Version + enabled
            LinearLayout row3 = new LinearLayout(requireContext());
            row3.setOrientation(LinearLayout.HORIZONTAL);
            row3.setGravity(Gravity.CENTER_VERTICAL);
            row3.setPadding(0, dp(4), 0, 0);

            TextView versionView = new TextView(requireContext());
            versionView.setTextSize(12);
            versionView.setTextColor(COLOR_MUTED);
            versionView.setId(android.R.id.content);
            row3.addView(versionView);

            TextView statusView = new TextView(requireContext());
            statusView.setTextSize(11);
            statusView.setTypeface(Typeface.DEFAULT_BOLD);
            statusView.setId(android.R.id.icon);
            LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            statusParams.setMargins(dp(12), 0, 0, 0);
            statusView.setLayoutParams(statusParams);
            row3.addView(statusView);

            item.addView(row3);

            return new VH(item);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            SkillItem skill = filteredSkills.get(position);
            holder.nameView.setText(skill.name);
            holder.categoryView.setText(skill.category.isEmpty() ? "general" : skill.category);
            holder.descView.setText(skill.description.isEmpty() ? "No description" : skill.description);
            holder.versionView.setText(skill.version.isEmpty() ? "" : "v" + skill.version);

            holder.statusView.setText(skill.enabled ? "Enabled" : "Disabled");
            holder.statusView.setTextColor(skill.enabled ? Color.parseColor("#2E7D32") : Color.parseColor("#C62828"));

            holder.itemView.setOnClickListener(v -> showSkillDetail(skill));
        }

        @Override
        public int getItemCount() {
            return filteredSkills.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView nameView, categoryView, descView, versionView, statusView;
            VH(View v) {
                super(v);
                nameView = v.findViewById(android.R.id.text1);
                categoryView = v.findViewById(android.R.id.text2);
                descView = v.findViewById(android.R.id.summary);
                versionView = v.findViewById(android.R.id.content);
                statusView = v.findViewById(android.R.id.icon);
            }
        }
    }
}
