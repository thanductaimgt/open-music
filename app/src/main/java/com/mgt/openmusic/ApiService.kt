package com.mgt.openmusic

import retrofit2.Call
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ApiService {
    @POST("api/search.php?callback=a")
    @FormUrlEncoded
    fun search(@FieldMap params: Map<String, String>): Call<String>
}