package com.mmdparsadev.service

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.mmdparsadev.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class OverlayController(private val context: Context) : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null
    
    private var lifecycleRegistry = LifecycleRegistry(this)
    private val mViewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    
    private var dismissalRunnable: Runnable? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = mViewModelStore
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun resetDismissalTimer() {
        dismissalRunnable?.let { handler.removeCallbacks(it) }
        dismissalRunnable = Runnable { hideOverlay() }
        handler.postDelayed(dismissalRunnable!!, 6000L)
    }

    fun showOverlay(conversions: List<Pair<String, String>>, targetRect: android.graphics.Rect? = null, onCopy: (String) -> Unit) {
        val canDraw = android.provider.Settings.canDrawOverlays(context)
        android.util.Log.d("FlashC", "OverlayController.showOverlay called. canDrawOverlays=$canDraw")
        resetDismissalTimer()
        if (composeView != null) {
            android.util.Log.d("FlashC", "OverlayController.showOverlay: updating existing overlay")
            updateOverlayContent(conversions, onCopy)
            return
        }

        val density = context.resources.displayMetrics.density
        val metrics = context.resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        val estimatedCardWidth = (230 * density).toInt()
        val estimatedHeight = (120 * conversions.size * density).toInt()

        val initialX: Int
        val initialY: Int
        if (targetRect != null && targetRect.width() > 0 && targetRect.height() > 0) {
            val targetX = targetRect.left
            val targetY = targetRect.bottom + (8 * density).toInt()
            initialX = targetX.coerceIn(16, (screenWidth - estimatedCardWidth - 16).coerceAtLeast(16))
            initialY = targetY.coerceIn(16, (screenHeight - estimatedHeight - 16).coerceAtLeast(16))
        } else {
            initialX = screenWidth - estimatedCardWidth - 50
            initialY = 250
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initialX
            y = initialY
        }

        android.util.Log.d("FlashC", "OverlayController.showOverlay: creating new ComposeView")
        val view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(this@OverlayController)
            setViewTreeViewModelStoreOwner(this@OverlayController)
            setViewTreeSavedStateRegistryOwner(this@OverlayController)
            
            setContent {
                MyApplicationTheme {
                    FloatingOverlayContainer(
                        conversions = conversions,
                        onClose = { hideOverlay() },
                        onCopy = {
                            resetDismissalTimer()
                            onCopy(it)
                        },
                        onDrag = { dx, dy ->
                            resetDismissalTimer()
                            params.x = (params.x + dx.toInt()).coerceIn(10, screenWidth - estimatedCardWidth - 10)
                            params.y = (params.y + dy.toInt()).coerceIn(10, screenHeight - estimatedHeight - 10)
                            try {
                                windowManager.updateViewLayout(this, params)
                            } catch (e: Exception) {
                                // Ignore updates if detached
                            }
                        }
                    )
                }
            }
        }

        composeView = view
        if (lifecycleRegistry.currentState == Lifecycle.State.DESTROYED) {
            lifecycleRegistry = LifecycleRegistry(this)
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
        }
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        try {
            android.util.Log.d("FlashC", "OverlayController.showOverlay: WindowManager.addView() with lifecycle=${lifecycleRegistry.currentState}")
            windowManager.addView(view, params)
            android.util.Log.d("FlashC", "OverlayController.showOverlay: addView() success")
        } catch (e: Exception) {
            android.util.Log.d("FlashC", "OverlayController.showOverlay: addView() failed: ${e.message}")
            // Silent catch in case permission is revoked suddenly
        }
    }

    private fun updateOverlayContent(conversions: List<Pair<String, String>>, onCopy: (String) -> Unit) {
        val density = context.resources.displayMetrics.density
        val metrics = context.resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        val estimatedCardWidth = (230 * density).toInt()
        val estimatedHeight = (120 * conversions.size * density).toInt()

        composeView?.setContent {
            MyApplicationTheme {
                FloatingOverlayContainer(
                    conversions = conversions,
                    onClose = { hideOverlay() },
                    onCopy = {
                        resetDismissalTimer()
                        onCopy(it)
                    },
                    onDrag = { dx, dy ->
                        resetDismissalTimer()
                        composeView?.let { view ->
                            val params = view.layoutParams as WindowManager.LayoutParams
                            params.x = (params.x + dx.toInt()).coerceIn(10, screenWidth - estimatedCardWidth - 10)
                            params.y = (params.y + dy.toInt()).coerceIn(10, screenHeight - estimatedHeight - 10)
                            try {
                                windowManager.updateViewLayout(view, params)
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                    }
                )
            }
        }
    }

    fun hideOverlay() {
        dismissalRunnable?.let { handler.removeCallbacks(it) }
        composeView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                // Ignore
            }
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            composeView = null
        }
    }
}

@Composable
fun FloatingOverlayContainer(
    conversions: List<Pair<String, String>>,
    onClose: () -> Unit,
    onCopy: (String) -> Unit,
    onDrag: (Float, Float) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    var localConversions by remember(conversions) { mutableStateOf(conversions) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        visible = true
    }

    fun animateAndClose() {
        visible = false
        scope.launch {
            kotlinx.coroutines.delay(250)
            onClose()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)) + 
                scaleIn(initialScale = 0.85f, animationSpec = tween(300)) + 
                slideInVertically(initialOffsetY = { -20 }, animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(250)) + 
               scaleOut(targetScale = 0.85f, animationSpec = tween(250)) + 
               slideOutVertically(targetOffsetY = { -20 }, animationSpec = tween(250))
    ) {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .heightIn(max = 500.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                },
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(8.dp)
        ) {
            if (localConversions.size > 1) {
                item {
                    Box(
                        modifier = Modifier
                            .width(230.dp)
                            .padding(bottom = 4.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        FilledTonalButton(
                            onClick = { animateAndClose() },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss all",
                                modifier = Modifier.size(14.dp)
                              )
                              Spacer(modifier = Modifier.width(4.dp))
                              Text(
                                  text = stringResource(com.mmdparsadev.R.string.all_clear),
                                  style = MaterialTheme.typography.labelMedium,
                                  fontWeight = FontWeight.Bold
                              )
                        }
                    }
                }
            }
            items(localConversions.size) { index ->
                val (original, converted) = localConversions[index]
                FloatingOverlayCardContent(
                    original = original,
                    converted = converted,
                    onClose = { 
                        val updated = localConversions.toMutableList()
                        updated.removeAt(index)
                        localConversions = updated
                        if (localConversions.isEmpty()) {
                            animateAndClose()
                        }
                    },
                    onCopy = { onCopy(converted) }
                )
            }
        }
    }
}

@Composable
fun FloatingOverlayCardContent(
    original: String,
    converted: String,
    onClose: () -> Unit,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(230.dp)
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(com.mmdparsadev.R.string.app_name),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row {
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = stringResource(com.mmdparsadev.R.string.copy),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(com.mmdparsadev.R.string.dismiss),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Column {
                Text(
                    text = original,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(MaterialTheme.colorScheme.onPrimaryContainer, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SyncAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = converted,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        maxLines = 10 // Increased to show all conversions
                    )
                }
            }
        }
    }
}

