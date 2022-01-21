package com.crystal.musicplayerapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
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
import java.util.concurrent.TimeUnit

class PlayerFragment : Fragment(R.layout.fragment_player) {

    private var playerModel: PlayerModel = PlayerModel()
    private var binding: FragmentPlayerBinding? = null
    private lateinit var playListAdapter: PlayListAdapter
    private lateinit var player: ExoPlayer
    private val updateSeekRunnable = Runnable {
        updateSeek()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fragmentPlayerBinding = FragmentPlayerBinding.bind(view)
        binding = fragmentPlayerBinding

        initPlayerView(fragmentPlayerBinding)
        initPlayListButton(fragmentPlayerBinding)
        initPlayControlButton(fragmentPlayerBinding)
        initSeekBar(fragmentPlayerBinding)
        initPlayListRecyclerView()
        getVideoListFromServer()
    }


    private fun initSeekBar(fragmentPlayerBinding: FragmentPlayerBinding) {
        fragmentPlayerBinding.playerSeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar != null) {
                    player.seekTo(seekBar.progress * 1000L)
                }
            }
        })

        fragmentPlayerBinding.playlistSeekBar.setOnTouchListener { _, _ ->
            false
        }
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

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                //플레이어의 상태 변경될 때
                updateSeek()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                //음악이 변경될 때
                val newIndex = mediaItem?.mediaId ?: return
                playerModel.currentPosition = newIndex.toInt()
                updatePlayerView(playerModel.currentMusicModel())
                playListAdapter.submitList(playerModel.getAdapterMusicList())
            }
        })
    }

    private fun updateSeek() {
        val duration = if (player.duration >= 0) player.duration else 0
        val position = player.currentPosition
        updateSeekBarUI(duration, position)

        val state = player.playbackState
        //재생 중일 때
        view?.removeCallbacks(updateSeekRunnable)
        if (state != Player.STATE_IDLE && state != Player.STATE_ENDED) {
            view?.postDelayed(updateSeekRunnable, 1000)
        }

    }

    private fun updateSeekBarUI(duration: Long, position: Long) {
        binding?.let {
            it.playlistSeekBar.max = (duration / 1000).toInt()
            it.playlistSeekBar.progress = (position / 1000).toInt()

            it.playerSeekBar.max = (duration / 1000).toInt()
            it.playerSeekBar.progress = (position / 1000).toInt()

            it.playTimeTextView.text = String.format(
                "%02d:%02d",
                TimeUnit.MINUTES.convert(position, TimeUnit.MILLISECONDS),
                TimeUnit.SECONDS.convert(position, TimeUnit.MILLISECONDS) % 60
            )

            it.totalTimeTextView.text = String.format(
                "%02d:%02d",
                TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS),
                TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS) % 60
            )
        }
    }

    private fun updatePlayerView(currentMusicModel: MusicModel?) {
        currentMusicModel ?: return
        binding?.let {
            it.trackTextView.text = currentMusicModel.track
            it.nameTextView.text = currentMusicModel.artist
            it.coverImageView
            Glide.with(it.coverImageView)
                .load(currentMusicModel.coverUrl)
                .into(it.coverImageView)

        }
    }

    private fun initPlayControlButton(fragmentPlayerBinding: FragmentPlayerBinding) {
        fragmentPlayerBinding.playControlImageView.setOnClickListener {
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }

        fragmentPlayerBinding.skipNextImageView.setOnClickListener {
            val nextMusicModel = playerModel.nextMusic() ?: return@setOnClickListener
            playMusic(nextMusicModel)
        }
        fragmentPlayerBinding.skipPrevImageView.setOnClickListener {
            val prevMusicModel = playerModel.prevMusic() ?: return@setOnClickListener
            playMusic(prevMusicModel)
        }
    }


    private fun initPlayListButton(fragmentPlayerBinding: FragmentPlayerBinding) {
        fragmentPlayerBinding.playlistImageView.setOnClickListener {
            //예외처리 서버에서 데이터가 다 불러오지 못한 경우, 전환하지 않고 예외처리 필요,
            // 데이터 load 완료시 PlayerModel 이 생성되기 때문에 currentPosition 값을 확인해서 판단
            if (playerModel.currentPosition == -1) {
                return@setOnClickListener
            }
            fragmentPlayerBinding.playerViewGroup.isVisible = playerModel.isWatchingPlayListView
            fragmentPlayerBinding.playListGroup.isVisible = playerModel.isWatchingPlayListView.not()
            playerModel.isWatchingPlayListView = playerModel.isWatchingPlayListView.not()
        }

    }

    private fun initPlayListRecyclerView() {
        playListAdapter = PlayListAdapter {
            playMusic(it)
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
                        playerModel = musicDto.mapper()
                        setMusicList(playerModel.getAdapterMusicList())
                        playListAdapter.submitList(playerModel.getAdapterMusicList())
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
    }

    private fun playMusic(musicModel: MusicModel) {
        playerModel.updateCurrentPosition(musicModel)
        player.seekTo(playerModel.currentPosition, 0)
        player.play()
    }

    override fun onStop() {
        super.onStop()
        player.pause()
        view?.removeCallbacks(updateSeekRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
        player.release()
        view?.removeCallbacks(updateSeekRunnable)
    }

    companion object {
        fun newInstance(): PlayerFragment {
            return PlayerFragment()
        }
    }
}
