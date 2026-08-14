package com.resonote.core.network.api

/** Retrofit aggregation root. Endpoint declarations live in cohesive protocol interfaces. */
internal interface MusicApi :
    ContentApi,
    PlaybackApi,
    SearchApi,
    LyricsApi,
    VideoApi,
    RecognitionApi,
    AccountApi
