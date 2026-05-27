package de.rwth_aachen.phyphox.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import de.rwth_aachen.phyphox.Helper.PiAnalysisSettings;
import de.rwth_aachen.phyphox.Helper.VideoUtils;
import de.rwth_aachen.phyphox.NetworkConnection.ApiService;
import de.rwth_aachen.phyphox.NetworkConnection.RetrofitClient;
import de.rwth_aachen.phyphox.R;
import de.rwth_aachen.phyphox.activity.PiAnalysisActivity;
import de.rwth_aachen.phyphox.model.JobsListResponse;
import de.rwth_aachen.phyphox.model.JobsListResponse.JobEntry;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

public class PiUploadFragment extends Fragment {
    private static final String TAG = "PiUploadFragment";

    private ImageView ivThumbnail;
    private TextView tvFileInfo;
    private MaterialButton btnNext;
    private Uri selectedVideoUri;
    private volatile boolean thumbnailLoadingCancelled = false;

    private final ActivityResultLauncher<String> videoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedVideoUri = uri;
                    displayVideoInfo(uri);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pi_upload, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ivThumbnail = view.findViewById(R.id.ivThumbnail);
        tvFileInfo = view.findViewById(R.id.tvFileInfo);
        btnNext = view.findViewById(R.id.btnNext);
        MaterialButton btnPickVideo = view.findViewById(R.id.btnPickVideo);

        btnPickVideo.setOnClickListener(v -> videoPickerLauncher.launch("video/*"));

        btnNext.setOnClickListener(v -> {
            if (selectedVideoUri != null) {
                ((PiAnalysisActivity) requireActivity()).navigateToSidecar(selectedVideoUri);
            }
        });

        MaterialButton btnJobHistory = view.findViewById(R.id.btnJobHistory);
        btnJobHistory.setOnClickListener(v -> fetchAndShowJobs());

        checkSettings();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        thumbnailLoadingCancelled = true;
    }

    private void fetchAndShowJobs() {
        String url = PiAnalysisSettings.getServerUrl(requireContext());
        String key = PiAnalysisSettings.getApiKey(requireContext());
        if (url.isEmpty() || key.isEmpty()) {
            Toast.makeText(requireContext(), "Configure Server URL and API Key in Settings first", Toast.LENGTH_LONG).show();
            return;
        }

        ApiService api = RetrofitClient.getRetrofitInstance(url).create(ApiService.class);
        api.listJobs(key).enqueue(new Callback<JobsListResponse>() {
            @Override
            public void onResponse(Call<JobsListResponse> call, Response<JobsListResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().jobs != null) {
                    showJobsDialog(response.body().jobs);
                } else {
                    Toast.makeText(requireContext(), "Failed to load jobs", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JobsListResponse> call, Throwable t) {
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showJobsDialog(List<JobEntry> jobs) {
        final String[] items = new String[jobs.size()];
        for (int i = 0; i < jobs.size(); i++) {
            JobEntry j = jobs.get(i);
            long mins = j.elapsedS / 60;
            long secs = j.elapsedS % 60;
            String statusIcon = "done".equalsIgnoreCase(j.status) ? "✓" :
                    "error".equalsIgnoreCase(j.status) ? "✗" : "⟳";
            items[i] = statusIcon + " " + j.step + " (" + j.status + ")\n" +
                    shortId(j.jobId) + "  ·  " + (int) j.progressPct + "%  ·  " +
                    String.format("%02d:%02d", mins, secs);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Job History")
                .setItems(items, (dialog, which) -> {
                    JobEntry entry = jobs.get(which);
                    if (isAdded()) {
                        ((PiAnalysisActivity) requireActivity()).navigateToResults(entry.jobId);
                    }
                })
                .setPositiveButton("Close", null)
                .show();
    }

    private String shortId(String jobId) {
        if (jobId == null || jobId.length() < 8) return jobId;
        return jobId.substring(0, 8) + "…";
    }

    private void checkSettings() {
        String url = PiAnalysisSettings.getServerUrl(requireContext());
        String key = PiAnalysisSettings.getApiKey(requireContext());
        if (url.isEmpty() || key.isEmpty()) {
            Toast.makeText(requireContext(), "Please configure Server URL and API Key in Settings", Toast.LENGTH_LONG).show();
            // Optionally navigate to settings or show a warning banner
        }
    }

    private void displayVideoInfo(Uri uri) {
        ivThumbnail.setVisibility(View.VISIBLE);
        tvFileInfo.setText("Video selected: " + uri.getLastPathSegment());
        tvFileInfo.setVisibility(View.VISIBLE);
        btnNext.setEnabled(true);

        thumbnailLoadingCancelled = false;
        Context context = getContext();
        if (context == null) return;
        new Thread(() -> {
            if (thumbnailLoadingCancelled) return;
            Bitmap frame = VideoUtils.extractFirstFrame(context, uri);
            if (isAdded() && !thumbnailLoadingCancelled) {
                ivThumbnail.post(() -> {
                    if (!isAdded() || thumbnailLoadingCancelled) return;
                    if (frame != null) {
                        ivThumbnail.setImageBitmap(frame);
                    } else {
                        ivThumbnail.setImageResource(R.drawable.ic_image_placeholder);
                    }
                });
            }
        }).start();
    }
}
