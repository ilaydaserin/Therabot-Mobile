package com.ilaydas.therabotmobile;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
// import android.widget.Button; // Bu satırı artık ihtiyacımız olmadığı için kaldırabiliriz
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import de.hdodenhof.circleimageview.CircleImageView;
import com.ilaydas.therabotmobile.Psychologist;

import java.util.Arrays; // Şu an kullanılmıyor, kaldırılabilir eğer TextUtils.join kullanılıyorsa

public class PsychologistDetail extends AppCompatActivity {

    private CircleImageView detailPsychologistImage;
    private TextView detailPsychologistName, detailPsychologistSpecialty,
            detailPsychologistFullBio, detailPsychologistExpertiseAreas,
            detailPsychologistContactInfo, detailPsychologistWorkingHours;
    private ProgressBar detailLoadingProgressBar;

    private FirebaseFirestore db;
    private String psychologistId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.psychologist_detail);

        detailPsychologistImage = findViewById(R.id.detail_psychologist_image);
        detailPsychologistName = findViewById(R.id.detail_psychologist_name);
        detailPsychologistSpecialty = findViewById(R.id.detail_psychologist_specialty);
        detailPsychologistFullBio = findViewById(R.id.detail_psychologist_full_bio);
        detailPsychologistExpertiseAreas = findViewById(R.id.detail_psychologist_expertise_areas);
        detailPsychologistContactInfo = findViewById(R.id.detail_psychologist_contact_info);
        detailPsychologistWorkingHours = findViewById(R.id.detail_psychologist_working_hours);
        // btnMakeAppointment = findViewById(R.id.btn_make_appointment);
        detailLoadingProgressBar = findViewById(R.id.detail_loading_progress_bar);

        db = FirebaseFirestore.getInstance();

        psychologistId = getIntent().getStringExtra("psychologistId");
        if (psychologistId != null) {
            loadPsychologistDetails(psychologistId);
        } else {
            Toast.makeText(this, "Psychologist information not found.", Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    private void loadPsychologistDetails(String id) {
        detailLoadingProgressBar.setVisibility(View.VISIBLE);
        db.collection("psychologists").document(id)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    detailLoadingProgressBar.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        Psychologist psychologist = documentSnapshot.toObject(Psychologist.class);
                        if (psychologist != null) {
                            detailPsychologistName.setText(psychologist.getName());
                            detailPsychologistSpecialty.setText(psychologist.getSpecialty());
                            detailPsychologistFullBio.setText(psychologist.getFullBio());

                            if (psychologist.getExpertiseAreas() != null && !psychologist.getExpertiseAreas().isEmpty()) {
                                detailPsychologistExpertiseAreas.setText(android.text.TextUtils.join(", ", psychologist.getExpertiseAreas()));

                            } else {
                                detailPsychologistExpertiseAreas.setText("Not specified");
                            }

                            detailPsychologistContactInfo.setText(psychologist.getContactInfo());
                            detailPsychologistWorkingHours.setText(psychologist.getWorkingHours());

                            if (psychologist.getProfileImageUrl() != null && !psychologist.getProfileImageUrl().isEmpty()) {
                                Glide.with(this)
                                        .load(psychologist.getProfileImageUrl())
                                        .placeholder(R.drawable.psikolog)
                                        .error(R.drawable.psikolog)
                                        .into(detailPsychologistImage);
                            } else {
                                detailPsychologistImage.setImageResource(R.drawable.mood1);
                            }
                        }
                    } else {
                        Toast.makeText(PsychologistDetail.this, "Psychologist not found.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    detailLoadingProgressBar.setVisibility(View.GONE);
                    Toast.makeText(PsychologistDetail.this, "Error loading psychologist details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
}