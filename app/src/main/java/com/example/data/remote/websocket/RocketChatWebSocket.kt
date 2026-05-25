package com.example.data.remote.websocket

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject

enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING
}

class RocketChatWebSocket(
    private val client: OkHttpClient
) {
    private val TAG = "RocketChatWS"
    private var webSocket: WebSocket? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _incomingMessages = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<String> = _incomingMessages

    private var currentUrl: String? = null
    private var isClosedPurposefully = false
    private var reconnectIntervalMs = 2000L

    fun connect(url: String) {
        val formattedUrl = when {
            url.startsWith("https://") -> url.replace("https://", "wss://")
            url.startsWith("http://") -> url.replace("http://", "ws://")
            !url.startsWith("ws://") && !url.startsWith("wss://") -> "wss://$url"
            else -> url
        }
        val fullWsUrl = if (formattedUrl.endsWith("/websocket")) formattedUrl else "$formattedUrl/websocket"
        
        Log.d(TAG, "Connecting to WebSocket: $fullWsUrl")
        currentUrl = fullWsUrl
        isClosedPurposefully = false
        _connectionState.value = ConnectionState.CONNECTING

        val request = Request.Builder()
            .url(fullWsUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket Opened")
                _connectionState.value = ConnectionState.CONNECTED
                reconnectIntervalMs = 2000L
                
                // Send DDP connection request
                val connectMsg = JSONObject().apply {
                    put("msg", "connect")
                    put("version", "1")
                    put("support", org.json.JSONArray().put("1"))
                }
                webSocket.send(connectMsg.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Got Message: $text")
                coroutineScope.launch {
                    _incomingMessages.emit(text)
                }

                // Handle DDP heartbeat (ping -> pong)
                try {
                    val jsonObj = JSONObject(text)
                    if (jsonObj.optString("msg") == "ping") {
                        val pongMsg = JSONObject().apply { put("msg", "pong") }
                        webSocket.send(pongMsg.toString())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Parsing error for: $text", e)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket Closing: $code / $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket Closed")
                _connectionState.value = ConnectionState.DISCONNECTED
                attemptReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket Error: ${t.message}", t)
                _connectionState.value = ConnectionState.DISCONNECTED
                attemptReconnect()
            }
        })
    }

    private fun attemptReconnect() {
        if (isClosedPurposefully) return
        _connectionState.value = ConnectionState.RECONNECTING
        coroutineScope.launch {
            delay(reconnectIntervalMs)
            reconnectIntervalMs = (reconnectIntervalMs * 2).coerceAtMost(30000L) // exp backoff max 30s
            currentUrl?.let { connect(it) }
        }
    }

    fun subscribeRoom(roomId: String) {
        val subMsg = JSONObject().apply {
            put("msg", "sub")
            put("id", "sub_$roomId")
            put("name", "stream-room-messages")
            put("params", org.json.JSONArray().put(roomId).put(false))
        }
        send(subMsg.toString())
    }

    fun send(message: String) {
        webSocket?.send(message)
    }

    fun disconnect() {
        isClosedPurposefully = true
        webSocket?.close(1000, "Normal closure")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }
}
