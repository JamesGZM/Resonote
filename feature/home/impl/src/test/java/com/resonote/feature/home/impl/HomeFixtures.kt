package com.resonote.feature.home.impl

import androidx.compose.ui.graphics.Color

object HomeFixtures {
    private val redNight = listOf(Color(0xFF5A061B), Color(0xFFE31353), Color(0xFFFF8DA9))
    private val blueRoom = listOf(Color(0xFF042E48), Color(0xFF0879BC), Color(0xFFBBD9F4))
    private val violetLake = listOf(Color(0xFF20164B), Color(0xFF786EDB), Color(0xFFF4A9BC))
    private val amberCoast = listOf(Color(0xFF6F2E19), Color(0xFFE38A52), Color(0xFFFFD9A8))
    private val greenForest = listOf(Color(0xFF123D36), Color(0xFF3A8068), Color(0xFFC6D9A8))
    private val silverPeak = listOf(Color(0xFF31495D), Color(0xFF8BAABD), Color(0xFFEAF0F2))

    val songs = listOf(
        HomeSongUiModel("quiet-track", "静默轨道", "Resonote Ensemble", "4:12", "HI-RES", true, redNight),
        HomeSongUiModel("blue-room", "蓝色房间", "Lin & The Archive", "4:29", "HI-RES", true, blueRoom),
        HomeSongUiModel("years-song", "那些年我们一起听过的歌", "陈粒", "5:41", "LOSSLESS", true, violetLake),
        HomeSongUiModel("evening-signal", "晚风信号", "林澈 · 潮汐记忆", "4:08", "LOSSLESS", false, amberCoast),
        HomeSongUiModel("forest-letter", "写给森林的信", "北岸合唱团", "3:54", null, false, greenForest),
        HomeSongUiModel("snowline", "雪线以北", "远山计划", "4:36", "LOSSLESS", false, silverPeak),
    )

    private val playlists = listOf(
        HomePlaylistUiModel("midnight", "深夜独白：安静的陪伴", "148.7万", violetLake),
        HomePlaylistUiModel("delta", "有关三角洲的歌曲", "657.6万", greenForest),
        HomePlaylistUiModel("sunset", "海岸落日与晚风", "340.5万", amberCoast),
        HomePlaylistUiModel("mountain", "边听边打天才少年", "487.8万", silverPeak),
        HomePlaylistUiModel("city", "城市醒来之前", "88.6万", blueRoom),
        HomePlaylistUiModel("afterglow", "漫长余晖收藏集", "126.4万", redNight),
    )

    fun state() = HomeContentUiState(
        radio = songs[3],
        dailySongs = songs,
        recommendedPlaylists = playlists,
        newSongs = songs.reversed(),
    )
}
