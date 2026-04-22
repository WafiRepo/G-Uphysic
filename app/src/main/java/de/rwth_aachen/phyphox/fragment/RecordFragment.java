package de.rwth_aachen.phyphox.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.widget.EditText;

import de.rwth_aachen.phyphox.Helper.SessionManager;
import de.rwth_aachen.phyphox.R;
import de.rwth_aachen.phyphox.activity.FeedbackActivity;
import de.rwth_aachen.phyphox.databinding.FragmentRecordBinding;
import de.rwth_aachen.phyphox.NetworkConnection.ApiService;
import de.rwth_aachen.phyphox.NetworkConnection.RetrofitClient;
import de.rwth_aachen.phyphox.NetworkConnection.SaveExperimentLocationRequest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecordFragment extends Fragment {

    FragmentRecordBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentRecordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnStart.setOnClickListener(v -> showLocationDialog());
    }

    private void showLocationDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_experiment_location, null);
        EditText etLocation = dialogView.findViewById(R.id.et_experiment_location);

        dialogView.findViewById(R.id.chip_kelas).setOnClickListener(v -> etLocation.setText("Kelas"));
        dialogView.findViewById(R.id.chip_playground).setOnClickListener(v -> etLocation.setText("Playground"));
        dialogView.findViewById(R.id.chip_rumah).setOnClickListener(v -> etLocation.setText("Rumah"));
        dialogView.findViewById(R.id.chip_lab).setOnClickListener(v -> etLocation.setText("Lab"));

        new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .setPositiveButton("Lanjut ke Kamera \uD83D\uDCF7", (dialog, which) -> {
                    String location = etLocation.getText() != null ? etLocation.getText().toString().trim() : "";
                    if (location.isEmpty()) location = "Lokasi tidak diisi";
                    saveLocationAndOpenCamera(location);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void saveLocationAndOpenCamera(String location) {
        String userId = SessionManager.getId(requireContext());
        if (userId == null) userId = "default";

        SaveExperimentLocationRequest request = new SaveExperimentLocationRequest(userId, location);
        ApiService api = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        api.saveExperimentLocation(request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(requireContext(), "Lokasi tersimpan: " + location, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Lokasi gagal disimpan, lanjut ke kamera.", Toast.LENGTH_SHORT).show();
                    }
                    Intent intent = new Intent(getActivity(), FeedbackActivity.class);
                    intent.putExtra("experiment_location", location);
                    startActivity(intent);
                });
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Koneksi gagal. Coba lagi atau lanjut ke kamera.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getActivity(), FeedbackActivity.class);
                    intent.putExtra("experiment_location", "");
                    startActivity(intent);
                });
            }
        });
    }
}
