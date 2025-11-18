package com.example.btl_androidnc_music.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.example.btl_androidnc_music.R;
import com.example.btl_androidnc_music.auth.AuthManager;
import com.example.btl_androidnc_music.data.db.AppDatabase;
import com.example.btl_androidnc_music.data.model.User;
import com.example.btl_androidnc_music.databinding.ActivityLoginBinding;
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

import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthManager authManager;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    private AppDatabase db;

    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authManager = new AuthManager(this);
        mAuth = FirebaseAuth.getInstance();

        // --- THÊM KHỞI TẠO DB ---
        db = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "music-db")
                .fallbackToDestructiveMigration().build();

        // Cấu hình Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Launcher cho kết quả Google Sign-In
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            firebaseAuthWithGoogle(account); // Gửi cả account
                        } catch (ApiException e) {
                            Log.w(TAG, "Google sign in failed", e);
                            handleAuthFailure("Đăng nhập Google thất bại");
                        }
                    } else {
                        // Người dùng nhấn back, không làm gì cả
                        setLoading(false);
                    }
                });

        binding.btnLogin.setOnClickListener(v -> loginWithEmail());
        binding.btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
        binding.tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void signInWithGoogle() {
        setLoading(true);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    // Nhận GoogleSignInAccount
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null && user.getEmail() != null) {
                            // Thêm user vào Room DB trước khi điều hướng
                            insertUserIntoDb(user.getEmail(), () -> checkPolicyAndNavigate(user));
                        } else {
                            handleAuthFailure("Không thể lấy thông tin user");
                        }
                    } else {
                        handleAuthFailure("Xác thực Firebase thất bại.");
                    }
                });
    }

    private void loginWithEmail() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ", Toast.LENGTH_SHORT).show();
            return;
        }
        setLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null && user.getEmail() != null) {
                            // Thêm user vào Room DB
                            insertUserIntoDb(user.getEmail(), () -> checkPolicyAndNavigate(user));
                        } else {
                            handleAuthFailure("Không thể lấy thông tin user");
                        }
                    } else {
                        handleAuthFailure("Đăng nhập thất bại: " + task.getException().getMessage());
                    }
                });
    }

    // Thêm user vào Room DB
    private void insertUserIntoDb(String email, Runnable onComplete) {
        Executors.newSingleThreadExecutor().execute(() -> {
            db.userDao().insertUser(new User(email));

            runOnUiThread(onComplete);
        });
    }

    // Hàm check policy
    private void checkPolicyAndNavigate(FirebaseUser user) {
        if (user == null) {
            setLoading(false);
            return;
        }
        String uid = user.getUid();
        if (authManager.hasAcceptedPolicy(uid)) {
            goToHome();
        } else {
            Intent policyIntent = new Intent(LoginActivity.this, PolicyActivity.class);
            policyIntent.putExtra("USER_UID", uid);
            startActivity(policyIntent);
            finish();
        }
    }

    private void goToHome() {
        startActivity(new Intent(LoginActivity.this, HomeActivity.class));
        finish();
    }

    private void handleAuthFailure(String message) {
        setLoading(false);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void setLoading(boolean isLoading) {
        binding.btnLogin.setEnabled(!isLoading);
        binding.btnGoogleSignIn.setEnabled(!isLoading);
    }
}