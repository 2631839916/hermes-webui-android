package com.hermes.webui.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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

public class LogsFragment extends Fragment {

    private RecyclerView recyclerView;
    private LogAdapter adapter;
    private final List<LogEntry> logs = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_logs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_logs);
        if (recyclerView == null) return;

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new LogAdapter();
        recyclerView.setAdapter(adapter);

        loadLogs();
    }

    private void loadLogs() {
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;

        HermesApi api = activity.getApi();
        if (api == null) return;

        api.getLogs(new HermesApi.ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                logs.clear();
                try {
                    JSONArray data = response.optJSONArray("logs");
                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject obj = data.getJSONObject(i);
                            logs.add(new LogEntry(
                                    obj.optString("timestamp", ""),
                                    obj.optString("level", "info"),
                                    obj.optString("message", "")
                            ));
                        }
                    }
                } catch (Exception e) {
                    Log.e("Hermes", e);
                }
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
                }
            }

            @Override
            public void onError(String e) {
                if (!isAdded()) return;
                Log.e("Hermes", e);
            }
        });
    }

    private static int getLevelColor(String level) {
        if (level == null) return Color.GRAY;
        switch (level.toLowerCase()) {
            case "error":
                return Color.RED;
            case "warn":
            case "warning":
                return Color.parseColor("#FF8C00");
            case "info":
            default:
                return Color.GRAY;
        }
    }

    private static class LogEntry {
        final String timestamp;
        final String level;
        final String message;

        LogEntry(String timestamp, String level, String message) {
            this.timestamp = timestamp;
            this.level = level;
            this.message = message;
        }
    }

    private class LogAdapter extends RecyclerView.Adapter<LogAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_log, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            LogEntry entry = logs.get(position);
            holder.timestamp.setText(entry.timestamp);
            holder.level.setText(entry.level.toUpperCase());
            holder.level.setTextColor(getLevelColor(entry.level));
            holder.message.setText(entry.message);
        }

        @Override
        public int getItemCount() {
            return logs.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView timestamp;
            final TextView level;
            final TextView message;

            ViewHolder(View view) {
                super(view);
                timestamp = view.findViewById(R.id.text_log_timestamp);
                level = view.findViewById(R.id.text_log_level);
                message = view.findViewById(R.id.text_log_message);
            }
        }
    }
}
