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

package bykozy.me.iptv_player.iptv

import java.net.HttpURLConnection
import java.net.URL

const val IPTV_USER_AGENT = "VLC/3.0.21 LibVLC/3.0.21"

data class Channel(
    val name: String,
    val url: String,
    val group: String? = null,
)

fun downloadPlaylist(playlistUrl: String): String {
    val connection = URL(playlistUrl).openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", IPTV_USER_AGENT)
        return connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}

fun parseM3u(text: String): List<Channel> {
    val channels = mutableListOf<Channel>()
    var name = "Stream"
    var group: String? = null
    val groupRegex = Regex("group-title=\"([^\"]*)\"", RegexOption.IGNORE_CASE)

    for (raw in text.lineSequence()) {
        val line = raw.trim()
        when {
            line.startsWith("#EXTINF", ignoreCase = true) -> {
                name = line.substringAfterLast(',').trim().ifEmpty { "Stream" }
                group = groupRegex.find(line)?.groupValues?.get(1)
            }
            line.isNotEmpty() && !line.startsWith("#") -> {
                channels += Channel(name, line, group)
                name = "Stream"
                group = null
            }
        }
    }
    return channels
}
