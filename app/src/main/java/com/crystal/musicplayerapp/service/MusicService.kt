package com.crystal.musicplayerapp.service

import retrofit2.Call
import retrofit2.http.GET

interface MusicService {
    //https://run.mocky.io/v3/7ab5c925-5ec4-4a92-9cc9-9ef5e0edc50f
    @GET("/v3/7ab5c925-5ec4-4a92-9cc9-9ef5e0edc50f")
    fun listMusics() : Call<MusicDto>
}