package de.rwth_aachen.phyphox.NetworkConnection;

import de.rwth_aachen.phyphox.model.JobResult;
import de.rwth_aachen.phyphox.model.JobStatus;
import de.rwth_aachen.phyphox.model.JobsListResponse;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @Multipart
    @POST("analyze")
    Call<SubmitResponse> submitJob(
            @Header("X-API-Key") String apiKey,
            @Part MultipartBody.Part video,
            @Part("sidecar") RequestBody sidecar
    );

    @GET("status/{jobId}")
    Call<JobStatus> getStatus(
            @Header("X-API-Key") String apiKey,
            @Path("jobId") String jobId
    );

    @GET("result/{jobId}")
    Call<JobResult> getResult(
            @Header("X-API-Key") String apiKey,
            @Path("jobId") String jobId
    );

    @DELETE("job/{jobId}")
    Call<DeleteResponse> deleteJob(
            @Header("X-API-Key") String apiKey,
            @Path("jobId") String jobId
    );

    @GET("jobs")
    Call<JobsListResponse> listJobs(
            @Header("X-API-Key") String apiKey
    );

    @GET("status/ping")
    Call<ResponseBody> ping(
            @Header("X-API-Key") String apiKey
    );

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

    @Multipart
    @POST("/process-image/")
    Call<ApiResponse> uploadImageForDevice(
            @Part MultipartBody.Part file,
            @Part("user_id") RequestBody userId,
            @Part("device_id") RequestBody deviceId
    );

    @GET("calculate-radius-auto")
    Call<RadiusResponse> calculateRadius(@Query("user_id") String userId);

    @GET("calculate-radius-auto")
    Call<RadiusResponse> calculateRadiusForDevice(
            @Query("user_id") String userId,
            @Query("device_id") int deviceId
    );
    
    @PUT("replace-radius/")
    Call<RadiusResponse> replaceRadius(
            @Query("user_id") String userId,
            @Query("new_radius") float radius
    );

    @Headers("Content-Type: application/json")
    @POST("/save-experiment-location/")
    Call<ResponseBody> saveExperimentLocation(@Body SaveExperimentLocationRequest request);

    @Headers("Content-Type: application/json")
    @POST("/inquiry/generate/problem-finding")
    Call<InquiryGenerateResponse> generateInquiryProblemFinding(@Body InquiryGenerateRequest request);

    @Headers("Content-Type: application/json")
    @POST("/inquiry/generate/problem-exploring-stage-1")
    Call<InquiryGenerateResponse> generateInquiryProblemExploring(@Body InquiryGenerateProblemExploringRequest request);

    @Headers("Content-Type: application/json")
    @POST("/inquiry/generate/problem-exploring-stage-1/submit-response")
    Call<SubmitResponseResponse> submitResponseProblemExploring(@Body SubmitResponseRequest request);

    @Headers("Content-Type: application/json")
    @POST("/inquiry/generate/problem-exploring-stage-2")
    Call<InquiryGenerateResponse> generateInquiryProblemExploringStage2(
            @Body InquiryGenerateProblemExploringStage2Request request);

    @Headers("Content-Type: application/json")
    @POST("/inquiry/generate/problem-exploring-stage-2/submit-response")
    Call<SubmitResponseResponse> submitResponseProblemExploringStage2(@Body SubmitResponseRequest request);

    @Headers("Content-Type: application/json")
    @POST("/validate-object-centripetal/")
    Call<ValidateObjectResponse> validateObjectCentripetal(@Body ValidateObjectRequest request);

    @DELETE("/delete-buffer-data/")
    Call<Void> deleteBufferData(@Query("user_id") String userId);

    @Headers("Content-Type: application/json")
    @POST("/log-photo-attempt/")
    Call<ResponseBody> logPhotoAttempt(@Body PhotoAttemptRequest request);
}

