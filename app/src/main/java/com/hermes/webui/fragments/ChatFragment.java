package com.hermes.webui.fragments;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.hermes.webui.MainActivity;
import com.hermes.webui.R;
import com.hermes.webui.api.HermesApi;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {
    private static final String TAG = "HermesChat";

    private static final int COLOR_BG = Color.parseColor("#FEFCF7");
    private static final int COLOR_SIDEBAR = Color.parseColor("#FAF7F0");
    private static final int COLOR_BORDER = Color.parseColor("#E0D8C8");
    private static final int COLOR_TEXT = Color.parseColor("#1A1610");
    private static final int COLOR_MUTED = Color.parseColor("#5C5344");
    private static final int COLOR_ACCENT = Color.parseColor("#B8860B");
    private static final int COLOR_USER_BG = Color.parseColor("#E8E0D0");
    private static final int COLOR_AI_BG = Color.parseColor("#F5F0E8");

    private LinearLayout rootLayout;
    private TextView statusText;
    private View statusDot;
    private ScrollView chatScrollView;
    private LinearLayout messageContainer;
    private EditText inputMessage;
    private ImageButton btnSend;
    private ProgressBar typingIndicator;

    private HermesApi api;
    private String currentSessionId;
    private boolean isStreaming = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootLayout = new LinearLayout(requireContext());
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(COLOR_BG);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // === 状态栏 ===
        LinearLayout statusBar = new LinearLayout(requireContext());
        statusBar.setOrientation(LinearLayout.HORIZONTAL);
        statusBar.setBackgroundColor(Color.parseColor("#16213e"));
        statusBar.setPadding(dp(12), dp(10), dp(12), dp(10));
        statusBar.setGravity(Gravity.CENTER_VERTICAL);
        statusBar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 连接灯
        statusDot = new View(requireContext());
        statusDot.setBackgroundColor(Color.RED);
        LinearLayout.LayoutParams dotP = new LinearLayout.LayoutParams(dp(12), dp(12));
        dotP.setMargins(0, 0, dp(8), 0);
        statusDot.setLayoutParams(dotP);
        statusBar.addView(statusDot);

        // 状态文字
        statusText = new TextView(requireContext());
        statusText.setText("未连接");
        statusText.setTextSize(13);
        statusText.setTextColor(Color.WHITE);
        statusBar.addView(statusText);

        rootLayout.addView(statusBar);

        // === 消息区域 ===
        chatScrollView = new ScrollView(requireContext());
        chatScrollView.setFillViewport(true);
        LinearLayout.LayoutParams scrollP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        chatScrollView.setLayoutParams(scrollP);

        messageContainer = new LinearLayout(requireContext());
        messageContainer.setOrientation(LinearLayout.VERTICAL);
        messageContainer.setPadding(dp(16), dp(12), dp(16), dp(12));
        chatScrollView.addView(messageContainer, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        rootLayout.addView(chatScrollView);

        // === 打字指示器 ===
        typingIndicator = new ProgressBar(requireContext());
        typingIndicator.setIndeterminate(true);
        LinearLayout.LayoutParams tip = new LinearLayout.LayoutParams(dp(24), dp(24));
        tip.gravity = Gravity.START;
        tip.setMargins(dp(16), 0, 0, dp(8));
        typingIndicator.setLayoutParams(tip);
        typingIndicator.setVisibility(View.GONE);
        rootLayout.addView(typingIndicator);

        // === 输入区域 ===
        LinearLayout inputArea = new LinearLayout(requireContext());
        inputArea.setOrientation(LinearLayout.HORIZONTAL);
        inputArea.setBackgroundColor(COLOR_SIDEBAR);
        inputArea.setPadding(dp(12), dp(10), dp(12), dp(10));
        inputArea.setGravity(Gravity.CENTER_VERTICAL);
        inputArea.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        inputMessage = new EditText(requireContext());
        inputMessage.setHint("输入消息...");
        inputMessage.setTextColor(COLOR_TEXT);
        inputMessage.setHintTextColor(COLOR_MUTED);
        inputMessage.setBackgroundColor(Color.WHITE);
        inputMessage.setPadding(dp(14), dp(12), dp(14), dp(12));
        inputMessage.setTextSize(15);
        inputMessage.setMaxLines(4);
        LinearLayout.LayoutParams inputP = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        inputP.setMargins(0, 0, dp(8), 0);
        inputMessage.setLayoutParams(inputP);
        inputArea.addView(inputMessage);

        btnSend = new ImageButton(requireContext());
        btnSend.setBackgroundColor(COLOR_ACCENT);
        btnSend.setImageResource(android.R.drawable.ic_menu_send);
        btnSend.setColorFilter(Color.WHITE);
        btnSend.setPadding(dp(14), dp(14), dp(14), dp(14));
        btnSend.setLayoutParams(new LinearLayout.LayoutParams(dp(50), dp(50)));
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "发送按钮被点击");
                doSendMessage();
            }
        });
        inputArea.addView(btnSend);

        rootLayout.addView(inputArea);

        return rootLayout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated");

        if (getActivity() instanceof MainActivity) {
            api = ((MainActivity) getActivity()).getApi();
        }

        if (api == null) {
            Log.e(TAG, "api is null!");
            Toast.makeText(requireContext(), "API未初始化", Toast.LENGTH_SHORT).show();
            updateStatus(false);
            return;
        }

        // 检查连接
        checkConnection();

        // 检查是否有传入的 session_id
        Bundle args = getArguments();
        if (args != null && args.containsKey("session_id")) {
            String sid = args.getString("session_id");
            if (sid != null && !sid.isEmpty()) {
                loadSession(sid);
            }
        }
    }

    private void checkConnection() {
        Log.d(TAG, "checkConnection");
        api.healthCheck(new HermesApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject result) {
                Log.d(TAG, "healthCheck success");
                if (isAdded()) updateStatus(true);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "healthCheck failed: " + error);
                if (isAdded()) updateStatus(false);
            }
        });
    }

    private void updateStatus(boolean connected) {
        if (!isAdded() || statusDot == null || statusText == null) return;
        requireActivity().runOnUiThread(() -> {
            statusDot.setBackgroundColor(connected ? Color.GREEN : Color.RED);
            statusText.setText(connected ? "已连接" : "未连接");
            Log.d(TAG, "状态更新: " + (connected ? "已连接" : "未连接"));
        });
    }

    private void doSendMessage() {
        Log.d(TAG, "doSendMessage api=" + (api != null));

        if (api == null) {
            Toast.makeText(requireContext(), "API未初始化，无法发送", Toast.LENGTH_SHORT).show();
            return;
        }

        String text = inputMessage.getText().toString().trim();
        Log.d(TAG, "text='" + text + "'");

        if (text.isEmpty()) {
            Toast.makeText(requireContext(), "请输入消息", Toast.LENGTH_SHORT).show();
            return;
        }

        // 显示用户消息
        addBubble("你", text, true);
        inputMessage.setText("");

        // 显示打字指示器
        typingIndicator.setVisibility(View.VISIBLE);
        isStreaming = true;

        // 发送到API
        api.sendChatMessage(currentSessionId, text, new HermesApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject result) {
                if (!isAdded()) return;
                Log.d(TAG, "sendChat success: " + result.toString().substring(0, Math.min(100, result.toString().length())));

                if (currentSessionId == null) {
                    currentSessionId = result.optString("session_id", null);
                }

                String streamId = result.optString("stream_id", "");
                String reply = result.optString("response", result.optString("message", ""));

                if (!streamId.isEmpty()) {
                    streamResponse(streamId);
                } else if (!reply.isEmpty()) {
                    typingIndicator.setVisibility(View.GONE);
                    isStreaming = false;
                    addBubble("Hermes", reply, false);
                } else {
                    typingIndicator.setVisibility(View.GONE);
                    isStreaming = false;
                    addBubble("Hermes", "（无回复）", false);
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                Log.e(TAG, "sendChat failed: " + error);
                typingIndicator.setVisibility(View.GONE);
                isStreaming = false;
                Toast.makeText(requireContext(), "发送失败: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void streamResponse(String streamId) {
        api.streamChat(streamId, new HermesApi.SseCallback() {
            TextView[] bubble = {null};

            @Override
            public void onData(String data) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    try {
                        JSONObject obj = new JSONObject(data);
                        String content = obj.optString("content", obj.optString("text", ""));
                        if (!content.isEmpty()) {
                            if (bubble[0] == null) {
                                View v = addBubble("Hermes", content, false);
                                bubble[0] = findTextView(v);
                            } else {
                                bubble[0].append(content);
                                scrollToBottom();
                            }
                        }
                    } catch (Exception ignored) {}
                });
            }

            @Override
            public void onComplete() {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    typingIndicator.setVisibility(View.GONE);
                    isStreaming = false;
                    if (bubble[0] == null) {
                        addBubble("Hermes", "（完成）", false);
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    typingIndicator.setVisibility(View.GONE);
                    isStreaming = false;
                    if (bubble[0] == null) {
                        addBubble("Hermes", "错误: " + error, false);
                    }
                });
            }
        });
    }

    private void loadSession(String sessionId) {
        api.getSession(sessionId, new HermesApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject result) {
                if (!isAdded()) return;
                currentSessionId = sessionId;
                JSONArray msgs = result.optJSONArray("messages");
                if (msgs != null) {
                    messageContainer.removeAllViews();
                    for (int i = 0; i < msgs.length(); i++) {
                        try {
                            JSONObject m = msgs.getJSONObject(i);
                            String role = m.optString("role", "user");
                            String content = m.optString("content", "");
                            if (!content.isEmpty()) {
                                addBubble("user".equals(role) ? "你" : "Hermes", content, "user".equals(role));
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) Toast.makeText(requireContext(), "加载失败: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private View addBubble(String sender, String content, boolean isUser) {
        LinearLayout bubble = new LinearLayout(requireContext());
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(dp(12), dp(8), dp(12), dp(8));

        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.setMargins(0, 0, 0, dp(8));

        if (isUser) {
            bp.gravity = Gravity.END;
            bubble.setBackgroundColor(COLOR_USER_BG);
        } else {
            bp.gravity = Gravity.START;
            bubble.setBackgroundColor(COLOR_AI_BG);
        }
        bubble.setLayoutParams(bp);

        TextView senderView = new TextView(requireContext());
        senderView.setText(sender);
        senderView.setTextSize(12);
        senderView.setTextColor(COLOR_MUTED);
        senderView.setTypeface(Typeface.DEFAULT_BOLD);
        senderView.setPadding(0, 0, 0, dp(4));
        bubble.addView(senderView);

        TextView contentView = new TextView(requireContext());
        contentView.setText(content);
        contentView.setTextSize(15);
        contentView.setTextColor(COLOR_TEXT);
        contentView.setLineSpacing(dp(2), 1.1f);
        bubble.addView(contentView);

        messageContainer.addView(bubble);
        scrollToBottom();
        return bubble;
    }

    private TextView findTextView(View view) {
        if (view instanceof TextView) return (TextView) view;
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                TextView tv = findTextView(vg.getChildAt(i));
                if (tv != null) return tv;
            }
        }
        return null;
    }

    private void scrollToBottom() {
        if (chatScrollView != null) {
            chatScrollView.post(() -> chatScrollView.fullScroll(ScrollView.FOCUS_DOWN));
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
