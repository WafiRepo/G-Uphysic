package de.rwth_aachen.phyphox.activity;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import de.rwth_aachen.phyphox.PictureCallback;
import de.rwth_aachen.phyphox.R;
import de.rwth_aachen.phyphox.databinding.ActivityCameraDetectionBinding;

public class CameraDetectionActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    public static final String TAG = "src";

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.wtf(TAG, "OpenCV failed to load!");
        }
    }

    private BaseLoaderCallback loaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");
                    binding.cameraview.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
            }
        }
    };
    private ActivityCameraDetectionBinding binding;
    private Bitmap currentDisplayBitmap; // Untuk track bitmap yang sedang ditampilkan
    private boolean isImageSaved = false; // Track status apakah gambar sudah disave

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCameraDetectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.cameraview.setCvCameraViewListener(this);

        // Disable tombol Next di awal
        binding.btnSave.setEnabled(false);

        // Setup capture button click listener
        binding.captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Ambil foto dengan circle detection
                binding.cameraview.takePicture(new JavaCameraView.PictureCallback() {
                    @Override
                    public void onPictureTaken(Mat picture) {
                        Log.i(TAG, "Picture taken - Mat size: " + picture.cols() + "x" + picture.rows() + " channels: " + picture.channels());

                        // Proses deteksi circle pada foto yang diambil
                        Mat processedImage = processCircleDetection(picture);

                        // Simpan foto yang sudah diproses
                        saveMatToFile(processedImage, "foto_with_circles.jpg");

                        // Tampilkan pesan
                        runOnUiThread(() -> {
                            Toast.makeText(CameraDetectionActivity.this,
                                    "Foto dengan circle detection berhasil diambil!", Toast.LENGTH_SHORT).show();

                            // Reset status save dan disable tombol Next
                            isImageSaved = false;
                            binding.btnSave.setEnabled(false);
                        });

                        // Bersihkan resource
                        processedImage.release();
                    }
                });
            }
        });

        // Setup Save button click listener
        binding.btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentDisplayBitmap != null) {
                    // Proses save gambar
                    isImageSaved = true;
                    binding.btnSave.setEnabled(true);
                    Toast.makeText(CameraDetectionActivity.this, "Gambar berhasil disimpan!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(CameraDetectionActivity.this, "Ambil foto terlebih dahulu!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Setup Next button click listener dengan validasi
        binding.btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isImageSaved) {
                    Toast.makeText(CameraDetectionActivity.this, "Tekan tombol Save terlebih dahulu!", Toast.LENGTH_LONG).show();
                } else {
                    // Lanjut ke halaman berikutnya
                    // Intent intent = new Intent(CameraDetectionActivity.this, NextActivity.class);
                    // startActivity(intent);
                    Toast.makeText(CameraDetectionActivity.this, "Lanjut ke halaman berikutnya", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Setup Back button click listener
        findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish(); // Kembali ke halaman sebelumnya
            }
        });
    }

    /**
     * Method untuk memproses deteksi circle pada Mat yang diberikan
     */
    private Mat processCircleDetection(Mat inputMat) {
        Log.i(TAG, "Processing circle detection - Input size: " + inputMat.cols() + "x" + inputMat.rows());

        // Konversi ke grayscale untuk deteksi
        Mat grayMat = new Mat();

        // Cek format input dan konversi yang sesuai
        if (inputMat.channels() == 4) {
            // RGBA format
            Imgproc.cvtColor(inputMat, grayMat, Imgproc.COLOR_RGBA2GRAY);
            Log.i(TAG, "Converted from RGBA to GRAY");
        } else if (inputMat.channels() == 3) {
            // RGB format
            Imgproc.cvtColor(inputMat, grayMat, Imgproc.COLOR_RGB2GRAY);
            Log.i(TAG, "Converted from RGB to GRAY");
        } else if (inputMat.channels() == 1) {
            // Sudah grayscale
            grayMat = inputMat.clone();
            Log.i(TAG, "Already in GRAY format");
        }

        // Buat salinan dari input untuk hasil akhir
        Mat resultMat = inputMat.clone();

        // Proses deteksi circle dengan multiple parameter sets
        Mat circles = new Mat();
        Mat blurred = new Mat();

        // Gaussian blur lebih efektif untuk circle detection
        Imgproc.GaussianBlur(grayMat, blurred, new Size(9, 9), 2, 2);

        // Try multiple parameter sets for better detection
        boolean circlesFound = false;

        // Parameter set 1: Lebih sensitif
        Imgproc.HoughCircles(
                blurred, circles, Imgproc.CV_HOUGH_GRADIENT,
                2,          // dp
                100,        // minDist
                100,        // Canny high threshold
                50,         // Accumulator threshold
                30,         // minRadius
                150         // maxRadius
        );
//
//        Log.i(TAG, "Parameter set 1 - Circles detected: " + circles.cols());
//
//        if (circles.cols() == 0) {
//            // Parameter set 2: Alternatif jika tidak ada yang terdeteksi
//            circles.release();
//            circles = new Mat();
//
//            Imgproc.HoughCircles(
//                    blurred, circles, Imgproc.CV_HOUGH_GRADIENT,
//                    2,          // dp
//                    80,         // minDist
//                    50,         // Canny high threshold (dikurangi)
//                    40,         // Accumulator threshold
//                    15,         // minRadius (lebih kecil)
//                    400         // maxRadius (lebih besar)
//            );
//
//            Log.i(TAG, "Parameter set 2 - Circles detected: " + circles.cols());
//        }
//
//        if (circles.cols() == 0) {
//            // Parameter set 3: Paling sensitif
//            circles.release();
//            circles = new Mat();
//
//            Imgproc.HoughCircles(
//                    blurred, circles, Imgproc.CV_HOUGH_GRADIENT,
//                    1,          // dp
//                    30,         // minDist
//                    80,         // Canny high threshold
//                    20,         // Accumulator threshold (sangat rendah)
//                    10,         // minRadius (sangat kecil)
//                    500         // maxRadius (sangat besar)
//            );
//
//            Log.i(TAG, "Parameter set 3 - Circles detected: " + circles.cols());
//        }

        // Gambar circle yang terdeteksi pada foto asli
        if (circles.cols() > 0) {
            for (int x = 0; x < Math.min(circles.cols(), 3); x++) { // Batasi untuk performance
                double circleVec[] = circles.get(0, x);

                if (circleVec == null) {
                    break;
                }

                Point center = new Point((int) circleVec[0], (int) circleVec[1]);
                int radius = (int) circleVec[2];

                // Gambar circle pada preview dengan warna putih
                Imgproc.circle(resultMat, center, 5, new Scalar(255, 255, 255), 3);
                Imgproc.circle(resultMat, center, radius, new Scalar(255, 255, 255), 2);
            }
        }else {
            Log.w(TAG, "No circles detected with any parameter set");

            // Tambahkan indicator bahwa tidak ada circle yang terdeteksi
            String noCircleText = "No circles detected";
            Point textPoint = new Point(50, 100);
            Imgproc.rectangle(resultMat, new Point(40, 70), new Point(350, 120), new Scalar(255, 255, 255), -1);
            Imgproc.putText(resultMat, noCircleText, textPoint, 0, 0.8, new Scalar(255, 0, 0), 2);
        }

        // Bersihkan resource
        circles.release();
        grayMat.release();
        blurred.release();

        return resultMat;
    }

    private void saveMatToFile(Mat mat, String filename) {
        Bitmap bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Bitmap displayBitmap = null;

        try {
            Utils.matToBitmap(mat, bitmap);

            File file = new File(getExternalFilesDir(null), filename);
            try (FileOutputStream out = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out); // Quality ditingkatkan
                out.flush();

                Log.i(TAG, "Foto berhasil disimpan: " + file.getAbsolutePath());

                // Buat salinan bitmap untuk ImageView
                displayBitmap = bitmap.copy(bitmap.getConfig(), false);

                // Tampilkan hasil di ImageView menggunakan salinan
                final Bitmap finalDisplayBitmap = displayBitmap;
                runOnUiThread(() -> {
                    if (finalDisplayBitmap != null && !finalDisplayBitmap.isRecycled()) {
                        // Cleanup bitmap sebelumnya jika ada
                        cleanupCurrentBitmap();

                        binding.cameraview.setVisibility(View.GONE);
                        binding.capturedImageView.setVisibility(View.VISIBLE);
                        binding.captureButton.setVisibility(View.GONE);
                        binding.capturedImageView.setBackground(new BitmapDrawable(getResources(), finalDisplayBitmap));

                        currentDisplayBitmap = finalDisplayBitmap;
                    }
                });

            } catch (IOException e) {
                Log.e(TAG, "Error saving image: " + e.getMessage());
                e.printStackTrace();

                // Jika gagal save, recycle displayBitmap juga
                if (displayBitmap != null && !displayBitmap.isRecycled()) {
                    displayBitmap.recycle();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing bitmap: " + e.getMessage());
            e.printStackTrace();

            // Cleanup jika ada error
            if (displayBitmap != null && !displayBitmap.isRecycled()) {
                displayBitmap.recycle();
            }
        } finally {
            // Recycle bitmap asli setelah selesai
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV not loaded");
        } else {
            Log.d(TAG, "OpenCV loaded successfully");
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        binding.cameraview.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.d(TAG, "Camera view started: " + width + "x" + height);
    }

    @Override
    public void onCameraViewStopped() {
        Log.d(TAG, "Camera view stopped");
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        // Proses frame untuk preview real-time (optional)
        Mat input = inputFrame.gray();
        Mat output = inputFrame.rgba();
        Mat circles = new Mat();

        // Gaussian blur untuk preview
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(input, blurred, new Size(9, 9), 2, 2);

        // Deteksi circle untuk preview (parameter lebih ringan)
        Imgproc.HoughCircles(
                blurred, circles, Imgproc.CV_HOUGH_GRADIENT,
                2,          // dp
                100,        // minDist
                100,        // Canny high threshold
                20,         // Accumulator threshold
                30,         // minRadius
                150         // maxRadius
        );

        // Gambar circle pada preview
        if (circles.cols() > 0) {
            for (int x = 0; x < Math.min(circles.cols(), 3); x++) { // Batasi untuk performance
                double circleVec[] = circles.get(0, x);

                if (circleVec == null) {
                    break;
                }

                Point center = new Point((int) circleVec[0], (int) circleVec[1]);
                int radius = (int) circleVec[2];

                // Gambar circle pada preview dengan warna putih
                Imgproc.circle(output, center, 5, new Scalar(255, 255, 255), 3);
                Imgproc.circle(output, center, radius, new Scalar(255, 255, 255), 2);
            }
        }

        // Bersihkan resource
        circles.release();
        blurred.release();
        input.release();

        return output;
    }

    /**
     * Method untuk cleanup bitmap yang sedang ditampilkan
     */
    private void cleanupCurrentBitmap() {
        if (currentDisplayBitmap != null && !currentDisplayBitmap.isRecycled()) {
            currentDisplayBitmap.recycle();
            currentDisplayBitmap = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cleanup bitmap saat activity di-destroy
        cleanupCurrentBitmap();
    }
}
