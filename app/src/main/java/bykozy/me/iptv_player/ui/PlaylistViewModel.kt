/* MIT License
 * 
 * Copyright (c) 2026 bykozy
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package bykozy.me.iptv_player.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import bykozy.me.iptv_player.iptv.Channel
import bykozy.me.iptv_player.iptv.downloadPlaylist
import bykozy.me.iptv_player.iptv.parseM3u
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaylistViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("iptv", Context.MODE_PRIVATE)

    var url by mutableStateOf(prefs.getString("url", "") ?: "")
    var query by mutableStateOf("")
    var channels by mutableStateOf(emptyList<Channel>())
        private set
    var playing by mutableStateOf<Channel?>(null)
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun load() {
        val playlistUrl = url.trim()
        if (playlistUrl.isEmpty()) {
            error = "Enter a playlist URL"
            return
        }
        viewModelScope.launch {
            loading = true
            error = null
            try {
                val text =
                    withContext(Dispatchers.IO) {
                        downloadPlaylist(playlistUrl)
                    }
                channels = parseM3u(text)
                prefs.edit { putString("url", playlistUrl) }
                if (channels.isEmpty()) {
                    error = "No streams found in playlist"
                }
            } catch (e: Exception) {
                channels = emptyList()
                error = e.message ?: e.toString()
            } finally {
                loading = false
            }
        }
    }
}
