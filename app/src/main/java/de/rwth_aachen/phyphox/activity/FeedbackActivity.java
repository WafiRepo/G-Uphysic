package de.rwth_aachen.phyphox.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.window.OnBackInvokedDispatcher;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.button.MaterialButton;
import de.rwth_aachen.phyphox.R;
import de.rwth_aachen.phyphox.fragment.CameraDetectionFragment;
import de.rwth_aachen.phyphox.fragment.ShareLocationFragment;

public class FeedbackActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        MaterialButton nextButton = findViewById(R.id.btn_next);
        loadFragment(new CameraDetectionFragment());

        // Set OnClickListener untuk tombol Next
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Mengganti fragment ke ShareLocationFragment
                loadFragment(new CameraDetectionFragment());
            }
        });
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    () -> {
                        Log.d("BackInvoked", "Langsung finish()");
                        finish();
                    }
            );
        }

    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }

    // Method untuk mengganti fragment
    private void loadFragment(Fragment fragment) {
        // Membuat transaksi fragment
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        // Ganti fragment yang ada dengan yang baru
        transaction.replace(R.id.fragment_container, fragment); // Pastikan ID layout container benar
        // Tambahkan transaksi ke back stack agar pengguna bisa kembali
        transaction.disallowAddToBackStack();
        // Commit transaksi
        transaction.commit();
    }
}
