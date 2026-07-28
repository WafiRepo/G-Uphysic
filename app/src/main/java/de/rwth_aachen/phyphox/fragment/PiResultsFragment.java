package de.rwth_aachen.phyphox.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.util.Map;

import de.rwth_aachen.phyphox.Helper.PiAnalysisSettings;
import de.rwth_aachen.phyphox.NetworkConnection.ApiService;
import de.rwth_aachen.phyphox.NetworkConnection.RetrofitClient;
import de.rwth_aachen.phyphox.R;
import de.rwth_aachen.phyphox.model.JobResult;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PiResultsFragment extends Fragment {
    private static final String TAG = "PiResultsFragment";
    private static final String ARG_JOB_ID = "job_id";

    private String jobId;
    private String serverBaseUrl;
    private LinearLayout llStatsCards;
    private ImageView ivSummaryPanel;
    private PlayerView playerView;
    private ExoPlayer player;
    private MaterialButton btnStudentPdf, btnTeacherPdf;

    public static PiResultsFragment newInstance(String jobId) {
        PiResultsFragment fragment = new PiResultsFragment();
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
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pi_results, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        serverBaseUrl = PiAnalysisSettings.getServerUrl(requireContext());
        if (serverBaseUrl.endsWith("/")) serverBaseUrl = serverBaseUrl.substring(0, serverBaseUrl.length() - 1);

        llStatsCards = view.findViewById(R.id.llStatsCards);
        ivSummaryPanel = view.findViewById(R.id.ivSummaryPanel);
        playerView = view.findViewById(R.id.playerView);
        btnStudentPdf = view.findViewById(R.id.btnStudentPdf);
        btnTeacherPdf = view.findViewById(R.id.btnTeacherPdf);
        MaterialButton btnNewAnalysis = view.findViewById(R.id.btnNewAnalysis);

        btnNewAnalysis.setOnClickListener(v -> requireActivity().recreate());

        loadResults();
    }

    private void loadResults() {
        String url = PiAnalysisSettings.getServerUrl(requireContext());
        String key = PiAnalysisSettings.getApiKey(requireContext());
        ApiService api = RetrofitClient.getRetrofitInstance(url).create(ApiService.class);

        api.getResult(key, jobId).enqueue(new Callback<JobResult>() {
            @Override
            public void onResponse(Call<JobResult> call, Response<JobResult> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displayResults(response.body());
                } else {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) errorBody = response.errorBody().string();
                    } catch (Exception ignored) {}
                    Log.e(TAG, "Load results failed — HTTP " + response.code() + " " + response.message() + " | " + errorBody);
                    Toast.makeText(requireContext(), "Gagal memuat hasil (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JobResult> call, Throwable t) {
                Log.e(TAG, "Load results onFailure: " + t.getMessage(), t);
                Toast.makeText(requireContext(), "Terjadi kesalahan: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayResults(JobResult result) {
        if (result.stats != null) {
            for (Map.Entry<String, Object> entry : result.stats.entrySet()) {
                String label = formatStatLabel(entry.getKey());
                String value = formatStatValue(entry.getValue());
                if (value != null) {
                    addStatCard(label, value);
                }
            }
        }

        if (result.files == null) return;

        if (result.files.summaryPanel != null) {
            String fullUrl = resolveFileUrl(result.files.summaryPanel);
            Log.d(TAG, "Loading summary panel: " + fullUrl);
            Glide.with(this).load(fullUrl).into(ivSummaryPanel);
        }

        btnStudentPdf.setOnClickListener(v -> openUrl(result.files.studentPdf));
        btnTeacherPdf.setOnClickListener(v -> openUrl(result.files.teacherPdf));

        if (result.files.annotatedVideo != null) {
            setupVideoPlayer(resolveFileUrl(result.files.annotatedVideo));
        }
    }

    private String resolveFileUrl(String path) {
        if (path == null) return null;
        if (path.startsWith("http://") || path.startsWith("https://")) return path;
        return serverBaseUrl + path;
    }

    private String formatStatLabel(String key) {
        return key.replace("_", " ");
    }

    private String formatStatValue(Object value) {
        if (value == null) return null;
        if (value instanceof Map || value instanceof java.util.List) return null;
        if (value instanceof Double) {
            double d = (Double) value;
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                return String.valueOf((long) d);
            }
            return String.format("%.4g", d);
        }
        return value.toString();
    }

    private void addStatCard(String label, String value) {
        View card = getLayoutInflater().inflate(R.layout.item_pi_stat_card, llStatsCards, false);
        TextView tvValue = card.findViewById(R.id.tvStatValue);
        TextView tvLabel = card.findViewById(R.id.tvStatLabel);

        tvValue.setText(value);
        tvLabel.setText(label);

        llStatsCards.addView(card);
    }

    private void setupVideoPlayer(String videoUrl) {
        player = new ExoPlayer.Builder(requireContext()).build();
        playerView.setPlayer(player);
        MediaItem mediaItem = MediaItem.fromUri(videoUrl);
        player.setMediaItem(mediaItem);
        player.prepare();
    }

    private void openUrl(String url) {
        if (url == null) return;
        String fullUrl = resolveFileUrl(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl));
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
