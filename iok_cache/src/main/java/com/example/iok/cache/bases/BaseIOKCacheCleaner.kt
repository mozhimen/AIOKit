package com.example.iok.cache.bases

import androidx.annotation.WorkerThread
import com.mozhimen.basick.elemk.commons.IABC_Listener
import com.mozhimen.basick.elemk.commons.IA_Listener
import com.mozhimen.basick.elemk.commons.ISuspendA_Listener
import com.mozhimen.basick.utilk.android.text.formatFileSize
import com.mozhimen.basick.utilk.android.util.UtilKLogWrapper
import com.mozhimen.basick.utilk.commons.IUtilK
import com.mozhimen.basick.utilk.java.io.UtilKFileDir
import com.mozhimen.basick.utilk.java.io.UtilKFileWrapper
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
abstract class BaseIOKCacheCleaner : IUtilK {

    //    private val MIN_CACHE_LIMIT = 64L.megaBytes()
    abstract val MIN_CACHE_LIMIT: Long

    //    private val MAX_CACHE_LIMIT = 10L.gigaBytes()
    abstract val MAX_CACHE_LIMIT: Long

    //////////////////////////////////////////////////////////////////////////////////

    fun getSupportedCacheLimits(): List<Long> =
        generateSequence(MIN_CACHE_LIMIT) { it * 2L }
            .takeWhile { it <= MAX_CACHE_LIMIT }
            .toList()

    fun getClosestCacheLimit(size: Long): Long =
        getSupportedCacheLimits().minByOrNull { abs(it - size) } ?: 0

    fun getDefaultCacheLimit(): Long {
        val defaultCacheSize = (UtilKStorage.getInternalMemorySize_ofTotal() * 0.01f).roundToLong()
        return getClosestCacheLimit(defaultCacheSize)
    }

    fun getCacheSize(file: List<File>): Long {
        val cacheFiles = UtilKFileWrapper.getFolderFiles_ofAllSorted(file)
        return UtilKFileWrapper.getFilesSize_ofTotal(cacheFiles)
    }

    //////////////////////////////////////////////////////////////////////////////////

    @WorkerThread
    suspend fun clean_ofCache() {
        withContext(Dispatchers.IO) {
            UtilKLogWrapper.i(TAG, "Running cache cleanup everything task")
            UtilKFileDir.Internal.getCache().listFiles()?.forEach {
                it.deleteRecursively()
            }
        }
    }

    @WorkerThread
    suspend fun clean(requestedLimit: Long, files: List<File>, onDelete: ISuspendA_Listener<File>? = null) {
        withContext(Dispatchers.IO) {
            UtilKLogWrapper.i(TAG, "Running cache cleanup lru task")
            val cacheLimit = if (requestedLimit != 0L) getClosestCacheLimit(requestedLimit) else 0L
            val cacheFiles = UtilKFileWrapper.getFolderFiles_ofAllSorted(files).toMutableList()
            val cacheSize = UtilKFileWrapper.getFilesSize_ofTotal(cacheFiles)
            UtilKLogWrapper.i(TAG,"Space used by cache: ${cacheSize.formatFileSize()} / ${cacheLimit.formatFileSize()}")

            var spaceToBeDeleted = maxOf(cacheSize - cacheLimit, 0)

            UtilKLogWrapper.i(TAG, "Freeing cache space: ${spaceToBeDeleted.formatFileSize()}")

            while (spaceToBeDeleted > 0) {
                if (cacheFiles.size <= 0) break
                val deletedFile = cacheFiles.removeAt(0)
                onDelete?.invoke(deletedFile)
                val size = deletedFile.length()

                if (deletedFile.delete()) {
                    spaceToBeDeleted -= size
                    UtilKLogWrapper.i(TAG, "Cache file deleted ${deletedFile.name}, size: ${size.formatFileSize()}")
                }
            }
        }
    }
}
