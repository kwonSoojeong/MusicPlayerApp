package com.crystal.musicplayerapp

import com.crystal.musicplayerapp.service.MusicDto

// 서버에서 내려받은 Entity 를 ui 에서 사용할 Model 로 mapping 해준다.
fun MusicEntity.mapper(id: Long): MusicModel {
    return MusicModel(
        id = id,
        streamUrl = this.streamUrl,
        artist = this.artist,
        coverUrl = this.coverUrl,
        track = this.track
    )
}

//Dto 의 정보로 Player Model 를 만든다
fun MusicDto.mapper(): PlayerModel =
    PlayerModel(
        playMusicList = this.musics.mapIndexed { index, musicEntity ->
            musicEntity.mapper(index.toLong())
        })