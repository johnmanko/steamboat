package com.johnmanko.app.steamboat.data.repo

import androidx.paging.PagingConfig

object PagingConfigs {
    val messages = PagingConfig(
        pageSize = 40,
        prefetchDistance = 20,
        initialLoadSize = 80,
        maxSize = 240,
        enablePlaceholders = false,
    )

    val lists = PagingConfig(
        pageSize = 30,
        prefetchDistance = 15,
        initialLoadSize = 60,
        enablePlaceholders = false,
    )
}
