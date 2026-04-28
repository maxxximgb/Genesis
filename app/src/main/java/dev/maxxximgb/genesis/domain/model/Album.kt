package dev.maxxximgb.genesis.domain.model

data class Album(
    val id: Long,
    val name: String,
    val artist: String?,
    val trackCount: Int,
    val albumArtUri: String?,
)
