package com.ilaydas.therabotmobile;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.ilaydas.therabotmobile.Psychologist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.hdodenhof.circleimageview.CircleImageView;

public class PsychologistProfileEdit extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PICK_DIPLOMA_REQUEST = 2;
    private static final int PICK_IDENTITY_REQUEST = 3;
    private static final String TAG = "PsychologistEdit";

    private CircleImageView ivProfileImage;
    private Button btnUploadImage;
    private EditText etEmail, etName, etSpecialty, etShortBio, etFullBio, etContactInfo, etWorkingHours, etExpertiseAreas;
    private Button btnSaveProfile;
    private ProgressBar progressBar;
    private Button btnDeleteListing;

    // Document upload components
    private Button btnUploadDiploma, btnUploadIdentityCard;
    private TextView tvDiplomaStatus, tvIdentityCardStatus;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private FirebaseAuth mAuth;

    private Uri imageUri;
    private String currentProfileImageUrl;

    // New document URIs and existing document URLs
    private Uri diplomaUri, identityCardUri;
    private String currentDiplomaUrl, currentIdentityCardUrl;

    // Variable to hold the current user's UID
    private String currentUserId;

    // Flag to track if an asynchronous operation is ongoing
    private boolean isOperationOngoing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_psychologist_profile_edit);

        // Initialize UI Components
        ivProfileImage = findViewById(R.id.iv_profile_image);
        btnUploadImage = findViewById(R.id.btn_upload_image);
        etEmail = findViewById(R.id.et_email);
        etName = findViewById(R.id.et_name);
        etSpecialty = findViewById(R.id.et_specialty);
        etShortBio = findViewById(R.id.et_short_bio);
        etFullBio = findViewById(R.id.et_full_bio);
        etContactInfo = findViewById(R.id.et_contact_info);
        etWorkingHours = findViewById(R.id.et_working_hours);
        etExpertiseAreas = findViewById(R.id.et_expertise_areas);
        btnSaveProfile = findViewById(R.id.btn_save_profile);
        progressBar = findViewById(R.id.profile_edit_progress_bar);
        btnDeleteListing = findViewById(R.id.btn_delete_listing);

        // Document upload UI components
        btnUploadDiploma = findViewById(R.id.btn_upload_diploma);
        btnUploadIdentityCard = findViewById(R.id.btn_upload_identity_card);
        tvDiplomaStatus = findViewById(R.id.tv_diploma_status);
        tvIdentityCardStatus = findViewById(R.id.tv_identity_card_status);

        // Firebase References
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        mAuth = FirebaseAuth.getInstance();

        // Get UID if user is logged in and automatically load profile
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            etEmail.setText(currentUser.getEmail());
            etEmail.setEnabled(false);
            loadProfileData(currentUserId);
        } else {
            Toast.makeText(this, "Please log in first.", Toast.LENGTH_LONG).show();
            setResult(Activity.RESULT_CANCELED);
            finish();
            return;
        }

        // Image Upload Button Listener
        btnUploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser(PICK_IMAGE_REQUEST, "image/*");
            }
        });

        // Document Upload Buttons
        btnUploadDiploma.setOnClickListener(v -> openFileChooser(PICK_DIPLOMA_REQUEST, "application/pdf,image/*"));
        btnUploadIdentityCard.setOnClickListener(v -> openFileChooser(PICK_IDENTITY_REQUEST, "application/pdf,image/*"));

        // Save Profile Button
        btnSaveProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Set flag to true when save operation starts
                isOperationOngoing = true;
                saveProfileData();
            }
        });

        // Delete Listing Button Listener
        btnDeleteListing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Set flag to true when delete operation starts
                isOperationOngoing = true;
                showDeleteConfirmationDialog();
            }
        });
    }

    private void openFileChooser(int requestCode, String mimeTypes) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(mimeTypes);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select a File"), requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri selectedUri = data.getData();
            if (requestCode == PICK_IMAGE_REQUEST) {
                imageUri = selectedUri;
                Glide.with(this).load(imageUri).into(ivProfileImage);
            } else if (requestCode == PICK_DIPLOMA_REQUEST) {
                diplomaUri = selectedUri;
                tvDiplomaStatus.setText(getFileName(selectedUri));
            } else if (requestCode == PICK_IDENTITY_REQUEST) {
                identityCardUri = selectedUri;
                tvIdentityCardStatus.setText(getFileName(selectedUri));
            }
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private boolean isValidEmail(String email) {
        Pattern pattern = Pattern.compile("^[_A-Za-z0-9-+]+(.[_A-Za-z0-9-]+)*@" +
                "[A-Za-z0-9-]+(.[_A-Za-z0-9]+)*(.[A-Za-z]{2,})$");
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    private void loadProfileData(String userId) {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("psychologists").document(userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Psychologist psychologist = document.toObject(Psychologist.class);
                                if (psychologist != null) {
                                    etName.setText(psychologist.getName());
                                    etSpecialty.setText(psychologist.getSpecialty());
                                    etShortBio.setText(psychologist.getShortBio());
                                    etFullBio.setText(psychologist.getFullBio());
                                    etContactInfo.setText(psychologist.getContactInfo());
                                    etWorkingHours.setText(psychologist.getWorkingHours());

                                    if (psychologist.getExpertiseAreas() != null && !psychologist.getExpertiseAreas().isEmpty()) {
                                        etExpertiseAreas.setText(TextUtils.join(", ", psychologist.getExpertiseAreas()));
                                    } else {
                                        etExpertiseAreas.setText("");
                                    }

                                    currentProfileImageUrl = psychologist.getProfileImageUrl();
                                    if (currentProfileImageUrl != null && !currentProfileImageUrl.isEmpty()) {
                                        Glide.with(PsychologistProfileEdit.this)
                                                .load(currentProfileImageUrl)
                                                .placeholder(R.drawable.psikolog)
                                                .error(R.drawable.psikolog)
                                                .into(ivProfileImage);
                                    } else {
                                        ivProfileImage.setImageResource(R.drawable.psikolog);
                                    }

                                    Map<String, String> verificationDocs = psychologist.getVerificationDocuments();
                                    if (verificationDocs != null) {
                                        currentDiplomaUrl = verificationDocs.get("diplomaUrl");
                                        currentIdentityCardUrl = verificationDocs.get("identityCardUrl");

                                        tvDiplomaStatus.setText(currentDiplomaUrl != null && !currentDiplomaUrl.isEmpty() ? "Uploaded: " + getFileNameFromUrl(currentDiplomaUrl) : "No File Selected");
                                        tvIdentityCardStatus.setText(currentIdentityCardUrl != null && !currentIdentityCardUrl.isEmpty() ? "Uploaded: " + getFileNameFromUrl(currentIdentityCardUrl) : "No File Selected");
                                    } else {
                                        clearDocumentStatus();
                                    }

                                }
                            } else {
                                clearFormFields();
                            }
                        } else {
                            Toast.makeText(PsychologistProfileEdit.this, "Error loading listing: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Listing loading error", task.getException());
                        }
                    }
                });
    }

    private void clearFormFields() {
        etName.setText("");
        etSpecialty.setText("");
        etShortBio.setText("");
        etFullBio.setText("");
        etContactInfo.setText("");
        etWorkingHours.setText("");
        etExpertiseAreas.setText("");
        ivProfileImage.setImageResource(R.drawable.psikolog);
        currentProfileImageUrl = null;
        imageUri = null;

        diplomaUri = null;
        identityCardUri = null;
        currentDiplomaUrl = null;
        currentIdentityCardUrl = null;
        clearDocumentStatus();
    }

    private void clearDocumentStatus() {
        tvDiplomaStatus.setText("No File Selected");
        tvIdentityCardStatus.setText("No File Selected");
    }

    private String getFileNameFromUrl(String url) {
        if (url == null || url.isEmpty()) return "Uploaded File";
        try {
            Uri uri = Uri.parse(url);
            String path = uri.getLastPathSegment();
            if (path != null) {
                String decodedPath = Uri.decode(path);
                int slashIndex = decodedPath.lastIndexOf('/');
                if (slashIndex != -1) {
                    decodedPath = decodedPath.substring(slashIndex + 1);
                }

                Pattern pattern = Pattern.compile("(.+?)(_\\d+)?(\\.\\w+)$");
                Matcher matcher = pattern.matcher(decodedPath);
                if (matcher.find()) {
                    return matcher.group(1) + matcher.group(3);
                }
                return decodedPath;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse file name from URL: " + url, e);
        }
        return "Uploaded File";
    }

    private String getFileExtension(Uri uri) {
        String extension = "";
        String mimeType = getContentResolver().getType(uri);
        if (mimeType != null) {
            switch (mimeType) {
                case "image/jpeg":
                    extension = "jpg";
                    break;
                case "image/png":
                    extension = "png";
                    break;
                case "application/pdf":
                    extension = "pdf";
                    break;
                default:
                    String file = uri.getLastPathSegment();
                    if (file != null && file.contains(".")) {
                        extension = file.substring(file.lastIndexOf(".") + 1);
                    }
                    break;
            }
        }
        return extension;
    }

    private void saveProfileData() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "User session not found. Please log in again.", Toast.LENGTH_LONG).show();
            progressBar.setVisibility(View.GONE); // Make sure it's gone if user session fails
            isOperationOngoing = false; // Operation finished due to user session error
            return;
        }

        String email = etEmail.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String specialty = etSpecialty.getText().toString().trim();
        String shortBio = etShortBio.getText().toString().trim();
        String fullBio = etFullBio.getText().toString().trim();
        String contactInfo = etContactInfo.getText().toString().trim();
        String workingHours = etWorkingHours.getText().toString().trim();
        String expertiseAreasText = etExpertiseAreas.getText().toString().trim();

        // --- VALIDATION SECTION ---
        if (email.isEmpty() || !isValidEmail(email)) {
            etEmail.setError("A valid email address is required!");
            etEmail.requestFocus();
            progressBar.setVisibility(View.GONE); // Hide progress bar on validation failure
            isOperationOngoing = false; // Operation finished due to validation error
            return;
        }
        if (name.isEmpty()) {
            etName.setError("Full Name cannot be empty!");
            etName.requestFocus();
            progressBar.setVisibility(View.GONE); // Hide progress bar on validation failure
            isOperationOngoing = false; // Operation finished due to validation error
            return;
        }
        if (specialty.isEmpty()) {
            etSpecialty.setError("Specialty cannot be empty!");
            etSpecialty.requestFocus();
            progressBar.setVisibility(View.GONE); // Hide progress bar on validation failure
            isOperationOngoing = false; // Operation finished due to validation error
            return;
        }
        if (contactInfo.isEmpty()) {
            etContactInfo.setError("Contact Information cannot be empty!");
            etContactInfo.requestFocus();
            progressBar.setVisibility(View.GONE); // Hide progress bar on validation failure
            isOperationOngoing = false; // Operation finished due to validation error
            return;
        }
        // --- END VALIDATION SECTION ---

        progressBar.setVisibility(View.VISIBLE); // Show progress bar immediately after validation
        // Show the desired Toast message immediately when the save process starts
        Toast.makeText(this, "Your information is being saved to the system.", Toast.LENGTH_SHORT).show();


        List<String> expertiseAreasList = new ArrayList<>();
        if (!expertiseAreasText.isEmpty()) {
            String[] areasArray = expertiseAreasText.split("\\s*,\\s*");
            for (String area : areasArray) {
                if (!area.isEmpty()) {
                    expertiseAreasList.add(area);
                }
            }
        }

        Map<String, Object> psychologistData = new HashMap<>();
        psychologistData.put("name", name);
        psychologistData.put("specialty", specialty);
        psychologistData.put("shortBio", shortBio);
        psychologistData.put("fullBio", fullBio);
        psychologistData.put("contactInfo", contactInfo);
        psychologistData.put("workingHours", workingHours);
        psychologistData.put("expertiseAreas", expertiseAreasList);
        psychologistData.put("isVerified", false); // Default to false until verified by admin
        psychologistData.put("registrationDate", FieldValue.serverTimestamp());
        psychologistData.put("email", email);


        List<Task<Uri>> uploadTasks = new ArrayList<>();

        if (imageUri != null) {
            String fileExtension = getFileExtension(imageUri);
            StorageReference fileReference = storageReference.child("profile_images/" + currentUserId + "." + fileExtension);
            uploadTasks.add(fileReference.putFile(imageUri).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return fileReference.getDownloadUrl();
                }
            }).addOnSuccessListener(uri -> psychologistData.put("profileImageUrl", uri.toString())));
        } else if (currentProfileImageUrl != null && !currentProfileImageUrl.isEmpty()) {
            psychologistData.put("profileImageUrl", currentProfileImageUrl);
        } else {
            psychologistData.put("profileImageUrl", "");
        }

        Map<String, String> verificationDocuments = new HashMap<>();

        if (currentDiplomaUrl != null && diplomaUri == null) verificationDocuments.put("diplomaUrl", currentDiplomaUrl);
        if (currentIdentityCardUrl != null && identityCardUri == null) verificationDocuments.put("identityCardUrl", currentIdentityCardUrl);

        if (diplomaUri != null) {
            String fileExtension = getFileExtension(diplomaUri);
            StorageReference diplomaRef = storageReference.child("verification_documents/" + currentUserId + "_diploma_" + System.currentTimeMillis() + "." + fileExtension);
            uploadTasks.add(diplomaRef.putFile(diplomaUri).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return diplomaRef.getDownloadUrl();
                }
            }).addOnSuccessListener(uri -> verificationDocuments.put("diplomaUrl", uri.toString())));
        }

        if (identityCardUri != null) {
            String fileExtension = getFileExtension(identityCardUri);
            StorageReference identityRef = storageReference.child("verification_documents/" + currentUserId + "_identity_" + System.currentTimeMillis() + "." + fileExtension);
            uploadTasks.add(identityRef.putFile(identityCardUri).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return identityRef.getDownloadUrl();
                }
            }).addOnSuccessListener(uri -> verificationDocuments.put("identityCardUrl", uri.toString())));
        }

        if (!uploadTasks.isEmpty()) {
            Tasks.whenAllSuccess(uploadTasks)
                    .addOnSuccessListener(o -> {
                        psychologistData.put("verificationDocuments", verificationDocuments);
                        saveProfileToFirestore(currentUserId, psychologistData);
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(PsychologistProfileEdit.this, "Error uploading files: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "FILE_UPLOAD_ERROR: File upload error", e);
                        setResult(Activity.RESULT_CANCELED); // Set result to canceled on file upload error
                        isOperationOngoing = false; // Operation finished with failure
                        finish(); // Close activity
                    });
        } else {
            psychologistData.put("verificationDocuments", verificationDocuments);
            saveProfileToFirestore(currentUserId, psychologistData);
        }
    }

    private void saveProfileToFirestore(String userId, Map<String, Object> psychologistData) {
        db.collection("psychologists").document(userId)
                .set(psychologistData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // This Toast indicates successful saving to Firestore and admin approval message
                        Toast.makeText(PsychologistProfileEdit.this, "Your listing has been successfully saved! Awaiting admin approval.", Toast.LENGTH_LONG).show();
                        progressBar.setVisibility(View.GONE);

                        Log.d(TAG, "FIRESTORE_SAVE_SUCCESS: Listing successfully saved.");
                        setResult(Activity.RESULT_OK);
                        isOperationOngoing = false; // Operation finished with success
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressBar.setVisibility(View.GONE);
                        // This Toast indicates failure during Firestore save
                        Toast.makeText(PsychologistProfileEdit.this, "FIRESTORE_SAVE_ERROR: Error saving listing: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "FIRESTORE_SAVE_ERROR: Listing saving error", e);
                        setResult(Activity.RESULT_CANCELED);
                        isOperationOngoing = false; // Operation finished with failure
                        finish();
                    }
                });
    }

    private void showDeleteConfirmationDialog() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "User session not found. Cannot delete listing.", Toast.LENGTH_LONG).show();
            isOperationOngoing = false; // Operation finished
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Listing")
                .setMessage("Are you sure you want to delete your listing? This action cannot be undone.")
                .setPositiveButton("Yes, Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        deleteListing(currentUserId);
                    }
                })
                .setNegativeButton("No, Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // If user cancels deletion, reset the flag
                        isOperationOngoing = false;
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteListing(String userId) {
        progressBar.setVisibility(View.VISIBLE);
        Log.d(TAG, "Delete operation started. ProgressBar visible.");

        db.collection("psychologists").document(userId)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Firestore document deleted for user: " + userId);
                        deleteAssociatedStorageFiles(userId);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(PsychologistProfileEdit.this, "FIRESTORE_DELETE_ERROR: Error deleting listing from database: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "FIRESTORE_DELETE_ERROR: Error deleting listing from Firestore", e);
                        setResult(Activity.RESULT_CANCELED);
                        isOperationOngoing = false; // Operation finished with failure
                        finish();
                    }
                });
    }

    private void deleteAssociatedStorageFiles(String userId) {
        List<Task<Void>> deleteTasks = new ArrayList<>();

        StorageReference profileImagesFolderRef = storageReference.child("profile_images/");
        profileImagesFolderRef.listAll()
                .addOnSuccessListener(new OnSuccessListener<ListResult>() {
                    @Override
                    public void onSuccess(ListResult listResult) {
                        for (StorageReference item : listResult.getItems()) {
                            if (item.getName().startsWith(userId + ".")) {
                                deleteTasks.add(item.delete()
                                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Deleted profile image: " + item.getName()))
                                        .addOnFailureListener(e -> Log.w(TAG, "Error deleting profile image: " + item.getName(), e)));
                            }
                        }
                        addVerificationDocsDeleteTasks(userId, deleteTasks);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "STORAGE_LIST_PROFILE_ERROR: Error listing profile images for deletion: " + e.getMessage(), e);
                    addVerificationDocsDeleteTasks(userId, deleteTasks);
                });
    }

    private void addVerificationDocsDeleteTasks(String userId, List<Task<Void>> deleteTasks) {
        StorageReference verificationDocsFolderRef = storageReference.child("verification_documents/");
        verificationDocsFolderRef.listAll()
                .addOnSuccessListener(new OnSuccessListener<ListResult>() {
                    @Override
                    public void onSuccess(ListResult listResult) {
                        for (StorageReference item : listResult.getItems()) {
                            if (item.getName().startsWith(userId + "_diploma_") || item.getName().startsWith(userId + "_identity_")) {
                                deleteTasks.add(item.delete()
                                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Deleted verification doc: " + item.getName()))
                                        .addOnFailureListener(e -> Log.w(TAG, "Error deleting verification doc: " + item.getName(), e)));
                            }
                        }
                        Tasks.whenAllComplete(deleteTasks)
                                .addOnCompleteListener(allTasks -> {
                                    progressBar.setVisibility(View.GONE);
                                    if (allTasks.isSuccessful()) {
                                        Toast.makeText(PsychologistProfileEdit.this, "Listing and associated files deleted successfully.", Toast.LENGTH_LONG).show();
                                        Log.d(TAG, "DELETE_SUCCESS: All associated files deleted successfully.");
                                        setResult(Activity.RESULT_OK); // Signal success to previous activity
                                    } else {
                                        Toast.makeText(PsychologistProfileEdit.this, "DELETE_PARTIAL_FAILURE: Listing deleted from database, but some files could not be deleted from storage.", Toast.LENGTH_LONG).show();
                                        Log.e(TAG, "DELETE_PARTIAL_FAILURE: Some files could not be deleted from storage.", allTasks.getException());
                                        setResult(Activity.RESULT_CANCELED); // Signal partial failure to previous activity
                                    }
                                    isOperationOngoing = false; // Operation finished
                                    finish();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(PsychologistProfileEdit.this, "STORAGE_LIST_DOC_ERROR: Error listing verification documents for deletion: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "STORAGE_LIST_DOC_ERROR: Error listing verification documents for deletion", e);
                    setResult(Activity.RESULT_CANCELED); // Signal failure to previous activity
                    isOperationOngoing = false; // Operation finished
                    finish();
                });
    }

    // Override onBackPressed to prevent premature closing during operations
    @Override
    public void onBackPressed() {
        if (isOperationOngoing) {
            Toast.makeText(this, "Please wait for the current operation to complete.", Toast.LENGTH_SHORT).show();
            // Optionally, show a dialog asking user if they really want to cancel and lose progress
            // new AlertDialog.Builder(this)
            //     .setTitle("Operation in Progress")
            //     .setMessage("An operation is currently running. Do you want to cancel and exit? Your changes might not be saved.")
            //     .setPositiveButton("Yes, Exit", (dialog, which) -> {
            //         super.onBackPressed(); // Allow back press
            //     })
            //     .setNegativeButton("No, Wait", null)
            //     .show();
        } else {
            // If no operation is ongoing, proceed with normal back press behavior
            super.onBackPressed();
        }
    }
}