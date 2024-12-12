package com.example.customkeyboard

import retrofit2.Call
import retrofit2.http.GET

interface ApiService {
    // Update APK
    @GET("V4/Others/Kurt/LatestVersionAPK/CustomKeyboard/output-metadata.json")
    fun getAppUpdateDetails(): Call<AppUpdateResponse>

}