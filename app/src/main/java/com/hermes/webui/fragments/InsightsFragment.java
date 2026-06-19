package com.hermes.webui.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
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

public class InsightsFragment extends Fragment {

    private RecyclerView recyclerView;
    private InsightAdapter adapter;
    private final List<InsightItem> insights = new ArrayList<>();
    private String selectedPeriod = "7d";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_insights, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_insights);
        if (recyclerView == null) return;

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new InsightAdapter();
        recyclerView.setAdapter(adapter);

        Spinner periodSpinner = view.findViewById(R.id.spinner_period);
        if (periodSpinner != null) {
            String[] periods = {"7d", "30d", "90d", "All"};
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_item, periods);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            periodSpinner.setAdapter(spinnerAdapter);
            periodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                    selectedPeriod = periods[position];
                    loadInsights();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        loadInsights();
    }

    private void loadInsights() {
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;

        HermesApi api = activity.getApi();
        if (api == null) return;

        api.getInsights(selectedPeriod, new HermesApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                insights.clear();
                try {
                    JSONObject stats = response.optJSONObject("stats");
                    if (stats != null) {
                        insights.add(new InsightItem("Total Sessions",
                                String.valueOf(stats.optInt("sessions", 0))));
                        insights.add(new InsightItem("Total Tokens Used",
                                formatNumber(stats.optLong("tokens", 0))));
                        insights.add(new InsightItem("Average Tokens per Session",
                                formatNumber(stats.optLong("avgTokens", 0))));
                        insights.add(new InsightItem("Total Messages",
                                String.valueOf(stats.optInt("messages", 0))));
                        insights.add(new InsightItem("Active Days",
                                String.valueOf(stats.optInt("activeDays", 0))));
                    }

                    JSONArray breakdown = response.optJSONArray("breakdown");
                    if (breakdown != null) {
                        for (int i = 0; i < breakdown.length(); i++) {
                            JSONObject obj = breakdown.getJSONObject(i);
                            insights.add(new InsightItem(
                                    obj.optString("label", ""),
                                    obj.optString("value", "0")
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

    private String formatNumber(long num) {
        if (num >= 1_000_000) return String.format("%.1fM", num / 1_000_000.0);
        if (num >= 1_000) return String.format("%.1fK", num / 1_000.0);
        return String.valueOf(num);
    }

    private static class InsightItem {
        final String label;
        final String value;

        InsightItem(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }

    private class InsightAdapter extends RecyclerView.Adapter<InsightAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_task, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            InsightItem item = insights.get(position);
            holder.title.setText(item.label);
            holder.schedule.setText(item.value);
            holder.status.setVisibility(View.GONE);
        }

        @Override
        public int getItemCount() {
            return insights.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView title;
            final TextView schedule;
            final TextView status;

            ViewHolder(View view) {
                super(view);
                title = view.findViewById(R.id.text_task_title);
                schedule = view.findViewById(R.id.text_task_schedule);
                status = view.findViewById(R.id.text_task_status);
            }
        }
    }
}
