package dev.maxxximgb.genesis.data.repository

import android.content.ContentResolver
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.paging.testing.asSnapshot
import dev.maxxximgb.genesis.data.media.MediaStoreSource
import dev.maxxximgb.genesis.domain.model.SortOrder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MediaLibraryRepositoryTest {

    private fun makeCursor(start: Int, count: Int): MatrixCursor {
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
            cursor.addRow(arrayOf<Any?>(id, "T$id", "A$id", "Al$id", id, 1000L, id))
        }
        return cursor
    }

    @Test
    fun pagedLibrarySnapshotEmitsAllItemsAcrossPages() = runTest {
        val resolver = mock<ContentResolver>()
        whenever(resolver.query(any<Uri>(), any(), any<Bundle>(), eq(null))).thenAnswer { invocation ->
            val args = invocation.getArgument<Bundle>(2)
            val limit = args.getInt(ContentResolver.QUERY_ARG_LIMIT)
            val offset = args.getInt(ContentResolver.QUERY_ARG_OFFSET)
            val available = (120 - offset).coerceAtLeast(0)
            makeCursor(offset, minOf(limit, available))
        }
        val repo = MediaLibraryRepositoryImpl(MediaStoreSource(resolver))

        val snapshot = repo.pagedLibrary(SortOrder.DATE_ADDED_DESC, "").flow.asSnapshot {
            scrollTo(index = 119)
        }

        assertEquals(120, snapshot.size)
        assertEquals(0L, snapshot.first().mediaStoreId)
        assertEquals(119L, snapshot.last().mediaStoreId)
    }

    @Test
    fun observeAlbumsAndObserveArtistsAreEmptyStubsInIter12() = runTest {
        val repo = MediaLibraryRepositoryImpl(MediaStoreSource(mock()))
        assertEquals(
            emptyList<dev.maxxximgb.genesis.domain.model.Album>(),
            repo.observeAlbums().first(),
        )
        assertEquals(
            emptyList<dev.maxxximgb.genesis.domain.model.Artist>(),
            repo.observeArtists().first(),
        )
    }

    @Test
    fun findByIdReturnsNullWhenResolverReturnsEmptyCursor() = runTest {
        val resolver = mock<ContentResolver>()
        whenever(resolver.query(any<Uri>(), any(), any<Bundle>(), eq(null))).thenReturn(makeCursor(0, 0))
        val repo = MediaLibraryRepositoryImpl(MediaStoreSource(resolver))
        assertNull(repo.findById(12345L))
    }
}
