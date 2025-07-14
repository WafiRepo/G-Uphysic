package de.rwth_aachen.phyphox.NetworkConnection;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface ApiService {
    @POST("/add_buffer_data/")
    Call<Void> addBufferData(@Body BufferData bufferData);

    @Headers("Content-Type: application/json")
    @POST("/intermediate-question")  // Endpoint
    Call<ApiResponse> getIntermediateQuestion(@Body ApiRequest request);

    @Headers("Content-Type: application/json")
    @POST("/easy-question")  // Endpoint
    Call<ApiResponse> getEasyQuestion(@Body ApiRequest request);

    @Headers("Content-Type: application/json")
    @POST("/advanced-question")  // Endpoint
    Call<ApiResponse> getAdvancedQuestion(@Body ApiRequest request);

    @Multipart
    @POST("/process-image/")
    Call<ApiResponse> uploadImage(
            @Part MultipartBody.Part file,
            @Part("user_id") RequestBody userId
    );

    @GET("calculate-radius-auto")
    Call<RadiusResponse> calculateRadius(@Query("user_id") String userId);
}

