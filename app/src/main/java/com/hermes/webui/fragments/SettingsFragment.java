package com.hermes.webui.fragments;

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

public class SettingsFragment extends Fragment {

    private RecyclerView recyclerView;
    private SettingsAdapter adapter;
    private final List<SettingItem> settings = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_settings);
        if (recyclerView == null) return;

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SettingsAdapter();
        recyclerView.setAdapter(adapter);

        loadSettings();
    }

    private void loadSettings() {
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;

        HermesApi api = activity.getApi();
        if (api == null) return;

        api.getSettings(new HermesApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                settings.clear();
                try {
                    JSONArray data = response.optJSONArray("settings");
                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject obj = data.getJSONObject(i);
                            settings.add(new SettingItem(
                                    obj.optString("key", ""),
                                    obj.optString("value", "")
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
            public void onError(String error) {
                if (!isAdded()) return;
                Log.e("Hermes", error);
            }
        });
    }

    private static class SettingItem {
        final String key;
        final String value;

        SettingItem(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    private class SettingsAdapter extends RecyclerView.Adapter<SettingsAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_memory, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SettingItem item = settings.get(position);
            holder.target.setText(item.key);
            holder.content.setText(item.value);
        }

        @Override
        public int getItemCount() {
            return settings.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView target;
            final TextView content;

            ViewHolder(View view) {
                super(view);
                target = view.findViewById(R.id.text_memory_target);
                content = view.findViewById(R.id.text_memory_content);
            }
        }
    }
}
