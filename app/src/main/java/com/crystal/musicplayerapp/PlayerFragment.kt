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
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PlayerFragment : Fragment(R.layout.fragment_player) {
    private var binding: FragmentPlayerBinding? = null
    private lateinit var playListAdapter: PlayListAdapter
    private lateinit var player: ExoPlayer

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fragmentPlayerBinding = FragmentPlayerBinding.bind(view)
        binding = fragmentPlayerBinding

        initPlayerView(fragmentPlayerBinding)
        initPlayListButton(fragmentPlayerBinding)
        initPlayControlButton(fragmentPlayerBinding)
        initPlayListRecyclerView()
        getVideoListFromServer()
    }

    private fun initPlayerView(fragmentPlayerBinding: FragmentPlayerBinding) {
        context?.let {
            player = ExoPlayer.Builder(it).build()
        }
        fragmentPlayerBinding.playerView.player = player
        player.addListener(object : Player.EventListener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                if (isPlaying) {
                    fragmentPlayerBinding.playControlImageView.setImageResource(R.drawable.pause_48)
                } else {
                    fragmentPlayerBinding.playControlImageView.setImageResource(R.drawable.play_arrow_48)
                }
            }
        })
    }

    private fun initPlayControlButton(fragmentPlayerBinding: FragmentPlayerBinding) {
        fragmentPlayerBinding.playControlImageView.setOnClickListener {
            val player = this.player ?: return@setOnClickListener
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
        fragmentPlayerBinding.skipNextImageView.setOnClickListener {

        }
        fragmentPlayerBinding.skipPrevImageView.setOnClickListener {

        }
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
                        setMusicList(modelList)
                        playListAdapter.submitList(modelList)
                    }

                }
                override fun onFailure(call: Call<MusicDto>, t: Throwable) {
                    Log.e("PlayerFragment", t.message.toString())
                }
            })
        }
    }

    private fun setMusicList(modelList: List<MusicModel>) {
        player.addMediaItems(modelList.map { model ->
            MediaItem.Builder()
                .setMediaId(model.id.toString())
                .setUri(model.streamUrl)
                .build()
        })
        player.prepare()
        player.play()
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
