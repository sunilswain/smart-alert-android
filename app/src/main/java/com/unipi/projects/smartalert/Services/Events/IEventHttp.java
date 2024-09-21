package com.unipi.projects.smartalert.Services.Events;

import com.unipi.projects.smartalert.Model.Events.SendEventRequest;

import io.reactivex.rxjava3.core.Single;
import okhttp3.RequestBody;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import okhttp3.MultipartBody;
import retrofit2.http.Multipart;
import retrofit2.http.Part;

public interface IEventHttp {
    @Multipart
    @POST("/events/create")
    Single<EventResult> SendEvent(
            @Part("type") RequestBody type,
            @Part("latitude") RequestBody latitude,
            @Part("longitude") RequestBody longitude,
            @Part("comment") RequestBody comment,
            @Part MultipartBody.Part photo,
            @Part("user_id") RequestBody userId
    );

    @GET("/events/stats")
    Single<EventUserStatisticsResult> GetUserStatistics(@Query("id") String id);
}
