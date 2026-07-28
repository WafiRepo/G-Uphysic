package de.rwth_aachen.phyphox.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;

import java.nio.file.FileVisitResult;

import de.rwth_aachen.phyphox.Helper.FirestoreUtil;
import de.rwth_aachen.phyphox.Helper.SessionManager;
import de.rwth_aachen.phyphox.databinding.ActivityLoginBinding;
import de.rwth_aachen.phyphox.databinding.PopupForgetPasswordBinding;
import de.rwth_aachen.phyphox.model.UserModel;

public class LoginActivity extends AppCompatActivity {

    ActivityLoginBinding binding;
    private FirebaseAuth auth;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        progressDialog = new ProgressDialog(this);

        auth = FirebaseAuth.getInstance();

        // Auto-login if user is already authenticated and session is valid
        if (auth.getCurrentUser() != null && SessionManager.getIsLogin(this)) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        View.OnClickListener btnLoginClick = v -> startActivity(new Intent(LoginActivity.this, MainActivity.class));
//        binding.btnLogin.setOnClickListener(btnLoginClick);

        binding.btnLogin.setOnClickListener(v -> loginUser());

        View.OnClickListener btnSignUpClick = v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        binding.tvLogin.setOnClickListener(btnSignUpClick);
        binding.tvForgot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialogForgetPass();
            }
        });
    }

    public void loginUser() {
        String email, password;
        email = binding.etEmail.getText().toString();
        password = binding.etPassword.getText().toString();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Harap masukkan email Anda", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Harap masukkan password Anda", Toast.LENGTH_SHORT).show();
            return;
        }
        progressDialog = new ProgressDialog(this); // Replace 'this' with 'requireContext()' if inside a Fragment
        progressDialog.setTitle("");
        progressDialog.setMessage("Memuat...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    getUser(auth.getCurrentUser().getUid());
                } else {
                    progressDialog.dismiss();
                    String errorMessage = "Login Failed!";
                    if (task.getException() != null) {
                        Exception exception = task.getException();
                        Log.e("LoginError", "Login failed: " + exception.getMessage());

                        if (exception instanceof FirebaseAuthException) {
                            FirebaseAuthException authException = (FirebaseAuthException) exception;
                            String errorCode = authException.getErrorCode();

                            switch (errorCode) {
                                case "ERROR_INVALID_EMAIL":
                                    errorMessage = "Email tidak valid. Silakan periksa format email Anda.";
                                    break;
                                case "ERROR_USER_NOT_FOUND":
                                    errorMessage = "Email tidak terdaftar. Silakan daftar terlebih dahulu.";
                                    break;
                                case "ERROR_WRONG_PASSWORD":
                                    errorMessage = "Password salah. Silakan coba lagi.";
                                    break;
                                case "ERROR_INVALID_CREDENTIAL":
                                    errorMessage = "Email atau password salah. Silakan periksa kembali.";
                                    break;
                                case "ERROR_USER_DISABLED":
                                    errorMessage = "Akun ini telah dinonaktifkan. Hubungi administrator.";
                                    break;
                                case "ERROR_TOO_MANY_REQUESTS":
                                    errorMessage = "Terlalu banyak percobaan login. Silakan coba lagi nanti.";
                                    break;
                                case "ERROR_NETWORK_REQUEST_FAILED":
                                    errorMessage = "Gagal terhubung ke server. Periksa koneksi internet Anda.";
                                    break;
                                default:
                                    errorMessage = "Login gagal: " + exception.getMessage();
                                    break;
                            }
                        } else {
                            errorMessage = exception.getMessage();
                        }
                    }
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    public void getUser(String id){
        FirestoreUtil.getDocument("user", id, UserModel.class, new FirestoreUtil.OnSuccessWithDataCallback<UserModel>() {
            @Override
            public void onSuccess(UserModel data) {
                progressDialog.dismiss();
                if (data == null) {
                    // Auth ada, dokumen Firestore belum ada → buat stub agar muncul di admin & konsisten dengan pengguna lain
                    FirebaseUser fu = auth.getCurrentUser();
                    String email = fu != null && fu.getEmail() != null ? fu.getEmail() : "";
                    String display = fu != null && fu.getDisplayName() != null && !fu.getDisplayName().isEmpty()
                            ? fu.getDisplayName()
                            : (email.contains("@") ? email.substring(0, email.indexOf('@')) : "User");
                    UserModel stub = new UserModel(id, display, email);
                    FirestoreUtil.addOrUpdateDocument("user", id, stub, () -> {
                        Toast.makeText(LoginActivity.this, "Profil awal disimpan. Silakan lengkapi di pengaturan bila perlu.", Toast.LENGTH_LONG).show();
                        SessionManager.setCustData(LoginActivity.this, display, id, email, 0L, 0L);
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }, exception -> {
                        Log.e("LoginActivity", "Gagal membuat dokumen user di Firestore", exception);
                        Toast.makeText(LoginActivity.this, "Profil belum tersimpan ke cloud; coba lagi nanti.", Toast.LENGTH_LONG).show();
                        SessionManager.setCustData(LoginActivity.this, display, id, email, 0L, 0L);
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    });
                    return;
                }
                Toast.makeText(LoginActivity.this, "Login berhasil", Toast.LENGTH_SHORT).show();
                Log.d("getUser","--> "+data.getId());
                Log.d("getUser","--> "+data.getName());
                Log.d("getUser","--> "+data.getEmail());
                SessionManager.setCustData(LoginActivity.this,data.getName(),data.getId(), data.getEmail(), data.getTotalVisitingIntroduction(), data.getTotalVisitDurationMillis());
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        }, new FirestoreUtil.OnFailureCallback() {
            @Override
            public void onFailure(Exception e) {
                progressDialog.dismiss();
                Toast.makeText(LoginActivity.this, "Login gagal! Silakan coba lagi", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void showDialogForgetPass() {
        AlertDialog alertDialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        PopupForgetPasswordBinding binding = PopupForgetPasswordBinding.inflate(LayoutInflater.from(this));
        builder.setView(binding.getRoot());
        alertDialog = builder.create();

        binding.tvSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!binding.etEmail.getText().toString().isEmpty()) {
                    forgotPassword(binding.etEmail.getText().toString());
                    alertDialog.dismiss();
                }
            }
        });

        alertDialog.show();
    }
    private void forgotPassword(String email) {
        auth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Cek email untuk mereset password", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Gagal mereset password", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
