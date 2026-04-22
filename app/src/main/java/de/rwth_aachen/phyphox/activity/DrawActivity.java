package de.rwth_aachen.phyphox.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Locale;
import java.util.UUID;

import de.rwth_aachen.phyphox.App;
import de.rwth_aachen.phyphox.Helper.DrawingView;
import de.rwth_aachen.phyphox.Helper.InquiryLogHelper;
import de.rwth_aachen.phyphox.Helper.SessionManager;
import de.rwth_aachen.phyphox.NetworkConnection.ApiService;
import de.rwth_aachen.phyphox.NetworkConnection.RetrofitClient;
import de.rwth_aachen.phyphox.NetworkConnection.SubmitResponseRequest;
import de.rwth_aachen.phyphox.R;
import de.rwth_aachen.phyphox.model.DataModel;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DrawActivity extends AppCompatActivity {

    private DrawingView mDrawingView;
    private long visitStartTime;
    private LinearLayout colorPickerOverlay;
    private View currentColorIndicator;
    private boolean isColorPickerVisible = false;
    private TextView tvCanvasHeaderTitle;
    private TextView tvCanvasHeaderSubtitle;
    private TextView tvInquiryFeedbackResult;
    private ScrollView svInquiryFeedbackResult;
    private final StringBuilder feedbackHistoryBuilder = new StringBuilder();
    private int feedbackTurn = 0;
    private String stage2SessionId;
    private String stage2GraphContext = "Screenshot phyphox + canvas: baca label sumbu dari gambar (contoh umum: ω vs t).";
    private String cachedGraphImageBase64;

    private static String stripQuestionFieldFromFeedback(String feedback) {
        if (feedback == null) return "";
        String out = feedback;
        out = out.replaceAll("(?im)^\\s*pertanyaan\\s*:\\s*.*(?:\\r?\\n)?", "");
        out = out.replaceAll("(?im)^\\s*pertanyaan\\s*-\\s*.*(?:\\r?\\n)?", "");
        out = out.replaceAll("(?im)^\\s*pertanyaan\\s+.*(?:\\r?\\n)?", "");
        out = out.replaceAll("(?im)^\\s*question\\s*:\\s*.*(?:\\r?\\n)?", "");
        out = out.replaceAll("(?im)^\\s*question\\s*-\\s*.*(?:\\r?\\n)?", "");
        out = out.replaceAll("(?im)^\\s*question\\s+.*(?:\\r?\\n)?", "");
        out = out.replaceAll("(?im)^\\s*inquiry\\s*:\\s*.*(?:\\r?\\n)?", "");
        out = out.replaceAll("(?im)^\\s*inquiry\\s*#?\\d*\\s*:\\s*.*(?:\\r?\\n)?", "");
        out = out.replaceAll("(?im)^\\s*type\\s*:\\s*.*(?:\\r?\\n)?", "");
        return out.trim();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw);

        // Initialize DrawingView
        mDrawingView = new DrawingView(this);

        // Add DrawingView to the layout
        LinearLayout imageScreenshot = findViewById(R.id.iv_screenshot);
        LinearLayout mDrawingPad = findViewById(R.id.view_drawing_pad);
        mDrawingPad.addView(mDrawingView);

        // Get the image from the intent and set it as background
        Intent intent = getIntent();
        String uri = intent.getStringExtra("Uri Image");
        Log.d("DRAW ACTIVITY", "onCreate: " + uri);
        File file = new File(getRealPathFromURI(Uri.parse(uri)));
        Drawable d = Drawable.createFromPath(file.getAbsolutePath());
        imageScreenshot.setBackground(d);

        // Set up buttons for undo, redo, clear, calculator, color picker and next
        FloatingActionButton btnUndo = findViewById(R.id.fab_undo);
        FloatingActionButton btnRedo = findViewById(R.id.fab_redo);
        FloatingActionButton btnClear = findViewById(R.id.fab_clear);
        FloatingActionButton btnCalculator = findViewById(R.id.fab_calculator);
        FloatingActionButton btnColorPicker = findViewById(R.id.fab_color_picker);
        android.widget.Button btnNext = findViewById(R.id.fab_next);
        
        // Initialize color picker elements
        colorPickerOverlay = findViewById(R.id.color_picker_overlay);
        currentColorIndicator = findViewById(R.id.current_color_indicator);
        tvCanvasHeaderTitle = findViewById(R.id.tv_canvas_header_title);
        tvCanvasHeaderSubtitle = findViewById(R.id.tv_canvas_header_subtitle);

        // Set click listeners for buttons
        btnUndo.setOnClickListener(v -> mDrawingView.undo());
        btnRedo.setOnClickListener(v -> mDrawingView.redo());
        btnClear.setOnClickListener(v -> mDrawingView.clear());
        
        // Color picker toggle
        btnColorPicker.setOnClickListener(v -> toggleColorPicker());

        // Handle Calculator button click - Launch custom calculator
        btnCalculator.setOnClickListener(v -> {
            Intent calculatorIntent = new Intent(DrawActivity.this, CalculatorActivity.class);
            startActivity(calculatorIntent);
        });

        // Tombol next: kompres JPEG (sama seperti API) lalu unggah ke Firebase Storage
        btnNext.setOnClickListener(v -> {
            Bitmap bmp = getViewAsBitmap(imageScreenshot);
            byte[] data = compressBitmapToJpeg(bmp, 2048, 85);
            if (bmp != null && !bmp.isRecycled()) {
                bmp.recycle();
            }
            if (data.length == 0) {
                Toast.makeText(this, "Gagal menyiapkan gambar untuk diunggah.", Toast.LENGTH_LONG).show();
                return;
            }
            uploadImageToFirestore(data, "question_image_" + System.currentTimeMillis());
        });

        // Tambahkan logic untuk icon info (introduction)
        FloatingActionButton ivInfo = findViewById(R.id.ivSign);
        ivInfo.setOnClickListener(v -> showIntroductionDialog());
        
        // Setup color selection buttons
        setupColorButtons();
        setupInquiryInputSubmission(imageScreenshot);
    }

    private void showIntroductionDialog() {
        visitStartTime = System.currentTimeMillis();
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_introduction, null, false);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();
        // Tombol tutup
        dialogView.findViewById(R.id.tvClose).setOnClickListener(v ->{
            updateTotalVisitingIntroduction(visitStartTime);
            dialog.dismiss();
        });
        dialog.show();
    }

    // Helper method to get the real file path from a URI
    private String getRealPathFromURI(Uri contentURI) {
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) {
            return contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            return cursor.getString(idx);
        }
    }

    public Bitmap getViewAsBitmap(View view) {
        // Enable drawing cache
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        // Create a bitmap from the view's drawing cache
        Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
        // Disable drawing cache
        view.setDrawingCacheEnabled(false);
        return bitmap;
    }

    public byte[] convertBitmapToBytes(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos); // Compress as PNG or JPEG
        return baos.toByteArray();
    }

    /**
     * Kompres bitmap ke JPEG dengan batas sisi terpanjang; mengecilkan ukuran payload base64 untuk API.
     */
    private static byte[] compressBitmapToJpeg(Bitmap source, int maxSide, int quality) {
        int w = source.getWidth();
        int h = source.getHeight();
        float maxDim = Math.max(w, h);
        Bitmap toCompress = source;
        if (maxDim > maxSide) {
            float scale = maxSide / maxDim;
            int nw = Math.max(1, Math.round(w * scale));
            int nh = Math.max(1, Math.round(h * scale));
            toCompress = Bitmap.createScaledBitmap(source, nw, nh, true);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        toCompress.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        if (toCompress != source) {
            toCompress.recycle();
        }
        return baos.toByteArray();
    }

    private void setupInquiryInputSubmission(LinearLayout imageScreenshot) {
        android.widget.EditText etInquiryInput = findViewById(R.id.et_inquiry_input);
        android.widget.Button btnSubmitInquiry = findViewById(R.id.btn_submit_inquiry_feedback);
        tvInquiryFeedbackResult = findViewById(R.id.tv_inquiry_feedback_result);
        svInquiryFeedbackResult = findViewById(R.id.sv_inquiry_feedback_result);
        stage2SessionId = UUID.randomUUID().toString();

        btnSubmitInquiry.setOnClickListener(v -> {
            String userInquiry = etInquiryInput.getText() != null ? etInquiryInput.getText().toString().trim() : "";
            if (userInquiry.isEmpty()) {
                Toast.makeText(this, "Silakan isi inquiry terlebih dahulu.", Toast.LENGTH_SHORT).show();
                return;
            }
            submitInquiryForFeedback(imageScreenshot, userInquiry);
        });
    }

    private String ensureGraphImageBase64(LinearLayout imageScreenshot) {
        if (cachedGraphImageBase64 != null && !cachedGraphImageBase64.isEmpty()) {
            return cachedGraphImageBase64;
        }
        ProgressDialog progress = new ProgressDialog(this);
        Bitmap bmp = getViewAsBitmap(imageScreenshot);
        byte[] jpeg = compressBitmapToJpeg(bmp, 1280, 80);
        if (jpeg.length > 900_000) {
            jpeg = compressBitmapToJpeg(bmp, 960, 70);
        }
        if (jpeg.length > 900_000) {
            jpeg = compressBitmapToJpeg(bmp, 800, 60);
        }
        bmp.recycle();
        cachedGraphImageBase64 = android.util.Base64.encodeToString(jpeg, android.util.Base64.NO_WRAP);
        return cachedGraphImageBase64;
    }

    private void submitInquiryForFeedback(LinearLayout imageScreenshot, String userInquiry) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("Mengirim inquiry");
        progress.setMessage("Menunggu feedback LLM...");
        progress.setCancelable(false);
        progress.show();

        String b64 = ensureGraphImageBase64(imageScreenshot);
        String uid = SessionManager.getId(this);
        if (uid == null || uid.isEmpty()) uid = "default";

        SubmitResponseRequest req = new SubmitResponseRequest(
                uid,
                userInquiry,
                "id",
                stage2SessionId,
                stage2GraphContext,
                b64
        );
        ApiService api = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        api.submitResponseProblemExploringStage2(req).enqueue(new Callback<de.rwth_aachen.phyphox.NetworkConnection.SubmitResponseResponse>() {
            @Override
            public void onResponse(Call<de.rwth_aachen.phyphox.NetworkConnection.SubmitResponseResponse> call, Response<de.rwth_aachen.phyphox.NetworkConnection.SubmitResponseResponse> response) {
                progress.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    String feedback = response.body().getFeedback();
                    String nextStep = response.body().getNextStep();
                    if (feedback == null || feedback.trim().isEmpty()) {
                        feedback = "none";
                    }
                    feedback = stripQuestionFieldFromFeedback(feedback);
                    nextStep = stripQuestionFieldFromFeedback(nextStep);
                    StringBuilder entry = new StringBuilder();
                    feedbackTurn++;
                    entry.append("Feedback #").append(feedbackTurn).append(":\n").append(feedback);
                    if (nextStep != null && !nextStep.trim().isEmpty()) {
                        entry.append("\n\nNext step: ").append(nextStep.trim());
                    }
                    if (feedbackHistoryBuilder.length() > 0) {
                        feedbackHistoryBuilder.append("\n\n----------------\n\n");
                    }
                    feedbackHistoryBuilder.append(entry);
                    String fbSummary = entry.toString();
                    byte[] graphJpeg = null;
                    try {
                        graphJpeg = android.util.Base64.decode(b64, android.util.Base64.NO_WRAP);
                    } catch (Exception ignored) {
                    }
                    InquiryLogHelper.logExploringStage2WithCanvasUpload(
                            DrawActivity.this,
                            stage2GraphContext != null ? stage2GraphContext : "Canvas / graph inquiry",
                            userInquiry,
                            fbSummary,
                            stage2SessionId,
                            graphJpeg
                    );
                    tvInquiryFeedbackResult.setText(feedbackHistoryBuilder.toString());
                    svInquiryFeedbackResult.setVisibility(View.VISIBLE);
                    tvCanvasHeaderTitle.setText("🧠 Buat Inquiry");
                    tvCanvasHeaderSubtitle.setText("Anda bisa edit inquiry lalu submit lagi untuk feedback baru.");
                    svInquiryFeedbackResult.post(() -> svInquiryFeedbackResult.fullScroll(View.FOCUS_DOWN));
                } else {
                    Toast.makeText(DrawActivity.this,
                            "Gagal mendapatkan feedback dari server.",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<de.rwth_aachen.phyphox.NetworkConnection.SubmitResponseResponse> call, Throwable t) {
                progress.dismiss();
                Toast.makeText(DrawActivity.this,
                        "Koneksi gagal: " + (t.getMessage() != null ? t.getMessage() : "unknown"),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Pesan error Firebase Storage yang lebih jelas (bukan hanya "unknown error").
     */
    private String formatFirebaseStorageError(Throwable e) {
        Throwable cur = e;
        for (int i = 0; i < 4 && cur != null; i++) {
            if (cur instanceof StorageException) {
                StorageException se = (StorageException) cur;
                int code = se.getErrorCode();
                if (code == StorageException.ERROR_NOT_AUTHORIZED) {
                    return "Akses ditolak oleh Firebase Storage. Periksa rules Storage dan autentikasi di aplikasi.";
                }
                if (code == StorageException.ERROR_NOT_AUTHENTICATED) {
                    return "Belum login ke Firebase. Pastikan pengguna sudah masuk jika rules meminta auth.";
                }
                if (code == StorageException.ERROR_QUOTA_EXCEEDED) {
                    return "Kuota penyimpanan Firebase tercapai.";
                }
                if (code == StorageException.ERROR_RETRY_LIMIT_EXCEEDED) {
                    return "Unggah gagal setelah beberapa percobaan. Coba lagi atau periksa jaringan.";
                }
                if (code == StorageException.ERROR_OBJECT_NOT_FOUND
                        || code == StorageException.ERROR_BUCKET_NOT_FOUND
                        || code == StorageException.ERROR_PROJECT_NOT_FOUND) {
                    return "Konfigurasi bucket/proyek Firebase tidak cocok. Periksa google-services.json.";
                }
                String msg = se.getMessage();
                if (msg != null && !msg.trim().isEmpty()) {
                    return msg;
                }
                return "Gagal unggah (kode Storage: " + code + ").";
            }
            cur = cur.getCause();
        }
        String msg = e != null ? e.getMessage() : null;
        if (msg != null && !msg.trim().isEmpty()) {
            return msg;
        }
        return "Terjadi kesalahan saat mengunggah gambar.";
    }

    public void uploadImageToFirestore(byte[] imageData, String fileName) {
        if (imageData == null || imageData.length == 0) {
            Toast.makeText(this, "Data gambar kosong.", Toast.LENGTH_LONG).show();
            return;
        }

        // Create and configure ProgressDialog
        ProgressDialog progressDialog = new ProgressDialog(this); // Replace 'this' with 'requireContext()' if inside a Fragment
        progressDialog.setTitle("Mengunggah gambar");
        progressDialog.setMessage("Mohon tunggu...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Get Firebase Storage instance
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        // Create a reference to the image
        StorageReference imageRef = storageRef.child("images/" + fileName);

        // Upload the image
        UploadTask uploadTask = imageRef.putBytes(imageData);
        uploadTask
                .addOnProgressListener(snapshot -> {
                    long total = snapshot.getTotalByteCount();
                    if (total <= 0) {
                        return;
                    }
                    double progress = (100.0 * snapshot.getBytesTransferred()) / total;
                    progressDialog.setMessage("Terunggah: " + (int) progress + "%");
                })
                .addOnSuccessListener(taskSnapshot -> {
                    // Get the download URL (wajib ada failure listener — tanpa ini error URL tidak tertangani)
                    imageRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                String downloadUrl = uri.toString();

                                // Save the download URL to DataModel
                                App app = (App) getApplication();
                                DataModel dataModel = app.getDataModel();
                                dataModel.setPhotoAcceleration(downloadUrl);
                                dataModel.setValueAcceleration(""); // No input value needed anymore
                                app.setDataModel(dataModel);
                                startActivity(new Intent(this, QuestionActivity.class));

                                Log.d("Firebase", "Image uploaded successfully: " + downloadUrl);

                                progressDialog.dismiss();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("Firebase", "getDownloadUrl failed", e);
                                progressDialog.dismiss();
                                Toast.makeText(this,
                                        "Gagal mendapatkan URL gambar: " + formatFirebaseStorageError(e),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("Firebase", "Image upload failed", e);
                    progressDialog.dismiss();
                    Toast.makeText(this,
                            "Unggah gambar gagal: " + formatFirebaseStorageError(e),
                            Toast.LENGTH_LONG).show();
                });
    }
    public void updateTotalVisitingIntroduction(long visitStartTimeMillis) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DocumentReference userDocRef = firestore.collection("user").document(SessionManager.getId(this));

        long visitDuration = System.currentTimeMillis() - visitStartTimeMillis;

        firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(userDocRef);

            Long currentTotal = snapshot.getLong("totalVisitingIntroduction");
            if (currentTotal == null) currentTotal = 0L;
            Long newTotal = currentTotal + 1;

            Long totalDuration = snapshot.getLong("totalVisitDurationMillis");
            if (totalDuration == null) totalDuration = 0L;
            long newTotalDuration = totalDuration + visitDuration;

            // Convert durasi ke format readable
            String lastDurationFormatted = formatDuration(visitDuration);
            String totalDurationFormatted = formatDuration(newTotalDuration);

            // Update Firestore
            transaction.update(userDocRef, "totalVisitingIntroduction", newTotal);
            transaction.update(userDocRef, "totalVisitDurationMillis", newTotalDuration);
            transaction.update(userDocRef, "totalVisitDurationFormatted", totalDurationFormatted);

            // Simpan juga ke SessionManager
            SessionManager.setKeyVisitingInto(this, newTotal);
            SessionManager.setTotalVisitDuration(this, newTotalDuration);

            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d("Firestore", "Berhasil update durasi dan format waktu");
        }).addOnFailureListener(e -> {
            Log.e("Firestore", "Gagal update durasi", e);
        });
    }
    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        seconds %= 60;
        minutes %= 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d jam %d menit %d detik", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format(Locale.getDefault(), "%d menit %d detik", minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%d detik", seconds);
        }
    }
    
    private void toggleColorPicker() {
        isColorPickerVisible = !isColorPickerVisible;
        colorPickerOverlay.setVisibility(isColorPickerVisible ? View.VISIBLE : View.GONE);
    }
    
    private void setupColorButtons() {
        // Color selection buttons
        View colorBlack = findViewById(R.id.color_black);
        View colorRed = findViewById(R.id.color_red);
        View colorBlue = findViewById(R.id.color_blue);
        View colorGreen = findViewById(R.id.color_green);
        View colorOrange = findViewById(R.id.color_orange);
        View colorPurple = findViewById(R.id.color_purple);
        View colorPink = findViewById(R.id.color_pink);
        View colorBrown = findViewById(R.id.color_brown);
        
        // Set click listeners for each color
        setColorClickListener(colorBlack, Color.BLACK);
        setColorClickListener(colorRed, Color.RED);
        setColorClickListener(colorBlue, Color.BLUE);
        setColorClickListener(colorGreen, Color.GREEN);
        setColorClickListener(colorOrange, Color.parseColor("#FF9800"));
        setColorClickListener(colorPurple, Color.parseColor("#9C27B0"));
        setColorClickListener(colorPink, Color.parseColor("#E91E63"));
        setColorClickListener(colorBrown, Color.parseColor("#795548"));
    }
    
    private void setColorClickListener(View colorView, int color) {
        colorView.setOnClickListener(v -> {
            mDrawingView.setColor(color);
            updateCurrentColorIndicator(color);
            toggleColorPicker(); // Hide color picker after selection
            Toast.makeText(this, "Warna berubah!", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void updateCurrentColorIndicator(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        drawable.setStroke(4, Color.WHITE);
        currentColorIndicator.setBackground(drawable);
    }

}




