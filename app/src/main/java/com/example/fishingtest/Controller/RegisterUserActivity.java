package com.example.fishingtest.Controller;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;

import com.example.fishingtest.Model.User;
import com.example.fishingtest.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Completed by Kan Bu on 8/06/2019.
 *
 * The controller for the "Register" activity.
 */

public class RegisterUserActivity<pulbic> extends AppCompatActivity {
    //Tag
    private final  String TAG = "User Registration";

    //Toolbar
    Toolbar mToolbar;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private AutoCompleteTextView mUsernameView;
    private EditText mPasswordView;
    private EditText mConfirmPasswordView;

    // Firebase instance variables
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_user);

        mEmailView = (AutoCompleteTextView) findViewById(R.id.register_email);
        mPasswordView = (EditText) findViewById(R.id.register_password);
        mConfirmPasswordView = (EditText) findViewById(R.id.register_confirm_password);
        mUsernameView = (AutoCompleteTextView) findViewById(R.id.register_username);

        // Keyboard sign in action
        mConfirmPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.integer.register_form_finished || id == EditorInfo.IME_NULL) {
                    attemptRegistration();
                    return true;
                }
                return false;
            }
        });

        // Get hold of an instance of FirebaseAuth
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(RegisterUserActivity.this, LogInActivity.class);
        finish();
        startActivity(intent);
    }

    // Executed when Sign Up button is pressed.
    public void signUp(View v) {
        attemptRegistration();
    }

    // Validate view values and update the Firebase
    private void attemptRegistration() {

        // Reset errors displayed in the form.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString().trim();
        String password = mPasswordView.getText().toString().trim();

        boolean cancel = false;
        View focusView = null;

        // For debugging purpose
        Log.d(TAG, "TextUtils.isEmpty(password): " + TextUtils.isEmpty(password));
        Log.d(TAG, "TextUtils.isEmpty(password) && !isPasswordValid(password): " + (TextUtils.isEmpty(password) && !isPasswordValid(password)));

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password) || !isPasswordValid(password)) {
            Log.d(TAG, "Password Invalid");
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }
        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Call create FirebaseUser()
            createFirebaseUser();
        }
    }

    // Validate the given Email address
    private boolean isEmailValid(String email) {
        // Checking logic
        return email.contains("@");
    }

    // Validate the given Password
    private boolean isPasswordValid(String password) {
        // Add own logic to check for a valid password
        String confirmPassword = mConfirmPasswordView.getText().toString();
        return confirmPassword.equals(password) && password.length() > 6;
    }

    // Create a Firebase user
    private void createFirebaseUser() {
        String email = mEmailView.getText().toString().trim();
        String password = mPasswordView.getText().toString().trim();

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this,
                new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "createUser onComplete: " + task.isSuccessful());

                        if(!task.isSuccessful()){
                            Log.d(TAG, "user creation failed", task.getException());
                            showErrorDialog("Registration attempt failed");
                        } else {
                            saveDisplayName();
                            addToDatabase();
                            Intent intent = new Intent(RegisterUserActivity.this, HomePageActivity.class);
                            finish();
                            startActivity(intent);
                        }
                    }
                });
    }


    // Save the display name to Shared Preferences
    private void saveDisplayName() {
        FirebaseUser user = mAuth.getCurrentUser();
        String displayName = mUsernameView.getText().toString().trim();

        if (user !=null) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build();
            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "User name updated.");
                            }
                        }
                    });
        }

    }

    private void addToDatabase() {
        // Initialize the database
        DatabaseReference databaseUsers;
        databaseUsers = FirebaseDatabase.getInstance().getReference("Users");

        String id = mAuth.getCurrentUser().getUid();
        User user = new User(id, mEmailView.getText().toString().trim(),mPasswordView.getText().toString().trim(), mUsernameView.getText().toString().trim());
        // Add the new user to the Firebase base
        databaseUsers.child(id).setValue(user);
    }


    // Create an alert dialog to show in case registration failed
    private void showErrorDialog(String message){
        new AlertDialog.Builder(this)
                .setTitle("Oops")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();

    }

}
