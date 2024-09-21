package com.unipi.projects.smartalert.Services.Events;

import com.unipi.projects.smartalert.Services.Auth.AuthResult;

import io.reactivex.rxjava3.core.Single;

public interface IEventService {

    public Single<EventResult> SendEvent(String type, String latitude, String longitude, String user_id);

    public Single<EventResult> SendEvent(String type, String latitude, String longitude, String comment, String user_id);

    // START v0.2
    public Single<EventResult> SendEvent(String type, String latitude, String longitude, String comment, String photo, String user_id);
    // END v0.2
    public Single<EventUserStatisticsResult> GetEventUserStatistics(String id);
}
