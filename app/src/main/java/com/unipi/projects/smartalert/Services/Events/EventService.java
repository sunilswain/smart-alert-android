package com.unipi.projects.smartalert.Services.Events;

import com.unipi.projects.smartalert.Model.Events.EventUserStatisticsRequest;
import com.unipi.projects.smartalert.Model.Events.SendEventRequest;
import com.unipi.projects.smartalert.Services.Auth.AuthResult;
import com.unipi.projects.smartalert.Services.Auth.IAuthHttp;
import com.unipi.projects.smartalert.Services.RetrofitService;

import java.io.File;

import io.reactivex.rxjava3.core.Single;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class EventService implements IEventService{


    @Override
    public Single<EventResult> SendEvent(String type, String latitude, String longitude, String user_id) {
        RequestBody typeRequestBody = RequestBody.create(MediaType.parse("text/plain"), type);
        RequestBody latitudeRequestBody = RequestBody.create(MediaType.parse("text/plain"), latitude);
        RequestBody longitudeRequestBody = RequestBody.create(MediaType.parse("text/plain"), longitude);
        RequestBody userIdRequestBody = RequestBody.create(MediaType.parse("text/plain"), user_id);

        IEventHttp eventHttp = RetrofitService.retrofit.create(IEventHttp.class);
        Single<EventResult> eventCall = eventHttp.SendEvent(typeRequestBody, latitudeRequestBody, longitudeRequestBody, null, null, userIdRequestBody);

        return eventCall;
    }

    @Override
    public Single<EventResult> SendEvent(String type, String latitude, String longitude, String comment, String photoPath, String user_id) {
        RequestBody typeRequestBody = RequestBody.create(MediaType.parse("text/plain"), type);
        RequestBody latitudeRequestBody = RequestBody.create(MediaType.parse("text/plain"), latitude);
        RequestBody longitudeRequestBody = RequestBody.create(MediaType.parse("text/plain"), longitude);
        RequestBody commentRequestBody = RequestBody.create(MediaType.parse("text/plain"), comment);
        RequestBody userIdRequestBody = RequestBody.create(MediaType.parse("text/plain"), user_id);

        // Prepare the photo file part
        File photoFile = new File(photoPath);
        RequestBody photoRequestBody = RequestBody.create(MediaType.parse("image/jpeg"), photoFile);
        MultipartBody.Part photoPart = MultipartBody.Part.createFormData("photo", photoFile.getName(), photoRequestBody);

        IEventHttp eventHttp = RetrofitService.retrofit.create(IEventHttp.class);
        Single<EventResult> eventCall = eventHttp.SendEvent(typeRequestBody, latitudeRequestBody, longitudeRequestBody, commentRequestBody, photoPart, userIdRequestBody);

        return eventCall;
    }

    // END v0.2
    @Override
    public Single<EventResult> SendEvent(String type, String latitude, String longitude, String comment, String user_id) {
        RequestBody typeRequestBody = RequestBody.create(MediaType.parse("text/plain"), type);
        RequestBody latitudeRequestBody = RequestBody.create(MediaType.parse("text/plain"), latitude);
        RequestBody longitudeRequestBody = RequestBody.create(MediaType.parse("text/plain"), longitude);
        RequestBody commentRequestBody = RequestBody.create(MediaType.parse("text/plain"), comment);
        RequestBody userIdRequestBody = RequestBody.create(MediaType.parse("text/plain"), user_id);

        IEventHttp eventHttp = RetrofitService.retrofit.create(IEventHttp.class);
        Single<EventResult> eventCall = eventHttp.SendEvent(typeRequestBody, latitudeRequestBody, longitudeRequestBody, null, null, userIdRequestBody);

        return eventCall;
    }

    @Override
    public Single<EventUserStatisticsResult> GetEventUserStatistics(String id) {
        EventUserStatisticsRequest request = new EventUserStatisticsRequest();

        request.setId(id);

        IEventHttp eventHttp = RetrofitService.retrofit.create(IEventHttp.class);

        Single<EventUserStatisticsResult> eventCall = eventHttp.GetUserStatistics(request.getId());

        return eventCall;
    }
}
