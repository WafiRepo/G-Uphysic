package de.rwth_aachen.phyphox.activity;

import android.os.Bundle;
import android.util.Log;
import android.window.OnBackInvokedDispatcher;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.button.MaterialButton;
import de.rwth_aachen.phyphox.R;
import de.rwth_aachen.phyphox.fragment.CameraDetectionFragment;

public class FeedbackActivity extends AppCompatActivity {

    public static final String EXTRA_DEVICE_COUNT = "device_count";

    private int deviceCount = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        deviceCount = getIntent().getIntExtra(EXTRA_DEVICE_COUNT, 1);

        loadCameraForDevice(1);

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

    public void loadCameraForDevice(int deviceIndex) {
        CameraDetectionFragment fragment = CameraDetectionFragment.newInstance(deviceIndex, deviceCount);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.disallowAddToBackStack();
        transaction.commit();
    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }
}
