package com.hermes.webui.fragments;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
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

public class ChatFragment extends Fragment {
    private static final String TAG = "HermesChat";

    private static final int COLOR_BG = Color.parseColor("#FEFCF7");
    private static final int COLOR_SIDEBAR = Color.parseColor("#FAF7F0");
    private static final int COLOR_TEXT = Color.parseColor("#1A1610");
    private static final int COLOR_MUTED = Color.parseColor("#5C5344");
    private static final int COLOR_ACCENT = Color.parseColor("#B8860B");
    private static final int COLOR_USER_BG = Color.parseColor("#E8E0D0");
    private static final int COLOR_AI_BG = Color.parseColor("#F5F0E8");

    private ScrollView chatScrollView;
    private LinearLayout messageContainer;
    private EditText inputMessage;
    private ImageButton btnSend;
    private ProgressBar typingIndicator;
    private View statusDot;
    private TextView statusLabel;

    private HermesApi api;
    private String currentSessionId;
    private boolean isStreaming = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

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
        root.addView(chatScrollView);

        // === 打字指示器 ===
        typingIndicator = new ProgressBar(requireContext());
        typingIndicator.setIndeterminate(true);
        LinearLayout.LayoutParams tip = new LinearLayout.LayoutParams(dp(20), dp(20));
        tip.gravity = Gravity.START;
        tip.setMargins(dp(16), 0, 0, dp(4));
        typingIndicator.setLayoutParams(tip);
        typingIndicator.setVisibility(View.GONE);
        root.addView(typingIndicator);

        // === 输入区域（含连接灯） ===
        LinearLayout inputArea = new LinearLayout(requireContext());
        inputArea.setOrientation(LinearLayout.VERTICAL);
        inputArea.setBackgroundColor(COLOR_SIDEBAR);
        inputArea.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 连接状态行（在输入框上方）
        LinearLayout statusRow = new LinearLayout(requireContext());
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        statusRow.setPadding(dp(12), dp(4), dp(12), dp(2));

        statusDot = new View(requireContext());
        statusDot.setBackgroundColor(Color.RED);
        LinearLayout.LayoutParams dotP = new LinearLayout.LayoutParams(dp(8), dp(8));
        dotP.setMargins(0, 0, dp(6), 0);
        statusDot.setLayoutParams(dotP);
        statusRow.addView(statusDot);

        statusLabel = new TextView(requireContext());
        statusLabel.setText("未连接");
        statusLabel.setTextSize(11);
        statusLabel.setTextColor(COLOR_MUTED);
        statusRow.addView(statusLabel);

        inputArea.addView(statusRow);

        // 输入行
        LinearLayout inputRow = new LinearLayout(requireContext());
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        inputRow.setPadding(dp(12), dp(4), dp(12), dp(10));

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
        inputRow.addView(inputMessage);

        btnSend = new ImageButton(requireContext());
        btnSend.setBackgroundColor(COLOR_ACCENT);
        btnSend.setImageResource(android.R.drawable.ic_menu_send);
        btnSend.setColorFilter(Color.WHITE);
        btnSend.setPadding(dp(14), dp(14), dp(14), dp(14));
        btnSend.setLayoutParams(new LinearLayout.LayoutParams(dp(48), dp(48)));
        btnSend.setOnClickListener(v -> doSendMessage());
        inputRow.addView(btnSend);

        inputArea.addView(inputRow);
        root.addView(inputArea);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() instanceof MainActivity) {
            api = ((MainActivity) getActivity()).getApi();
        }
        if (api == null) {
            Toast.makeText(requireContext(), "API未初始化", Toast.LENGTH_SHORT).show();
            updateStatus(false);
            return;
        }
        checkConnection();
    }

    private void checkConnection() {
        api.healthCheck(new HermesApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject result) {
                if (isAdded()) updateStatus(true);
            }
            @Override
            public void onError(String error) {
                if (isAdded()) updateStatus(false);
            }
        });
    }

    private void updateStatus(boolean connected) {
        if (!isAdded() || statusDot == null) return;
        requireActivity().runOnUiThread(() -> {
            statusDot.setBackgroundColor(connected ? Color.GREEN : Color.RED);
            statusLabel.setText(connected ? "已连接" : "未连接");
        });
    }

    private void doSendMessage() {
        if (api == null) {
            Toast.makeText(requireContext(), "API未初始化", Toast.LENGTH_SHORT).show();
            return;
        }

        String text = inputMessage.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(requireContext(), "请输入消息", Toast.LENGTH_SHORT).show();
            return;
        }

        // 如果没有session，先创建
        if (currentSessionId == null) {
            typingIndicator.setVisibility(View.VISIBLE);
            api.createSession(new HermesApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject result) {
                    if (!isAdded()) return;
                    JSONObject session = result.optJSONObject("session");
                    if (session != null) {
                        currentSessionId = session.optString("session_id", null);
                    }
                    if (currentSessionId != null) {
                        actuallySend(text);
                    } else {
                        typingIndicator.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "创建会话失败", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    typingIndicator.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "创建会话失败: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            actuallySend(text);
        }
    }

    private void actuallySend(String text) {
        addBubble("你", text, true);
        inputMessage.setText("");
        typingIndicator.setVisibility(View.VISIBLE);
        isStreaming = true;

        api.sendChatMessage(currentSessionId, text, new HermesApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject result) {
                if (!isAdded()) return;
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
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
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
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    typingIndicator.setVisibility(View.GONE);
                    isStreaming = false;
                });
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
        bp.gravity = isUser ? Gravity.END : Gravity.START;
        bubble.setBackgroundColor(isUser ? COLOR_USER_BG : COLOR_AI_BG);
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
