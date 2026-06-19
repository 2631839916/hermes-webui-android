package com.hermes.webui.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class ProfilesFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProfileAdapter adapter;
    private final List<ProfileItem> profiles = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profiles, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_profiles);
        if (recyclerView == null) return;

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ProfileAdapter();
        recyclerView.setAdapter(adapter);

        loadProfiles();
    }

    private void loadProfiles() {
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;

        HermesApi api = activity.getApi();
        if (api == null) return;

        api.getProfiles(new HermesApi.ApiCallback<JSONArray>() {
            @Override
            public void onSuccess(JSONArray response) {
                if (!isAdded()) return;
                profiles.clear();
                try {
                    JSONArray data = response.optJSONArray("profiles");
                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject obj = data.getJSONObject(i);
                            profiles.add(new ProfileItem(
                                    obj.optString("name", "default"),
                                    obj.optBoolean("active", false)
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

    private void switchProfile(String profileName) {
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;

        HermesApi api = activity.getApi();
        if (api == null) return;

        Toast.makeText(requireContext(), "Profile switch: " + profileName, Toast.LENGTH_SHORT).show(); api.getProfiles( new HermesApi.ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONArray response) {
                if (!isAdded()) return;
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Switched to profile: " + profileName,
                                Toast.LENGTH_SHORT).show();
                        loadProfiles();
                    });
                }
            }

            @Override
            public void onError(String e) {
                if (!isAdded()) return;
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Failed to switch profile",
                                    Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private static class ProfileItem {
        final String name;
        final boolean active;

        ProfileItem(String name, boolean active) {
            this.name = name;
            this.active = active;
        }
    }

    private class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_kanban, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ProfileItem item = profiles.get(position);
            holder.title.setText(item.name);
            holder.status.setText(item.active ? "Active" : "Inactive");
            holder.assignee.setVisibility(View.GONE);

            holder.itemView.setOnClickListener(v -> switchProfile(item.name));
        }

        @Override
        public int getItemCount() {
            return profiles.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView title;
            final TextView status;
            final TextView assignee;

            ViewHolder(View view) {
                super(view);
                title = view.findViewById(R.id.text_kanban_title);
                status = view.findViewById(R.id.text_kanban_status);
                assignee = view.findViewById(R.id.text_kanban_assignee);
            }
        }
    }
}
