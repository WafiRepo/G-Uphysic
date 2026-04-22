package de.rwth_aachen.phyphox.activity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import de.rwth_aachen.phyphox.Helper.InquiryLogHelper;
import de.rwth_aachen.phyphox.Helper.SessionManager;
import de.rwth_aachen.phyphox.NetworkConnection.ApiService;
import de.rwth_aachen.phyphox.NetworkConnection.InquiryGenerateProblemExploringRequest;
import de.rwth_aachen.phyphox.NetworkConnection.InquiryGenerateResponse;
import de.rwth_aachen.phyphox.NetworkConnection.RetrofitClient;
import de.rwth_aachen.phyphox.NetworkConnection.SubmitResponseRequest;
import de.rwth_aachen.phyphox.NetworkConnection.SubmitResponseResponse;
import de.rwth_aachen.phyphox.R;
import de.rwth_aachen.phyphox.databinding.ActivityExperimentInquiryBinding;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Halaman analisis setelah user menyelesaikan canvas (DrawActivity) atau flow eksperimen.
 * Stage 1: generate problem-exploring-stage-1 (teks).
 * Stage 2: pertanyaan dari API dengan konteks gambar grafik + canvas (problem-exploring-stage-2).
 */
public class ExperimentInquiryActivity extends AppCompatActivity {

    public static final String EXTRA_INQUIRY_STAGE = "inquiry_stage";
    public static final String EXTRA_PREFILLED_INQUIRY = "prefilled_inquiry";
    public static final String EXTRA_GRAPH_CONTEXT = "graph_context";
    public static final String EXTRA_SESSION_ID = "inquiry_session_id";

    public static final int INQUIRY_STAGE_1 = 1;
    public static final int INQUIRY_STAGE_2 = 2;

    /** Base64 gambar terlalu besar untuk Intent; dipindahkan sesaat sebelum startActivity. */
    private static volatile String sPendingGraphImageBase64;

    public static void setPendingStage2GraphImageBase64(String base64) {
        sPendingGraphImageBase64 = base64;
    }

    private ActivityExperimentInquiryBinding binding;
    private String userId;

    private int inquiryStage = INQUIRY_STAGE_1;
    private String graphImageBase64;
    private String graphContext;
    private String sessionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExperimentInquiryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        userId = SessionManager.getId(this);
        if (userId == null || userId.isEmpty()) userId = "default";

        inquiryStage = getIntent().getIntExtra(EXTRA_INQUIRY_STAGE, INQUIRY_STAGE_1);
        graphContext = getIntent().getStringExtra(EXTRA_GRAPH_CONTEXT);
        sessionId = getIntent().getStringExtra(EXTRA_SESSION_ID);
        if (inquiryStage == INQUIRY_STAGE_2 && sPendingGraphImageBase64 != null) {
            graphImageBase64 = sPendingGraphImageBase64;
            sPendingGraphImageBase64 = null;
        }
        String prefilled = getIntent().getStringExtra(EXTRA_PREFILLED_INQUIRY);

        binding.etExperimentUserAnswer.setHint("Ketik jawaban Anda di sini...");

        binding.btnBackExperimentInquiry.setOnClickListener(v -> finish());

        if (inquiryStage == INQUIRY_STAGE_2 && prefilled != null && !prefilled.trim().isEmpty()) {
            binding.tvExperimentInquiryText.setText(prefilled.trim());
        } else {
            binding.tvExperimentInquiryText.setText("Loading...");
            fetchExperimentInquiry();
        }

