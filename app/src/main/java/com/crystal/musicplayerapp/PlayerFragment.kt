package com.crystal.musicplayerapp

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.crystal.musicplayerapp.databinding.FragmentPlayerBinding
import com.crystal.musicplayerapp.service.MusicDto
import com.crystal.musicplayerapp.service.MusicService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PlayerFragment : Fragment(R.layout.fragment_player) {
    private var binding: FragmentPlayerBinding? = null
    private lateinit var playListAdapter: PlayListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fragmentPlayerBinding = FragmentPlayerBinding.bind(view)
        binding = fragmentPlayerBinding

        initPlayListButton(fragmentPlayerBinding)
        initPlayListRecyclerView()
        getVideoListFromServer()
    }


    var isWatchingPlayListView: Boolean = true
    private fun initPlayListButton(fragmentPlayerBinding: FragmentPlayerBinding) {
        fragmentPlayerBinding.playlistImageView.setOnClickListener {
            //todo 예외처리 서버에서 데이터가 다 불러오지 못한 경우, 전환하지 않고 예외처리 필요
            fragmentPlayerBinding.playerViewGroup.isVisible = isWatchingPlayListView
            fragmentPlayerBinding.playListGroup.isVisible = isWatchingPlayListView.not()
            isWatchingPlayListView = isWatchingPlayListView.not()
        }

    }

    private fun initPlayListRecyclerView() {
        playListAdapter = PlayListAdapter { model ->
            model.isPlaying
            model.streamUrl
            //todo 음악을 재생
        }
        binding?.let { binding ->
            binding.playListRecyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = playListAdapter
            }
        }
    }

    private fun getVideoListFromServer() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://run.mocky.io")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(MusicService::class.java).also { musicService ->
            musicService.listMusics().enqueue(object : Callback<MusicDto> {
                override fun onResponse(call: Call<MusicDto>, response: Response<MusicDto>) {
                    Log.d("PlayerFragment", "${response.body()}")

                    response.body()?.let { musicDto ->
                        val modelList = musicDto.musics.mapIndexed { index, musicEntity ->
                            musicEntity.mapper(index.toLong())
                        }
                        playListAdapter.submitList(modelList)
                    }

                }

                override fun onFailure(call: Call<MusicDto>, t: Throwable) {
                    Log.e("PlayerFragment", t.message.toString())
                }

            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

    companion object {
        fun newInstance(): PlayerFragment {
            return PlayerFragment()
        }
    }
}
