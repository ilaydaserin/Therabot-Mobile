package com.ilaydas.therabotmobile;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView; // Make sure this is imported
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.common.SignInButton; // Import this for Google Sign-In Button

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Register extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;

    private EditText fullNameEditText, dateOfBirthEditText, emailEditText, passwordEditText;
    private Button registerButton;
    private SignInButton googleSignUpButton;
    private TextView alreadyHaveAccountTextView;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseFirestore db;

    private Calendar calendar; // For DatePickerDialog

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Configure Google Sign In options (same as MainActivity)
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Ensure this string resource is correctly configured in strings.xml
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Initialize UI components
        fullNameEditText = findViewById(R.id.registerFullNameEditText);
        dateOfBirthEditText = findViewById(R.id.registerDateOfBirthEditText);
        emailEditText = findViewById(R.id.registerEmailEditText);
        passwordEditText = findViewById(R.id.registerPasswordEditText);
        registerButton = findViewById(R.id.registerButton);
        googleSignUpButton = findViewById(R.id.googleSignUpButton); // Get reference for SignInButton
        alreadyHaveAccountTextView = findViewById(R.id.alreadyHaveAccountTextView); // Get reference for the new TextView

        calendar = Calendar.getInstance(); // Initialize calendar for DatePickerDialog

        // Set click listener for DateOfBirth EditText to show DatePicker
        dateOfBirthEditText.setOnClickListener(v -> showDatePickerDialog());

        // Set click listeners for buttons
        registerButton.setOnClickListener(v -> registerUserWithEmail());
        googleSignUpButton.setOnClickListener(v -> signInWithGoogle());

        // Set click listener for "Already have an account?" text
        alreadyHaveAccountTextView.setOnClickListener(v -> {
            startActivity(new Intent(Register.this, MainActivity.class));
            finish(); // Finish Register activity to go back to MainActivity
        });
    }

    private void showDatePickerDialog() {
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateOfBirthField();
        };

        // Create and show DatePickerDialog
        new DatePickerDialog(
                Register.this,
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void updateDateOfBirthField() {
        // Define date format for display
        String myFormat = "dd/MM/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
        // Set the formatted date to the EditText
        dateOfBirthEditText.setText(sdf.format(calendar.getTime()));
    }

    private void registerUserWithEmail() {
        String fullName = fullNameEditText.getText().toString().trim();
        String dateOfBirth = dateOfBirthEditText.getText().toString().trim(); // Date of Birth as String
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Basic validation for all fields
        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(dateOfBirth) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Password length validation
        if (password.length() < 6) { // Firebase requires a minimum password length of 6 characters
            Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create user with Email and Password using Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // If authentication is successful, save additional user details to Firestore
                            saveUserDetailsToFirestore(user.getUid(), fullName, dateOfBirth, email);
                        }
                    } else {
                        // Registration failed, show error message
                        Toast.makeText(Register.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void signInWithGoogle() {
        // Start the Google Sign-In intent
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Handle the result of the Google Sign-In intent
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, now authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, show error message
                Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        // Build a Firebase credential with the Google ID token
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        // Authenticate with Firebase using the Google credential
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // For Google registration, we get email and display name from Google.
                            // Date of Birth is not provided by Google directly, so we use what's in the EditText.
                            String fullName = user.getDisplayName() != null ? user.getDisplayName() : fullNameEditText.getText().toString().trim();
                            String email = user.getEmail();
                            String dateOfBirth = dateOfBirthEditText.getText().toString().trim(); // Get from EditText

                            saveUserDetailsToFirestore(user.getUid(), fullName, dateOfBirth, email);
                        }
                    } else {
                        // Google Authentication failed, show error message
                        Toast.makeText(Register.this, "Google Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserDetailsToFirestore(String userId, String fullName, String dateOfBirth, String email) {
        // Create a Map to store user data
        Map<String, Object> user = new HashMap<>();
        user.put("fullName", fullName);
        user.put("dateOfBirth", dateOfBirth); // Storing Date of Birth as a String (e.g., "DD/MM/YYYY")
        user.put("email", email);
        // You might consider adding a creation timestamp: user.put("createdAt", FieldValue.serverTimestamp());

        // Save user data to Firestore in a "users" collection with the user's UID as the document ID
        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(Register.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                    // Navigate to Mainmenu after saving details
                    Intent intent = new Intent(Register.this, Mainmenu.class);
                    // Clear the activity stack to prevent going back to Register/Login screens
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish(); // Close Register activity
                })
                .addOnFailureListener(e -> {
                    //Toast.makeText(Register.this, "Failed to save user details: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    // Even if saving details to Firestore fails, the Firebase Auth user is created.
                    // You might want to log this error for debugging and still navigate the user to the main menu,
                    // or prompt them to complete their profile later.
                    Intent intent = new Intent(Register.this, Mainmenu.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
    }
}