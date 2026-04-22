package de.rwth_aachen.phyphox.NetworkConnection;

import de.rwth_aachen.phyphox.BuildConfig;

import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit;
    /** Fallback jika BuildConfig kosong — sama dengan default di build.gradle */
    private static final String DEFAULT_API_BASE_URL = "http://140.115.126.90:8000/";

    private static String resolveBaseUrl() {
        String url = BuildConfig.LOCAL_API_BASE_URL;
        if (url == null || url.trim().isEmpty()) {
            return DEFAULT_API_BASE_URL;
        }
        return url.endsWith("/") ? url : (url + "/");
    }

    /**
     * Menggabungkan base URL API dengan path relatif (mis. image_path dari upload) agar bisa dipakai sebagai src gambar di admin.
     */
    @Nullable
    public static String resolveMediaUrl(@Nullable String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.trim().isEmpty()) {
            return null;
        }
        String p = pathOrUrl.trim();
        if (p.startsWith("http://") || p.startsWith("https://")) {
            return p;
        }
        String base = getRetrofitInstance().baseUrl().toString();
        if (p.startsWith("/")) {
            p = p.substring(1);
        }
        if (base.endsWith("/")) {
            return base + p;
        }
        return base + "/" + p;
    }

    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            final String baseUrl = resolveBaseUrl();
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);  // Log full request & response

            // Create OkHttp client with logging
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)  // Add logging
                    .connectTimeout(90, TimeUnit.SECONDS)  // Set timeout
                    .readTimeout(90, TimeUnit.SECONDS)
                    .writeTimeout(90, TimeUnit.SECONDS)
                    .build();

            // Build Retrofit instance
            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)  // Use custom OkHttp client
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
