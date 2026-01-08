package de.rwth_aachen.phyphox.NetworkConnection;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit;
    // Local development: Use 10.0.2.2 for Android emulator (maps to localhost)
    // For physical device on same network, use your computer's IP (e.g., 192.168.1.4)
    private static final String BASE_URL = "http://10.0.2.2:8000"; // Emulator: localhost
    // private static final String BASE_URL = "http://192.168.1.4:8000"; // Physical device: uncomment and use this

    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
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
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)  // Use custom OkHttp client
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
