package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.download.ApkDownloader
import com.example.model.ApkItem
import com.example.ui.theme.*
import com.example.viewmodel.StoreUiState
import com.example.viewmodel.StoreViewModel
import kotlinx.coroutines.delay

private const val TELEGRAM_URL = "https://t.me/SagarTech99"

@Composable
fun GhostStoreApp(viewModel: StoreViewModel, downloader: ApkDownloader) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(navController = navController)
        }
        composable("home") {
            HomeScreen(navController = navController, viewModel = viewModel, downloader = downloader)
        }
        composable(
            route = "details/{apkName}",
            arguments = listOf(navArgument("apkName") { type = NavType.StringType })
        ) { backStackEntry ->
            val apkName = backStackEntry.arguments?.getString("apkName") ?: ""
            DetailsScreen(
                apkName = apkName,
                navController = navController,
                viewModel = viewModel,
                downloader = downloader
            )
        }
        composable("terms") {
            TermsScreen(navController = navController)
        }
    }
}

// ==================================================
// SPLASH SCREEN
// ==================================================
@Composable
fun SplashScreen(navController: NavController) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        // Animate elements entering screen
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(1000)
        )
        delay(2200) // Beautiful delay
        navController.navigate("home") {
            popUpTo("splash") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GhostBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Stylized vector ghost launcher card
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(GhostPrimary.copy(alpha = 0.3f), GhostBackground)
                        )
                    )
                    .border(BorderStroke(2.dp, GhostPrimary), RoundedCornerShape(32.dp))
                    .padding(16.dp)
                    .alpha(alpha.value),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = "Ghost Store Robot Icon",
                    tint = GhostSecondary,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Animated title
            Text(
                text = "GHOST STORE",
                color = GhostTextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(alpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Pulse subtitle
            Text(
                text = "THE ULTIMATE APK MARKETPLACE",
                color = GhostSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(alpha.value)
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = GhostSecondary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

// ==================================================
// HOME SCREEN
// ==================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: StoreViewModel,
    downloader: ApkDownloader
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val filteredApks by viewModel.filteredApks.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    // Bottom navigation selection state
    var selectedTab by remember { mutableStateOf("home") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = GhostBackground,
        topBar = {
            Column(
                modifier = Modifier
                    .background(GhostBackground)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Main Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { navController.navigate("terms") }
                    ) {
                        // Small Brand icon in top left with Linear Gradient and letter 'G'
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(GhostPrimary, GhostSecondary)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "G",
                                color = Color.Black,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(10.dp))
                        
                        Row {
                            Text(
                                text = "Ghost ",
                                color = Color.White,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "Store",
                                color = GhostSecondary,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Terms Icon Button (Subtle, sleek)
                        IconButton(onClick = { navController.navigate("terms") }) {
                            Icon(
                                imageVector = Icons.Outlined.Gavel,
                                contentDescription = "Terms & Conditions",
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        // Notification Icon Button
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(GhostSurface)
                                .clickable {
                                    Toast.makeText(context, "No unread notifications", Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = GhostPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Top header Upload button
                        Button(
                            onClick = {
                                openTelegramChannel(context)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GhostPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(22.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier
                                .height(42.dp)
                                .testTag("upload_apk_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Upload icon",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Upload",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Modern Seek/Search Bar
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = {
                        Text(
                            text = "Search apps & games...",
                            color = GhostTextSecondary,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Icon",
                            tint = GhostTextSecondary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear Search",
                                    tint = GhostTextSecondary
                                )
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = GhostSurface,
                        unfocusedContainerColor = GhostSurface,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = GhostPrimary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .border(BorderStroke(1.dp, GhostBorder), RoundedCornerShape(16.dp))
                        .testTag("search_bar"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Custom category filter list
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { categoryName ->
                        val isSelected = selectedCategory.trim() == categoryName.trim()
                        val colorBorder = if (isSelected) GhostPrimary.copy(alpha = 0.3f) else GhostBorder
                        val colorBackground = if (isSelected) GhostPrimary.copy(alpha = 0.2f) else GhostSurface
                        val textColor = if (isSelected) GhostPrimary else Color(0xFFD1D5DB)
                        
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(colorBackground)
                                .border(BorderStroke(1.dp, colorBorder), CircleShape)
                                .clickable {
                                    viewModel.selectCategory(categoryName)
                                }
                                .padding(horizontal = 16.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = categoryName,
                                color = textColor,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = GhostBorder, thickness = 1.dp)
                NavigationBar(
                    containerColor = GhostSurfaceVariant,
                    tonalElevation = 0.dp,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    NavigationBarItem(
                        selected = selectedTab == "home" && selectedCategory == "All",
                        onClick = {
                            selectedTab = "home"
                            viewModel.selectCategory("All")
                        },
                        icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home tab") },
                        label = { Text("HOME", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GhostPrimary,
                            selectedTextColor = GhostPrimary,
                            indicatorColor = GhostPrimary.copy(alpha = 0.1f),
                            unselectedIconColor = Color.White.copy(alpha = 0.4f),
                            unselectedTextColor = Color.White.copy(alpha = 0.4f)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedCategory.trim().equals("Games", ignoreCase = true),
                        onClick = {
                            selectedTab = "games"
                            viewModel.selectCategory("Games")
                        },
                        icon = { Icon(imageVector = Icons.Default.SportsEsports, contentDescription = "Games tab") },
                        label = { Text("GAMES", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GhostPrimary,
                            selectedTextColor = GhostPrimary,
                            indicatorColor = GhostPrimary.copy(alpha = 0.1f),
                            unselectedIconColor = Color.White.copy(alpha = 0.4f),
                            unselectedTextColor = Color.White.copy(alpha = 0.4f)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == "apps" && !selectedCategory.trim().equals("Games", ignoreCase = true) && selectedCategory != "All",
                        onClick = {
                            selectedTab = "apps"
                            viewModel.selectCategory("Tools")
                        },
                        icon = { Icon(imageVector = Icons.Default.Apps, contentDescription = "Apps tab") },
                        label = { Text("APPS", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GhostPrimary,
                            selectedTextColor = GhostPrimary,
                            indicatorColor = GhostPrimary.copy(alpha = 0.1f),
                            unselectedIconColor = Color.White.copy(alpha = 0.4f),
                            unselectedTextColor = Color.White.copy(alpha = 0.4f)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == "trending",
                        onClick = {
                            selectedTab = "trending"
                            viewModel.selectCategory("All")
                            Toast.makeText(context, "Sorting APKs by Global Rating", Toast.LENGTH_SHORT).show()
                        },
                        icon = { Icon(imageVector = Icons.Default.TrendingUp, contentDescription = "Trending tab") },
                        label = { Text("TRENDING", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GhostPrimary,
                            selectedTextColor = GhostPrimary,
                            indicatorColor = GhostPrimary.copy(alpha = 0.1f),
                            unselectedIconColor = Color.White.copy(alpha = 0.4f),
                            unselectedTextColor = Color.White.copy(alpha = 0.4f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is StoreUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GhostPrimary)
                    }
                }
                is StoreUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Error Connecting",
                            tint = GhostError,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.message,
                            color = Color.White,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.fetchApks() },
                            colors = ButtonDefaults.buttonColors(containerColor = GhostPrimary)
                        ) {
                            Text("Retry Connection")
                        }
                    }
                }
                is StoreUiState.Success -> {
                    // Decide if trending selected, sorting success results by rating
                    val displayedApks = if (selectedTab == "trending") {
                        filteredApks.sortedByDescending { it.rating.toDoubleOrNull() ?: 0.0 }
                    } else {
                        filteredApks
                    }

                    if (displayedApks.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = "No items matched",
                                tint = GhostTextSecondary,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No App Store items found",
                                color = GhostTextSecondary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try modifying search or category filters",
                                color = GhostTextSecondary.copy(alpha = 0.7f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            // Featured Banner Carousel Header
                            // Only show if search query is empty and "All" or "Games" category selected
                            if (searchQuery.isEmpty() && selectedCategory == "All") {
                                item {
                                    HomeFeaturedBannerSection(
                                        apks = state.apks.take(3),
                                        onApkSelect = { navController.navigate("details/${it.name}") }
                                    )
                                }
                                
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Trending Apps",
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "View all",
                                            color = GhostSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable {
                                                Toast.makeText(context, "Viewing all applications", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }

                            items(displayedApks) { item ->
                                ApkItemStoreRowCard(
                                    item = item,
                                    onItemClick = { navController.navigate("details/${item.name}") },
                                    downloader = downloader,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================================================
// FEATURED BANNER SECTION
// ==================================================
@Composable
fun HomeFeaturedBannerSection(apks: List<ApkItem>, onApkSelect: (ApkItem) -> Unit) {
    if (apks.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Featured Releases",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 10.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(apks) { apk ->
                Box(
                    modifier = Modifier
                        .width(312.dp)
                        .height(170.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.horizontalGradient(colors = listOf(GhostPrimary, GhostTertiary)))
                        .clickable { onApkSelect(apk) }
                ) {
                    // Banner background image
                    AsyncImage(
                        model = apk.banner,
                        contentDescription = "Featured Banner background",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Shaded Vignette overlay to keep text readable
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f)),
                                    startY = 60f
                                )
                            )
                    )

                    // Text contents & GET Button Row
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GhostSurface)
                            ) {
                                AsyncImage(
                                    model = apk.icon,
                                    contentDescription = "${apk.name} Small Icon",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(10.dp))
                            
                            Column {
                                Text(
                                    text = apk.name,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${apk.developer} • ${apk.category}",
                                    color = GhostSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // GET Pill Button (matching bg-white text-black in spec)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .clickable { onApkSelect(apk) }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "GET",
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Floating banner tag top right
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(GhostPrimary.copy(alpha = 0.85f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "PREMIUM",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

// ==================================================
// APK ROW CARD COMPOSE
// ==================================================
@Composable
fun ApkItemStoreRowCard(
    item: ApkItem,
    onItemClick: () -> Unit,
    downloader: ApkDownloader,
    viewModel: StoreViewModel
) {
    val context = LocalContext.current
    val filename = viewModel.getApkFilename(item)
    
    // Check if fully downloaded locally
    var isDownloaded by remember(filename) {
        mutableStateOf(downloader.isApkDownloaded(filename))
    }
    var isDownloading by remember(filename) {
        mutableStateOf(downloader.isApkDownloading(filename))
    }

    // Dynamic state updates when clicked or changed
    LaunchedEffect(key1 = filename) {
        while (true) {
            val status = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                downloader.isApkDownloaded(filename)
            }
            val downloading = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                downloader.isApkDownloading(filename)
            }
            if (isDownloaded != status) {
                isDownloaded = status
            }
            if (isDownloading != downloading) {
                isDownloading = downloading
            }
            delay(1000) // check occasionally to update list UI live
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onItemClick() }
            .testTag("apk_card_${item.name.replace(" ", "_")}"),
        colors = CardDefaults.cardColors(containerColor = GhostSurface),
        border = BorderStroke(1.dp, GhostBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rounded App Icon with diagonal gradient background and thin white/10 border
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF374151), Color(0xFF1F2937))
                        )
                    )
                    .border(BorderStroke(1.dp, Color(0x1AFFFFFF)), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = item.icon,
                    contentDescription = "${item.name} symbol launcher",
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Info Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = item.developer,
                    color = GhostTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Stats row (Size + Custom version badge)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Rating Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GhostBorder)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating star symbol",
                            tint = GhostRatingGold,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = item.rating,
                            color = GhostRatingGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Size text
                    Text(
                        text = item.size,
                        color = GhostTextSecondary,
                        fontSize = 11.sp
                    )

                    // Bullet circle separator
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(GhostTextSecondary.copy(alpha = 0.5f))
                    )

                    // Version text
                    Text(
                        text = "v${item.version}",
                        color = GhostSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Action Quick Download/Install button
            if (isDownloaded) {
                Button(
                    onClick = {
                        downloader.installApk(filename)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GhostPrimary,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("INSTALL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            } else if (isDownloading) {
                Button(
                    onClick = {},
                    enabled = false,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GhostSurfaceVariant,
                        contentColor = GhostTextSecondary,
                        disabledContainerColor = GhostSurfaceVariant,
                        disabledContentColor = GhostTextSecondary
                    ),
                    border = BorderStroke(1.dp, GhostBorder),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("DOWNLOADING...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        downloader.downloadApk(item.cleanedDownloadUrl, filename) { downloadId ->
                            viewModel.registerDownload(downloadId, item.name)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GhostBackground,
                        contentColor = GhostSecondary
                    ),
                    border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("DOWNLOAD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==================================================
// DETAILS SCREEN
// ==================================================
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    apkName: String,
    navController: NavController,
    viewModel: StoreViewModel,
    downloader: ApkDownloader
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val apkItem = when (val state = uiState) {
        is StoreUiState.Success -> state.apks.find { it.name.trim() == apkName.trim() }
        else -> null
    }

    if (apkItem == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GhostBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("App data details not loaded", color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { navController.popBackStack() }) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    val filename = viewModel.getApkFilename(apkItem)
    var isDownloaded by remember(filename) {
        mutableStateOf(downloader.isApkDownloaded(filename))
    }
    var isDownloading by remember(filename) {
        mutableStateOf(downloader.isApkDownloading(filename))
    }

    LaunchedEffect(key1 = filename) {
        while (true) {
            val status = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                downloader.isApkDownloaded(filename)
            }
            val downloading = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                downloader.isApkDownloading(filename)
            }
            if (isDownloaded != status) {
                isDownloaded = status
            }
            if (isDownloading != downloading) {
                isDownloading = downloading
            }
            delay(1000)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = GhostBackground,
        topBar = {
            TopAppBar(
                title = { Text(text = "App Details", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Return home")
                    }
                },
                actions = {
                    IconButton(onClick = { openTelegramChannel(context) }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share App Dev", tint = GhostSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GhostBackground,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = GhostSecondary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(GhostBackground)
        ) {
            // Scrollable specifications column
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                // Main Header Section with Big App Icon and description
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Big App Icon
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF374151), Color(0xFF1F2937))
                                    )
                                )
                                .border(BorderStroke(1.dp, Color(0x1AFFFFFF)), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = apkItem.icon,
                                contentDescription = "${apkItem.name} logo image",
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = apkItem.name,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = apkItem.developer,
                                color = GhostSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = "Category: ${apkItem.category}",
                                color = GhostTextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                // Grid stats info row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(GhostSurface)
                            .border(BorderStroke(1.dp, GhostBorder.copy(alpha = 0.5f)), RoundedCornerShape(16.dp))
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = apkItem.rating,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Rating Star",
                                    tint = GhostRatingGold,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(text = "Rating", color = GhostTextSecondary, fontSize = 11.sp)
                        }

                        // Divider line
                        Divider(
                            color = GhostBorder,
                            modifier = Modifier
                                .height(32.dp)
                                .width(1.dp)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = apkItem.size,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(text = "APK Size", color = GhostTextSecondary, fontSize = 11.sp)
                        }

                        // Divider line
                        Divider(
                            color = GhostBorder,
                            modifier = Modifier
                                .height(32.dp)
                                .width(1.dp)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = apkItem.version,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(text = "Version", color = GhostTextSecondary, fontSize = 11.sp)
                        }
                    }
                }

                // SCREENSHOT DETAILS
                item {
                    Text(
                        text = "Screenshots",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                    )

                    val screenshotsList = apkItem.screenshots
                    if (screenshotsList.isNullOrEmpty()) {
                        // Fallback simple mock screenshots card with banner
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .width(180.dp)
                                        .height(320.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(GhostSurface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = apkItem.banner,
                                        contentDescription = "Fallback screenshot",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(screenshotsList) { ssUrl ->
                                Card(
                                    modifier = Modifier
                                        .width(180.dp)
                                        .height(300.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    AsyncImage(
                                        model = ssUrl,
                                        contentDescription = "App Screenshot Detail view",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }

                // Description Box
                item {
                    Text(
                        text = "About this application",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 24.dp, bottom = 10.dp)
                    )
                    
                    Text(
                        text = apkItem.description,
                        color = GhostTextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // Developer note info section
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(GhostPrimary.copy(alpha = 0.08f))
                            .border(BorderStroke(1.dp, GhostPrimary.copy(alpha = 0.3f)), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Note",
                                tint = GhostPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Ghost Distribution System",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "This APK file is distributed securely. Updates are polled in real-time. If you find any issues, please submit feedback.",
                                    color = GhostTextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // Bottom full-bleed CTA Button Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GhostSurface)
                    .border(BorderStroke(1.dp, GhostBorder), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .padding(16.dp)
            ) {
                if (isDownloaded) {
                    Button(
                        onClick = { downloader.installApk(filename) },
                        colors = ButtonDefaults.buttonColors(containerColor = GhostSuccess),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("install_button")
                    ) {
                        Icon(imageVector = Icons.Default.CloudDone, contentDescription = "Done install")
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "INSTALL PACKAGE",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (isDownloading) {
                    Button(
                        onClick = {},
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GhostSurfaceVariant,
                            contentColor = GhostTextSecondary,
                            disabledContainerColor = GhostSurfaceVariant,
                            disabledContentColor = GhostTextSecondary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("downloading_button")
                    ) {
                        Icon(imageVector = Icons.Default.HourglassEmpty, contentDescription = "Downloading status indicator")
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "DOWNLOADING PACKAGE...",
                            color = GhostTextSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            downloader.downloadApk(apkItem.cleanedDownloadUrl, filename) { downloadId ->
                                viewModel.registerDownload(downloadId, apkItem.name)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GhostPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("download_button")
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = "Download start")
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "DOWNLOAD APK (${apkItem.size})",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ==================================================
// TERMS & CONDITIONS SCREEN
// ==================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsScreen(navController: NavController) {
    val context = LocalContext.current
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = GhostBackground,
        topBar = {
            TopAppBar(
                title = { Text("Terms & Conditions") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Go back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GhostBackground,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Legal Agreement & Disclaimer",
                    color = GhostSecondary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                Text(
                    text = "By referencing, downloading, or using any packages or services distributed via Ghost Store, you accept these terms in full. Under no circumstances shall Ghost Store or its developers be held liable for any data loss, device issues, hardware bricking, or system instability resulting from downloaded local executables.",
                    color = GhostTextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }

            item {
                Text(
                    text = "1. Content Distribution",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "All APK binaries are provided dynamically using external direct links. Ghost Store acts solely as an indexing platform. We are not responsible for hosting files or controlling content modifications performed by application developers.",
                    color = GhostTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                Text(
                    text = "2. Security & User Liability",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Users remain solely liable for testing. Enable \"Install Unknown Apps\" strictly at your own discretion. Please verify APK hash sums if you require secure authentication validation.",
                    color = GhostTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                Text(
                    text = "3. Submissions & Auditing",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ghost Store does not support general public app uploads. Upload requests are processed strictly by submitting code links to our verified Telegram publisher group. If an indexed entry belongs to you and you require its urgent retrieval or removal, please contact the developer via our support channel immediately.",
                    color = GhostTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { openTelegramChannel(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = GhostPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("terms_contact_button")
                ) {
                    Icon(imageVector = Icons.Default.Forum, contentDescription = "Forum chat")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CONTACT SAGARTECH ON TELEGRAM")
                }
            }
        }
    }
}

/**
 * Clean helper function to trigger Telegram redirection.
 */
private fun openTelegramChannel(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(TELEGRAM_URL)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Telegram app not found. Accessing via web browser.", Toast.LENGTH_SHORT).show()
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(TELEGRAM_URL))
        context.startActivity(webIntent)
    }
}
