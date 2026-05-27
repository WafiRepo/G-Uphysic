package de.rwth_aachen.phyphox.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import de.rwth_aachen.phyphox.R;
import de.rwth_aachen.phyphox.activity.PiAnalysisActivity;

public class PiUploadFragment extends Fragment {

    private ImageView ivThumbnail;
    private TextView tvFileInfo;
    private MaterialButton btnNext;
    private Uri selectedVideoUri;

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

        checkSettings();
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
        Bitmap thumbnail = VideoUtils.extractFirstFrame(requireContext(), uri);
        if (thumbnail != null) {
            ivThumbnail.setImageBitmap(thumbnail);
            ivThumbnail.setVisibility(View.VISIBLE);
        }

        tvFileInfo.setText("Video selected: " + uri.getLastPathSegment());
        tvFileInfo.setVisibility(View.VISIBLE);
        btnNext.setEnabled(true);
    }
}
