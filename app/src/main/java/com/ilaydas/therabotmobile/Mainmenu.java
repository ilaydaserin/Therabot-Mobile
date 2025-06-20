package com.ilaydas.therabotmobile;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class Mainmenu extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainmenu);

        // CardView'ları tanımla
        CardView btnChatbot = findViewById(R.id.btn_chatbot);
        CardView btnTests = findViewById(R.id.btn_tests);
        CardView btnMoods = findViewById(R.id.btn_moods);
        CardView btnNotes = findViewById(R.id.btn_notes);
        CardView btnPsychologists = findViewById(R.id.btn_psychologists);

        // Chatbot CardView'a tıklanınca ChatbotActivity'yi başlat
        btnChatbot.setOnClickListener(v -> {
            Intent intent = new Intent(Mainmenu.this, chatbot.class);
            startActivity(intent);
        });

        // Tests CardView'a tıklanınca TestsActivity'yi başlat
        btnTests.setOnClickListener(v -> {
            Intent intent = new Intent(Mainmenu.this, Tests.class);
            startActivity(intent);
        });

        // Daily Moods CardView'a tıklanınca DailyMoodActivity'yi başlat
        btnMoods.setOnClickListener(v -> {
            Intent intent = new Intent(Mainmenu.this, DailyMood.class);
            startActivity(intent);
        });

        // Journal CardView'a tıklanınca JournalActivity'yi başlat
        btnNotes.setOnClickListener(v -> {
            Intent intent = new Intent(Mainmenu.this, Journal.class);
            startActivity(intent);
        });

        // Psychologists CardView'a tıklanınca PsychologistsActivity'yi başlat
        btnPsychologists.setOnClickListener(v -> {
            Intent intent = new Intent(Mainmenu.this, Psychologists.class);
            startActivity(intent);
        });
    }
}
