package com.dynamixwebdesign.relisten.car

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.dynamixwebdesign.relisten.car.api.RelistenApi
import com.dynamixwebdesign.relisten.car.api.RelistenRepository
import com.dynamixwebdesign.relisten.car.resumption.QueueStateStore
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

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
        // API calls fail fast (streaming keeps the default, patient client above).
        val apiClient = okHttp.newBuilder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()
        repository = RelistenRepository(RelistenApi(apiClient, ::hasValidatedNetwork))
        queueStateStore = QueueStateStore(this)
    }

    private fun hasValidatedNetwork(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
