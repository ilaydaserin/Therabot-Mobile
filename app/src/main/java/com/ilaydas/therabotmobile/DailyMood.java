package com.ilaydas.therabotmobile;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DailyMood extends AppCompatActivity {

    private MaterialCalendarView calendarView;
    private TextView selectedMoodText;
    private ImageView selectedMoodEmoji;
    private LinearLayout selectedMoodContainer;
    private Button monthlyAnalysisButton, saveNoteButton;
    private MaterialButton btnSelectDate;
    private EditText dailyNoteEditText;

    private String selectedMood = "";
    private Map<String, Integer> moodColorMap;
    private Map<String, Integer> moodDrawableMap;
    private Map<String, Integer> moodSmallDrawableMap;
    private Map<Date, MoodEntry> moodDataMap;
    private Date currentSelectedDate;
    private boolean isCurrentDate = true;

    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_mood);

        FirebaseApp.initializeApp(this);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, login.class));
            finish();
            return;
        }

        userId = mAuth.getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        initializeMoodMaps();
        moodDataMap = new HashMap<>();
        currentSelectedDate = getNormalizedDate(new Date());
        loadSavedMoods();
        setupListeners();
    }

    private void initializeViews() {
        calendarView = findViewById(R.id.calendarView);
        selectedMoodText = findViewById(R.id.selectedMoodText);
        selectedMoodEmoji = findViewById(R.id.selectedMoodEmoji);
        selectedMoodContainer = findViewById(R.id.selectedMoodContainer);
        monthlyAnalysisButton = findViewById(R.id.monthlyAnalysisButton);
        saveNoteButton = findViewById(R.id.saveNoteButton);
        dailyNoteEditText = findViewById(R.id.dailyNoteEditText);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        btnSelectDate.setText("Select Date"); // Buton başlangıç metni
    }

    private void initializeMoodMaps() {
        moodColorMap = new HashMap<>();
        moodColorMap.put("very_happy", Color.parseColor("#1F2E4A"));
        moodColorMap.put("happy", Color.parseColor("#1F2E4A"));
        moodColorMap.put("neutral", Color.parseColor("#1F2E4A"));
        moodColorMap.put("sad", Color.parseColor("#1F2E4A"));
        moodColorMap.put("very_sad", Color.parseColor("#1F2E4A"));

        moodDrawableMap = new HashMap<>();
        moodDrawableMap.put("very_happy", R.drawable.sentiment_verryhappy);
        moodDrawableMap.put("happy", R.drawable.sentiment_happy);
        moodDrawableMap.put("neutral", R.drawable.sentiment_neutral_24dp_d16d6a_f_ll0_wght400_grad0_opsz24);
        moodDrawableMap.put("sad", R.drawable.sentiment_sad);
        moodDrawableMap.put("very_sad", R.drawable.sentiment_verrysad);

        moodSmallDrawableMap = new HashMap<>();
        moodSmallDrawableMap.put("very_happy", R.drawable.sentiment_verryhappy);
        moodSmallDrawableMap.put("happy", R.drawable.sentiment_happy);
        moodSmallDrawableMap.put("neutral", R.drawable.sentiment_neutral_24dp_d16d6a_f_ll0_wght400_grad0_opsz24);
        moodSmallDrawableMap.put("sad", R.drawable.sentiment_sad);
        moodSmallDrawableMap.put("very_sad", R.drawable.sentiment_verrysad);
    }

    private void setupListeners() {
        // Tarih seçme butonu
        btnSelectDate.setOnClickListener(v -> showDatePickerDialog());

        // Takvimde tarih seçme
        calendarView.setOnDateChangedListener((widget, calendarDay, selected) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(calendarDay.getYear(), calendarDay.getMonth() - 1, calendarDay.getDay());
            currentSelectedDate = getNormalizedDate(calendar.getTime());
            checkIfCurrentDate();
            updateUIForSelectedDate();
        });

        // Aylık analiz butonu
        monthlyAnalysisButton.setOnClickListener(v -> showMonthlyAnalysis());

        // Not kaydetme butonu
        saveNoteButton.setOnClickListener(v -> saveDailyNote());

        // Not değişikliklerini dinleme
        dailyNoteEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                saveNoteButton.setEnabled(s.length() > 0 && isCurrentDate);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void checkIfCurrentDate() {
        Date today = getNormalizedDate(new Date());
        isCurrentDate = currentSelectedDate.equals(today);
        dailyNoteEditText.setEnabled(isCurrentDate);
        saveNoteButton.setEnabled(isCurrentDate && dailyNoteEditText.getText().length() > 0);
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentSelectedDate);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(year, month, dayOfMonth);
                    currentSelectedDate = getNormalizedDate(selectedCalendar.getTime());
                    checkIfCurrentDate();
                    updateUIForSelectedDate();

                    // Takvimde seçili tarihi güncelle
                    calendarView.setSelectedDate(CalendarDay.from(year, month + 1, dayOfMonth));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void updateUIForSelectedDate() {
        MoodEntry entry = moodDataMap.get(currentSelectedDate);

        if (entry != null) {
            // Seçilen tarihte kayıtlı veri varsa
            if (entry.mood != null) {
                selectedMood = entry.mood;
                selectedMoodContainer.setVisibility(View.VISIBLE);
                selectedMoodText.setText(getMoodDisplayName(selectedMood));
                selectedMoodEmoji.setImageResource(moodDrawableMap.get(selectedMood));

                int color = moodColorMap.get(selectedMood);
                selectedMoodContainer.getBackground().setTint(color);
            } else {
                selectedMoodContainer.setVisibility(View.GONE);
            }

            dailyNoteEditText.setText(entry.note != null ? entry.note : "");
        } else {
            // Seçilen tarihte kayıtlı veri yoksa
            selectedMoodContainer.setVisibility(View.GONE);
            dailyNoteEditText.setText("");
        }

        // Tarih butonunu güncelle (bugünse "Today", değilse tarih)
        if (isCurrentDate) {
            btnSelectDate.setText("Today");
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            btnSelectDate.setText(sdf.format(currentSelectedDate));
        }
    }

    public void selectMood(View view) {
        if (!isCurrentDate) {
            Toast.makeText(this, "You can only select mood for today", Toast.LENGTH_SHORT).show();
            return;
        }

        String mood = (String) view.getTag();
        selectedMood = mood;

        selectedMoodContainer.setVisibility(View.VISIBLE);
        selectedMoodText.setText(getMoodDisplayName(mood));
        selectedMoodEmoji.setImageResource(moodDrawableMap.get(mood));

        int color = moodColorMap.get(mood);
        selectedMoodContainer.getBackground().setTint(color);

        MoodEntry entry = moodDataMap.getOrDefault(currentSelectedDate, new MoodEntry());
        entry.mood = mood;
        moodDataMap.put(currentSelectedDate, entry);

        saveMoodEntry(currentSelectedDate, entry);
        updateCalendarDecorators();
    }

    private void saveDailyNote() {
        if (!isCurrentDate) {
            Toast.makeText(this, "You can only save notes for today", Toast.LENGTH_SHORT).show();
            return;
        }

        String note = dailyNoteEditText.getText().toString();

        MoodEntry entry = moodDataMap.getOrDefault(currentSelectedDate, new MoodEntry());
        entry.note = note;
        moodDataMap.put(currentSelectedDate, entry);

        saveMoodEntry(currentSelectedDate, entry);
        Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show();
    }

    private Date getNormalizedDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private void saveMoodEntry(Date date, MoodEntry entry) {
        Map<String, Object> moodEntry = new HashMap<>();
        moodEntry.put("date", date);
        moodEntry.put("mood", entry.mood);
        moodEntry.put("note", entry.note);
        moodEntry.put("userId", userId);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateString = sdf.format(date);

        db.collection("moodEntries")
                .document(userId + "_" + dateString)
                .set(moodEntry)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(DailyMood.this, "Failed to save data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadSavedMoods() {
        if (userId == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("moodEntries")
                .whereEqualTo("userId", userId)
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        moodDataMap.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Date date = document.getDate("date");
                                String mood = document.getString("mood");
                                String note = document.getString("note");

                                if (date != null && mood != null) {
                                    MoodEntry entry = new MoodEntry();
                                    entry.mood = mood;
                                    entry.note = note;
                                    moodDataMap.put(getNormalizedDate(date), entry);
                                }
                            } catch (Exception e) {
                                Log.e("Firestore", "Error processing document", e);
                            }
                        }
                        updateCalendarDecorators();
                        checkIfCurrentDate();
                        updateUIForSelectedDate();
                    } else {
                        Log.e("Firestore", "Error loading data", task.getException());
                        Toast.makeText(DailyMood.this, "Error loading data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateCalendarDecorators() {
        calendarView.removeDecorators();

        for (Map.Entry<Date, MoodEntry> entry : moodDataMap.entrySet()) {
            Date date = entry.getKey();
            String mood = entry.getValue().mood;

            Integer smallDrawableResId = moodSmallDrawableMap.get(mood);
            if (smallDrawableResId != null) {
                calendarView.addDecorator(new SmallMoodDecorator(date, smallDrawableResId));
            }
        }
    }

    private String getMoodDisplayName(String moodKey) {
        switch (moodKey) {
            case "very_happy": return "Very Happy";
            case "happy": return "Happy";
            case "neutral": return "Neutral";
            case "sad": return "Sad";
            case "very_sad": return "Very Sad";
            default: return "Unknown";
        }
    }

    private void showMonthlyAnalysis() {
        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentYear = calendar.get(Calendar.YEAR);

        // Mood sayılarını tutacak map
        Map<String, Integer> moodCounts = new HashMap<>();
        moodCounts.put("very_happy", 0);
        moodCounts.put("happy", 0);
        moodCounts.put("neutral", 0);
        moodCounts.put("sad", 0);
        moodCounts.put("very_sad", 0);

        // Seçilen aya ait moodları say
        for (Map.Entry<Date, MoodEntry> entry : moodDataMap.entrySet()) {
            calendar.setTime(entry.getKey());
            int entryMonth = calendar.get(Calendar.MONTH);
            int entryYear = calendar.get(Calendar.YEAR);

            if (entryMonth == currentMonth && entryYear == currentYear) {
                String mood = entry.getValue().mood;
                moodCounts.put(mood, moodCounts.get(mood) + 1);
            }
        }

        // Toplam giriş sayısını hesapla
        int totalEntries = moodCounts.get("very_happy") + moodCounts.get("happy") +
                moodCounts.get("neutral") + moodCounts.get("sad") +
                moodCounts.get("very_sad");

        if (totalEntries == 0) {
            Toast.makeText(this, "No data found for this month", Toast.LENGTH_SHORT).show();
            return;
        }

        // En sık görülen moodu bul
        String mostFrequentMood = "";
        int maxCount = 0;
        for (Map.Entry<String, Integer> entry : moodCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mostFrequentMood = entry.getKey();
            }
        }

        // Diyalog oluştur
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getMonthName(currentMonth) + " " + currentYear + " Analysis");

        // Özel layout inflate et
        View view = LayoutInflater.from(this).inflate(R.layout.monthly_analysis_dialog, null);
        builder.setView(view);

        // View'leri bul
        LinearLayout chartContainer = view.findViewById(R.id.chartContainer);
        TextView veryHappyText = view.findViewById(R.id.veryHappyText);
        TextView happyText = view.findViewById(R.id.happyText);
        TextView neutralText = view.findViewById(R.id.neutralText);
        TextView sadText = view.findViewById(R.id.sadText);
        TextView verySadText = view.findViewById(R.id.verySadText);
        TextView summaryText = view.findViewById(R.id.summaryText);

        // Mood sayılarını ayarla
        veryHappyText.setText(getMoodDisplayName("very_happy") + ": " + moodCounts.get("very_happy"));
        happyText.setText(getMoodDisplayName("happy") + ": " + moodCounts.get("happy"));
        neutralText.setText(getMoodDisplayName("neutral") + ": " + moodCounts.get("neutral"));
        sadText.setText(getMoodDisplayName("sad") + ": " + moodCounts.get("sad"));
        verySadText.setText(getMoodDisplayName("very_sad") + ": " + moodCounts.get("very_sad"));

        // Grafik oluştur
        chartContainer.removeAllViews();
        int maxValue = Collections.max(moodCounts.values());

        String[] moodOrder = {"very_happy", "happy", "neutral", "sad", "very_sad"};
        for (String mood : moodOrder) {
            int count = moodCounts.get(mood);
            if (count == 0) continue;

            // Bar view oluştur
            View barView = new View(this);
            int barHeight = (int) (200 * ((float) count / maxValue));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    120,
                    barHeight
            );
            params.setMargins(20, 200 - barHeight, 20, 0);
            barView.setLayoutParams(params);
            barView.setBackgroundColor(moodColorMap.get(mood));

            // Etiket oluştur
            TextView label = new TextView(this);
            label.setText(String.valueOf(count));
            label.setTextColor(Color.BLACK);
            label.setTextSize(12);
            label.setGravity(Gravity.CENTER_HORIZONTAL);

            LinearLayout barContainer = new LinearLayout(this);
            barContainer.setOrientation(LinearLayout.VERTICAL);
            barContainer.addView(barView);
            barContainer.addView(label);

            chartContainer.addView(barContainer);
        }

        // Özet metni ayarla
        String summary = String.format(Locale.getDefault(),
                "You have recorded a total of %d daily moods this month.\n" +
                        "Most frequent mood: %s (%d times)",
                totalEntries, getMoodDisplayName(mostFrequentMood), maxCount);

        summaryText.setText(summary);

        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private String getMonthName(int month) {
        return new SimpleDateFormat("MMMM", Locale.getDefault()).format(new Date(0, month, 1));
    }

    private static class MoodEntry {
        String mood;
        String note;
    }

    private class SmallMoodDecorator implements DayViewDecorator {
        private final Date date;
        private final int drawableResId;

        public SmallMoodDecorator(Date date, int drawableResId) {
            this.date = date;
            this.drawableResId = drawableResId;
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            return day.getYear() == calendar.get(Calendar.YEAR) &&
                    day.getMonth() == calendar.get(Calendar.MONTH) + 1 &&
                    day.getDay() == calendar.get(Calendar.DAY_OF_MONTH);
        }

        @Override
        public void decorate(DayViewFacade view) {
            view.setBackgroundDrawable(ContextCompat.getDrawable(DailyMood.this, drawableResId));
        }
    }
}