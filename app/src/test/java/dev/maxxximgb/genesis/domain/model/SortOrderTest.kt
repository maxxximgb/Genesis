package dev.maxxximgb.genesis.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SortOrderTest {

    @Test
    fun mapsEachEnumValueToExpectedSql() {
        assertEquals("date_added DESC", SortOrder.DATE_ADDED_DESC.toMediaStoreSql())
        assertEquals("title COLLATE NOCASE ASC", SortOrder.TITLE_ASC.toMediaStoreSql())
        assertEquals("title COLLATE NOCASE DESC", SortOrder.TITLE_DESC.toMediaStoreSql())
        assertEquals("artist COLLATE NOCASE ASC", SortOrder.ARTIST_ASC.toMediaStoreSql())
        assertEquals("album COLLATE NOCASE ASC", SortOrder.ALBUM_ASC.toMediaStoreSql())
        assertEquals("duration ASC", SortOrder.DURATION_ASC.toMediaStoreSql())
        assertEquals("duration DESC", SortOrder.DURATION_DESC.toMediaStoreSql())
    }
}
