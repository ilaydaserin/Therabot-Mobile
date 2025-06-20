package com.ilaydas.therabotmobile;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.app.Activity; // Activity sınıfını import et
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable; // @Nullable annotation'ı için import

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.ilaydas.therabotmobile.PsychologistAdapter;
import com.ilaydas.therabotmobile.Psychologist;

import java.util.ArrayList;
import java.util.List;

public class Psychologists extends AppCompatActivity {

    private RecyclerView psychologistRecyclerView;
    private PsychologistAdapter psychologistAdapter;
    private List<Psychologist> psychologistList;
    private ProgressBar loadingProgressBar;
    private Button btnPostAd;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private static final int REQUEST_CODE_EDIT_PROFILE = 1; // Unique request code for PsychologistProfileEdit

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_psychologists);

        psychologistRecyclerView = findViewById(R.id.psychologist_recycler_view);
        loadingProgressBar = findViewById(R.id.loading_progress_bar);
        btnPostAd = findViewById(R.id.btn_post_ad);

        psychologistList = new ArrayList<>();
        psychologistAdapter = new PsychologistAdapter(this, psychologistList);
        psychologistRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        psychologistRecyclerView.setAdapter(psychologistAdapter);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initial load of psychologists when the activity is created
        loadPsychologists();

        // Listener for the "Post Ad" button
        btnPostAd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null) {
                    // If user is logged in, navigate to the profile edit screen
                    Intent intent = new Intent(Psychologists.this, PsychologistProfileEdit.class);
                    // Start activity for result to know when it finishes and if changes occurred
                    startActivityForResult(intent, REQUEST_CODE_EDIT_PROFILE);
                } else {
                    // If user is not logged in, show a warning
                    Toast.makeText(Psychologists.this, "Please log in to post an ad.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // Method to load psychologists from Firestore
    private void loadPsychologists() {
        loadingProgressBar.setVisibility(View.VISIBLE);
        db.collection("psychologists")
                .whereEqualTo("isVerified", true) // Assuming you only want to show verified psychologists
                .get()
                .addOnCompleteListener(task -> {
                    loadingProgressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        psychologistList.clear(); // Clear existing list before adding new data

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Psychologist psychologist = document.toObject(Psychologist.class);
                                psychologist.setId(document.getId()); // Set the document ID to the psychologist object
                                psychologistList.add(psychologist);
                                Log.d("FirestoreData", "Psychologist loaded: " + psychologist.getName() + " (ID: " + psychologist.getId() + ")");
                            } catch (Exception e) {
                                Log.e("FirestoreError", "Error converting document to Psychologist object: " + document.getId() + " - " + e.getMessage(), e);
                                Toast.makeText(Psychologists.this, "Error processing data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }

                        psychologistAdapter.notifyDataSetChanged(); // Notify adapter that data has changed

                        if (psychologistList.isEmpty()) {
                            Toast.makeText(Psychologists.this, "No verified psychologists found in the database.", Toast.LENGTH_LONG).show();
                            Log.d("FirestoreData", "Psychologist list is empty.");
                        } else {
                            Log.d("FirestoreData", psychologistList.size() + " psychologists successfully loaded.");
                        }
                    } else {
                        Toast.makeText(Psychologists.this, "Error loading psychologists: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("FirestoreError", "General error loading psychologist data: ", task.getException());
                    }
                });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_EDIT_PROFILE) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Listing updated successfully!", Toast.LENGTH_SHORT).show();
                loadPsychologists(); // Reload the entire list
            }
            /*
            else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, "Listing operation cancelled or failed.", Toast.LENGTH_SHORT).show();
            }*/
        }
    }
}