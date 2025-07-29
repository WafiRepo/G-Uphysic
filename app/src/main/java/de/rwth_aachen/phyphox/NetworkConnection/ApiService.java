package de.rwth_aachen.phyphox.NetworkConnection;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PUT;
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

    @Headers("Content-Type: application/json")
    @POST("/overwrite-label/")
    Call<ApiResponse> overwriteLabel(@Body OverwriteLabelRequest request);

    @Multipart
    @POST("/process-image/")
    Call<ApiResponse> uploadImage(
            @Part MultipartBody.Part file,
            @Part("user_id") RequestBody userId
    );

    @GET("calculate-radius-auto")
    Call<RadiusResponse> calculateRadius(@Query("user_id") String userId);
    
    @PUT("replace-radius/")
    Call<RadiusResponse> replaceRadius(
            @Query("user_id") String userId,
            @Query("new_radius") float radius
    );
}

