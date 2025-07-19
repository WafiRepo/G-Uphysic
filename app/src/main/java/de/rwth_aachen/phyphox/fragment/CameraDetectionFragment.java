package de.rwth_aachen.phyphox.fragment;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.ExifInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import de.rwth_aachen.phyphox.App;
import de.rwth_aachen.phyphox.ExperimentList;
import de.rwth_aachen.phyphox.Helper.SessionManager;
import de.rwth_aachen.phyphox.NetworkConnection.ApiResponse;
import de.rwth_aachen.phyphox.NetworkConnection.ApiService;
import de.rwth_aachen.phyphox.NetworkConnection.RetrofitClient;
import de.rwth_aachen.phyphox.activity.CameraDetectionActivity;
import de.rwth_aachen.phyphox.databinding.ActivityCameraDetectionBinding;
import de.rwth_aachen.phyphox.databinding.FragmentShareLocationBinding;
import de.rwth_aachen.phyphox.model.DataModel;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class CameraDetectionFragment extends Fragment implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "ShareLocationFragment";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private ActivityCameraDetectionBinding binding;
    String downloadUrl = "";
    ProgressDialog progressDialog;
    private Bitmap currentDisplayBitmap; // Untuk track bitmap yang sedang ditampilkan
    private File photoFile;
    private Mat latestPreviewFrame;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(getActivity()) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.d(TAG, "OpenCV loaded successfully");
                    binding.cameraview.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
