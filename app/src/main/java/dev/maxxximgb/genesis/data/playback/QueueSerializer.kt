package dev.maxxximgb.genesis.data.playback

object QueueSerializer {

    fun serialize(ids: List<Long>): String = ids.joinToString(",")

    fun deserialize(csv: String): List<Long> {
        if (csv.isBlank()) return emptyList()
        return csv.split(",").mapNotNull { it.trim().toLongOrNull() }
    }
}
