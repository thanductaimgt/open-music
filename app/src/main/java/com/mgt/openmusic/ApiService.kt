package com.mgt.openmusic

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    @POST("api/search.php?callback=a")
    @FormUrlEncoded
    fun search(@FieldMap params: Map<String, String>): Call<String>

    @GET
    fun downloadAudio(@Url url:String):Call<ResponseBody>
}