package com.ilaydas.therabotmobile;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import com.google.gson.JsonObject;

public interface ApiService {
    @POST("/chat") // Flask API'nın endpoint'i
    Call<JsonObject> sendMessage(@Body JsonObject message);
}
