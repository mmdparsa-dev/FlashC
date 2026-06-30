package com.mmdparsadev.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmdparsadev.data.database.HistoryEntity
import com.mmdparsadev.data.preferences.AppPreferences
import com.mmdparsadev.R


@OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    context: Context,
    prefs: AppPreferences,
    isOverlayGranted: Boolean,
    isAccessibilityGranted: Boolean,
    onCheckPermissions: () -> Unit,
    onComplete: () -> Unit,
    language: String,
    onLanguageChanged: (String) -> Unit
) {
    var currentStep by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }
    
    // Animation states for content entrance
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        androidx.compose.animation.AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { width -> width } + fadeIn(tween(400))).togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut(tween(400))
                    )
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn(tween(400))).togetherWith(
                        slideOutHorizontally { width -> width } + fadeOut(tween(400))
                    )
                }.using(SizeTransform(clip = false))
            },
            label = "OnboardingTransition"
        ) { step ->
            if (step == 0) {
                // Language Selection
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(800)) + scaleIn(initialScale = 0.8f, animationSpec = tween(800))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(80.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.select_language),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    AnimatedVisibility(
                        visible = visible,
                        enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn(tween(600, delayMillis = 200))
                    ) {
                        Column {
                            Button(
                                onClick = {
                                    prefs.appLanguage = "en"
                                    onLanguageChanged("en")
                                    currentStep = 1
                                },
                                modifier = Modifier.fillMaxWidth().height(64.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (language == "en") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (language == "en") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                            ) {
                                Text(stringResource(R.string.lang_english), style = MaterialTheme.typography.titleMedium)
                            }
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            Button(
                                onClick = {
                                    prefs.appLanguage = "fa"
                                    onLanguageChanged("fa")
                                    currentStep = 1
                                },
                                modifier = Modifier.fillMaxWidth().height(64.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (language == "fa") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (language == "fa") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                            ) {
                                Text(stringResource(R.string.lang_persian), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            } else {
                val contentVisible = remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { contentVisible.value = true }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(40.dp))
                        AnimatedVisibility(
                            visible = contentVisible.value,
                            enter = expandVertically() + fadeIn(tween(600))
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(28.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                        }
                    }

                    item {
                        AnimatedVisibility(
                            visible = contentVisible.value,
                            enter = slideInVertically(initialOffsetY = { 20 }) + fadeIn(tween(600, delayMillis = 100))
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.welcome_title),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.welcome_subtitle),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    item {
                        AnimatedVisibility(
                            visible = contentVisible.value,
                            enter = slideInVertically(initialOffsetY = { 30 }) + fadeIn(tween(600, delayMillis = 200))
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(
                                        text = stringResource(R.string.permissions_status),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    // ... Row contents remain largely same but with slight padding improvements
                                    PermissionItem(
                                        title = stringResource(R.string.draw_over_apps),
                                        description = stringResource(R.string.draw_over_apps_desc),
                                        isGranted = isOverlayGranted,
                                        onClick = {
                                            if (!isOverlayGranted) {
                                                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                                context.startActivity(intent)
                                            }
                                        }
                                    )

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    PermissionItem(
                                        title = stringResource(R.string.accessibility_service),
                                        description = stringResource(R.string.accessibility_desc),
                                        isGranted = isAccessibilityGranted,
                                        onClick = {
                                            if (!isAccessibilityGranted) {
                                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                                context.startActivity(intent)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        AnimatedVisibility(
                            visible = contentVisible.value,
                            enter = slideInVertically(initialOffsetY = { 40 }) + fadeIn(tween(600, delayMillis = 300))
                        ) {
                            Button(
                                onClick = {
                                    prefs.isOnboardingCompleted = true
                                    onComplete()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp),
                                shape = RoundedCornerShape(20.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                Text(stringResource(R.string.get_started), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionItem(title: String, description: String, isGranted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                contentColor = if (isGranted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary
            ),
            modifier = Modifier.padding(start = 12.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (isGranted) stringResource(R.string.granted) else stringResource(R.string.missing), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    
    var currentLanguage by remember { mutableStateOf(prefs.appLanguage) }
    var isOnboardingCompleted by remember { mutableStateOf(prefs.isOnboardingCompleted) }
    val currentTab by viewModel.currentTab.collectAsState()
    
    // Check overlay and accessibility permission status reactively
    var isOverlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    
    // Simple helper to check accessibility service status
    fun isAccessibilityEnabled(context: Context): Boolean {
        val expectedService = "${context.packageName}/com.example.service.FlashCAccessibilityService"
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        return enabledServices.contains(expectedService)
    }
    
    var isAccessibilityGranted by remember { mutableStateOf(isAccessibilityEnabled(context)) }

    // Recheck permissions on resume/re-focus
    LaunchedEffect(currentTab) {
        isOverlayGranted = Settings.canDrawOverlays(context)
        isAccessibilityGranted = isAccessibilityEnabled(context)
    }

    androidx.compose.animation.Crossfade(
        targetState = isOnboardingCompleted,
        animationSpec = androidx.compose.animation.core.tween(500),
        label = "OnboardingCrossfade"
    ) { completed ->
        if (!completed) {
            OnboardingScreen(
                context = context,
                prefs = prefs,
                isOverlayGranted = isOverlayGranted,
                isAccessibilityGranted = isAccessibilityGranted,
                onCheckPermissions = {
                    isOverlayGranted = Settings.canDrawOverlays(context)
                    isAccessibilityGranted = isAccessibilityEnabled(context)
                },
                onComplete = {
                    isOnboardingCompleted = true
                },
                language = currentLanguage,
                onLanguageChanged = { newLang ->
                    currentLanguage = newLang
                    if (context is android.app.Activity) {
                        context.recreate()
                    }
                }
            )
        } else {

    // Shared Text Alert Dialog Integration
        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
        android.util.Log.d("FlashC", "In MainScreen before collectAsState() for showSharedTextDialog - Thread: ${Thread.currentThread().name}, Lifecycle: ${lifecycleOwner.lifecycle.currentState}, Activity Hash: ${context.hashCode()}, ViewModel Hash: ${viewModel.hashCode()}")
        val sharedTextDialogValue by viewModel.showSharedTextDialog.collectAsState()
        android.util.Log.d("FlashC", "After collectAsState(), sharedTextDialogValue is: $sharedTextDialogValue")
        
        // Use ViewModel state directly to avoid scope issues
        val showFileSheet by viewModel.showFileConversionSheet.collectAsState()
        
         if (showFileSheet) {
            ModalBottomSheet(
              onDismissRequest = {
                viewModel.showFileConversionSheet.value = false
              }
              ) {
               FileConversionContent(viewModel)
            }
        } 
        
        if (sharedTextDialogValue != null) {
            android.util.Log.d("FlashC", "sharedTextDialogValue is NOT null: $sharedTextDialogValue - Right before AlertDialog is composed")
        } else {
            android.util.Log.d("FlashC", "sharedTextDialogValue is null during recomposition")
        }

        sharedTextDialogValue?.let { sharedText ->
            val rate = prefs.lastUsdRate
            val eurRate = prefs.lastEurRate
            val prefDisplay = prefs.preferredCurrencyDisplay
            
            // Re-parse the shared text using the ViewModel logic
            val convertedText = remember(sharedText) {
                // Simplified parsing for dialog based on ViewModel logic
                val rate = prefs.lastUsdRate
                val eurRate = prefs.lastEurRate
                val enabledCategories = prefs.enabledUnitCategories.split(",").map { it.trim().lowercase() }.toSet()
                
                var result = com.mmdparsadev.engine.plugins.CurrencyConverterPlugin().convertCurrencyInText(
                    sharedText,
                    rate,
                    eurRate,
                    prefs.showUsdConversion,
                    prefs.showEurConversion
                )
                result = com.mmdparsadev.engine.plugins.UnitConverterPlugin().parseAndConvertText(result, enabledCategories, prefs.appLanguage)
                result
            }

            AlertDialog(
                onDismissRequest = {
                    android.util.Log.d("FlashC", "Inside onDismissRequest for AlertDialog")
                    viewModel.showSharedTextDialog.value = null
                },
                title = { Text(text = stringResource(R.string.smart_file_conversion), fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = stringResource(R.string.smart_insights), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = convertedText,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            android.util.Log.d("FlashC", "Inside AlertDialog confirmButton")
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("FlashC Converted", convertedText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, context.getString(R.string.results_copied), Toast.LENGTH_SHORT).show()
                            viewModel.showSharedTextDialog.value = null
                        }
                    ) {
                        Text(text = stringResource(R.string.copy))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.showSharedTextDialog.value = null }) {
                        Text(text = stringResource(R.string.dismiss))
                    }
                }
            )
        }
        
        // ... (rest of Scaffold)

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.app_name),
                                fontWeight = FontWeight.Normal,
                                fontSize = 24.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    actions = {},
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { viewModel.currentTab.value = 0 },
                        icon = { Icon(imageVector = Icons.Default.Workspaces, contentDescription = stringResource(R.string.workspace)) },
                        label = { Text(text = stringResource(R.string.workspace)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { viewModel.currentTab.value = 1 },
                        icon = { Icon(imageVector = Icons.Default.History, contentDescription = stringResource(R.string.history)) },
                        label = { Text(text = stringResource(R.string.history)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { viewModel.currentTab.value = 2 },
                        icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = stringResource(R.string.settings)) },
                        label = { Text(text = stringResource(R.string.settings)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                    },
                    label = "TabTransition"
                ) { tab ->
                    when (tab) {
                        0 -> WorkspaceTab(viewModel)
                        1 -> HistoryTab(viewModel)
                        2 -> SettingsTab(
                            viewModel = viewModel,
                            isOverlayGranted = isOverlayGranted,
                            isAccessibilityGranted = isAccessibilityGranted,
                            onCheckPermissions = {
                                isOverlayGranted = Settings.canDrawOverlays(context)
                                isAccessibilityGranted = isAccessibilityEnabled(context)
                            },
                            currentLanguage = currentLanguage,
                            onLanguageChanged = { newLang ->
                                currentLanguage = newLang
                                if (context is android.app.Activity) {
                                    context.recreate()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun WorkspaceTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val currentLanguage = prefs.appLanguage
    val textInput by viewModel.textInput.collectAsState()
    val textOutput by viewModel.textOutput.collectAsState()

    val fileUri by viewModel.selectedFileUri.collectAsState()
    val fileName by viewModel.selectedFileName.collectAsState()
    val fileType by viewModel.selectedFileType.collectAsState()
    val fileSize by viewModel.selectedFileSize.collectAsState()
    val suggestions by viewModel.suggestedFormats.collectAsState()
    val chosenFormat by viewModel.chosenOutputFormat.collectAsState()

    val isConverting by viewModel.isConverting.collectAsState()
    val progress by viewModel.conversionProgress.collectAsState()
    val result by viewModel.activeConversionResult.collectAsState()

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        viewModel.selectFile(context, uri)
    }

    var showBottomSheet by remember { mutableStateOf(false) }
    LaunchedEffect(fileUri) {
        if (fileUri != null) {
            showBottomSheet = true
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Section 1: Quick Text Analyzer
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.smart_text_analyzer),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.parse_units_instantly),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { viewModel.onTextChange(it) },
                        label = { Text(stringResource(R.string.input_text_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        trailingIcon = {
                            if (textInput.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onTextChange("") }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        }
                    )

                    if (textOutput.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondaryContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.smart_insights),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            val clip = android.content.ClipData.newPlainText("FlashC Converted", textOutput)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, context.getString(R.string.results_copied), Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = stringResource(R.string.copy),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = textOutput,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 1.5: Plugin Capabilities Grid
        item {
            Column {
                Text(
                    text = stringResource(R.string.active_conversion_plugins),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Plugin 1: Media
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(R.string.category_media), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.category_media_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 1)
                        }
                    }

                    // Plugin 2: Documents
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(R.string.category_documents), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.category_documents_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 1)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Plugin 3: Currency
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(R.string.category_currency), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.category_currency_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 1)
                        }
                    }

                    // Plugin 4: Units
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SquareFoot,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(R.string.category_units), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.category_units_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 1)
                        }
                    }
                }
            }
        }

        // Section 2: File Converter Engine
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.local_file_converter),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.local_sandbox_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (fileUri == null) {
                        // Drop area button
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clickable { filePicker.launch("*/*") },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = null
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.select_file_to_convert),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(R.string.file_types_supported),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    } else {
                        // Display simple file card info indicating active conversion in bottom sheet
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { showBottomSheet = true },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = fileName ?: stringResource(R.string.no_file_selected),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = stringResource(R.string.tap_to_open_conversion_card),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { viewModel.selectFile(context, null) }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.remove_file),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileConversionContent(viewModel: MainViewModel) {
    // Real implementation pending
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IgnoreAppsScreen(prefs: AppPreferences, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    
    val allApps = remember {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
            .filter { (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 || it.packageName == context.packageName }
            .map { app ->
                Triple(
                    app.loadLabel(pm).toString(),
                    app.packageName,
                    app
                )
            }.sortedBy { it.first.lowercase() }
        apps
    }
    
    val filteredApps = remember(searchQuery) {
        if (searchQuery.isBlank()) allApps
        else allApps.filter { it.first.contains(searchQuery, ignoreCase = true) || it.second.contains(searchQuery, ignoreCase = true) }
    }
    
    var ignoredApps by remember { mutableStateOf(prefs.ignoredApps) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ignored_apps_title)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.done))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text(stringResource(R.string.search_apps)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(filteredApps) { (label, packageName, appInfo) ->
                    val isIgnored = ignoredApps.contains(packageName)
                    ListItem(
                        headlineContent = { Text(label, fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text(packageName, style = MaterialTheme.typography.bodySmall) },
                        leadingContent = {
                            val icon = remember { appInfo.loadIcon(context.packageManager) }
                            androidx.compose.foundation.Image(
                                bitmap = icon.toBitmap().asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(40.dp)
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = isIgnored,
                                onCheckedChange = { checked ->
                                    val newList = ignoredApps.toMutableSet()
                                    if (checked) newList.add(packageName) else newList.remove(packageName)
                                    ignoredApps = newList
                                    prefs.ignoredApps = newList
                                }
                            )
                        },
                        modifier = Modifier.clickable {
                            val checked = !isIgnored
                            val newList = ignoredApps.toMutableSet()
                            if (checked) newList.add(packageName) else newList.remove(packageName)
                            ignoredApps = newList
                            prefs.ignoredApps = newList
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
                
                if (filteredApps.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.no_apps_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryTab(viewModel: MainViewModel) {
    val historyList by viewModel.history.collectAsState(initial = emptyList())
    var selectedItem by remember { mutableStateOf<HistoryEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.conversion_registry),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = stringResource(R.string.historical_transaction_db),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            if (historyList.isNotEmpty()) {
                IconButton(onClick = { viewModel.clearHistory() }) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = stringResource(R.string.clear_logs),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.HistoryToggleOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.no_records_saved),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = stringResource(R.string.review_conversions),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(historyList, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedItem = item },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val statusColor = if (item.isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            val statusIcon = if (item.isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline

                            Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.inputName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${item.inputType.uppercase()} → ${item.outputType.uppercase()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            IconButton(onClick = { viewModel.deleteHistoryItem(item.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete_log),
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Details Modal
    selectedItem?.let { item ->
        AlertDialog(
            onDismissRequest = { selectedItem = null },
            title = {
                Text(
                    text = stringResource(R.string.conversion_details_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.source_file_label), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Text(item.inputName, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.conversion_path_label), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.conversion_to, item.inputType.uppercase(), item.outputType.uppercase()), style = MaterialTheme.typography.bodySmall)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.outcome_status_label), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Text(
                            if (item.isSuccess) stringResource(R.string.success_label) else stringResource(R.string.failed_label),
                            color = if (item.isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (item.details.isNotEmpty()) {
                        Column {
                            Text(stringResource(R.string.engine_logs_label), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Text(
                                    text = item.details,
                                    modifier = Modifier.padding(8.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedItem = null }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}

@Composable
fun SettingsTab(
    viewModel: MainViewModel,
    isOverlayGranted: Boolean,
    isAccessibilityGranted: Boolean,
    onCheckPermissions: () -> Unit,
    currentLanguage: String,
    onLanguageChanged: (String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }

    var overlayEnabled by remember { mutableStateOf(prefs.isOverlayEnabled) }
    var clipboardEnabled by remember { mutableStateOf(prefs.isClipboardParseEnabled) }
    var currencySyncEnabled by remember { mutableStateOf(prefs.isCurrencySyncEnabled) }
    var currencyDetectionEnabled by remember { mutableStateOf(prefs.isCurrencyEnabled) }
    var unitDetectionEnabled by remember { mutableStateOf(prefs.isUnitsEnabled) }
    var usdRate by remember { mutableStateOf(prefs.lastUsdRate) }
    var eurRate by remember { mutableStateOf(prefs.lastEurRate) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(text = stringResource(R.string.system_integration),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Text(text = stringResource(R.string.system_integration_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        // Section 1: Permissions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.required_permissions),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.permissions_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Permission 1: Overlay
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.screen_overlay_permission),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isOverlayGranted) stringResource(R.string.authorized) else stringResource(R.string.denied_click_to_grant),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isOverlayGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                        Button(
                            onClick = {
                                if (!isOverlayGranted) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                } else {
                                    Toast.makeText(context, context.getString(R.string.overlay_permission_granted_toast), Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isOverlayGranted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                                contentColor = if (isOverlayGranted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(if (isOverlayGranted) stringResource(R.string.active) else stringResource(R.string.authorize))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // Permission 2: Accessibility
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(R.string.accessibility_service),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isAccessibilityGranted) stringResource(R.string.active) else stringResource(R.string.disabled_click_to_enable),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isAccessibilityGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                        Button(
                            onClick = {
                                if (!isAccessibilityGranted) {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    context.startActivity(intent)
                                } else {
                                    Toast.makeText(context, context.getString(R.string.accessibility_service_active_toast), Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAccessibilityGranted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                                contentColor = if (isAccessibilityGranted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(if (isAccessibilityGranted) stringResource(R.string.active) else stringResource(R.string.enable))
                        }
                    }
                }
            }
        }

        // Section 2: Toggle Switches
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = stringResource(R.string.automation_caches),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Toggle 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(R.string.smart_screen_overlays), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(text = stringResource(R.string.smart_screen_overlays_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                        Switch(
                            checked = overlayEnabled,
                            onCheckedChange = {
                                overlayEnabled = it
                                prefs.isOverlayEnabled = it
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // Toggle 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(R.string.analyze_clipboard), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(text = stringResource(R.string.analyze_clipboard_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                        Switch(
                            checked = clipboardEnabled,
                            onCheckedChange = {
                                clipboardEnabled = it
                                prefs.isClipboardParseEnabled = it
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // Toggle 3: Currency
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(R.string.category_currency), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(text = stringResource(R.string.category_currency_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                        Switch(
                            checked = currencyDetectionEnabled,
                            onCheckedChange = {
                                currencyDetectionEnabled = it
                                prefs.isCurrencyEnabled = it
                            }
                        )
                    }

                    if (currencyDetectionEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp), thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // USD Toggle
                        var showUsd by remember { mutableStateOf(prefs.showUsdConversion) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = stringResource(R.string.show_usd_conversion), style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = showUsd,
                                onCheckedChange = {
                                    showUsd = it
                                    prefs.showUsdConversion = it
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // EUR Toggle
                        var showEur by remember { mutableStateOf(prefs.showEurConversion) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = stringResource(R.string.show_eur_conversion), style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = showEur,
                                onCheckedChange = {
                                    showEur = it
                                    prefs.showEurConversion = it
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // Toggle 4: Units
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(R.string.category_units), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(text = stringResource(R.string.category_units_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                        Switch(
                            checked = unitDetectionEnabled,
                            onCheckedChange = {
                                unitDetectionEnabled = it
                                prefs.isUnitsEnabled = it
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Ignore Apps (Excluded Apps)
                    var showIgnoreAppsScreen by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showIgnoreAppsScreen = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(R.string.ignored_apps_title), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(text = stringResource(R.string.ignored_apps_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    if (showIgnoreAppsScreen) {
                        androidx.compose.ui.window.Dialog(
                            onDismissRequest = { showIgnoreAppsScreen = false },
                            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                        ) {
                            IgnoreAppsScreen(prefs = prefs, onDismiss = { showIgnoreAppsScreen = false })
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // Currency Preferences Section
                    Text(
                        text = stringResource(R.string.currency_preferences),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    var showUsd by remember { mutableStateOf(prefs.showUsdConversion) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = stringResource(R.string.show_usd_conversion), style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = showUsd,
                            onCheckedChange = {
                                showUsd = it
                                prefs.showUsdConversion = it
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    var showEur by remember { mutableStateOf(prefs.showEurConversion) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = stringResource(R.string.show_eur_conversion), style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = showEur,
                            onCheckedChange = {
                                showEur = it
                                prefs.showEurConversion = it
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // Language Toggle Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(R.string.app_language), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(text = stringResource(R.string.language_toggle_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = currentLanguage == "en",
                                onClick = {
                                    prefs.appLanguage = "en"
                                    onLanguageChanged("en")
                                },
                                label = { Text(stringResource(R.string.lang_english)) }
                            )
                            FilterChip(
                                selected = currentLanguage == "fa",
                                onClick = {
                                    prefs.appLanguage = "fa"
                                    onLanguageChanged("fa")
                                },
                                label = { Text(stringResource(R.string.lang_persian)) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = stringResource(R.string.exchange_rate_baseline),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Button(
                                    onClick = {
                                        viewModel.syncRatesNow(context) {
                                            usdRate = prefs.lastUsdRate
                                            eurRate = prefs.lastEurRate
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Sync",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.sync_now), fontSize = 11.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            val dateStr = if (prefs.lastRateFetchTime > 0L) java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(prefs.lastRateFetchTime)) else stringResource(R.string.never_updated)
                            Text(
                                text = stringResource(R.string.last_updated_prefix, dateStr),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.currency_usd_prefix),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(R.string.currency_tomans_suffix, String.format("%,.0f", usdRate)),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.currency_eur_prefix),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(R.string.currency_tomans_suffix, String.format("%,.0f", eurRate)),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
