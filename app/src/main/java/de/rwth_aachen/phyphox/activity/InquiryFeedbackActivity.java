package de.rwth_aachen.phyphox.activity;

import android.animation.ObjectAnimator;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.rwth_aachen.phyphox.Helper.InquiryLogHelper;
import de.rwth_aachen.phyphox.NetworkConnection.ApiService;
import de.rwth_aachen.phyphox.NetworkConnection.InquiryGenerateProblemExploringRequest;
import de.rwth_aachen.phyphox.NetworkConnection.InquiryGenerateResponse;
import de.rwth_aachen.phyphox.NetworkConnection.RetrofitClient;
import de.rwth_aachen.phyphox.NetworkConnection.SubmitResponseRequest;
import de.rwth_aachen.phyphox.NetworkConnection.SubmitResponseResponse;
import de.rwth_aachen.phyphox.R;
import de.rwth_aachen.phyphox.databinding.ActivityInquiryFeedbackBinding;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InquiryFeedbackActivity extends AppCompatActivity {

    private static final String TAG = "InquiryFeedback";

    /** Nyawa: salah 3× → lanjut ke halaman berikutnya (sama seperti jawaban benar). */
    private static final int MAX_LIVES = 3;

    private int livesRemaining = MAX_LIVES;
    private ObjectAnimator trumpetAnimator;

    private final Runnable navigateAfterLivesDepletedRunnable = () -> {
        if (!isFinishing()) {
            startExperiment();
        }
    };

    public static final String EXTRA_OBJECT_NAME = "object_name";
    public static final String EXTRA_USER_ID = "user_id";
    public static final String EXTRA_SERVER_IMAGE_PATH = "server_image_path";

    /** Fallback jika API generate inquiry problem_exploring gagal */
    private static final String DEFAULT_QUESTION_TEMPLATE = "You've just captured an image of %s. How does centripetal acceleration relate to the motion of the %s?";

    /** Label sederhana untuk hasil pencocokan tipe jawaban dari backend. */
    private static final Map<String, String> ANSWER_TYPE_FEEDBACK = new HashMap<>();
    static {
        ANSWER_TYPE_FEEDBACK.put("correct", "Jawaban Anda sudah mengarah benar.");
        ANSWER_TYPE_FEEDBACK.put("wrong", "Jawaban Anda masih perlu perbaikan.");
        ANSWER_TYPE_FEEDBACK.put("uncertain", "Penjelasan singkat untuk membantu pemahaman:");
    }

    private ActivityInquiryFeedbackBinding binding;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInquiryFeedbackBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String objectName = getIntent().getStringExtra(EXTRA_OBJECT_NAME);
        userId = getIntent().getStringExtra(EXTRA_USER_ID);
        if (userId == null || userId.isEmpty()) userId = "default";

        binding.tvInquiryText.setText("Loading...");
        binding.etUserAnswer.setText("");
        binding.etUserAnswer.setHint("Ketik jawaban Anda di sini...");
        binding.btnGetFeedback.setEnabled(false);

        binding.etUserAnswer.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                boolean hasText = !TextUtils.isEmpty(s.toString().trim());
                binding.btnGetFeedback.setEnabled(hasText);
                binding.btnGetFeedback.setAlpha(hasText ? 1f : 0.6f);
            }
        });

        binding.btnGetFeedback.setOnClickListener(v -> onGetFeedbackClicked());

        binding.btnBackInquiryFeedback.setOnClickListener(v -> finish());

        updateHearts();

        fetchProblemExploringInquiry(objectName);
    }

    private void onGetFeedbackClicked() {
        String answer = binding.etUserAnswer.getText().toString().trim();
        if (answer.isEmpty()) {
            Toast.makeText(this, "Isi jawaban terlebih dahulu.", Toast.LENGTH_SHORT).show();
            return;
        }
        callSubmitResponse(answer);
    }

    private void updateHearts() {
        TextView[] hearts = { binding.tvHeart1, binding.tvHeart2, binding.tvHeart3 };
        for (int i = 0; i < hearts.length; i++) {
            hearts[i].setAlpha(i < livesRemaining ? 1f : 0.35f);
        }
    }

    private void vibrateSuccess() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v == null || !v.hasVibrator()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createWaveform(new long[]{0, 60, 80, 60, 80, 120}, -1));
        } else {
            v.vibrate(220);
        }
    }

    private void shakeScreen() {
        View v = binding.scrollContent;
        v.animate().cancel();
        v.setTranslationX(0f);
        v.animate()
                .translationX(14f)
                .setDuration(45)
                .withEndAction(() -> v.animate()
                        .translationX(-12f)
                        .setDuration(45)
                        .withEndAction(() -> v.animate()
                                .translationX(10f)
                                .setDuration(45)
                                .withEndAction(() -> v.animate()
                                        .translationX(0f)
                                        .setDuration(50)
                                        .start())))
                .start();
    }

    private void startTrumpetAnimation() {
        if (trumpetAnimator != null) {
            trumpetAnimator.cancel();
        }
        View trumpet = binding.tvTrumpet;
        trumpet.setRotation(0f);
        trumpetAnimator = ObjectAnimator.ofFloat(trumpet, View.ROTATION, -20f, 20f);
        trumpetAnimator.setDuration(550);
        trumpetAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        trumpetAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        trumpetAnimator.start();
    }

    private void stopTrumpetAnimation() {
        if (trumpetAnimator != null) {
            trumpetAnimator.cancel();
            trumpetAnimator = null;
        }
        binding.tvTrumpet.setRotation(0f);
        binding.tvTrumpet.setTranslationY(0f);
    }

    private void showCelebrationOverlay() {
        binding.overlayCongrats.setVisibility(View.VISIBLE);
        binding.btnCongratsContinue.setOnClickListener(v -> {
            stopTrumpetAnimation();
            binding.overlayCongrats.setVisibility(View.GONE);
            startExperiment();
        });
        shakeScreen();
        vibrateSuccess();
        startTrumpetAnimation();
    }

    @Override
    protected void onDestroy() {
        binding.getRoot().removeCallbacks(navigateAfterLivesDepletedRunnable);
        stopTrumpetAnimation();
        super.onDestroy();
    }

    private void fetchProblemExploringInquiry(String objectName) {
        ApiService api = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        InquiryGenerateProblemExploringRequest req = new InquiryGenerateProblemExploringRequest(userId, "centripetal acceleration", "id");
        Log.d(TAG, "Request inquiry: userId=" + userId + " (object context resolved from DB)");
        api.generateInquiryProblemExploring(req).enqueue(new Callback<InquiryGenerateResponse>() {
            @Override
            public void onResponse(Call<InquiryGenerateResponse> call, Response<InquiryGenerateResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getInquiry() != null && !response.body().getInquiry().isEmpty()) {
                    binding.tvInquiryText.setText(response.body().getInquiry());
                    InquiryLogHelper.log(
                            InquiryFeedbackActivity.this,
                            InquiryLogHelper.KIND_EXPLORING_STAGE1,
                            response.body().getInquiry(),
                            null,
                            null,
                            null,
                            getIntent().getStringExtra(EXTRA_OBJECT_NAME),
                            null,
                            null,
                            null,
                            null
                    );
                    Log.d(TAG, "Inquiry OK: " + response.body().getInquiry().substring(0, Math.min(50, response.body().getInquiry().length())) + "...");
                } else {
                    String errMsg = "Response tidak valid";
                    if (!response.isSuccessful()) {
                        try {
                            errMsg = "HTTP " + response.code() + (response.errorBody() != null ? ": " + response.errorBody().string() : "");
                        } catch (Exception e) { errMsg = "HTTP " + response.code(); }
                    } else if (response.body() == null || response.body().getInquiry() == null || response.body().getInquiry().isEmpty()) {
                        errMsg = "Inquiry kosong dari API";
                    }
                    Log.e(TAG, "Inquiry gagal: " + errMsg);
                    binding.tvInquiryText.setText(buildDefaultQuestion(objectName));
                    String shortMsg = errMsg.length() > 60 ? errMsg.substring(0, 57) + "..." : errMsg;
                    Toast.makeText(InquiryFeedbackActivity.this, "API gagal: " + shortMsg, Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(Call<InquiryGenerateResponse> call, Throwable t) {
                Log.e(TAG, "Inquiry onFailure: " + t.getMessage(), t);
                binding.tvInquiryText.setText(buildDefaultQuestion(objectName));
                Toast.makeText(InquiryFeedbackActivity.this, "Pertanyaan default. Error: " + (t.getMessage() != null ? t.getMessage() : "koneksi gagal"), Toast.LENGTH_LONG).show();
            }
        });
    }

    private String buildDefaultQuestion(String objectName) {
        if (objectName == null || objectName.trim().isEmpty()) objectName = "object";
        String name = objectName.trim();
        if (name.length() > 0) {
            name = name.substring(0, 1).toLowerCase() + (name.length() > 1 ? name.substring(1) : "");
        }
        String article = "aeiou".indexOf(name.isEmpty() ? 'x' : name.charAt(0)) >= 0 ? "an" : "a";
        return String.format(DEFAULT_QUESTION_TEMPLATE, article + " " + name, name);
    }

    private void callSubmitResponse(String responseText) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("Mengevaluasi jawaban");
        progress.setMessage("Jawaban Anda sedang dievaluasi oleh LLM untuk mendeteksi miskonsepsi...");
        progress.setCancelable(false);
        progress.show();

        ApiService api = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        // problem_exploring: pertanyaan "How does centripetal acceleration relate to the motion of X?" adalah analisis
        String currentInquiryText = binding.tvInquiryText.getText() != null ? binding.tvInquiryText.getText().toString().trim() : "";
        SubmitResponseRequest req = new SubmitResponseRequest(userId, responseText, "id", currentInquiryText);
        api.submitResponseProblemExploring(req).enqueue(new Callback<SubmitResponseResponse>() {
            @Override
            public void onResponse(Call<SubmitResponseResponse> call, Response<SubmitResponseResponse> response) {
                progress.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    String q = binding.tvInquiryText.getText() != null ? binding.tvInquiryText.getText().toString().trim() : "";
                    InquiryLogHelper.log(
                            InquiryFeedbackActivity.this,
                            InquiryLogHelper.KIND_EXPLORING_STAGE1,
                            q,
                            responseText,
                            summarizeSubmitResponse(response.body()),
                            null,
                            getIntent().getStringExtra(EXTRA_OBJECT_NAME),
                            null,
                            null,
                            null,
                            null
                    );
                    showResult(response.body());
                } else {
                    Toast.makeText(InquiryFeedbackActivity.this, "Gagal menganalisis respons.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<SubmitResponseResponse> call, Throwable t) {
                progress.dismiss();
                Toast.makeText(InquiryFeedbackActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showResult(SubmitResponseResponse result) {
        String feedback = result.getFeedback() != null ? result.getFeedback().trim() : "";
        String nextStep = result.getNextStep() != null ? result.getNextStep().trim() : "";
        String matchedType = result.getMatchedAnswerType() != null ? result.getMatchedAnswerType().trim().toLowerCase() : "";
        List<String> steps = result.getStepByStep();

        boolean isCorrect = "correct".equals(matchedType);
        if (!isCorrect) {
            livesRemaining = Math.max(0, livesRemaining - 1);
            updateHearts();
        }

        binding.labelFeedback.setVisibility(View.VISIBLE);
        binding.cardFeedback.setVisibility(View.VISIBLE);
        binding.tvFeedbackMessage.setVisibility(View.VISIBLE);

        StringBuilder sb = new StringBuilder();
        String typeHint = ANSWER_TYPE_FEEDBACK.get(matchedType);
        if (typeHint != null && !typeHint.isEmpty()) {
            sb.append(typeHint).append("\n\n");
        }
        if ("uncertain".equals(matchedType) && steps != null && !steps.isEmpty()) {
            for (int i = 0; i < steps.size(); i++) {
                String line = steps.get(i);
                if (line != null && !line.trim().isEmpty()) {
                    sb.append(i + 1).append(". ").append(line.trim()).append("\n\n");
                }
            }
            if (result.getExample() != null && !result.getExample().trim().isEmpty()) {
                sb.append("Contoh: ").append(result.getExample().trim()).append("\n\n");
            }
            if (result.getComprehensionCheck() != null && !result.getComprehensionCheck().trim().isEmpty()) {
                sb.append(result.getComprehensionCheck().trim());
            }
        } else {
            if (!feedback.isEmpty()) {
                sb.append(feedback);
            } else {
                sb.append("Terima kasih, jawaban Anda sudah diterima.");
            }
            if (!nextStep.isEmpty()) {
                sb.append("\n\n").append(nextStep);
            }
        }
        binding.tvFeedbackMessage.setText(sb.toString());

        if (livesRemaining <= 0 && !isCorrect) {
            binding.cardFeedback.setCardBackgroundColor(0xFFFFF3CD);
            binding.tvEditInstruction.setVisibility(View.VISIBLE);
            binding.tvEditInstruction.setText("Kesempatan habis. Anda akan diarahkan ke halaman berikutnya.");
            binding.btnGetFeedback.setEnabled(false);
            binding.btnGetFeedback.setAlpha(0.5f);
            binding.etUserAnswer.setEnabled(false);
            Toast.makeText(this, "Kesempatan menjawab habis.", Toast.LENGTH_LONG).show();
            binding.getRoot().postDelayed(navigateAfterLivesDepletedRunnable, 2200);
            return;
        }

        if ("wrong".equals(matchedType)) {
            binding.cardFeedback.setCardBackgroundColor(0xFFFFF3CD); // amber/warning
            binding.tvEditInstruction.setVisibility(View.VISIBLE);
            binding.tvEditInstruction.setText("Silakan perbaiki jawaban Anda di atas, lalu tekan \"Get Feedback\" lagi.");
            binding.btnGetFeedback.setText("Get Feedback");
            binding.btnGetFeedback.setEnabled(true);
            binding.btnGetFeedback.setAlpha(1f);
            binding.btnGetFeedback.setOnClickListener(v -> onGetFeedbackClicked());
        } else if ("uncertain".equals(matchedType)) {
            binding.cardFeedback.setCardBackgroundColor(0xFFE7F3FF); // light blue
            binding.tvEditInstruction.setVisibility(View.VISIBLE);
            binding.tvEditInstruction.setText("Silakan perbaiki jawaban Anda di atas, lalu tekan \"Get Feedback\" lagi.");
            binding.btnGetFeedback.setText("Get Feedback");
            binding.btnGetFeedback.setEnabled(true);
            binding.btnGetFeedback.setAlpha(1f);
            binding.btnGetFeedback.setOnClickListener(v -> onGetFeedbackClicked());
        } else {
            binding.cardFeedback.setCardBackgroundColor(0xFFD4EDDA); // green
            binding.tvEditInstruction.setVisibility(View.GONE);
            binding.btnGetFeedback.setText("Mulai eksperimen");
            binding.btnGetFeedback.setEnabled(true);
            binding.btnGetFeedback.setAlpha(1f);
            binding.btnGetFeedback.setOnClickListener(v -> {
                stopTrumpetAnimation();
                binding.overlayCongrats.setVisibility(View.GONE);
                startExperiment();
            });
            showCelebrationOverlay();
        }
    }

    private void startExperiment() {
        startActivity(new android.content.Intent(this, de.rwth_aachen.phyphox.ExperimentList.class));
        finish();
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
