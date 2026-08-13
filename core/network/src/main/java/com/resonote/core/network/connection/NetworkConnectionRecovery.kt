package com.resonote.core.network.connection

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient

/** Discards pooled sockets after Android's default network changes. */
@Singleton
class NetworkConnectionRecovery @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    private val connectionPools = ConnectionPoolInvalidator()
    private val started = AtomicBoolean(false)
    private val networkLock = Any()
    private var activeNetwork: Network? = null
    private var hasObservedNetwork = false

    fun register(client: OkHttpClient) {
        connectionPools.register(client.connectionPool)
    }

    fun start() {
        if (!started.compareAndSet(false, true)) return
        connectivityManager.registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val shouldEvict = synchronized(networkLock) {
                        val changed = hasObservedNetwork && activeNetwork != network
                        hasObservedNetwork = true
                        activeNetwork = network
                        changed
                    }
                    if (shouldEvict) connectionPools.evictAll()
                }

                override fun onLost(network: Network) {
                    val shouldEvict = synchronized(networkLock) {
                        if (activeNetwork != network) {
                            false
                        } else {
                            activeNetwork = null
                            true
                        }
                    }
                    if (shouldEvict) connectionPools.evictAll()
                }
            },
        )
    }
}

internal class ConnectionPoolInvalidator {
    private val connectionPools = CopyOnWriteArraySet<ConnectionPool>()

    fun register(connectionPool: ConnectionPool) {
        connectionPools += connectionPool
    }

    fun evictAll() {
        connectionPools.forEach(ConnectionPool::evictAll)
    }
}
