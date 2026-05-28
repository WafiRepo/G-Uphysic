package de.rwth_aachen.phyphox.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import de.rwth_aachen.phyphox.Helper.PiAnalysisSettings;
import de.rwth_aachen.phyphox.NetworkConnection.ApiService;
import de.rwth_aachen.phyphox.NetworkConnection.DeleteResponse;
import de.rwth_aachen.phyphox.NetworkConnection.RetrofitClient;
import de.rwth_aachen.phyphox.R;
import de.rwth_aachen.phyphox.activity.PiAnalysisActivity;
import de.rwth_aachen.phyphox.model.JobStatus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PiProgressFragment extends Fragment {

    private static final String ARG_JOB_ID = "job_id";

    private String jobId;
    private TextView tvStepName, tvElapsedTime;
    private ProgressBar progressBar;
    private ImageView ivStepIcon;
    private Handler pollHandler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;
    private Handler timerHandler;
    private long startTime;

    public static PiProgressFragment newInstance(String jobId) {
        PiProgressFragment fragment = new PiProgressFragment();
        Bundle args = new Bundle();
        args.putString(ARG_JOB_ID, jobId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            jobId = getArguments().getString(ARG_JOB_ID);
        }
        startTime = System.currentTimeMillis();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pi_progress, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvStepName = view.findViewById(R.id.tvStepName);
        tvElapsedTime = view.findViewById(R.id.tvElapsedTime);
        progressBar = view.findViewById(R.id.progressBar);
        ivStepIcon = view.findViewById(R.id.ivStepIcon);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancel);

        btnCancel.setOnClickListener(v -> cancelJob());

        startPolling();
        startTimer();
    }

    private void startTimer() {
        timerHandler = new Handler(Looper.getMainLooper());
        timerHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isAdded()) {
                    long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                    long mins = elapsed / 60;
                    long secs = elapsed % 60;
                    tvElapsedTime.setText(String.format("%02d:%02d", mins, secs));
                    timerHandler.postDelayed(this, 1000);
                }
            }
        });
    }

    private void startPolling() {
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                checkStatus();
                pollHandler.postDelayed(this, 10000);
            }
        };
        pollHandler.post(pollRunnable);
    }

    private void checkStatus() {
        String url = PiAnalysisSettings.getServerUrl(requireContext());
        String key = PiAnalysisSettings.getApiKey(requireContext());
        ApiService api = RetrofitClient.getRetrofitInstance(url).create(ApiService.class);

        api.getStatus(key, jobId).enqueue(new Callback<JobStatus>() {
            @Override
            public void onResponse(Call<JobStatus> call, Response<JobStatus> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateUI(response.body());
                }
            }

            @Override
            public void onFailure(Call<JobStatus> call, Throwable t) {
                // Ignore network errors for now
            }
        });
    }

    private void updateUI(JobStatus status) {
        tvStepName.setText(status.step);
        progressBar.setProgress((int) status.progressPct);

        if ("done".equalsIgnoreCase(status.status)) {
            stopPolling();
            ((PiAnalysisActivity) requireActivity()).navigateToResults(jobId);
        } else if ("error".equalsIgnoreCase(status.status) || "failed".equalsIgnoreCase(status.status)) {
            stopPolling();
            Toast.makeText(requireContext(), "Processing failed: " + status.message, Toast.LENGTH_LONG).show();
            requireActivity().onBackPressed();
        }
    }

    private void cancelJob() {
        String url = PiAnalysisSettings.getServerUrl(requireContext());
        String key = PiAnalysisSettings.getApiKey(requireContext());
        ApiService api = RetrofitClient.getRetrofitInstance(url).create(ApiService.class);
        api.deleteJob(key, jobId).enqueue(new Callback<DeleteResponse>() {
            @Override
            public void onResponse(Call<DeleteResponse> call, Response<DeleteResponse> response) {
                // Job cleaned up server-side
            }

            @Override
            public void onFailure(Call<DeleteResponse> call, Throwable t) {
                // Ignore — best effort cleanup
            }
        });
        requireActivity().onBackPressed();
    }

    private void stopPolling() {
        if (pollHandler != null && pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopPolling();
        if (timerHandler != null) {
            timerHandler.removeCallbacksAndMessages(null);
            timerHandler = null;
        }
    }
}