//            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, getContext(), mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = ActivityCameraDetectionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        progressDialog = new ProgressDialog(requireContext());
        binding.btnSave.setOnClickListener(v -> {
            App app = (App) requireActivity().getApplication();
            DataModel dataModel = app.getDataModel();
//            if (!dataModel.getPhoto().isEmpty()) {
                startActivity(new Intent(getActivity(), ExperimentList.class));
//            } else {
//                Toast.makeText(getActivity(), "Upload Photo First", Toast.LENGTH_LONG).show();
//            }
        });
        binding.btnUpload.setOnClickListener(v -> {
            progressDialog.setTitle("Uploading Image");
            progressDialog.setMessage("Please wait while the image is being uploaded...");
            progressDialog.setCancelable(false);
            progressDialog.show();
            binding.btnSave.setEnabled(false);
            uploadImageToFirestore(convertBitmapToBytes(currentDisplayBitmap), "capture_image_" + System.currentTimeMillis());

        });
        // Initialize the PreviewView

        // Set up the click listener for the capture button
        binding.captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Ambil foto dengan circle detection
                binding.cameraview.takePicture(new JavaCameraView.PictureCallback() {
                    @Override
                    public void onPictureTaken(Mat picture) {
                        Log.i(TAG, "Picture taken - Mat size: " + picture.cols() + "x" + picture.rows() + " channels: " + picture.channels());

                        // Proses deteksi circle pada foto yang diambil
//                        Mat processedImage = processCircleDetection(picture);

                        // Simpan foto yang sudah diproses
                        saveMatToFile(latestPreviewFrame, "foto_with_circles.jpg");

                        // Tampilkan pesan
                        requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(),
                                "Foto dengan circle detection berhasil diambil!", Toast.LENGTH_SHORT).show());

                        // Bersihkan resource
                        latestPreviewFrame.release();
                    }
                });
            }
        });
        // Pastikan CameraView diinisialisasi dengan benar
        binding.cameraview.setVisibility(View.VISIBLE);
        binding.cameraview.setCvCameraViewListener(this);
        // Set camera index jika perlu
        binding.cameraview.setCameraIndex(JavaCameraView.CAMERA_ID_BACK);

        // Periksa permission
        if (checkCameraPermission()) {
            startCamera();
        } else {
            requestCameraPermission();
        }
    }
    private void startCamera() {
        if (getActivity() == null || !isAdded()) return;

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),
                    Manifest.permission.CAMERA)) {
                // Tampilkan penjelasan mengapa perlu permission
                new AlertDialog.Builder(requireContext())
                        .setTitle("Camera Permission Needed")
                        .setMessage("This app needs camera permission to function")
                        .setPositiveButton("OK", (d, w) -> requestPermissions(
                                new String[]{Manifest.permission.CAMERA},
                                CAMERA_PERMISSION_REQUEST_CODE))
                        .show();
            } else {
                requestPermissions(new String[]{Manifest.permission.CAMERA},
                        CAMERA_PERMISSION_REQUEST_CODE);
            }
            return;
        }

        // Pastikan OpenCV sudah terload
        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(getContext(), "OpenCV failed to load", Toast.LENGTH_LONG).show();
            return;
        }

        binding.cameraview.enableView();
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
    }





    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                showPermissionDeniedMessage("Camera");
            }
        }
    }

    private void showPermissionDeniedMessage(String permission) {
        new AlertDialog.Builder(requireContext()).setTitle(permission + " Permission Denied").setMessage("This feature requires " + permission + " permission to function. Please grant it in settings.").setPositiveButton("OK", (dialog, which) -> dialog.dismiss()).show();
    }

    public byte[] convertBitmapToBytes(Bitmap bitmap) {
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();

            // Kompres dengan kualitas 80% format JPEG
            boolean success = bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);

            if (!success) {
            }

            return baos.toByteArray();
        } finally {
            try {
                if (baos != null) {
                    baos.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error closing stream", e);
            }
        }
    }

    public void uploadImageToFirestore(byte[] imageData, String fileName) {
        // Create and configure ProgressDialog


        // Get Firebase Storage instance
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        // Create a reference to the image
        StorageReference imageRef = storageRef.child("images/" + fileName);

        // Upload the image
        UploadTask uploadTask = imageRef.putBytes(imageData);
        uploadTask.addOnProgressListener(snapshot -> {
            // Update the ProgressDialog with the upload progress
            double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
            progressDialog.setMessage("Uploaded: " + (int) progress + "%");
        }).addOnSuccessListener(taskSnapshot -> {
            // Get the download URL
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                // Hide ProgressDialog
                downloadUrl = uri.toString();


                // Save download URL or perform other tasks
                App app = (App) requireActivity().getApplication();
                DataModel dataModel = app.getDataModel();
                dataModel.setIdCustomer(SessionManager.getId(requireActivity()));
                dataModel.setPhoto(downloadUrl);
                Log.d("dataModel", "Image uploaded  " + dataModel.getPhotoDraw());
                Log.d("dataModel", "Image uploaded  " + SessionManager.getId(requireActivity()));
                uploadImage(photoFile);
                Log.d("Firebase", "Image uploaded successfully: " + downloadUrl);
            });
        }).addOnFailureListener(e -> {
            // Handle upload failure
            progressDialog.dismiss();
            Log.e("Firebase", "Image upload failed", e);
            Toast.makeText(requireContext(), "Image upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }
    private void uploadImage(File file) {
        // Create RequestBody instance from file
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);

        // MultipartBody.Part is used to send also the actual file name
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        // Initialize your API service using your Retrofit client
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        RequestBody userIdBody = RequestBody.create(MediaType.parse("text/plain"), SessionManager.getId(requireContext()));

        // Enqueue the call asynchronously
        apiService.uploadImage(body, userIdBody).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    binding.cardResult.setVisibility(View.VISIBLE);

                    ApiResponse apiResponse = response.body();
                    binding.tvLabel.setText(apiResponse.getLabel());
                    Log.d(TAG, "Upload successful: " + apiResponse.getMessage());
                    Toast.makeText(requireContext(), "Upload successful: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        // Ambil error body dan parse jadi JSON
                        String errorBody = response.errorBody().string();
                        JSONObject jsonObject = new JSONObject(errorBody);
                        String errorMessage = jsonObject.optString("error", "Unknown error");

                        Toast.makeText(requireContext(), "Upload failed: " + errorMessage, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Upload failed: " + errorMessage);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(requireContext(), "Upload failed: Unknown error", Toast.LENGTH_LONG).show();
                    }
                }
                binding.btnSave.setEnabled(true);
                progressDialog.dismiss();
            }
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                binding.btnSave.setEnabled(true);
                progressDialog.dismiss();
                Toast.makeText(requireContext(), "Image upload API failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Upload error: " + t.getMessage());
            }
        });
    }


    @Override
    public void onPause() {
        super.onPause();
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
        Mat input = inputFrame.gray();
        Mat output = inputFrame.rgba();

        // Rotasi menggunakan warpAffine
        Mat rotatedInput = rotateImage(input, 270);
        Mat rotatedOutput = rotateImage(output, 270);

        Mat circles = new Mat();
        Mat blurred = new Mat();

        Imgproc.GaussianBlur(rotatedInput, blurred, new Size(9, 9), 2, 2);

        Imgproc.HoughCircles(
                blurred, circles, Imgproc.CV_HOUGH_GRADIENT,
                2, 100, 100, 200, 30, 150
        );

        if (circles.cols() > 0) {
            for (int x = 0; x < Math.min(circles.cols(), 1); x++) { // hanya lingkaran terbesar
                double circleVec[] = circles.get(0, x);
                if (circleVec == null) break;

                Point center = new Point((int) circleVec[0], (int) circleVec[1]);
                int radius = (int) circleVec[2];

                // 1. Lingkaran hijau
                Imgproc.circle(rotatedOutput, center, radius, new Scalar(0, 255, 0), 4);

                // 2. Garis radius (r) ke kanan
                Point edge = new Point(center.x + radius, center.y);
                Imgproc.line(rotatedOutput, center, edge, new Scalar(255, 255, 0), 3);

                // 3. Label "r" di tengah radius
                Imgproc.putText(rotatedOutput, "r",
                    new Point(center.x + radius / 2, center.y - 10),
                    Core.FONT_HERSHEY_SIMPLEX, 1.2, new Scalar(255, 0, 0), 3);

                // 4. Garis melengkung 'w' (arc lebih panjang)
                double arcRadius = radius * 0.8;
                double arcStart = Math.PI / 4;      // 45 derajat
                double arcEnd = 8 * Math.PI / 9;    // ~160 derajat
                Point prev = null;
                for (double theta = arcStart; theta <= arcEnd; theta += Math.PI / 60) {
                    Point pt = new Point(
                        center.x + arcRadius * Math.cos(theta),
                        center.y + arcRadius * Math.sin(theta)
                    );
                    if (prev != null) {
                        Imgproc.line(rotatedOutput, prev, pt, new Scalar(0, 255, 255), 3);
                    }
                    prev = pt;
                }
                // Panah di ujung arc, tangensial
                double arrowTheta = arcEnd;
                Point arcTip = new Point(
                    center.x + arcRadius * Math.cos(arrowTheta),
                    center.y + arcRadius * Math.sin(arrowTheta)
                );
                double tangentAngle = arrowTheta + Math.PI / 1.75; // tangensial searah putaran
                double arrowLen = 40;
                Point arrowEnd = new Point(
                    arcTip.x + arrowLen * Math.cos(tangentAngle),
                    arcTip.y + arrowLen * Math.sin(tangentAngle)
                );
                Imgproc.arrowedLine(rotatedOutput, arcTip, arrowEnd, new Scalar(0, 255, 255), 3, 8, 0, 0.3);

                // Label 'w' di ujung panah (warna merah)
                Imgproc.putText(rotatedOutput, "w",
                    new Point(arrowEnd.x + 10, arrowEnd.y + 10),
                    Core.FONT_HERSHEY_SIMPLEX, 1.2, new Scalar(255, 0, 0), 3);

                // 5. Panah a (percepatan sentripetal) - geser ke atas
                double aAngle = -Math.PI / 2.5; // lebih ke atas dari -45 derajat
                double aLen = radius * 0.6;   // diperpanjang
                Point aStart = new Point(
                    center.x + radius * Math.cos(aAngle),
                    center.y + radius * Math.sin(aAngle)
                );
                Point aEnd = new Point(
                    center.x + (radius - aLen) * Math.cos(aAngle),
                    center.y + (radius - aLen) * Math.sin(aAngle)
                );
                Imgproc.arrowedLine(rotatedOutput, aStart, aEnd, new Scalar(0, 255, 255), 3, 8, 0, 0.2);
                // Label 'a' hanya satu, digeser lebih jauh dari panah
                Imgproc.putText(rotatedOutput, "a",
                    new Point(aEnd.x - 40, aEnd.y - 20),
                    Core.FONT_HERSHEY_SIMPLEX, 1.2, new Scalar(255, 0, 0), 3);
            }
        }

        circles.release();
        blurred.release();
        input.release();
        rotatedInput.release();

        latestPreviewFrame = rotatedOutput;
        return rotatedOutput;
    }

    private Mat rotateImage(Mat source, double angle) {
        // Gunakan Point biasa, bukan Point2f
        Point center = new Point(source.cols() / 2.0, source.rows() / 2.0);
        Mat rotMatrix = Imgproc.getRotationMatrix2D(center, angle, 1.0);

        Mat rotated = new Mat();
        Imgproc.warpAffine(source, rotated, rotMatrix, source.size());

        rotMatrix.release();
        return rotated;
    }

    private void saveMatToFile(Mat mat, String filename) {
        Bitmap bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Bitmap displayBitmap = null;

        try {
            Utils.matToBitmap(mat, bitmap);

            photoFile = new File(requireContext().getExternalFilesDir(null), filename);
            try (FileOutputStream out = new FileOutputStream(photoFile)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out); // Quality ditingkatkan
                out.flush();

                Log.i(TAG, "Foto berhasil disimpan: " + photoFile.getAbsolutePath());

                // Buat salinan bitmap untuk ImageView
                displayBitmap = bitmap.copy(bitmap.getConfig(), false);

                // Tampilkan hasil di ImageView menggunakan salinan
                final Bitmap finalDisplayBitmap = displayBitmap;
                requireActivity().runOnUiThread(() -> {
                    if (finalDisplayBitmap != null && !finalDisplayBitmap.isRecycled()) {
                        // Cleanup bitmap sebelumnya jika ada
//                        cleanupCurrentBitmap();
//
//                        binding.cameraview.setVisibility(View.GONE);
//                        binding.capturedImageView.setVisibility(View.VISIBLE);
                        binding.captureButton.setVisibility(View.GONE);
//                        binding.capturedImageView.setBackground(new BitmapDrawable(getResources(), finalDisplayBitmap));

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
    private void cleanupCurrentBitmap() {
        if (currentDisplayBitmap != null && !currentDisplayBitmap.isRecycled()) {
            currentDisplayBitmap.recycle();
            currentDisplayBitmap = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanupCurrentBitmap();
    }
}
