package de.rwth_aachen.phyphox.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;

import java.util.Objects;

import de.rwth_aachen.phyphox.Helper.FirestoreUtil;
import de.rwth_aachen.phyphox.databinding.ActivityRegisterBinding;
import de.rwth_aachen.phyphox.model.UserModel;

public class RegisterActivity extends AppCompatActivity {

    ActivityRegisterBinding binding;
    private FirebaseAuth authFirebase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        View.OnClickListener btnLoginClick = v -> startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
        binding.btnSignup.setOnClickListener(btnLoginClick);

        View.OnClickListener btnSignUpClick = v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
        };
        binding.tvLogin.setOnClickListener(btnSignUpClick);

        authFirebase = FirebaseAuth.getInstance();
        binding.btnSignup.setOnClickListener(v -> registerNewUser());
    }

    public void registerNewUser() {
        String email, password;
        email = binding.etEmail.getText().toString().trim();
        password = binding.etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please input your email", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please input your password", Toast.LENGTH_SHORT).show();
            return;
        }

        authFirebase.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                   insertData(authFirebase.getCurrentUser().getUid());
                } else {
                    String errorMessage = "Registration failed!";
                    if (task.getException() != null) {
                        Exception exception = task.getException();
                        Log.e("RegisterError", "Registration failed: " + exception.getMessage());
                        
                        if (exception instanceof FirebaseAuthException) {
                            FirebaseAuthException authException = (FirebaseAuthException) exception;
                            String errorCode = authException.getErrorCode();
                            
                            switch (errorCode) {
                                case "ERROR_INVALID_EMAIL":
                                    errorMessage = "Email tidak valid. Silakan periksa format email Anda.";
                                    break;
                                case "ERROR_EMAIL_ALREADY_IN_USE":
                                    errorMessage = "Email sudah terdaftar. Silakan gunakan email lain atau login.";
                                    break;
                                case "ERROR_WEAK_PASSWORD":
                                    errorMessage = "Password terlalu lemah. Gunakan minimal 6 karakter.";
                                    break;
                                case "ERROR_NETWORK_REQUEST_FAILED":
                                    errorMessage = "Gagal terhubung ke server. Periksa koneksi internet Anda.";
                                    break;
                                default:
                                    errorMessage = "Registrasi gagal: " + exception.getMessage();
                                    break;
                            }
                        } else {
                            errorMessage = exception.getMessage();
                        }
                    }
                    Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    private void insertData(String id){
        UserModel usermodel= new UserModel(
                id,
                binding.etName.getText().toString(),
                binding.etEmail.getText().toString()
        );
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Save data to Server");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        FirestoreUtil.addOrUpdateDocument("user",id, usermodel,
                () -> {
                    progressDialog.dismiss();
                    Toast.makeText(RegisterActivity.this, "Registration Success", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    startActivity(intent);
                },
                e -> {
                    progressDialog.dismiss();
                    Toast.makeText(RegisterActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
    }
}
