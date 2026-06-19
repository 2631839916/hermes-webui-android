package com.hermes.webui.fragments;

import android.os.Bundle;
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

public class WorkspacesFragment extends Fragment {

    private RecyclerView recyclerView;
    private WorkspaceAdapter adapter;
    private final List<WorkspaceItem> workspaces = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_workspaces, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_workspaces);
        if (recyclerView == null) return;

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new WorkspaceAdapter();
        recyclerView.setAdapter(adapter);

        loadWorkspaces();
    }

    private void loadWorkspaces() {
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;

        HermesApi api = activity.getApi();
        if (api == null) return;

        api.getWorkspaces(new HermesApi.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                workspaces.clear();
                try {
                    JSONArray data = response.optJSONArray("workspaces");
                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject obj = data.getJSONObject(i);
                            workspaces.add(new WorkspaceItem(
                                    obj.optString("name", "Unknown"),
                                    obj.optString("path", ""),
                                    obj.optBoolean("active", false)
                            ));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
                }
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                e.printStackTrace();
            }
        });
    }

    private static class WorkspaceItem {
        final String name;
        final String path;
        final boolean active;

        WorkspaceItem(String name, String path, boolean active) {
            this.name = name;
            this.path = path;
            this.active = active;
        }
    }

    private class WorkspaceAdapter extends RecyclerView.Adapter<WorkspaceAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_kanban, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            WorkspaceItem item = workspaces.get(position);
            holder.title.setText(item.name);
            holder.status.setText(item.active ? "Active" : "Inactive");
            holder.assignee.setText(item.path);
        }

        @Override
        public int getItemCount() {
            return workspaces.size();
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
