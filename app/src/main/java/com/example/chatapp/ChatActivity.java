package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chatapp.adapter.ChatRecyclerAdapter;
import com.example.chatapp.adapter.SearchUserRecyclerAdapter;
import com.example.chatapp.model.ChatMessageModel;
import com.example.chatapp.model.ChatroomModel;
import com.example.chatapp.model.UserModel;
import com.example.chatapp.utils.AndroidUtil;
import com.example.chatapp.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {
    UserModel otherUser;
    String chatroomId;
    ChatroomModel chatroomModel;
    ChatRecyclerAdapter adapter;

    EditText messageInput;
    TextView otherUsername, textAvailability;
    ImageButton sendMessageBtn, audioMessageBtn, backBtn, attachmentBtn;
    ImageView profilePic;
    RecyclerView recyclerView;
    ProgressDialog dialog;

    RelativeLayout bottomLayout, receiveRequestLayout, sendRequestLayout;
    TextView receiveRequestTextView, sendRequestTextView;
    Button acceptBtn, rejectBtn, sendRequestBtn;

    String filePath;
    SharedPreferences sharedPreferences;
    List<Boolean> requestList;
    boolean requestGiven = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        sharedPreferences = this.getSharedPreferences("com.example.chatapp", MODE_PRIVATE);

        otherUser = AndroidUtil.getUserModelFromIntent(getIntent());
        chatroomId = FirebaseUtil.getChatroomId(FirebaseUtil.currentUserId(), otherUser.getUserId());

        messageInput = findViewById(R.id.chat_message_input);
        otherUsername = findViewById(R.id.other_username);
        textAvailability = findViewById(R.id.textAvailability);
        backBtn = findViewById(R.id.back_btn);
        sendMessageBtn = findViewById(R.id.message_send_btn);
        audioMessageBtn = findViewById(R.id.audio_send_btn);
        attachmentBtn = findViewById(R.id.attachment_btn);
        profilePic = findViewById(R.id.profile_pic_image_view);
        recyclerView = findViewById(R.id.chat_recycler_view);

        bottomLayout = findViewById(R.id.bottom_layout);
        receiveRequestLayout = findViewById(R.id.receiveRequestLayout);
        sendRequestLayout = findViewById(R.id.sendRequestLayout);
        receiveRequestTextView = findViewById(R.id.receiveRequestTextView);
        sendRequestTextView = findViewById(R.id.sendRequestTextView);
        acceptBtn = findViewById(R.id.acceptBtn);
        rejectBtn = findViewById(R.id.rejectBtn);
        sendRequestBtn = findViewById(R.id.sendRequestBtn);

        dialog = new ProgressDialog(this);
        dialog.setMessage("Sending image...");
        dialog.setCancelable(false);

        otherUsername.setText(otherUser.getUsername());
        getOrCreateChatroomModel();

        FirebaseUtil.getOtherProfilePicStorageRef(otherUser.getUserId()).getDownloadUrl()
                .addOnCompleteListener(t -> {
                    if (t.isSuccessful()) {
                        Uri uri = t.getResult();
                        AndroidUtil.setProfilePic(this, uri, profilePic);
                    }
                });

        backBtn.setOnClickListener(v -> onBackPressed());

        sendRequestBtn.setOnClickListener(v -> {
            chatroomModel = new ChatroomModel(
                    chatroomId,
                    (FirebaseUtil.currentUserId().hashCode() < otherUser.getUserId().hashCode()) ? Arrays.asList(FirebaseUtil.currentUserId(), otherUser.getUserId()) : Arrays.asList(otherUser.getUserId(), FirebaseUtil.currentUserId()),
                    (FirebaseUtil.currentUserId().hashCode() < otherUser.getUserId().hashCode()) ? Arrays.asList(true, false) : Arrays.asList(false, true),
                    Timestamp.now(),
                    ""
            );
            chatroomModel.setLastMessage(FirebaseUtil.encrypt("Request"));
            chatroomModel.setLastMessageTimestamp(Timestamp.now());
            FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);

            sendRequestBtn.setEnabled(false);
            sendRequestBtn.setText("Request Sent");
        });

        acceptBtn.setOnClickListener(v -> {
            FirebaseUtil.getChatroomReference(chatroomId).update("request", Arrays.asList(true, true));

            finish();
            startActivity(getIntent());
        });

        rejectBtn.setOnClickListener(v -> {
            FirebaseUtil.getChatroomReference(chatroomId).delete();

            sendRequestLayout.setVisibility(View.VISIBLE);
            bottomLayout.setVisibility(View.GONE);
            receiveRequestLayout.setVisibility(View.GONE);
        });

        sendMessageBtn.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (message.isEmpty())
                return;
            sendMessageToUser(message);
        });

        audioMessageBtn.setOnClickListener(v -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to text");

            try {
                startActivityForResult(intent, 111);
            } catch (Exception e) {
                Toast.makeText(this, e.getMessage() + "", Toast.LENGTH_SHORT).show();
            }
        });

        attachmentBtn.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, 101);

        });

        messageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.length() == 0) {
                    sendMessageBtn.setVisibility(View.GONE);
                    audioMessageBtn.setVisibility(View.VISIBLE);
                } else {
                    sendMessageBtn.setVisibility(View.VISIBLE);
                    audioMessageBtn.setVisibility(View.GONE);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    private void setupChatRecyclerView() {
        bottomLayout.setVisibility(View.VISIBLE);
        receiveRequestLayout.setVisibility(View.GONE);
        sendRequestLayout.setVisibility(View.GONE);

        Query query = FirebaseUtil.getChatroomMessageReference(chatroomId)
                .orderBy("timestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<ChatMessageModel> options = new FirestoreRecyclerOptions.Builder<ChatMessageModel>()
                .setQuery(query, ChatMessageModel.class)
                .build();

        adapter = new ChatRecyclerAdapter(options, getApplicationContext());
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setReverseLayout(true);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
        adapter.startListening();
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                recyclerView.smoothScrollToPosition(0);
            }
        });
    }

    private void sendMessageToUser(String message) {
        chatroomModel.setLastMessageTimestamp(Timestamp.now());
        chatroomModel.setLastMessageSenderId(FirebaseUtil.currentUserId());
        chatroomModel.setLastMessage(FirebaseUtil.encrypt(message));
        FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);

        ChatMessageModel chatMessageModel = new ChatMessageModel(FirebaseUtil.encrypt(message), FirebaseUtil.currentUserId(), Timestamp.now());
        FirebaseUtil.getChatroomMessageReference(chatroomId).add(chatMessageModel)
                .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentReference> task) {
                        if (task.isSuccessful()) {
                            messageInput.setText("");
                            sendNotification(message);
                        }
                    }
                });
    }

    private void getOrCreateChatroomModel() {
        requestList = new ArrayList<>();
        FirebaseUtil.getChatroomReference(chatroomId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                chatroomModel = task.getResult().toObject(ChatroomModel.class);
                if (chatroomModel == null) {
                    sendRequestLayout.setVisibility(View.VISIBLE);
                    receiveRequestLayout.setVisibility(View.GONE);
                    bottomLayout.setVisibility(View.GONE);
                    sendRequestTextView.setText("Do you want to send request to chat with " + otherUser.getUsername() + "?");
                } else {
                    int i;
                    if (FirebaseUtil.currentUserId().hashCode() < otherUser.getUserId().hashCode())
                        i = 0;
                    else i = 1;
//                    Log.i("CHECK", i+"");

                    if (chatroomModel.getRequest().get(0) && chatroomModel.getRequest().get(1))
                        setupChatRecyclerView();
                    else if (!chatroomModel.getRequest().get(i)) {
                        receiveRequestLayout.setVisibility(View.VISIBLE);
                        sendRequestLayout.setVisibility(View.GONE);
                        bottomLayout.setVisibility(View.GONE);

                        receiveRequestTextView.setText("Do you want to accept the request to chat with " + otherUser.getUsername() + "?");
                    } else {
                        sendRequestLayout.setVisibility(View.VISIBLE);
                        receiveRequestLayout.setVisibility(View.GONE);
                        bottomLayout.setVisibility(View.GONE);

                        sendRequestBtn.setEnabled(false);
                        sendRequestBtn.setText("Request Sent");
                    }
                }
            }
            else {
                sendRequestLayout.setVisibility(View.VISIBLE);
                receiveRequestLayout.setVisibility(View.GONE);
                bottomLayout.setVisibility(View.GONE);
                sendRequestTextView.setText("Do you want to send request to chat with " + otherUser.getUsername() + "?");
            }
        });
    }

    private void sendNotification(String message) {
        FirebaseUtil.currentUserDetails().get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                UserModel currentUser = task.getResult().toObject(UserModel.class);
                try {
                    JSONObject jsonObject = new JSONObject();

                    JSONObject notificationObj = new JSONObject();
                    notificationObj.put("title", currentUser.getUsername());
                    notificationObj.put("body", message);

                    JSONObject dataObj = new JSONObject();
                    dataObj.put("userId", currentUser.getUserId());

                    jsonObject.put("notification", notificationObj);
                    jsonObject.put("data", dataObj);
                    jsonObject.put("to", otherUser.getFcmToken());
                    callApi(jsonObject);
                } catch (Exception e) {
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 101) {
            if (data != null && data.getData() != null) {
                int imageIdx = sharedPreferences.getInt("imageIdx", 1);

                dialog.show();
                Uri imageUri = data.getData();
                FirebaseUtil.getImageMessageStorageReference(imageIdx).putFile(imageUri)
                        .addOnCompleteListener(t -> {
                            dialog.dismiss();
                            FirebaseUtil.getImageMessageStorageReference(imageIdx).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    filePath = uri.toString();

                                    chatroomModel.setLastMessageTimestamp(Timestamp.now());
                                    chatroomModel.setLastMessageSenderId(FirebaseUtil.currentUserId());
                                    chatroomModel.setLastMessage(FirebaseUtil.encrypt("Photo"));
                                    FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);

                                    ChatMessageModel chatMessageModel = new ChatMessageModel("photo_img", FirebaseUtil.currentUserId(), Timestamp.now());
                                    chatMessageModel.setImageUrl(filePath);
                                    FirebaseUtil.getChatroomMessageReference(chatroomId).add(chatMessageModel)
                                            .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                                @Override
                                                public void onComplete(@NonNull Task<DocumentReference> task) {
                                                    if (task.isSuccessful()) {
                                                        messageInput.setText("");
                                                        sendNotification("Photo");
                                                    }
                                                }
                                            });
                                }
                            });
                            sharedPreferences.edit().putInt("imageIdx", imageIdx + 1).apply();
                        });
            }
        }

        if (requestCode == 111) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                messageInput.setText(Objects.requireNonNull(result).get(0));
            }
        }
    }

    private void callApi(JSONObject jsonObject) {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        OkHttpClient client = new OkHttpClient();
        String url = "https://fcm.googleapis.com/fcm/send";

        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Authorization", "Bearer AAAAX7uZChI:APA91bGNJ1jDzowlGHlJAY2s5DLniWNpALgBbqDfaNYB1e1dqVVN3v0b8qDHVe6WIZMRlPPH5wYVdpw0BEl9VlmQCNrG3SgHkahU_cQdbpeI-O63xgDWZKlZa38wvKIiZceOqP5IXV82")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseFirestore.getInstance().collection("users").document(otherUser.getUserId())
                .addSnapshotListener(ChatActivity.this, ((value, error) -> {
                    if (error != null)
                        return;
                    int availability = 0;
                    if (value != null) {
                        if (value.getLong("availability") != null) {
                            availability = Objects.requireNonNull(
                                    value.getLong("availability").intValue()
                            );
                        }
                    }
                    if (availability == 1)
                        textAvailability.setVisibility(View.VISIBLE);
                    else
                        textAvailability.setVisibility(View.GONE);
                }));

        FirebaseUtil.setAvailability(1);
    }

    @Override
    protected void onPause() {
        super.onPause();
        FirebaseUtil.setAvailability(0);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
//        if (chatroomModel.getLastMessage() == null)
//            FirebaseFirestore.getInstance().collection("chatrooms").document(chatroomId).delete();
    }
}