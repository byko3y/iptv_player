/*
 * Copyright 2026 Vincent Tsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Modified by byko3y

package bykozy.me.iptv_player.ui.screens

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import bykozy.me.iptv_player.iptv.Channel
import bykozy.me.iptv_player.iptv.IPTV_USER_AGENT
import bykozy.me.iptv_player.ui.PlaylistViewModel
import bykozy.me.iptv_player.ui.theme.AppTheme

@Composable
fun MainScreen(viewModel: PlaylistViewModel = viewModel()) {
    val playing = viewModel.playing
    if (playing != null) {
        PlayerScreen(channel = playing, onBack = { viewModel.playing = null })
    } else {
        PlaylistScreen(viewModel)
    }
}

@Composable
private fun PlaylistScreen(viewModel: PlaylistViewModel) {
    val visible =
        remember(viewModel.channels, viewModel.query) {
            val query = viewModel.query.trim()
            if (query.isEmpty()) {
                viewModel.channels
            } else {
                viewModel.channels.filter {
                    it.name.contains(query, ignoreCase = true) ||
                        it.group.orEmpty().contains(query, ignoreCase = true)
                }
            }
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = viewModel.url,
            onValueChange = { viewModel.url = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("M3U playlist URL") },
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.load() }, enabled = !viewModel.loading) {
                Text("Load")
            }
            if (viewModel.loading) {
                CircularProgressIndicator()
            }
        }
        viewModel.error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
        if (viewModel.channels.isNotEmpty()) {
            OutlinedTextField(
                value = viewModel.query,
                onValueChange = { viewModel.query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Filter") },
                singleLine = true,
            )
            Text("${visible.size} / ${viewModel.channels.size} streams")
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(visible, key = { it.url + it.name }) { channel ->
                ListItem(
                    headlineContent = { Text(channel.name) },
                    supportingContent =
                        channel.group?.let { group ->
                            { Text(group) }
                        },
                    modifier = Modifier.clickable { viewModel.playing = channel },
                )
                HorizontalDivider()
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun PlayerScreen(
    channel: Channel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val player =
        remember(channel.url) {
            val http =
                DefaultHttpDataSource.Factory()
                    .setUserAgent(IPTV_USER_AGENT)
                    .setAllowCrossProtocolRedirects(true)
            ExoPlayer.Builder(context)
                .setMediaSourceFactory(DefaultMediaSourceFactory(http))
                .build()
                .apply {
                    val mediaItem =
                        MediaItem.Builder()
                            .setUri(channel.url)
                            .apply {
                                if (channel.url.contains(".m3u8", ignoreCase = true)) {
                                    setMimeType(MimeTypes.APPLICATION_M3U8)
                                }
                            }
                            .build()
                    setMediaItem(mediaItem)
                    prepare()
                    playWhenReady = true
                }
        }

    DisposableEffect(player) {
        onDispose { player.release() }
    }
    BackHandler(onBack = onBack)

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                useController = true
                keepScreenOn = true
            }
        },
        update = { it.player = player },
        modifier = Modifier.fillMaxSize(),
    )
}

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    AppTheme(useSystemUIController = false) {
        Text("IPTV Player")
    }
}
