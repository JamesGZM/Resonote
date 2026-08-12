package com.resonote.core.data

import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.UserProfile

interface UserProfileRepository {
    suspend fun loadProfile(): CollectionLoadResult<UserProfile>
}
