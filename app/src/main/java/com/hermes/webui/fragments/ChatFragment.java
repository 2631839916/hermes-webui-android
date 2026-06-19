package com.hermes.webui.fragments;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
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

public class ChatFragment extends Fragment {
    private static final String TAG = "ChatFragment";

    // Colors
    private static final int COLOR_BG = Color.parseColor("#FEFCF7");
    private static final int COLOR_SIDEBAR = Color.parseColor("#FAF7F0");
    private static final int COLOR_BORDER = Color.parseColor("#E0D8C8");
    private static final int COLOR_TEXT = Color.parseColor("#1A1610");
    private static final int COLOR_MUTED = Color.parseColor("#5C5344");
    private static final int COLOR_ACCENT = Color.parseColor("#B8860B");

    private RecyclerView messageList;
    private RecyclerView sessionList;
    private EditText inputMessage;
    private ImageButton btnSend;
    private ProgressBar typingIndicator;
    private ScrollView chatScrollView;
    private LinearLayout messageContainer;

    private HermesApi api;
    private String currentSessionId = null;
    private String currentStreamId = null;
    private boolean isStreaming = false;

    private final List<MessageItem> messages = new ArrayList<>();
    private final List<SessionItem> sessions = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Root layout: horizontal with sidebar + chat area
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackgroundColor(COLOR_BG);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Sidebar for sessions
        LinearLayout sidebar = createSidebar();
        root.addView(sidebar);

