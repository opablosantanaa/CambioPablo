package com.opablosantanaa.cambiopablo.api;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiService {
    @GET("json/last/{pairs}")
    Call<Map<String, Currency>> getExchangeRate(@Path("pairs") String pairs);
}
