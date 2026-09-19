package com.routina.bite.ui

import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.routina.bite.R
import com.routina.bite.data.decodePhoto
import com.routina.bite.data.photoFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 照片的顯示。解碼一律在 IO 執行緒，解完的縮圖放進一個 LruCache——
 * 列表會同時出現好幾張，沒有快取每次捲回來都要重解。
 */

/** [Missing] 是「檔案不在」：那一筆就當作沒有照片，不畫也不佔空間 */
sealed interface PhotoState {
    data object Loading : PhotoState
    data object Missing : PhotoState
    data class Ready(val image: ImageBitmap) : PhotoState
}

// 6 MB 大約放得下幾十張列表縮圖。全螢幕大圖不放進來（一張就 3 MB 以上，會把縮圖全擠掉）
private val photoCache = object : LruCache<String, ImageBitmap>(6 * 1024 * 1024) {
    override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
}

@Composable
fun rememberPhoto(name: String, maxEdgePx: Int, cache: Boolean = true): PhotoState {
    val context = LocalContext.current
    val key = "$name@$maxEdgePx"
    var state by remember(key) {
        mutableStateOf<PhotoState>(
            photoCache.get(key)?.let { PhotoState.Ready(it) } ?: PhotoState.Loading
        )
    }
    LaunchedEffect(key) {
        if (state is PhotoState.Ready) return@LaunchedEffect
        val image = withContext(Dispatchers.IO) {
            photoFile(context, name)?.let { decodePhoto(it, maxEdgePx)?.asImageBitmap() }
        }
        state = if (image == null) {
            PhotoState.Missing
        } else {
            if (cache) photoCache.put(key, image)
            PhotoState.Ready(image)
        }
    }
    return state
}

/** 方形圓角縮圖。還在解就畫一塊中性底色（不然圖解完整列會跳），檔案不在就什麼都不畫 */
@Composable
fun PhotoThumb(name: String, size: Dp, corner: Dp, modifier: Modifier = Modifier) {
    val maxEdgePx = with(LocalDensity.current) { size.roundToPx() }
    PhotoThumb(rememberPhoto(name, maxEdgePx), size, corner, modifier)
}

/** 呼叫端已經有狀態（例如表單要依狀態決定顯示縮圖還是「加照片」）就用這個，不要再解一次 */
@Composable
fun PhotoThumb(state: PhotoState, size: Dp, corner: Dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(corner)
    when (state) {
        PhotoState.Missing -> Unit
        PhotoState.Loading -> Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        is PhotoState.Ready -> Image(
            bitmap = state.image,
            contentDescription = stringResource(R.string.photo_thumb),
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(shape)
        )
    }
}

/** 全螢幕看整張：黑底、置中、點一下關掉。不做縮放手勢 */
@Composable
fun PhotoViewerDialog(name: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // 存下來的圖長邊就是 1600px，用它當目標等於解出整張
        val state = rememberPhoto(name, maxEdgePx = 1600, cache = false)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            if (state is PhotoState.Ready) {
                Image(
                    bitmap = state.image,
                    contentDescription = stringResource(R.string.photo_view),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
