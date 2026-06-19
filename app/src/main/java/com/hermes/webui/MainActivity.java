package com.hermes.webui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "HermesWebUI";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate 开始");

        try {
            setContentView(R.layout.activity_main);
            Log.d(TAG, "setContentView 成功");

            // 测试基本控件
            setupBasicUI();
            Log.d(TAG, "setupBasicUI 成功");

        } catch (Exception e) {
            Log.e(TAG, "错误: " + e.getMessage(), e);
            // 显示错误信息
            TextView tv = new TextView(this);
            tv.setText("错误: " + e.getMessage());
            tv.setPadding(50, 50, 50, 50);
            tv.setTextSize(16);
            setContentView(tv);
        }
    }

    private void setupBasicUI() {
        try {
            // 找到关键控件
            DrawerLayout drawer = findViewById(R.id.drawerLayout);
            Log.d(TAG, "drawerLayout: " + (drawer != null ? "找到" : "未找到"));

            RecyclerView msgList = findViewById(R.id.messageList);
            Log.d(TAG, "messageList: " + (msgList != null ? "找到" : "未找到"));

            EditText input = findViewById(R.id.inputMessage);
            Log.d(TAG, "inputMessage: " + (input != null ? "找到" : "未找到"));

            ImageButton send = findViewById(R.id.btnSend);
            Log.d(TAG, "btnSend: " + (send != null ? "找到" : "未找到"));

            // 设置发送按钮
            if (send != null && input != null && msgList != null) {
                List<String> messages = new ArrayList<>();
                messages.add("Hermes: 你好！我是 Hermes，你的 AI 助手。");

                RecyclerView.Adapter adapter = new RecyclerView.Adapter() {
                    @Override
                    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                        TextView tv = new TextView(parent.getContext());
                        tv.setPadding(30, 20, 30, 20);
                        tv.setTextSize(16);
                        return new RecyclerView.ViewHolder(tv) {};
                    }

                    @Override
                    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                        ((TextView) holder.itemView).setText(messages.get(position));
                    }

                    @Override
                    public int getItemCount() {
                        return messages.size();
                    }
                };

                msgList.setLayoutManager(new LinearLayoutManager(this));
                msgList.setAdapter(adapter);

                send.setOnClickListener(v -> {
                    String text = input.getText().toString().trim();
                    if (!text.isEmpty()) {
                        messages.add("你: " + text);
                        messages.add("Hermes: 收到！");
                        adapter.notifyDataSetChanged();
                        input.setText("");
                        msgList.scrollToPosition(messages.size() - 1);
                    }
                });

                Log.d(TAG, "UI 设置完成");
            }
        } catch (Exception e) {
            Log.e(TAG, "setupBasicUI 错误: " + e.getMessage(), e);
            throw e;
        }
    }
}