        // Divider
        View divider = new View(requireContext());
        divider.setBackgroundColor(COLOR_BORDER);
        divider.setLayoutParams(new LinearLayout.LayoutParams(dp(1), ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(divider);

        // Chat area
        LinearLayout chatArea = createChatArea();
        root.addView(chatArea);

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
        loadSessions();
    }

    private LinearLayout createSidebar() {
        LinearLayout sidebar = new LinearLayout(requireContext());
        sidebar.setOrientation(LinearLayout.VERTICAL);
        sidebar.setBackgroundColor(COLOR_SIDEBAR);
        LinearLayout.LayoutParams sidebarParams = new LinearLayout.LayoutParams(dp(240), ViewGroup.LayoutParams.MATCH_PARENT);
        sidebar.setLayoutParams(sidebarParams);
        sidebar.setPadding(dp(12), dp(16), dp(12), dp(12));

        // Title
        TextView title = new TextView(requireContext());
        title.setText("Sessions");
        title.setTextSize(18);
        title.setTextColor(COLOR_TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, dp(12));
        sidebar.addView(title);

        // New chat button
        TextView btnNew = new TextView(requireContext());
        btnNew.setText("+ New Chat");
        btnNew.setTextColor(Color.WHITE);
        btnNew.setBackgroundColor(COLOR_ACCENT);
        btnNew.setGravity(Gravity.CENTER);
        btnNew.setPadding(dp(12), dp(10), dp(12), dp(10));
        btnNew.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams newParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        newParams.setMargins(0, 0, 0, dp(12));
        btnNew.setLayoutParams(newParams);
        btnNew.setOnClickListener(v -> newChat());
        sidebar.addView(btnNew);

        // Session list
        sessionList = new RecyclerView(requireContext());
        sessionList.setLayoutManager(new LinearLayoutManager(requireContext()));
        sessionList.setAdapter(new SessionAdapter());
        sidebar.addView(sessionList, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        return sidebar;
    }

    private LinearLayout createChatArea() {
        LinearLayout chatArea = new LinearLayout(requireContext());
        chatArea.setOrientation(LinearLayout.VERTICAL);
        chatArea.setBackgroundColor(COLOR_BG);
        chatArea.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        // Messages area using ScrollView + LinearLayout for simplicity
        chatScrollView = new ScrollView(requireContext());
        chatScrollView.setFillViewport(true);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        chatScrollView.setLayoutParams(scrollParams);

        messageContainer = new LinearLayout(requireContext());
        messageContainer.setOrientation(LinearLayout.VERTICAL);
        messageContainer.setPadding(dp(16), dp(12), dp(16), dp(12));
        chatScrollView.addView(messageContainer, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        chatArea.addView(chatScrollView);

        // Typing indicator
        typingIndicator = new ProgressBar(requireContext());
        typingIndicator.setIndeterminate(true);
        LinearLayout.LayoutParams typingParams = new LinearLayout.LayoutParams(dp(24), dp(24));
        typingParams.gravity = Gravity.START;
        typingParams.setMargins(dp(16), 0, 0, dp(8));
        typingIndicator.setLayoutParams(typingParams);
        typingIndicator.setVisibility(View.GONE);
        chatArea.addView(typingIndicator);

        // Input area
        LinearLayout inputArea = new LinearLayout(requireContext());
        inputArea.setOrientation(LinearLayout.HORIZONTAL);
        inputArea.setBackgroundColor(COLOR_SIDEBAR);
        inputArea.setPadding(dp(12), dp(8), dp(12), dp(8));
        inputArea.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        inputMessage = new EditText(requireContext());
        inputMessage.setHint("Type a message...");
        inputMessage.setTextColor(COLOR_TEXT);
        inputMessage.setHintTextColor(COLOR_MUTED);
        inputMessage.setBackgroundColor(Color.WHITE);
        inputMessage.setPadding(dp(14), dp(10), dp(14), dp(10));
        inputMessage.setTextSize(15);
        inputMessage.setMaxLines(4);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        inputParams.setMargins(0, 0, dp(8), 0);
        inputMessage.setLayoutParams(inputParams);
        inputArea.addView(inputMessage);

        btnSend = new ImageButton(requireContext());
        btnSend.setBackgroundColor(COLOR_ACCENT);
        btnSend.setImageResource(android.R.drawable.ic_menu_send);
        btnSend.setColorFilter(Color.WHITE);
        btnSend.setPadding(dp(12), dp(12), dp(12), dp(12));
        btnSend.setLayoutParams(new LinearLayout.LayoutParams(dp(48), dp(48)));
        btnSend.setOnClickListener(v -> sendMessage());
        inputArea.addView(btnSend);

        chatArea.addView(inputArea);

        return chatArea;
    }

    private void loadSessions() {
        if (api == null) return;
        api.getSessions(new HermesApi.ApiCallback<JSONArray>() {
            @Override
            public void onSuccess(JSONArray result) {
                sessions.clear();
                for (int i = 0; i < result.length() && i < 50; i++) {
                    try {
                        JSONObject s = result.getJSONObject(i);
                        String id = s.optString("session_id", s.optString("id", ""));
                        String title = s.optString("title", "Untitled");
                        String preview = s.optString("preview", s.optString("last_message", ""));
                        sessions.add(new SessionItem(id, title, preview));
                    } catch (Exception ignored) {}
                }
                if (sessionList != null && sessionList.getAdapter() != null) {
                    sessionList.getAdapter().notifyDataSetChanged();
                }
                if (!sessions.isEmpty() && currentSessionId == null) {
                    loadSessionMessages(sessions.get(0));
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Failed to load sessions: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadSessionMessages(SessionItem session) {
        currentSessionId = session.id;
        messages.clear();
        messageContainer.removeAllViews();

        if (api == null) return;
        api.getSession(session.id, new HermesApi.ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONArray result) {
                JSONArray msgs = result.optJSONArray("messages");
                if (msgs != null) {
                    for (int i = 0; i < msgs.length(); i++) {
                        try {
                            JSONObject m = msgs.getJSONObject(i);
                            String role = m.optString("role", "user");
                            String content = m.optString("content", "");
                            if (!content.isEmpty()) {
                                boolean isUser = "user".equals(role);
                                addMessageBubble(isUser ? "You" : "Hermes", content, isUser);
                            }
                        } catch (Exception ignored) {}
                    }
                }
                scrollToBottom();
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Failed to load messages: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void sendMessage() {
        if (api == null) return;
        String text = inputMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        addMessageBubble("You", text, true);
        inputMessage.setText("");
        scrollToBottom();

        // Show typing indicator
        typingIndicator.setVisibility(View.VISIBLE);
        isStreaming = true;

        api.sendChatMessage(currentSessionId, text, new HermesApi.ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONArray result) {
                if (!isAdded()) return;
                if (currentSessionId == null) {
                    currentSessionId = result.optString("session_id", null);
                }

                String streamId = result.optString("stream_id", "");
                String reply = result.optString("response", result.optString("message", ""));

                if (!streamId.isEmpty()) {
                    currentStreamId = streamId;
                    streamResponse(streamId);
                } else if (!reply.isEmpty()) {
                    typingIndicator.setVisibility(View.GONE);
                    isStreaming = false;
                    addMessageBubble("Hermes", reply, false);
                    scrollToBottom();
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
                Toast.makeText(requireContext(), "Send failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void streamResponse(String streamId) {
        if (api == null) return;

        // Create a temporary bubble for streaming
        TextView[] streamBubble = {null};
        StringBuilder streamContent = new StringBuilder();

        // Add a placeholder message
        View bubbleView = addMessageBubble("Hermes", "", false);
        // Find the content text view in the bubble (last TextView added)
        if (bubbleView instanceof LinearLayout) {
            LinearLayout container = (LinearLayout) bubbleView;
            for (int i = 0; i < container.getChildCount(); i++) {
                View child = container.getChildAt(i);
                if (child instanceof TextView && i > 0) {
                    streamBubble[0] = (TextView) child;
                    break;
                }
            }
        }

        api.streamChat(streamId, new HermesApi.SseCallback() {
            @Override
            public void onData(String data) {
                if (!isAdded()) return;
                try {
                    // The data might be plain text or JSON
                    String content = data;
                    try {
                        JSONObject obj = new JSONObject(data);
                        content = obj.optString("content", obj.optString("text", data));
                    } catch (Exception ignored) {
                        // Use raw data as content
                    }
                    streamContent.append(content);
                    if (streamBubble[0] != null) {
                        streamBubble[0].setText(renderMarkdown(streamContent.toString()));
                    }
                    scrollToBottom();
                } catch (Exception e) {
                    Log.e(TAG, "SSE parse error", e);
                }
            }

            @Override
            public void onComplete() {
                if (!isAdded()) return;
                typingIndicator.setVisibility(View.GONE);
                isStreaming = false;
                currentStreamId = null;
                if (streamBubble[0] != null && streamContent.length() == 0) {
                    streamBubble[0].setText("(No response)");
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                typingIndicator.setVisibility(View.GONE);
                isStreaming = false;
                if (streamBubble[0] != null) {
                    streamBubble[0].setText("Error: " + error);
                }
            }
        });
    }

    private View addMessageBubble(String sender, String content, boolean isUser) {
        LinearLayout bubble = new LinearLayout(requireContext());
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(dp(12), dp(8), dp(12), dp(8));

        LinearLayout.LayoutParams bubbleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bubbleParams.setMargins(0, 0, 0, dp(8));

        if (isUser) {
            bubbleParams.gravity = Gravity.END;
            bubble.setBackgroundColor(Color.parseColor("#E8E0D0"));
        } else {
            bubbleParams.gravity = Gravity.START;
            bubble.setBackgroundColor(Color.parseColor("#F5F0E8"));
        }
        bubble.setLayoutParams(bubbleParams);


        // Sender label
        TextView senderView = new TextView(requireContext());
        senderView.setText(sender);
        senderView.setTextSize(12);
        senderView.setTextColor(COLOR_MUTED);
        senderView.setTypeface(Typeface.DEFAULT_BOLD);
        senderView.setPadding(0, 0, 0, dp(4));
        bubble.addView(senderView);

        // Content
        TextView contentView = new TextView(requireContext());
        contentView.setText(renderMarkdown(content));
        contentView.setTextSize(15);
        contentView.setTextColor(COLOR_TEXT);
        contentView.setMovementMethod(LinkMovementMethod.getInstance());
        contentView.setLineSpacing(dp(2), 1.1f);
        bubble.addView(contentView);

        messageContainer.addView(bubble);
        scrollToBottom();
        return bubble;
    }

    private void newChat() {
        currentSessionId = null;
        currentStreamId = null;
        messages.clear();
        messageContainer.removeAllViews();
        if (isAdded()) {
            Toast.makeText(requireContext(), "New chat started", Toast.LENGTH_SHORT).show();
        }
    }

    private void scrollToBottom() {
        if (chatScrollView != null) {
            chatScrollView.post(() -> chatScrollView.fullScroll(View.FOCUS_DOWN));
        }
    }

    /**
     * Simple markdown rendering: **bold**, `code`, ```code blocks```, [links](url)
     */
    private CharSequence renderMarkdown(String text) {
        if (text == null || text.isEmpty()) return "";

        SpannableStringBuilder ssb = new SpannableStringBuilder(text);

        // Bold: **text**
        int start = 0;
        while (true) {
            int boldStart = text.indexOf("**", start);
            if (boldStart == -1) break;
            int boldEnd = text.indexOf("**", boldStart + 2);
            if (boldEnd == -1) break;

            String inner = text.substring(boldStart + 2, boldEnd);
            ssb.replace(boldStart, boldEnd + 2, inner);
            ssb.setSpan(new StyleSpan(Typeface.BOLD), boldStart, boldStart + inner.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text = ssb.toString();
            start = boldStart + inner.length();
        }

        // Inline code: `text`
        start = 0;
        while (true) {
            int codeStart = text.indexOf('`', start);
            if (codeStart == -1) break;
            int codeEnd = text.indexOf('`', codeStart + 1);
            if (codeEnd == -1) break;

            ssb.setSpan(new BackgroundColorSpan(Color.parseColor("#E8E0D0")),
                    codeStart, codeEnd + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new TypefaceSpan("monospace"),
                    codeStart, codeEnd + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            start = codeEnd + 1;
        }

        // Code blocks: ```...```
        start = 0;
        while (true) {
            int blockStart = text.indexOf("```", start);
            if (blockStart == -1) break;
            int blockEnd = text.indexOf("```", blockStart + 3);
            if (blockEnd == -1) break;

            ssb.setSpan(new BackgroundColorSpan(Color.parseColor("#E0DBD0")),
                    blockStart, blockEnd + 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            start = blockEnd + 3;
        }

        return ssb;
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (api != null && currentStreamId != null) {
            api.cancelChat(currentStreamId, new HermesApi.ApiCallback<JSONObject>() {
                @Override public void onSuccess(JSONArray result) {}
                @Override public void onError(String error) {}
            });
        }
    }

    // Inner classes
    private static class MessageItem {
        String sender, content;
        boolean isUser;
        MessageItem(String sender, String content, boolean isUser) {
            this.sender = sender;
            this.content = content;
            this.isUser = isUser;
        }
    }

    private static class SessionItem {
        String id, title, preview;
        SessionItem(String id, String title, String preview) {
            this.id = id;
            this.title = title;
            this.preview = preview;
        }
    }

    private class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout item = new LinearLayout(requireContext());
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(dp(10), dp(10), dp(10), dp(10));
            item.setBackgroundColor(COLOR_SIDEBAR);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, dp(4));
            item.setLayoutParams(params);

            TextView titleView = new TextView(requireContext());
            titleView.setTextSize(14);
            titleView.setTextColor(COLOR_TEXT);
            titleView.setTypeface(Typeface.DEFAULT_BOLD);
            titleView.setMaxLines(1);
            titleView.setId(android.R.id.text1);
            item.addView(titleView);

            TextView previewView = new TextView(requireContext());
            previewView.setTextSize(12);
            previewView.setTextColor(COLOR_MUTED);
            previewView.setMaxLines(2);
            previewView.setId(android.R.id.text2);
            item.addView(previewView);

            return new VH(item);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            SessionItem session = sessions.get(position);
            holder.titleView.setText(session.title);
            holder.previewView.setText(session.preview);
            holder.itemView.setOnClickListener(v -> loadSessionMessages(session));
        }

        @Override
        public int getItemCount() {
            return sessions.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView titleView, previewView;
            VH(View v) {
                super(v);
                titleView = v.findViewById(android.R.id.text1);
                previewView = v.findViewById(android.R.id.text2);
            }
        }
    }
}
