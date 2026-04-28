package dev.maxxximgb.genesis.data.media

import android.content.ContentResolver
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.paging.PagingSource
import dev.maxxximgb.genesis.domain.model.SortOrder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MediaStorePagingSourceTest {

    private fun cursorOfRange(start: Int, count: Int): MatrixCursor {
        val cursor = MatrixCursor(
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATE_ADDED,
            )
        )
        repeat(count) { i ->
            val id = (start + i).toLong()
            cursor.addRow(
                arrayOf<Any?>(
                    id, "Track $id", "Artist $id", "Album $id", id, 1000L, id
                )
            )
        }
        return cursor
    }

    private fun mockResolverServingTotal(total: Int): ContentResolver {
        val resolver = mock<ContentResolver>()
        whenever(resolver.query(any<Uri>(), any(), any<Bundle>(), eq(null))).thenAnswer { invocation ->
            val args = invocation.getArgument<Bundle>(2)
            val limit = args.getInt(ContentResolver.QUERY_ARG_LIMIT)
            val offset = args.getInt(ContentResolver.QUERY_ARG_OFFSET)
            val available = (total - offset).coerceAtLeast(0)
            cursorOfRange(offset, minOf(limit, available))
        }
        return resolver
    }

    @Test
    fun loadsFirstPageThenSecondAndStopsAtEnd() = runTest {
        val resolver = mockResolverServingTotal(total = 75)
        val source = MediaStoreSource(resolver)
        val ps = MediaStorePagingSource(source, SortOrder.DATE_ADDED_DESC, "")

        val first = ps.load(
            PagingSource.LoadParams.Refresh(key = 0, loadSize = 50, placeholdersEnabled = false)
        ) as PagingSource.LoadResult.Page

        assertEquals(50, first.data.size)
        assertEquals(0L, first.data.first().mediaStoreId)
        assertEquals(49L, first.data.last().mediaStoreId)
        assertNull(first.prevKey)
        assertEquals(50, first.nextKey)

        val second = ps.load(
            PagingSource.LoadParams.Append(key = 50, loadSize = 50, placeholdersEnabled = false)
        ) as PagingSource.LoadResult.Page

        assertEquals(25, second.data.size)
        assertEquals(50L, second.data.first().mediaStoreId)
        assertEquals(74L, second.data.last().mediaStoreId)
        assertEquals(0, second.prevKey)
        assertNull(second.nextKey)
    }

    @Test
    fun mapsCursorRowToTrackIncludingNullableFields() = runTest {
        val resolver = mock<ContentResolver>()
        whenever(resolver.query(any<Uri>(), any(), any<Bundle>(), eq(null))).thenAnswer {
            val cursor = MatrixCursor(
                arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.ALBUM_ID,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.DATE_ADDED,
                )
            )
            cursor.addRow(arrayOf<Any?>(7L, "Title 7", null, null, null, 1234L, 99L))
            cursor
        }
        val source = MediaStoreSource(resolver)
        val ps = MediaStorePagingSource(source, SortOrder.DATE_ADDED_DESC, "")

        val page = ps.load(
            PagingSource.LoadParams.Refresh(key = 0, loadSize = 50, placeholdersEnabled = false)
        ) as PagingSource.LoadResult.Page

        assertEquals(1, page.data.size)
        val track = page.data[0]
        assertEquals(7L, track.mediaStoreId)
        assertEquals("Title 7", track.title)
        assertNull(track.artist)
        assertNull(track.album)
        assertNull(track.albumId)
        assertEquals(1234L, track.durationMs)
        assertEquals(99L, track.dateAdded)
        assertEquals(
            "${MediaStore.Audio.Media.EXTERNAL_CONTENT_URI}/7",
            track.contentUri,
        )
    }

    @Test
    fun forwardsSearchQueryAsLowercaseLikePattern() = runTest {
        val resolver = mock<ContentResolver>()
        whenever(resolver.query(any<Uri>(), any(), any<Bundle>(), eq(null))).thenReturn(
            cursorOfRange(0, 0)
        )
        val source = MediaStoreSource(resolver)
        val ps = MediaStorePagingSource(source, SortOrder.DATE_ADDED_DESC, "ABBA")

        ps.load(PagingSource.LoadParams.Refresh(key = 0, loadSize = 50, placeholdersEnabled = false))

        val captor = argumentCaptor<Bundle>()
        org.mockito.kotlin.verify(resolver).query(any<Uri>(), any(), captor.capture(), eq(null))
        val args = captor.firstValue
        val selectionArgs = args.getStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS)
        assertEquals(arrayListOf("%abba%", "%abba%"), selectionArgs?.toList())
    }
}
