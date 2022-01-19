package com.crystal.musicplayerapp

// 서버에서 내려받은 Entity 를 ui 에서 사용할 Model 로 mapping 해준다.
fun MusicEntity.mapper(id: Long): MusicModel{
    return MusicModel(
        id = id,
        streamUrl = this.streamUrl,
        artist = this.artist,
        coverUrl = this.coverUrl,
        track = this.track
    )
}