package com.dynamixwebdesign.relisten.car

import android.app.Application
import com.dynamixwebdesign.relisten.car.api.RelistenApi
import com.dynamixwebdesign.relisten.car.api.RelistenRepository
import com.dynamixwebdesign.relisten.car.resumption.QueueStateStore
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File

class RelistenCarApp : Application() {
    lateinit var okHttp: OkHttpClient
        private set
    lateinit var repository: RelistenRepository
        private set
    lateinit var queueStateStore: QueueStateStore
        private set

    override fun onCreate() {
        super.onCreate()
        val userAgent =
            "relisten-car/${BuildConfig.VERSION_NAME} (unofficial fork; eking@dynamixwebdesign.com)"
        okHttp = OkHttpClient.Builder()
            .cache(Cache(File(cacheDir, "http"), 50L * 1024 * 1024))
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", userAgent)
                        .build()
                )
            }
            .addNetworkInterceptor { chain ->
                // The API is inconsistent with cache headers; a short client-side max-age makes
                // browsing snappy and guarantees the disk cache has entries for offline fallback.
                val response = chain.proceed(chain.request())
                if (chain.request().method == "GET" &&
                    chain.request().url.host == "api.relisten.net"
                ) {
                    response.newBuilder()
                        .removeHeader("Pragma")
                        .header("Cache-Control", "public, max-age=300")
                        .build()
                } else {
                    response
                }
            }
            .build()
        repository = RelistenRepository(RelistenApi(okHttp))
        queueStateStore = QueueStateStore(this)
    }
}
