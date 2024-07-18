package com.example.iok.cache

import androidx.annotation.WorkerThread
import com.mozhimen.basick.utilk.android.text.formatFileSize
import com.mozhimen.basick.utilk.android.util.UtilKLogWrapper
import com.mozhimen.basick.utilk.java.io.UtilKFileDir
import com.mozhimen.basick.utilk.java.io.UtilKFileWrapper
import com.mozhimen.basick.utilk.kotlin.gigaBytes
import com.mozhimen.basick.utilk.kotlin.megaBytes
import com.mozhimen.basick.utilk.wrapper.UtilKStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * @ClassName CacheCleaner
 * @Description TODO
 * @Author Mozhimen & Kolin Zhao
 * @Date 2024/5/11
 * @Version 1.0
 */
object CacheCleaner {

    private val MIN_CACHE_LIMIT = 64L.megaBytes()
    private val MAX_CACHE_LIMIT = 10L.gigaBytes()

    //////////////////////////////////////////////////////////////////////////////////

    @JvmStatic
    fun getSupportedCacheLimits(): List<Long> =
        generateSequence(MIN_CACHE_LIMIT) { it * 2L }
            .takeWhile { it <= MAX_CACHE_LIMIT }
            .toList()

    @JvmStatic
    fun getClosestCacheLimit(size: Long): Long =
        getSupportedCacheLimits().minByOrNull { abs(it - size) } ?: 0

    @JvmStatic
    fun getDefaultCacheLimit(): Long {
        val defaultCacheSize = (UtilKStorage.getInternalMemorySize_ofTotal() * 0.01f).roundToLong()
        return getClosestCacheLimit(defaultCacheSize)
    }

    //////////////////////////////////////////////////////////////////////////////////

    @WorkerThread
    suspend fun cleanCache() {
        withContext(Dispatchers.IO) {
            UtilKLogWrapper.i("Running cache cleanup everything task")
            UtilKFileDir.Internal.getCache().listFiles()?.forEach {
                it.deleteRecursively()
            }
        }
    }

    @WorkerThread
    suspend fun clean(requestedLimit: Long, vararg file: File) {
        withContext(Dispatchers.IO) {
            UtilKLogWrapper.i("Running cache cleanup lru task")
            val cacheLimit = getClosestCacheLimit(requestedLimit)
            val cacheFiles = UtilKFileWrapper.getFolderFiles_ofAllSorted(*file).toMutableList()
            val cacheSize = UtilKFileWrapper.getFilesSize_ofTotal(cacheFiles)
            UtilKLogWrapper.i("Space used by cache: ${cacheSize.formatFileSize()} / ${cacheLimit.formatFileSize()}")

            var spaceToBeDeleted = maxOf(cacheSize - cacheLimit, 0)

            UtilKLogWrapper.i("Freeing cache space: ${spaceToBeDeleted.formatFileSize()}")

            while (spaceToBeDeleted > 0) {
                val deletedFile = cacheFiles.removeAt(0)
                val size = deletedFile.length()

                if (deletedFile.delete()) {
                    spaceToBeDeleted -= size
                    UtilKLogWrapper.i("Cache file deleted ${deletedFile.name}, size: ${size.formatFileSize()}")
                }
            }
        }
    }
}
