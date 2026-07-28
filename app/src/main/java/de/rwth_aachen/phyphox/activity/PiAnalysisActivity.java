package de.rwth_aachen.phyphox.activity;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;

import de.rwth_aachen.phyphox.Helper.FileExtensions;
import de.rwth_aachen.phyphox.Helper.PiAnalysisSettings;
import de.rwth_aachen.phyphox.NetworkConnection.ApiService;
import de.rwth_aachen.phyphox.NetworkConnection.RetrofitClient;
import de.rwth_aachen.phyphox.NetworkConnection.SubmitResponse;
import de.rwth_aachen.phyphox.R;
import de.rwth_aachen.phyphox.fragment.PiProgressFragment;
import de.rwth_aachen.phyphox.fragment.PiResultsFragment;
import de.rwth_aachen.phyphox.fragment.PiSettingsFragment;
import de.rwth_aachen.phyphox.fragment.PiSidecarFragment;
import de.rwth_aachen.phyphox.fragment.PiUploadFragment;
import de.rwth_aachen.phyphox.model.SidecarJson;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PiAnalysisActivity extends AppCompatActivity {
    private static final String TAG = "PiAnalysisActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pi_analysis);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new PiUploadFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_settings) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new PiSettingsFragment())
                    .addToBackStack(null)
                    .commit();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void navigateToSidecar(Uri videoUri) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, PiSidecarFragment.newInstance(videoUri))
                .addToBackStack(null)
                .commit();
    }

    public void startAnalysis(Uri videoUri, SidecarJson sidecar) {
        String url = PiAnalysisSettings.getServerUrl(this);
        String key = PiAnalysisSettings.getApiKey(this);

        Log.d(TAG, "=== Starting analysis ===");
        Log.d(TAG, "Server URL: " + url);
        Log.d(TAG, "API Key (masked): " + (key.length() > 4 ? key.substring(0, 4) + "..." : "***empty***"));
        Log.d(TAG, "Video URI: " + videoUri);

        ApiService api = RetrofitClient.getRetrofitInstance(url).create(ApiService.class);

        java.io.File file = FileExtensions.uriToFile(this, videoUri);
        if (file == null || !file.exists()) {
            Log.e(TAG, "File not found or null after uriToFile conversion");
            Toast.makeText(this, "Gagal menyiapkan file video", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "File size: " + file.length() + " bytes");

        String mimeType = getContentResolver().getType(videoUri);
        if (mimeType == null) mimeType = "video/mp4";
        Log.d(TAG, "MIME type: " + mimeType);

        RequestBody videoBody = RequestBody.create(MediaType.parse(mimeType), file);
        MultipartBody.Part videoPart = MultipartBody.Part.createFormData("video", file.getName(), videoBody);

        String sidecarStr = new Gson().toJson(sidecar);
        Log.d(TAG, "Sidecar JSON: " + sidecarStr);
        RequestBody sidecarBody = RequestBody.create(MediaType.parse("application/json"), sidecarStr);

        api.submitJob(key, videoPart, sidecarBody).enqueue(new Callback<SubmitResponse>() {
            @Override
            public void onResponse(Call<SubmitResponse> call, Response<SubmitResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Submit OK — job_id: " + response.body().jobId);
                    navigateToProgress(response.body().jobId);
                } else {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) errorBody = response.errorBody().string();
                    } catch (Exception ignored) {}
                    Log.e(TAG, "Submit failed — HTTP " + response.code() + " " + response.message() + " | body: " + errorBody);
                    Toast.makeText(PiAnalysisActivity.this, "Pengiriman gagal (" + response.code() + ")", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<SubmitResponse> call, Throwable t) {
                Log.e(TAG, "Submit onFailure — " + t.getClass().getSimpleName() + ": " + t.getMessage(), t);
                Toast.makeText(PiAnalysisActivity.this, "Terjadi kesalahan: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void navigateToProgress(String jobId) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, PiProgressFragment.newInstance(jobId))
                .commit(); // Don't add to backstack to avoid going back to sidecar
    }

    public void navigateToResults(String jobId) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, PiResultsFragment.newInstance(jobId))
                .commit();
    }
}
