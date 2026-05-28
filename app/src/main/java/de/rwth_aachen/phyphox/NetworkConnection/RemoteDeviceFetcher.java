package de.rwth_aachen.phyphox.NetworkConnection;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Mengambil data sensor dari Phyphox Remote Server (port 8080) pada device kedua
 * yang berada di jaringan WiFi yang sama, lalu meneruskannya ke FastAPI backend
 * dengan device_id = 2.
 *
 * Contoh penggunaan:
 *   RemoteDeviceFetcher.fetch("192.168.1.5", userId, new RemoteDeviceFetcher.Callback() {
 *       public void onSuccess() { ... }
 *       public void onError(String message) { ... }
 *   });
 */
public class RemoteDeviceFetcher {

    private static final String TAG = "RemoteDeviceFetcher";
    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int READ_TIMEOUT_MS    = 5000;

    public interface Callback {
        void onSuccess();
        void onError(String message);
    }

    public static void fetch(String deviceIp, String userId, Callback callback) {
        new FetchTask(deviceIp, userId, callback).execute();
    }

    @SuppressWarnings("deprecation")
    private static class FetchTask extends AsyncTask<Void, Void, BufferData> {

        private final String deviceIp;
        private final String userId;
        private final Callback callback;
        private String errorMessage = null;

        FetchTask(String deviceIp, String userId, Callback callback) {
            this.deviceIp  = deviceIp;
            this.userId    = userId;
            this.callback  = callback;
        }

        @Override
        protected BufferData doInBackground(Void... voids) {
            try {
                String urlStr = "http://" + deviceIp.trim() + ":8080/get?acc=full&gyr=full&t=full";
                Log.d(TAG, "Fetching from: " + urlStr);

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(READ_TIMEOUT_MS);
                conn.setRequestMethod("GET");
                conn.connect();

                int code = conn.getResponseCode();
                if (code < 200 || code >= 300) {
                    errorMessage = "Device 2 mengembalikan HTTP " + code;
                    return null;
                }

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "utf-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                conn.disconnect();

                String rawJson = sb.toString();
                Log.d(TAG, "Raw response (first 500): " +
                        (rawJson.length() > 500 ? rawJson.substring(0, 500) : rawJson));
                return parsePhyphoxResponse(rawJson);

            } catch (Exception e) {
                errorMessage = "Gagal terhubung ke Device 2: " + e.getMessage();
                Log.e(TAG, errorMessage, e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(BufferData bufferData) {
            if (bufferData == null) {
                if (callback != null)
                    callback.onError(errorMessage != null ? errorMessage : "Respon tidak valid dari Device 2");
                return;
            }

            ApiService api = RetrofitClient.getRetrofitInstance().create(ApiService.class);
            api.addBufferData(bufferData).enqueue(new retrofit2.Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Data Device 2 berhasil dikirim ke server");
                        if (callback != null) callback.onSuccess();
                    } else {
                        String msg = "Server menolak data Device 2 (HTTP " + response.code() + ")";
                        Log.e(TAG, msg);
                        if (callback != null) callback.onError(msg);
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    String msg = "Gagal mengirim data Device 2: " + t.getMessage();
                    Log.e(TAG, msg);
                    if (callback != null) callback.onError(msg);
                }
            });
        }

        /**
         * Parse format Phyphox Remote Server:
         * {"buffer": {"acc": {"buffer": [...]}, "gyr": {"buffer": [...]}, "t": {"buffer": [...]}}}
         */
        private BufferData parsePhyphoxResponse(String json) throws Exception {
            JSONObject root    = new JSONObject(json);
            JSONObject buffers = root.getJSONObject("buffer");

            List<Double> acc = parseBuffer(buffers, "acc");
            List<Double> gyr = parseBuffer(buffers, "gyr");
            List<Double> t   = parseBuffer(buffers, "t");

            // gyr_squared tidak ada di Phyphox remote — hitung dari gyr
            List<Double> gyrSquared = new ArrayList<>(gyr.size());
            for (double v : gyr) gyrSquared.add(v * v);

            if (acc.isEmpty() || gyr.isEmpty() || t.isEmpty()) {
                errorMessage = "Buffer Device 2 kosong — pastikan eksperimen sedang berjalan";
                return null;
            }

            Log.d(TAG, "Parsed Device 2: acc=" + acc.size() + " gyr=" + gyr.size() + " t=" + t.size());
            return new BufferData(acc, gyr, gyrSquared, t, userId, 2);
        }

        private List<Double> parseBuffer(JSONObject buffers, String name) {
            List<Double> result = new ArrayList<>();
            try {
                if (!buffers.has(name)) return result;
                JSONArray arr = buffers.getJSONObject(name).getJSONArray("buffer");
                for (int i = 0; i < arr.length(); i++) {
                    result.add(arr.isNull(i) ? 0.0 : arr.getDouble(i));
                }
            } catch (Exception e) {
                Log.w(TAG, "Gagal parse buffer '" + name + "': " + e.getMessage());
            }
            return result;
        }
    }
}
