package com.mozhimen.iok.scoped.helpers

import com.mozhimen.cachek.datastore.CacheKDS
import com.mozhimen.cachek.datastore.helpers.CacheKDSProvider
import com.mozhimen.cachek.datastore.temps.CacheKDSVarPropertyBoolean

/**
 * @ClassName ScopedCacheHelper
 * @Description TODO
 * @Author Mozhimen / Kolin Zhao
 * @Date 2023/12/10 23:55
 * @Version 1.0
 */
object ScopedCache {
    private val _cacheKDSProviderScoped: CacheKDSProvider by lazy { CacheKDS.instance.with("manifestk_permission_scoped") }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    var hasPermissionAndroidData: Boolean by CacheKDSVarPropertyBoolean(_cacheKDSProviderScoped,"hasPermissionAndroidData")
    var hasPermissionAndroidObb: Boolean by CacheKDSVarPropertyBoolean(_cacheKDSProviderScoped,"hasPermissionAndroidObb")
}