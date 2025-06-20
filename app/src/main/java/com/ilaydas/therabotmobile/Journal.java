package com.ilaydas.therabotmobile;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Journal extends AppCompatActivity {

    private TextView txtSelectedDate, txtDayName;
    private EditText etJournalEntry;
    private MaterialButton btnSelectDate, btnSaveJournal; // 'saveNoteButton' olarak XML'de tanımladıysanız dikkat edin

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private Date selectedDate = new Date();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("d MMMM yyyy", Locale.getDefault()); // 'yyyy' ekledim
    private final SimpleDateFormat dayNameFormat = new SimpleDateFormat("EEEE", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journal);

        // Firebase başlat
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // View'ları bağla
        initViews();

        // Bugünün tarihini ayarla
        updateDateUI(new Date());

        // Tarih seçme butonu
        btnSelectDate.setOnClickListener(v -> showDatePicker());

        // Kaydet butonu
        btnSaveJournal.setOnClickListener(v -> saveJournalEntry()); // btnSaveJournal kullanıyoruz

        // Seçilen tarihin günlüğünü yükle
        loadJournalEntry();
    }

    private void initViews() {
        txtSelectedDate = findViewById(R.id.txtSelectedDate);
        txtDayName = findViewById(R.id.txtDayName);
        etJournalEntry = findViewById(R.id.etJournalEntry);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        // XML'de saveNoteButton olarak ID verdiyseniz burayı düzeltmelisiniz
        // Örneğin: btnSaveJournal = findViewById(R.id.saveNoteButton);
        btnSaveJournal = findViewById(R.id.btnSaveJournal); // Şu anki kodunuzda bu ID var
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(selectedDate);

        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(year, month, dayOfMonth);
                    selectedDate = selectedCalendar.getTime();
                    updateDateUI(selectedDate);
                    loadJournalEntry();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Gelecek tarihleri seçmeyi engellemek isterseniz (opsiyonel)
        datePicker.getDatePicker().setMaxDate(System.currentTimeMillis());

        datePicker.show();
    }

    private void updateDateUI(Date date) {
        txtSelectedDate.setText(displayDateFormat.format(date));
        txtDayName.setText(dayNameFormat.format(date));

        // Tarih bugünden farklıysa EditText'i devre dışı bırak
        // ve ipucunu değiştir, kaydetme butonunu pasif et.
        if (isToday(date)) {
            etJournalEntry.setEnabled(true);
            etJournalEntry.setHint("What did you experience today? Write your thoughts...");
            btnSaveJournal.setEnabled(true);
        } else {
            etJournalEntry.setEnabled(false);
            etJournalEntry.setHint("Journal entries for past days cannot be edited.");
            btnSaveJournal.setEnabled(false);
        }
    }

    private boolean isToday(Date date) {
        Calendar today = Calendar.getInstance();
        Calendar selected = Calendar.getInstance();
        selected.setTime(date);

        return today.get(Calendar.YEAR) == selected.get(Calendar.YEAR) &&
                today.get(Calendar.MONTH) == selected.get(Calendar.MONTH) &&
                today.get(Calendar.DAY_OF_MONTH) == selected.get(Calendar.DAY_OF_MONTH);
    }

    private void loadJournalEntry() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "You must log in!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        String dateKey = dateFormat.format(selectedDate);

        db.collection("journals")
                .document(userId)
                .collection("entries")
                .document(dateKey)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            etJournalEntry.setText(document.getString("content"));
                        } else {
                            etJournalEntry.setText("");
                        }
                    } else {
                        Toast.makeText(this, "An error occurred while loading data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveJournalEntry() {
        // Yalnızca bugünün günlüğü düzenlenebilir kontrolü
        if (!isToday(selectedDate)) {
            Toast.makeText(this, "You can only edit today's journal entry.", Toast.LENGTH_LONG).show();
            return;
        }

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "You must log in", Toast.LENGTH_SHORT).show();
            return;
        }

        String content = etJournalEntry.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Please enter diary entry", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        String dateKey = dateFormat.format(selectedDate);

        Map<String, Object> journalEntry = new HashMap<>();
        journalEntry.put("date", selectedDate);
        journalEntry.put("content", content);
        journalEntry.put("timestamp", System.currentTimeMillis());

        db.collection("journals")
                .document(userId)
                .collection("entries")
                .document(dateKey)
                .set(journalEntry)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Journal saved", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "An error occurred while registering", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}