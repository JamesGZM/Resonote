package com.resonote.core.network.model

enum class NetworkRecommendationMode(val cardId: Int) {
    Personal(1),
    Nostalgia(2),
    Popular(3),
    HiddenGems(4),
    Vip(6),
}
