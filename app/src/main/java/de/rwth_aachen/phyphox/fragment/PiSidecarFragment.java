package de.rwth_aachen.phyphox.fragment;

import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.media3.common.Player;
import androidx.media3.common.VideoSize;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;

import de.rwth_aachen.phyphox.R;
import de.rwth_aachen.phyphox.activity.PiAnalysisActivity;
import de.rwth_aachen.phyphox.model.SidecarJson;
import de.rwth_aachen.phyphox.ui.MarkerView;

public class PiSidecarFragment extends Fragment {

    private static final String ARG_VIDEO_URI = "video_uri";

    private Uri videoUri;
    private MarkerView markerView;
    private TextView tvInstruction;
    private TextInputEditText etObjectLabel, etRefLabel, etPhysicalSize;
    private PlayerView playerView;
    private ExoPlayer player;
    private boolean isMarkingObject = true;
    private int videoWidth = 0, videoHeight = 0;
    private MaterialButton btnAnalyze, btnReset;

    public static PiSidecarFragment newInstance(Uri videoUri) {
        PiSidecarFragment fragment = new PiSidecarFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_VIDEO_URI, videoUri);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            videoUri = getArguments().getParcelable(ARG_VIDEO_URI);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pi_sidecar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        playerView = view.findViewById(R.id.playerView);
        markerView = view.findViewById(R.id.markerView);
        tvInstruction = view.findViewById(R.id.tvInstruction);
        etObjectLabel = view.findViewById(R.id.etObjectLabel);
        etRefLabel = view.findViewById(R.id.etRefLabel);
        etPhysicalSize = view.findViewById(R.id.etPhysicalSize);
        btnAnalyze = view.findViewById(R.id.btnAnalyze);
        btnReset = view.findViewById(R.id.btnReset);
        btnAnalyze.setEnabled(false);
        btnReset.setVisibility(View.GONE);

        setupPlayer();

        markerView.setOnPointMarkedListener(new MarkerView.OnPointMarkedListener() {
            @Override
            public void onObjectMarked(float x, float y) {
                isMarkingObject = false;
                markerView.setMarkingObject(false);
                tvInstruction.setText("Tap the rotation center");
                btnReset.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCenterMarked(float x, float y) {
                tvInstruction.setText("Marking complete — tap Analyze");
                btnAnalyze.setEnabled(videoWidth > 0 && videoHeight > 0);
            }
        });

        btnReset.setOnClickListener(v -> {
            markerView.reset();
            isMarkingObject = true;
            tvInstruction.setText("Tap the object to track");
            btnReset.setVisibility(View.GONE);
            btnAnalyze.setEnabled(false);
        });

        btnAnalyze.setOnClickListener(v -> submitAnalysis());
    }

    private void setupPlayer() {
        player = new ExoPlayer.Builder(requireContext()).build();
        playerView.setPlayer(player);
        player.setMediaItem(androidx.media3.common.MediaItem.fromUri(videoUri));
        player.setPlayWhenReady(false);
        player.prepare();

        player.addListener(new Player.Listener() {
            @Override
            public void onVideoSizeChanged(VideoSize videoSize) {
                videoWidth = videoSize.width;
                videoHeight = videoSize.height;
            }

            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    // Make sure we are at frame 0
                    player.seekTo(0);
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (player != null) {
            player.release();
            player = null;
        }
    }

    private void submitAnalysis() {
        PointF objPoint = markerView.getObjectPoint();
        PointF centerPoint = markerView.getCenterPoint();

        if (objPoint == null || centerPoint == null) {
            Toast.makeText(requireContext(), "Harap tandai objek dan pusat terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        String objLabel = etObjectLabel.getText().toString();
        String refLabel = etRefLabel.getText().toString();
        String sizeStr = etPhysicalSize.getText().toString();
        Double physSize = sizeStr.isEmpty() ? null : Double.parseDouble(sizeStr);

        if (objLabel.isEmpty() || refLabel.isEmpty()) {
            Toast.makeText(requireContext(), "Harap isi label terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        SidecarJson sidecar = new SidecarJson();
        sidecar.trackedObject = new SidecarJson.TrackedObject();
        sidecar.trackedObject.visualCues = new ArrayList<>(Arrays.asList(objLabel));

        sidecar.referenceGeometry = new SidecarJson.ReferenceGeometry();
        sidecar.referenceGeometry.label = refLabel;
        sidecar.referenceGeometry.bboxCenterPx = new ArrayList<>(Arrays.asList((int)centerPoint.x, (int)centerPoint.y));
        sidecar.referenceGeometry.physicalSize = physSize;

        // Note: we need to pass video dimensions and display dimensions for scaling
        sidecar.videoW = videoWidth;
        sidecar.videoH = videoHeight;
        sidecar.displayW = markerView.getWidth();
        sidecar.displayH = markerView.getHeight();

        ((PiAnalysisActivity) requireActivity()).startAnalysis(videoUri, sidecar);
    }
}
