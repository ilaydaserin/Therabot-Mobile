package com.ilaydas.therabotmobile;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class chatbot extends AppCompatActivity {

    private EditText etMessage;
    private Button btnSend;
    private LinearLayout chatContainer;
    private ApiService apiService;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        chatContainer = findViewById(R.id.chat_container);
        scrollView = findViewById(R.id.scrollView);

        apiService = RetrofitClient.getClient().create(ApiService.class);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userMessage = etMessage.getText().toString().trim();
                if (!userMessage.isEmpty()) {
                    etMessage.setText("");
                    addMessage(userMessage, true); // kullanıcı mesajı
                    addMessage("...", false); // bekleme noktası
                    sendMessageToAPI(userMessage);
                }
            }
        });
    }

    private void sendMessageToAPI(String message) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("message", message);

        apiService.sendMessage(jsonObject).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Üç noktayı sil
                    removeLastMessage();

                    String botResponse = response.body().get("response").getAsString();
                    addMessage(botResponse, false); // bot cevabı
                } else {
                    removeLastMessage();
                    addMessage("Yanıt alınamadı!", false);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                removeLastMessage();
                addMessage("Bağlantı hatası: " + t.getMessage(), false);
            }
        });
    }

    private void addMessage(String message, boolean isUser) {
        TextView textView = new TextView(this);
        textView.setText(message);
        textView.setTextSize(16);
        textView.setTextColor(Color.BLACK);
        textView.setPadding(24, 16, 24, 16); // iç boşluk

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(16, 8, 16, 8); // dış boşluk

        if (isUser) {
            textView.setBackgroundResource(R.drawable.user_message_bg);
            params.gravity = Gravity.END;
        } else {
            textView.setBackgroundResource(R.drawable.bot_message_bg);
            params.gravity = Gravity.START;
        }

        textView.setLayoutParams(params);
        chatContainer.addView(textView);

        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }



    private void removeLastMessage() {
        int count = chatContainer.getChildCount();
        if (count > 0) {
            chatContainer.removeViewAt(count - 1);
        }
    }
}
