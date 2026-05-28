package de.rwth_aachen.phyphox.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import de.rwth_aachen.phyphox.Helper.PiAnalysisSettings;
import de.rwth_aachen.phyphox.NetworkConnection.ApiService;
import de.rwth_aachen.phyphox.NetworkConnection.RetrofitClient;
import de.rwth_aachen.phyphox.R;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PiSettingsFragment extends Fragment {

    private TextInputEditText etServerUrl, etApiKey;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pi_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etServerUrl = view.findViewById(R.id.etServerUrl);
        etApiKey = view.findViewById(R.id.etApiKey);
        MaterialButton btnTestConnection = view.findViewById(R.id.btnTestConnection);
        MaterialButton btnSave = view.findViewById(R.id.btnSave);

        etServerUrl.setText(PiAnalysisSettings.getServerUrl(requireContext()));
        etApiKey.setText(PiAnalysisSettings.getApiKey(requireContext()));

        btnSave.setOnClickListener(v -> saveSettings());
        btnTestConnection.setOnClickListener(v -> testConnection());
    }

    private void saveSettings() {
        String url = etServerUrl.getText().toString();
        String key = etApiKey.getText().toString();

        PiAnalysisSettings.setServerUrl(requireContext(), url);
        PiAnalysisSettings.setApiKey(requireContext(), key);

        Toast.makeText(requireContext(), "Settings saved", Toast.LENGTH_SHORT).show();
    }

    private void testConnection() {
        String url = etServerUrl.getText().toString();
        String key = etApiKey.getText().toString();

        if (url.isEmpty()) {
            Toast.makeText(requireContext(), "Enter Server URL first", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService api = RetrofitClient.getRetrofitInstance(url).create(ApiService.class);
        api.ping(key).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Connection successful!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Connection failed: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
