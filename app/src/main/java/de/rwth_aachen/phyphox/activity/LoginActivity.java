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
            Toast.makeText(this, "Please input your email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please input your password", Toast.LENGTH_SHORT).show();
            return;
        }
        progressDialog = new ProgressDialog(this); // Replace 'this' with 'requireContext()' if inside a Fragment
        progressDialog.setTitle("");
        progressDialog.setMessage("Loading...");
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
                Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                Log.d("getUser","--> "+data.getId());
                Log.d("getUser","--> "+data.getName());
                Log.d("getUser","--> "+data.getEmail());
                SessionManager.setCustData(LoginActivity.this,data.getName(),data.getId(), data.getEmail(), data.getTotalVisitingIntroduction(), data.getTotalVisitDurationMillis());
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                progressDialog.dismiss();
            }
        }, new FirestoreUtil.OnFailureCallback() {
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(LoginActivity.this, "Login Failed! Please try again", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "Check email to reset password", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to reset password", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
