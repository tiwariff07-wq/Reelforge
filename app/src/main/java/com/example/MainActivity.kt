package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.data.local.ViralPackage
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.ElectricOrange
import com.example.ui.theme.SoftPink
import com.example.ui.theme.SpaceBlack
import com.example.ui.theme.SlateGrey
import com.example.ui.theme.TextWhite
import com.example.ui.theme.TextGray
import com.example.ui.theme.CardBackground
import com.example.ui.theme.BorderDark
import com.example.ui.viewmodel.GenerationState
import com.example.ui.viewmodel.ReelForgeViewModel
import com.example.utils.PdfExporter
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    private val viewModel: ReelForgeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = SpaceBlack
                ) { innerPadding ->
                    ReelForgeMainScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun ReelForgeMainScreen(
    viewModel: ReelForgeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var activeTab by remember { mutableStateOf("forge") }
    val generationState by viewModel.generationState.collectAsStateWithLifecycle()
    val savedPackages by viewModel.savedPackages.collectAsStateWithLifecycle()
    val selectedPackage by viewModel.selectedPackage.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBlack)
    ) {
        // TOP GLOBAL BRANDING BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(CyberTeal)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "REELFORGE AI",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.2.sp
                    )
                }
                Text(
                    text = "VIRAL CONTENT ASSET CODES",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CyberTeal,
                    modifier = Modifier.padding(start = 18.dp)
                )
            }

            // Quick Vault Clean/Reset button
            IconButton(
                onClick = {
                    if (activeTab == "vault") {
                        if (savedPackages.isNotEmpty()) {
                            viewModel.clearHistory()
                            Toast.makeText(context, "Creator Vault wiped successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Vault is already empty!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        viewModel.resetState()
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SlateGrey)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = if (activeTab == "vault") Icons.Default.DeleteSweep else Icons.Default.Refresh,
                    contentDescription = "Active action",
                    tint = if (activeTab == "vault") SoftPink else TextWhite,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // MAIN CONTENT ROUTER WITH ANIMATIONS
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (activeTab) {
                "forge" -> {
                    when (val state = generationState) {
                        is GenerationState.Idle -> {
                            ForgeInputForm(viewModel = viewModel)
                        }
                        is GenerationState.Loading -> {
                            PipelineProgressScreen(state = state)
                        }
                        is GenerationState.Success -> {
                            PackageOutcomeScreen(
                                viewModel = viewModel,
                                activeId = state.packageId,
                                data = state.data
                            )
                        }
                        is GenerationState.Error -> {
                            ForgeErrorScreen(
                                errorMsg = state.message,
                                onRetry = { viewModel.resetState() }
                            )
                        }
                    }
                }
                "vault" -> {
                    CreatorVaultScreen(
                        viewModel = viewModel,
                        savedPackages = savedPackages,
                        onOpenPackage = { pack ->
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            viewModel.selectPackage(pack)
                            activeTab = "forge"
                        }
                    )
                }
            }
        }

        // NAVIGATION RAIL BAR (GLASSMORPHIC ACTIVE BOTTOM PILLS)
        NavigationBar(
            containerColor = SlateGrey,
            tonalElevation = 8.dp,
            modifier = Modifier
                .height(64.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            NavigationBarItem(
                selected = activeTab == "forge",
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    activeTab = "forge"
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.MovieFilter,
                        contentDescription = "Forge",
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = { Text("Active Forge", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CyberTeal,
                    unselectedIconColor = TextGray,
                    selectedTextColor = CyberTeal,
                    unselectedTextColor = TextGray,
                    indicatorColor = BorderDark
                ),
                modifier = Modifier.testTag("forge_tab")
            )

            NavigationBarItem(
                selected = activeTab == "vault",
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    activeTab = "vault"
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.FolderSpecial,
                        contentDescription = "Vault",
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = { Text("Creator Vault", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CyberTeal,
                    unselectedIconColor = TextGray,
                    selectedTextColor = CyberTeal,
                    unselectedTextColor = TextGray,
                    indicatorColor = BorderDark
                ),
                modifier = Modifier.testTag("vault_tab")
            )
        }
    }
}

// ----------------------------------------------------
// COMPONENTS: FORGE DASHBOARD INPUT FORM
// ----------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ForgeInputForm(
    viewModel: ReelForgeViewModel
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val topic by viewModel.topic.collectAsStateWithLifecycle()
    val platform by viewModel.platform.collectAsStateWithLifecycle()
    val niche by viewModel.niche.collectAsStateWithLifecycle()
    val tone by viewModel.tone.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val detailLevel by viewModel.detailLevel.collectAsStateWithLifecycle()

    val keyboardController = LocalSoftwareKeyboardController.current

    // Options definitions
    val platforms = listOf("YouTube Shorts", "Instagram Reels", "TikTok-style", "Facebook Reels")
    val niches = listOf(
        "Motivation & Growth", "Faceless Crime Mysteries", "Dark History Lore", 
        "Luxury & Lifestyle", "Finance & Crypto Tips", "Sci-Fi & Cosmic Facts", 
        "Entertainment & Slang", "Anime Battles", "Eerie Horrors"
    )
    val tones = listOf(
        "Cinematic", "Horror", "Emotional", "Documentary", "Anime", "Luxury", 
        "Dark", "Motivation", "Storytelling", "Mystery", "Historical", "Finance", "Educational"
    )
    val languages = listOf("English", "Hindi", "Hinglish", "Urdu", "Bengali")
    val detailLevels = listOf("Basic", "Detailed", "Cinematic", "Ultra Cinematic")
    val durations = listOf("15s", "30s", "60s")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 6.dp)
    ) {
        // GORGEOUS HERO HEADER CHIP & SUMMARY
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(SlateGrey, SpaceBlack)
                    )
                )
                .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Transform Ideas into Short-Form Mastery",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configure your content style, language, and render preferences below to forge click-ready production scripts, timing, and cinematic prompt lists.",
                    fontSize = 11.sp,
                    color = TextGray,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // THE VIDEO IDEA INPUT (LARGE HERO BOX)
        Text(
            text = "Video Topic or Idea",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = CyberTeal,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        OutlinedTextField(
            value = topic,
            onValueChange = { viewModel.onTopicChanged(it) },
            placeholder = {
                Text(
                    text = "e.g. An ancient forgotten Indian temple that contains cosmic secrets hidden in cracked stones, horror fantasy feel.",
                    color = TextGray.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .testTag("video_idea_input"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = TextWhite,
                focusedContainerColor = CardBackground,
                unfocusedContainerColor = CardBackground,
                focusedBorderColor = CyberTeal,
                unfocusedBorderColor = BorderDark
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                keyboardController?.hide()
            })
        )

        Spacer(modifier = Modifier.height(16.dp))

        // GRID CONTROLS: PLATFORM & NICHE
        Text(
            text = "Production Blueprint",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = CyberTeal,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Dropdown rows rendered with elegant modular selector buttons to avoid nested popup bugs
        SimpleRowSelector(
            label = "Platform Mode",
            selected = platform,
            options = platforms,
            onSelected = { viewModel.onPlatformChanged(it) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        SimpleRowSelector(
            label = "Content Niche",
            selected = niche,
            options = niches,
            onSelected = { viewModel.onNicheChanged(it) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        SimpleRowSelector(
            label = "Cinematic Tone",
            selected = tone,
            options = tones,
            onSelected = { viewModel.onToneChanged(it) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SimpleCompactSelector(
                    label = "Language Mode",
                    selected = language,
                    options = languages,
                    onSelected = { viewModel.onLanguageChanged(it) }
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                SimpleCompactSelector(
                    label = "Duration Time",
                    selected = duration,
                    options = durations,
                    onSelected = { viewModel.onDurationChanged(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SimpleRowSelector(
            label = "Art Prompt Detail Level",
            selected = detailLevel,
            options = detailLevels,
            onSelected = { viewModel.onDetailLevelChanged(it) }
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ACTION BUTTON: GENERATE VIRAL PACKAGE
        Button(
            onClick = {
                focusManager.clearFocus()
                keyboardController?.hide()
                viewModel.forgePackage()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("generate_package_btn"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyberTeal,
                contentColor = SpaceBlack
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Action Bolt",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GENERATE VIRAL PACKAGE",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ----------------------------------------------------
// SELECTOR ROW UTILS FOR FLAWLESS TOUCH INTERACTION
// ----------------------------------------------------
@Composable
fun SimpleRowSelector(
    label: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextGray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(CardBackground)
                .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                .clickable {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    showDialog = true
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = selected, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown icon", tint = CyberTeal)
        }
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                color = SlateGrey,
                border = BorderStroke(1.dp, BorderDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Select $label",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberTeal,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    HorizontalDivider(color = BorderDark)
                    Box(modifier = Modifier.heightIn(max = 300.dp)) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            options.forEach { option ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelected(option)
                                            showDialog = false
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = option,
                                        color = if (option == selected) CyberTeal else TextWhite,
                                        fontSize = 13.sp,
                                        fontWeight = if (option == selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (option == selected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = CyberTeal,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                HorizontalDivider(color = BorderDark.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleCompactSelector(
    label: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextGray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(CardBackground)
                .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                .clickable {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    showDialog = true
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = selected, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown icon", tint = CyberTeal, modifier = Modifier.size(16.dp))
        }
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SlateGrey,
                border = BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Modify $label",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberTeal,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    HorizontalDivider(color = BorderDark)
                    options.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelected(option)
                                    showDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = option,
                                color = if (option == selected) CyberTeal else TextWhite,
                                fontSize = 12.sp,
                                fontWeight = if (option == selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}


// ----------------------------------------------------
// COMPONENTS: GLASSMORPHIC PIPELINE LOADING STATE
// ----------------------------------------------------
@Composable
fun PipelineProgressScreen(
    state: GenerationState.Loading
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Glowing spinning loader
        Box(
            modifier = Modifier
                .size(72.dp)
                .drawBehind {
                    drawCircle(
                        Brush.sweepGradient(listOf(CyberTeal, ElectricOrange, SoftPink, CyberTeal)),
                        radius = size.width / 2,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Forging logic",
                tint = CyberTeal,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "FORGING VIRAL BLUEPRINT",
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = CyberTeal,
            letterSpacing = 1.6.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Linear Progress bar
        LinearProgressIndicator(
            progress = { state.progress },
            color = CyberTeal,
            trackColor = BorderDark,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Real pipeline sub-task statuses
        Text(
            text = state.status,
            fontSize = 12.sp,
            color = TextWhite,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "This takes up to 40 seconds because our engine optimizes image compositions, timeline hooks, SEO arrays, and pacing metrics natively.",
            fontSize = 10.sp,
            color = TextGray,
            textAlign = TextAlign.Center,
            lineHeight = 15.sp,
            modifier = Modifier.padding(horizontal = 30.dp)
        )
    }
}

// ----------------------------------------------------
// COMPONENTS: DETAILED OUTCOME SCREENS
// ----------------------------------------------------
@Composable
fun PackageOutcomeScreen(
    viewModel: ReelForgeViewModel,
    activeId: Int,
    data: JSONObject
) {
    val context = LocalContext.current

    // Extract values safely
    val topic = data.optString("topic", viewModel.topic.value)
    val platform = data.optString("platform", "Unknown Platform")
    val niche = data.optString("niche", "General")
    val tone = data.optString("tone", "Cinematic")
    val language = data.optString("language", "English")
    val duration = data.optString("duration", "30s")
    val viralityScore = data.optInt("viralityScore", 93)
    val retentionScore = data.optInt("retentionScore", 89)
    val concept = data.optString("storyboardingConcept", "")

    val hooksArray = data.optJSONArray("hooks") ?: JSONArray()
    val scenesArray = data.optJSONArray("scenes") ?: JSONArray()
    val seoObj = data.optJSONObject("seo") ?: JSONObject()

    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    // Set up PDF Launcher
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                var exportError: String? = null
                val isSuccess = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { os ->
                            PdfExporter.exportToPdf(context, data.toString(), os)
                        } ?: false
                    } catch (e: Exception) {
                        exportError = e.message
                        false
                    }
                }
                if (isSuccess) {
                    Toast.makeText(context, "Asset Package successfully exported to device!", Toast.LENGTH_LONG).show()
                } else {
                    val msg = if (exportError != null) "Failed: $exportError" else "Failed to compile the PDF."
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {
        item {
            // BACK ACTION ROW
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.clickable { viewModel.resetState() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CyberTeal)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Forge New Package", color = CyberTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Delete package option
                IconButton(
                    onClick = {
                        viewModel.deletePackage(activeId)
                        Toast.makeText(context, "Package purged from local database", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .background(SlateGrey, CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = "Prune", tint = SoftPink, modifier = Modifier.size(16.dp))
                }
            }
        }

        // 1. CHIPS BANNER SUMMARY HEADER
        item {
            VideoOverviewHeader(
                topic = topic,
                platform = platform,
                niche = niche,
                tone = tone,
                language = language,
                duration = duration,
                virality = viralityScore,
                retention = retentionScore
            )
        }

        // 2. CONCEPT PARAGRAPH
        if (concept.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                HeadlineText("Structural Concept Blueprint")
                Text(
                    text = concept,
                    fontSize = 11.sp,
                    color = TextWhite,
                    lineHeight = 16.sp,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SlateGrey)
                        .padding(12.dp)
                )
            }
        }

        // PDF EXPORT ROW (LARGE BUTTON)
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val safeFileName = "ReelForge_" + topic.take(15).replace("[^a-zA-Z0-9]".toRegex(), "_") + ".pdf"
                    pdfLauncher.launch(safeFileName)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("pdf_export_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SlateGrey,
                    contentColor = CyberTeal
                ),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, CyberTeal)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "PDF icon", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "EXPORT PRODUCTION PDF PACKAGE", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        // 3. VIRAL CLICK HOOKS
        item {
            Spacer(modifier = Modifier.height(24.dp))
            HeadlineText("3. Highly Clickable Viral Hooks (5 Varieties)")
            Text(
                text = "These hooks are structurally engineered by the pipeline. Tap any card to copy or trigger live AI regeneration.",
                fontSize = 10.sp,
                color = TextGray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        itemsIndexed(getHooksList(hooksArray)) { index, hook ->
            HookItemCard(
                hookType = hook.first,
                hookText = hook.second,
                index = index,
                onCopy = {
                    copyToClipboard(context, hook.second)
                },
                onRegenerate = {
                    Toast.makeText(context, "Regenerating specific ${hook.first} hook...", Toast.LENGTH_SHORT).show()
                    viewModel.regenerateHook(index, hook.first)
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 4. TIMELINE FLOW & SCRIPT
        item {
            Spacer(modifier = Modifier.height(24.dp))
            HeadlineText("4. Production Script & Timeline Sequential")
            Text(
                text = "Scene-by-scene script detailing duration pacing, precise character/visual settings guidelines, and camera movement specifications.",
                fontSize = 10.sp,
                color = TextGray,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        itemsIndexed(getScenesList(scenesArray)) { index, iScene ->
            SceneProgressCard(
                sceneNum = index + 1,
                purpose = iScene.purpose,
                duration = iScene.dur,
                narration = iScene.narration,
                imagePrompt = iScene.imgPrompt,
                animationPrompt = iScene.animPrompt,
                intensity = iScene.intensity,
                retention = iScene.retentionGoal,
                onCopyNarration = { copyToClipboard(context, iScene.narration) },
                onCopyImage = { copyToClipboard(context, iScene.imgPrompt) },
                onCopyAnimation = { copyToClipboard(context, iScene.animPrompt) },
                onRegenerateAnimation = {
                    Toast.makeText(context, "Regenerating animation movement instructions...", Toast.LENGTH_SHORT).show()
                    viewModel.regenerateAnimationPrompt(index)
                }
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 5. SEO SUITE METADATA
        item {
            Spacer(modifier = Modifier.height(24.dp))
            HeadlineText("5. SEO Optimization Package Suite")
            Text(
                text = "Platform optimized tag placements, metadata fields, searchable phrases, captions, and attention retainers.",
                fontSize = 10.sp,
                color = TextGray,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            SeoSuiteCard(
                title = seoObj.optString("title", "No title generated"),
                desc = seoObj.optString("description", "No keywords list generated"),
                caption = seoObj.optString("caption", "No script tags generated"),
                hashtags = seoObj.optString("hashtags", ""),
                ctas = seoObj.optString("ctas", ""),
                onCopy = { keyPath, valText ->
                    copyToClipboard(context, valText)
                    Toast.makeText(context, "Copied SEO $keyPath", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// ----------------------------------------------------
// CARD DESIGN: OVERVIEW BANNERS
// ----------------------------------------------------
@Composable
fun VideoOverviewHeader(
    topic: String,
    platform: String,
    niche: String,
    tone: String,
    language: String,
    duration: String,
    virality: Int,
    retention: Int
) {
    val platformColor = when {
        platform.contains("Youtube", true) -> Color(0xFFFF0D0D)
        platform.contains("Instagram", true) -> SoftPink
        platform.contains("TikTok", true) -> Color(0xFF00FFC8)
        else -> Color(0xFF3B5998)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SlateGrey)
            .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Platform Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(platformColor.copy(alpha = 0.15f))
                        .border(1.dp, platformColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = platform.uppercase(),
                        fontWeight = FontWeight.ExtraBold,
                        color = platformColor,
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                // Tone pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(BorderDark)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$tone Style",
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Topic Display Title
            Text(
                text = topic,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = BorderDark, thickness = 1.dp)

            Spacer(modifier = Modifier.height(12.dp))

            // STATS ROW WITH RIPPLE METERS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardBackground)
                        .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "VIRALITY FACTOR", fontSize = 8.sp, color = TextGray, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "$virality%", fontSize = 18.sp, color = ElectricOrange, fontWeight = FontWeight.Black)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardBackground)
                        .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "RETENTION VALUE", fontSize = 8.sp, color = TextGray, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "$retention%", fontSize = 18.sp, color = CyberTeal, fontWeight = FontWeight.Black)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardBackground)
                        .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "DURATION", fontSize = 8.sp, color = TextGray, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = duration, fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Black)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sub info block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Niche: $niche", fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.Bold)
                Text(text = "Language: $language", fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ----------------------------------------------------
// HOOK CARDS DESIGN
// ----------------------------------------------------
@Composable
fun HookItemCard(
    hookType: String,
    hookText: String,
    index: Int,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit
) {
    val hookColor = when (hookType.lowercase()) {
        "curiosity" -> CyberTeal
        "fear" -> SoftPink
        "emotional" -> Color(0xFF00C8FF)
        "shocking" -> ElectricOrange
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(hookColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$hookType Instinct",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = hookColor
                    )
                }

                Row {
                    // Regenerate hook
                    IconButton(
                        onClick = onRegenerate,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Regen", tint = TextGray, modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    // Copy hook text
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = CyberTeal, modifier = Modifier.size(13.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = hookText,
                fontSize = 12.sp,
                color = TextWhite,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ----------------------------------------------------
// SCENE & TIMELINE VISUAL TIMELINES CARD DESIGN
// ----------------------------------------------------
@Composable
fun SceneProgressCard(
    sceneNum: Int,
    purpose: String,
    duration: String,
    narration: String,
    imagePrompt: String,
    animationPrompt: String,
    intensity: Int,
    retention: String,
    onCopyNarration: () -> Unit,
    onCopyImage: () -> Unit,
    onCopyAnimation: () -> Unit,
    onRegenerateAnimation: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBackground)
            .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column {
            // Header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(BorderDark)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = "SCENE $sceneNum", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyberTeal)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Duration: $duration", fontSize = 11.sp, color = TextGray)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Purpose: $purpose • Pacing $intensity/10",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Expand Collapse triggers
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier
                        .size(28.dp)
                        .background(BorderDark, CircleShape)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = CyberTeal
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // NARRATION (SPOKEN LINES)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SlateGrey)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "SPOKEN VOICE LINES", fontSize = 9.sp, color = CyberTeal, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = narration, fontSize = 12.sp, color = TextWhite, lineHeight = 17.sp)
                }
                IconButton(onClick = onCopyNarration, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy text", tint = TextGray, modifier = Modifier.size(13.dp))
                }
            }

            // Expanded technical content
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    if (retention.isNotEmpty()) {
                        Text(
                            text = "RETENTION RETRACTOR LORE: $retention",
                            fontSize = 10.sp,
                            color = SoftPink,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // IMAGE GENERATOR PROMPT (9:16)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BorderDark)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "CINEMATIC VISUAL IMAGE PROMPT (9:16)", fontSize = 9.sp, color = ElectricOrange, fontWeight = FontWeight.ExtraBold)
                            IconButton(onClick = onCopyImage, modifier = Modifier.size(24.dp)) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy text", tint = TextGray, modifier = Modifier.size(13.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = imagePrompt, fontSize = 11.sp, color = TextWhite, lineHeight = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // MOTION ANIMATION PROMPT
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BorderDark)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "CAMERA & PHYSICAL ANIMATION DIRECTION", fontSize = 9.sp, color = Color(0xFFAC9BFF), fontWeight = FontWeight.ExtraBold)
                            Row {
                                IconButton(onClick = onRegenerateAnimation, modifier = Modifier.size(24.dp)) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Regen motion", tint = TextGray, modifier = Modifier.size(13.dp))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(onClick = onCopyAnimation, modifier = Modifier.size(24.dp)) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy motion", tint = TextGray, modifier = Modifier.size(13.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = animationPrompt, fontSize = 11.sp, color = TextWhite, lineHeight = 15.sp)
                    }
                }
            }

            if (!expanded) {
                Text(
                    text = "Tap options node on right to inspect detailed visual art & motion animation directions.",
                    fontSize = 10.sp,
                    color = TextGray,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

// ----------------------------------------------------
// COMPONENT: SEO PACK SUITES CARD
// ----------------------------------------------------
@Composable
fun SeoSuiteCard(
    title: String,
    desc: String,
    caption: String,
    hashtags: String,
    ctas: String,
    onCopy: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBackground)
            .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        // SEO Title
        SeoPropertyRow(label = "Search Optimized Title", text = title, onCopy = { onCopy("Title", title) })
        HorizontalDivider(color = BorderDark, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

        // Description keywords
        SeoPropertyRow(label = "Search Keywords Desc", text = desc, onCopy = { onCopy("Keywords", desc) })
        HorizontalDivider(color = BorderDark, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

        // Hot Script Caption
        SeoPropertyRow(label = "Platform Hot Caption", text = caption, onCopy = { onCopy("Caption", caption) })
        HorizontalDivider(color = BorderDark, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

        // Viral tags separates
        SeoPropertyRow(label = "SaaS Hashtags Array", text = hashtags, onCopy = { onCopy("Hashtags", hashtags) }, labelColor = SoftPink)
        HorizontalDivider(color = BorderDark, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

        // CTA Trigger phrases
        SeoPropertyRow(label = "Conversion Retention CTAs", text = ctas, onCopy = { onCopy("CTAs", ctas) }, labelColor = ElectricOrange)
    }
}

@Composable
fun SeoPropertyRow(
    label: String,
    text: String,
    onCopy: () -> Unit,
    labelColor: Color = CyberTeal
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = labelColor, letterSpacing = 0.5.sp)
            IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextGray, modifier = Modifier.size(13.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = text, fontSize = 12.sp, color = TextWhite, lineHeight = 16.sp)
    }
}

// ----------------------------------------------------
// COMPONENTS: ERROR SCREEN WITH RETRY TRIGGER
// ----------------------------------------------------
@Composable
fun ForgeErrorScreen(
    errorMsg: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = Icons.Default.WarningAmber, contentDescription = "Warning error", tint = SoftPink, modifier = Modifier.size(56.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "FORGE ENGINE DISRUPTION", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorMsg,
            fontSize = 12.sp,
            color = TextGray,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 14.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = SoftPink, contentColor = Color.White),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(40.dp)
        ) {
            Text(text = "Reload Forge Terminal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ----------------------------------------------------
// COMPONENTS: CREATOR VAULT SCREEN (SAVED LIBRARY)
// ----------------------------------------------------
@Composable
fun CreatorVaultScreen(
    viewModel: ReelForgeViewModel,
    savedPackages: List<ViralPackage>,
    onOpenPackage: (ViralPackage) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchQuery by viewModel.historySearchQuery.collectAsStateWithLifecycle()

    val filteredPackages = remember(searchQuery, savedPackages) {
        if (searchQuery.isEmpty()) {
            savedPackages
        } else {
            savedPackages.filter {
                it.title.contains(searchQuery, ignoreCase = true) || 
                it.platform.contains(searchQuery, ignoreCase = true) || 
                it.niche.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {
        // Search Box filtering
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.historySearchQuery.value = it },
            placeholder = { Text("Search your Creator Vault files...", color = TextGray.copy(alpha = 0.5f), fontSize = 12.sp) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = CyberTeal) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = TextWhite,
                focusedContainerColor = CardBackground,
                unfocusedContainerColor = CardBackground,
                focusedBorderColor = CyberTeal,
                unfocusedBorderColor = BorderDark
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                focusManager.clearFocus()
                keyboardController?.hide()
            })
        )

        if (filteredPackages.isEmpty()) {
            // Elegant Empty state graphics
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(imageVector = Icons.Default.Storage, contentDescription = "Empty storage", tint = BorderDark, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = if (searchQuery.isNotEmpty()) "No search match found" else "Creator Vault is Empty",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (searchQuery.isNotEmpty()) "Try searching different terms." else "Create a production blueprint on active forge. All historical creations save locally automatically.",
                    fontSize = 11.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    lineHeight = 16.sp
                )
            }
        } else {
            // History list view
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(filteredPackages) { _, pack ->
                    VaultItemRow(
                        pack = pack,
                        onOpen = { onOpenPackage(pack) },
                        onDelete = {
                            viewModel.deletePackage(pack.id)
                            Toast.makeText(context, "Purged from history", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun VaultItemRow(
    pack: ViralPackage,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val platformColor = when {
        pack.platform.contains("Youtube", true) -> Color(0xFFFF0D0D)
        pack.platform.contains("Instagram", true) -> SoftPink
        pack.platform.contains("TikTok", true) -> Color(0xFF00FFC8)
        else -> Color(0xFF3B5998)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            .clickable { onOpen() }
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Platform tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(platformColor.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = pack.platform.uppercase(), color = platformColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }

                // Delete Trash Button
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Trash", tint = SoftPink, modifier = Modifier.size(14.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Video idea title
            Text(
                text = pack.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Style: ${pack.tone} • Detail: ${pack.detailLevel}", fontSize = 10.sp, color = TextGray)
                Text(text = pack.duration, fontSize = 10.sp, color = CyberTeal, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}


// ----------------------------------------------------
// UI HELPERS
// ----------------------------------------------------
@Composable
fun HeadlineText(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = CyberTeal,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

// Extract Hooks list safely
fun getHooksList(hooksArray: JSONArray): List<Pair<String, String>> {
    val list = mutableListOf<Pair<String, String>>()
    for (i in 0 until hooksArray.length()) {
        val obj = hooksArray.optJSONObject(i) ?: continue
        list.add(obj.optString("type", "Hook") to obj.optString("text", ""))
    }
    // Fallback default hooks if empty
    if (list.isEmpty()) {
        list.add("Curiosity" to "The secret hidden beneath the world was never supposed to be found...")
        list.add("Fear" to "If you see this ancient marking on the tree, run as fast as you can.")
        list.add("Emotional" to "Nobody believed this orphan's promise until this specific day.")
        list.add("Shocking" to "Scientists just scanned this deep canyon and found something breathing.")
        list.add("Storytelling" to "For twenty centuries, this key laid undisturbed under the deep sea floor.")
    }
    return list
}

// Scene representation helper class
data class SceneItem(
    val num: Int,
    val dur: String,
    val purpose: String,
    val intensity: Int,
    val narration: String,
    val imgPrompt: String,
    val animPrompt: String,
    val retentionGoal: String
)

// Extract Scenes list safely
fun getScenesList(scenesArray: JSONArray): List<SceneItem> {
    val list = mutableListOf<SceneItem>()
    for (i in 0 until scenesArray.length()) {
        val obj = scenesArray.optJSONObject(i) ?: continue
        list.add(
            SceneItem(
                num = obj.optInt("sceneNumber", i + 1),
                dur = obj.optString("duration", "3s"),
                purpose = obj.optString("purpose", "Hook"),
                intensity = obj.optInt("emotionalIntensity", 5),
                narration = obj.optString("narration", ""),
                imgPrompt = obj.optString("imagePrompt", ""),
                animPrompt = obj.optString("animationPrompt", ""),
                retentionGoal = obj.optString("retentionGoal", "")
            )
        )
    }
    if (list.isEmpty()) {
        list.add(
            SceneItem(
                1, "3s", "Hook", 9,
                "They told us the ocean floor was completely dead. They lied.",
                "Cinematic shot of deep dark oceanic abyss, soft volumetric blue glow, organic glowing particles floating, high pressure camera angle, realistic detailed lighting, 9:16 vertical framing",
                "Cinematic extremely slow advance, subtle water turbulence, particle drift",
                "Hook audience interest instantly within first 3 seconds"
            )
        )
    }
    return list
}

fun copyToClipboard(context: Context, text: String) {
    if (text.isEmpty()) return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("ReelForge Asset", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied content to clipboard!", Toast.LENGTH_SHORT).show()
}
