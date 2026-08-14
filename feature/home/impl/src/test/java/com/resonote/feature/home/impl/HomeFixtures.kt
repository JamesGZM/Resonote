package com.resonote.feature.home.impl

object HomeFixtures {
    val songs = listOf(
        HomeSongUiModel("quiet-track", "静默轨道", "Resonote Ensemble", "4:12", "HI-RES", true),
        HomeSongUiModel("blue-room", "蓝色房间", "Lin & The Archive", "4:29", "HI-RES", true),
        HomeSongUiModel("years-song", "那些年我们一起听过的歌", "陈粒", "5:41", "LOSSLESS", true),
        HomeSongUiModel("evening-signal", "晚风信号", "林澈 · 潮汐记忆", "4:08", "LOSSLESS", false),
        HomeSongUiModel("forest-letter", "写给森林的信", "北岸合唱团", "3:54"),
        HomeSongUiModel("snowline", "雪线以北", "远山计划", "4:36", "LOSSLESS", false),
    )

    private val playlists = listOf(
        HomePlaylistUiModel("midnight", "深夜独白：安静的陪伴", "148.7万"),
        HomePlaylistUiModel("delta", "有关三角洲的歌曲", "657.6万"),
        HomePlaylistUiModel("sunset", "海岸落日与晚风", "340.5万"),
        HomePlaylistUiModel("mountain", "边听边打天才少年", "487.8万"),
        HomePlaylistUiModel("city", "城市醒来之前", "88.6万"),
        HomePlaylistUiModel("afterglow", "漫长余晖收藏集", "126.4万"),
    )

    fun state() = HomeContentUiState(
        radio = songs[3],
        dailySongs = songs,
        recommendedPlaylists = playlists,
        newSongs = songs.reversed(),
    )
}