        binding.btnSubmitExperimentAnswer.setOnClickListener(v -> {
            String answer = binding.etExperimentUserAnswer.getText().toString().trim();
            if (answer.isEmpty()) {
                Toast.makeText(this, "Isi jawaban terlebih dahulu.", Toast.LENGTH_SHORT).show();
                return;
            }
            submitResponse(answer);
        });
    }

    private void fetchExperimentInquiry() {
        ApiService api = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        InquiryGenerateProblemExploringRequest req = new InquiryGenerateProblemExploringRequest(
                userId,
                "centripetal acceleration"
        );
        api.generateInquiryProblemExploring(req).enqueue(new Callback<InquiryGenerateResponse>() {
            @Override
            public void onResponse(Call<InquiryGenerateResponse> call, Response<InquiryGenerateResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getInquiry() != null && !response.body().getInquiry().isEmpty()) {
                    binding.tvExperimentInquiryText.setText(response.body().getInquiry());
                    // Jangan log exploring_stage2 di sini: tanpa URL grafik membuat entri kosong di admin.
                    // Stage 2 dicatat saat submit (dengan upload canvas jika ada base64).
                    if (inquiryStage != INQUIRY_STAGE_2) {
                        InquiryLogHelper.log(
                                ExperimentInquiryActivity.this,
                                InquiryLogHelper.KIND_EXPLORING_STAGE1,
                                response.body().getInquiry(),
                                null,
                                null,
                                sessionId,
                                null,
                                null,
                                null,
                                null
                        );
                    }
                } else {
                    binding.tvExperimentInquiryText.setText(
                            "Now that we have the real-time data, let's analyze the graph together. " +
                                    "Notice how the centripetal acceleration (y-axis) changes as you adjust the angular velocity and radius (x-axis). " +
                                    "What patterns or trends do you notice in the graph between centripetal acceleration and angular velocity?"
                    );
                }
            }

            @Override
            public void onFailure(Call<InquiryGenerateResponse> call, Throwable t) {
                binding.tvExperimentInquiryText.setText(
                        "Now that we have the real-time data, let's analyze the graph together. " +
                                "Notice how the centripetal acceleration (y-axis) changes as you adjust the angular velocity and radius (x-axis). " +
                                "What patterns or trends do you notice in the graph between centripetal acceleration and angular velocity?"
                );
                Toast.makeText(ExperimentInquiryActivity.this, "Pertanyaan default ditampilkan.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitResponse(String responseText) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("Mengevaluasi jawaban");
        progress.setMessage("Jawaban Anda sedang dievaluasi...");
        progress.setCancelable(false);
        progress.show();

        ApiService api = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        Call<SubmitResponseResponse> call;
        if (inquiryStage == INQUIRY_STAGE_2) {
            String ctx = !TextUtils.isEmpty(graphContext)
                    ? graphContext
                    : "Grafik eksperimen phyphox + analisis canvas";
            SubmitResponseRequest req = new SubmitResponseRequest(
                    userId,
                    responseText,
                    "id",
                    sessionId,
                    ctx,
                    graphImageBase64
            );
            call = api.submitResponseProblemExploringStage2(req);
        } else {
            call = api.submitResponseProblemExploring(new SubmitResponseRequest(userId, responseText));
        }
        call.enqueue(new Callback<SubmitResponseResponse>() {
            @Override
            public void onResponse(Call<SubmitResponseResponse> call, Response<SubmitResponseResponse> response) {
                progress.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    String inquiryShown = binding.tvExperimentInquiryText.getText() != null
                            ? binding.tvExperimentInquiryText.getText().toString().trim() : "";
                    String fb = summarizeSubmitResponse(response.body());
                    if (inquiryStage == INQUIRY_STAGE_2 && graphImageBase64 != null && !graphImageBase64.trim().isEmpty()) {
                        byte[] jpeg;
                        try {
                            jpeg = android.util.Base64.decode(graphImageBase64.trim(), android.util.Base64.NO_WRAP);
                        } catch (Exception e) {
                            jpeg = null;
                        }
                        InquiryLogHelper.logExploringStage2WithCanvasUpload(
                                ExperimentInquiryActivity.this,
                                inquiryShown,
                                responseText,
                                fb,
                                sessionId,
                                jpeg
                        );
                    } else {
                        InquiryLogHelper.log(
                                ExperimentInquiryActivity.this,
                                inquiryStage == INQUIRY_STAGE_2 ? InquiryLogHelper.KIND_EXPLORING_STAGE2 : InquiryLogHelper.KIND_EXPLORING_STAGE1,
                                inquiryShown,
                                responseText,
                                fb,
                                sessionId,
                                null,
                                null,
                                null,
                                null
                        );
                    }
                    Toast.makeText(ExperimentInquiryActivity.this, "Jawaban telah dikirim.", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(ExperimentInquiryActivity.this, "Gagal mengirim jawaban.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SubmitResponseResponse> call, Throwable t) {
                progress.dismiss();
                Toast.makeText(ExperimentInquiryActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static String summarizeSubmitResponse(SubmitResponseResponse r) {
        if (r == null) return "";
        StringBuilder sb = new StringBuilder();
        if (r.getMatchedAnswerType() != null && !r.getMatchedAnswerType().trim().isEmpty()) {
            sb.append("matched_answer_type: ").append(r.getMatchedAnswerType().trim());
        }
        if (r.getFeedback() != null && !r.getFeedback().trim().isEmpty()) {
            if (sb.length() > 0) sb.append("\n\n");
            sb.append(r.getFeedback().trim());
        }
        if (r.getNextStep() != null && !r.getNextStep().trim().isEmpty()) {
            sb.append("\n\n").append(r.getNextStep().trim());
        }
        return sb.toString();
    }
}
