package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.os.Build
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.data.*
import com.example.data.db.*
import com.example.viewmodel.MangaViewModel
import com.example.viewmodel.Screen
import com.example.viewmodel.UiState
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                NefuSoftApp()
            }
        }
    }
}

@Composable
fun NefuSoftApp(viewModel: MangaViewModel = viewModel()) {
    val context = LocalContext.current
    BackHandler {
        if (!viewModel.navigateBack()) {
            (context as? ComponentActivity)?.finish()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        bottomBar = {
            if (viewModel.currentScreen is Screen.Home) {
                NavigationBar(
                    containerColor = Color(0xFF070707),
                    modifier = Modifier.navigationBarsPadding(),
                    tonalElevation = 0.dp
                ) {
                    val navItems = listOf(
                        Triple(0, "Beranda", Icons.Default.Home),
                        Triple(1, "Jelajah", Icons.Default.Search),
                        Triple(2, "Favorit", Icons.Default.Favorite),
                        Triple(3, "Riwayat", Icons.Default.Refresh)
                    )
                    navItems.forEach { (index, label, icon) ->
                        val selected = viewModel.selectedBottomTab == index
                        NavigationBarItem(
                            selected = selected,
                            onClick = { viewModel.setBottomTab(index) },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (selected) Color.White else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) Color.White else Color.Gray
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color(0xFF1E1E1E)
                            )
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = Color.Black
        ) {
            AnimatedContent(
                targetState = viewModel.currentScreen,
                transitionSpec = {
                    val slideIn = slideInHorizontally(
                        initialOffsetX = { fullWidth -> (fullWidth * 0.15f).toInt() },
                        animationSpec = tween(320, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(260))
                    val slideOut = slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -(fullWidth * 0.15f).toInt() },
                        animationSpec = tween(320, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(260))
                    slideIn togetherWith slideOut
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    is Screen.Home -> HomeScreen(viewModel)
                    is Screen.Detail -> DetailScreen(screen.id, screen.title, screen.img, viewModel)
                    is Screen.Reader -> ReaderScreen(
                        screen.id,
                        screen.title,
                        screen.mangaId,
                        screen.mangaTitle,
                        screen.img,
                        viewModel
                    )
                    is Screen.TopicResult -> TopicResultScreen(screen.term, screen.value, screen.title, viewModel)
                }
            }
        }
    }
}

@Composable
fun shimmerBrush(showShimmer: Boolean = true): Brush {
    return if (showShimmer) {
        val transition = rememberInfiniteTransition(label = "shimmerTransition")
        val translateAnimation by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmerTranslate"
        )
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF070707),
                Color(0xFF161616),
                Color(0xFF070707)
            ),
            start = Offset(translateAnimation, translateAnimation),
            end = Offset(translateAnimation + 180f, translateAnimation + 180f)
        )
    } else {
        SolidColor(Color.Transparent)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(viewModel: MangaViewModel) {
    when (viewModel.selectedBottomTab) {
        0 -> BerandaSubTab(viewModel)
        1 -> JelajahSubTab(viewModel)
        2 -> FavoritSubTab(viewModel)
        3 -> RiwayatSubTab(viewModel)
    }
}

// 1. BERANDA (HOME) SCREEN WITH ROW-SCROLL SECTIONS & GRAPHIC PROMO BANNER
@Composable
fun BerandaSubTab(viewModel: MangaViewModel) {
    val latestState by viewModel.latestListState.collectAsStateWithLifecycle()
    val popularState by viewModel.popularListState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val gifImageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    val greeting = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Selamat Pagi 🌅"
            in 12..15 -> "Selamat Siang ☀️"
            in 16..18 -> "Selamat Sore 🌇"
            else -> "Selamat Malam 🌙"
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(R.drawable.nefusoft_logo)
                    .crossfade(true)
                    .build(),
                contentDescription = "Nee-Manga Logo",
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(0.5.dp, Color(0xFF1E1E1E), RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Nee-Manga",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // welcoming dynamic greeting
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = greeting,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Mau baca komik seru apa hari ini?",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // PROMO HERO GRAPHIC GIF BANNER
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .height(175.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.5.dp, Color(0xFF1E1E1E))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data("https://raw.githubusercontent.com/alip-jmbd/alipp/main/3228db284a435b9e3562e8f4a82e5351.gif")
                                .crossfade(true)
                                .build(),
                            imageLoader = gifImageLoader,
                            contentDescription = "Hot Promo Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color(0xDD000000))
                                    )
                                )
                        )
                        Text(
                            text = "⚡ REKOMENDASI UTAMA",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFF4E50),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 12.dp, bottom = 28.dp)
                        )
                        Text(
                            text = "Buka Pintu Petualangan Tanpa Batas!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                        )
                    }
                }
            }

            // QUICK NAVIGATION CATEGORIES
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Kategori Terpopuler",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
                val quickTags = listOf(
                    Triple("🔥 Manga", "type", "manga"),
                    Triple("⚡ Manhwa", "type", "manhwa"),
                    Triple("✨ Manhua", "type", "manhua"),
                    Triple("🎨 Berwarna", "colorized", "colorized"),
                    Triple("⚔️ Action", "genres", "action"),
                    Triple("🎭 Comedy", "genres", "comedy")
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickTags) { (label, term, value) ->
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF0F0F0F), RoundedCornerShape(12.dp))
                                .border(0.5.dp, Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.navigateTo(Screen.TopicResult(term, value, label))
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // HOT POPULER ROW
            item {
                SectionHeaderRow(title = "Komik Populer", onSeeAll = {
                    viewModel.navigateTo(Screen.TopicResult("terpopuler", "populer", "Komik Populer"))
                })
                when (val state = popularState) {
                    is UiState.Loading -> HorizontalShimmerRow()
                    is UiState.Success -> {
                        if (state.data.isEmpty()) {
                            NoDataRow()
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(state.data.take(15)) { item ->
                                    MangaRowCard(item) {
                                        viewModel.navigateTo(Screen.Detail(item.extractId(), item.title, item.img))
                                    }
                                }
                            }
                        }
                    }
                    is UiState.Error -> ErrorRow(state.message) { viewModel.loadPopularList() }
                    else -> {}
                }
            }

            // BARU RILIS ROW
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeaderRow(title = "Komik Terbaru", onSeeAll = {
                    viewModel.navigateTo(Screen.TopicResult("latest", "latest", "Update Terbaru"))
                })
                when (val state = latestState) {
                    is UiState.Loading -> HorizontalShimmerRow()
                    is UiState.Success -> {
                        if (state.data.isEmpty()) {
                            NoDataRow()
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(state.data.take(15)) { item ->
                                    MangaRowCard(item) {
                                        viewModel.navigateTo(Screen.Detail(item.extractId(), item.title, item.img))
                                    }
                                }
                            }
                        }
                    }
                    is UiState.Error -> ErrorRow(state.message) { viewModel.loadLatestList() }
                    else -> {}
                }
            }
        }
    }
}

// 2. JELAJAH / FIND TAB WITH FILTERING CHIPS & LIVE DEBOUNCED SEARCH RESULTS
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JelajahSubTab(viewModel: MangaViewModel) {
    val searchState by viewModel.searchListState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    val genresList = listOf(
        "Action" to "action",
        "Adventure" to "adventure",
        "Comedy" to "comedy",
        "Drama" to "drama",
        "Fantasy" to "fantasy",
        "Isekai" to "isekai",
        "Mystery" to "mystery",
        "Romance" to "romance",
        "Sci-Fi" to "sci-fi",
        "Slice of Life" to "slice-of-life",
        "Supernatural" to "supernatural",
        "Thriller" to "thriller",
        "School" to "school",
        "Shounen" to "shounen",
        "Seinen" to "seinen"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Debounced Search Bar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = { Text("Ketik judul komik... (Live Search)", fontSize = 13.sp, color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF0F0F0F),
                    unfocusedContainerColor = Color(0xFF0F0F0F),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    viewModel.executeSearch()
                    focusManager.clearFocus()
                }),
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (viewModel.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            )
        }

        if (viewModel.searchQuery.isNotEmpty()) {
            // Live Search Results (Grid display with pagination buttons)
            Box(modifier = Modifier.weight(1f)) {
                when (val state = searchState) {
                    is UiState.Loading -> LoadingGrid()
                    is UiState.Success -> {
                        if (state.data.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Komik tidak ditemukan", color = Color.Gray, fontSize = 13.sp)
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    contentPadding = PaddingValues(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(state.data) { item ->
                                        MangaGridCard(item) {
                                            viewModel.navigateTo(Screen.Detail(item.extractId(), item.title, item.img))
                                        }
                                    }
                                }
                                PaginationControl(
                                    page = viewModel.searchPage,
                                    onPrev = { viewModel.prevSearchPage() },
                                    onNext = { viewModel.nextSearchPage() }
                                )
                            }
                        }
                    }
                    is UiState.Error -> {
                        ErrorState(state.message) { viewModel.loadSearchResults() }
                    }
                    else -> {}
                }
            }
        } else {
            // Browse Index (Filter Cards and Genre chips)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 24.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Kategori & Tipe",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CategoryIndexCard("MANGA", Modifier.weight(1f), Color(0xFF1E3C72)) {
                            viewModel.navigateTo(Screen.TopicResult("type", "manga", "Daftar Manga"))
                        }
                        CategoryIndexCard("MANHWA", Modifier.weight(1f), Color(0xFF2A5298)) {
                            viewModel.navigateTo(Screen.TopicResult("type", "manhwa", "Daftar Manhwa"))
                        }
                        CategoryIndexCard("MANHUA", Modifier.weight(1f), Color(0xFF536976)) {
                            viewModel.navigateTo(Screen.TopicResult("type", "manhua", "Daftar Manhua"))
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Edisi Warna",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CategoryIndexCard("BERWARNA", Modifier.weight(1f), Color(0xFF8E2DE2)) {
                            viewModel.navigateTo(Screen.TopicResult("colorized", "colorized", "Edisi Berwarna"))
                        }
                        CategoryIndexCard("HITAM PUTIH", Modifier.weight(1f), Color(0xFF3E5151)) {
                            viewModel.navigateTo(Screen.TopicResult("colorized", "bnw", "Edisi Hitam Putih"))
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Eksplor Genre Komik",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        genresList.forEach { (name, value) ->
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF0F0F0F), RoundedCornerShape(16.dp))
                                    .border(0.5.dp, Color(0xFF1C1C1C), RoundedCornerShape(16.dp))
                                    .clickable {
                                        viewModel.navigateTo(Screen.TopicResult("genres", value, "Genre: $name"))
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = name,
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 3. FAVORIT SUB TAB
@Composable
fun FavoritSubTab(viewModel: MangaViewModel) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle(initialValue = emptyList())

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Komik Favorit",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
        }

        if (favorites.isEmpty()) {
            EmptyState(
                icon = Icons.Default.FavoriteBorder,
                text = "Belum ada komik favorit\nKetuk ikon hati di halaman detail komik"
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(favorites) { fav ->
                    val fakeItem = MangaItem(
                        title = fav.title,
                        url = "id=${fav.id}",
                        img = fav.img,
                        type = fav.type,
                        views = null,
                        score = fav.score,
                        status = fav.status,
                        colorized = null,
                        data = null
                    )
                    MangaGridCard(fakeItem) {
                        viewModel.navigateTo(Screen.Detail(fav.id, fav.title, fav.img))
                    }
                }
            }
        }
    }
}

// 4. RIWAYAT (HISTORY) SUB TAB
@Composable
fun RiwayatSubTab(viewModel: MangaViewModel) {
    val history by viewModel.history.collectAsStateWithLifecycle(initialValue = emptyList())

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Riwayat Baca",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            if (history.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearHistory() }) {
                    Text("Hapus Semua", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (history.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Refresh,
                text = "Belum ada riwayat baca\nMulai membaca pilihan komikmu"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 16.dp)
            ) {
                items(history) { record ->
                    HistoryRowItem(record, onDelete = {
                        viewModel.deleteHistoryItem(record.id)
                    }, onClick = {
                        viewModel.navigateTo(Screen.Detail(record.id, record.mangaTitle, record.img))
                    })
                }
            }
        }
    }
}

// COMPOSE UTILITY VIEWS
@Composable
fun SectionHeaderRow(title: String, onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = onSeeAll)
        ) {
            Text(
                text = "Lihat Semua",
                fontSize = 11.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "See All",
                tint = Color.Gray,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun CategoryIndexCard(label: String, modifier: Modifier, background: Color, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun MangaRowCard(item: MangaItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(135.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(135.dp)
                .aspectRatio(0.70f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF101010))
                .border(0.5.dp, Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.img)
                    .crossfade(true)
                    .build(),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        val textChapter = item.data?.chapter ?: ""
        if (textChapter.isNotBlank()) {
            Text(
                text = "Ch ${textChapter}",
                fontSize = 10.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun HorizontalShimmerRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(4) {
            Column(modifier = Modifier.width(135.dp)) {
                Box(
                    modifier = Modifier
                        .width(135.dp)
                        .aspectRatio(0.70f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF101010))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(12.dp)
                        .background(Color(0xFF151515))
                )
            }
        }
    }
}

@Composable
fun NoDataRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Daftar komik kosong.", color = Color.Gray, fontSize = 11.sp)
    }
}

@Composable
fun ErrorRow(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Format Error / Gagal terhubung.", color = Color.Gray, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(6.dp))
            TextButton(
                onClick = onRetry,
                modifier = Modifier.height(28.dp),
                border = BorderStroke(0.5.dp, Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Coba Lagi", color = Color.White, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF161616),
                modifier = Modifier.size(60.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun PaginationControl(page: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPrev,
            enabled = page > 1,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowLeft,
                contentDescription = "Sebelumnya",
                tint = if (page > 1) Color.White else Color.DarkGray
            )
        }

        Text(
            text = "Halaman $page",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color.LightGray
        )

        IconButton(
            onClick = onNext,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Berikutnya",
                tint = Color.White
            )
        }
    }
}

@Composable
fun LoadingGrid() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(12) {
            ShimmerCard()
        }
    }
}

@Composable
fun ShimmerCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.70f)
                .background(shimmerBrush())
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(12.dp)
                .background(shimmerBrush())
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(10.dp)
                .background(shimmerBrush())
        )
    }
}

@Composable
fun MangaGridCard(item: MangaItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.70f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF101010))
                .border(0.5.dp, Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.img)
                    .crossfade(true)
                    .build(),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Score Badge
            if (!item.score.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .background(Color(0xE6000000), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = item.score,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 15.sp
        )

        val textChapter = item.data?.chapter ?: ""
        if (textChapter.isNotBlank()) {
            Text(
                text = "Ch ${textChapter}",
                fontSize = 10.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun HistoryRowItem(record: HistoryMangaEntity, onDelete: () -> Unit, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val context = LocalContext.current
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(record.img)
                .crossfade(true)
                .build(),
            contentDescription = record.mangaTitle,
            modifier = Modifier
                .size(width = 46.dp, height = 64.dp)
                .clip(RoundedCornerShape(4.dp))
                .border(0.5.dp, Color(0xFF1E1E1E), RoundedCornerShape(4.dp))
                .background(Color(0xFF0F0F0F)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.mangaTitle,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Terakhir baca: ${record.chapterTitle}",
                fontSize = 11.sp,
                color = Color.LightGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            val timeLabel = android.text.format.DateUtils.getRelativeTimeSpanString(
                record.timestamp,
                System.currentTimeMillis(),
                android.text.format.DateUtils.MINUTE_IN_MILLIS
            ).toString()

            Text(
                text = timeLabel,
                fontSize = 10.sp,
                color = Color.Gray
            )
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Hapus Riwayat",
                tint = Color.Gray,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    id: String,
    title: String,
    img: String?,
    viewModel: MangaViewModel
) {
    val detailState by viewModel.detailState.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite(id).collectAsStateWithLifecycle(initialValue = false)

    LaunchedEffect(id) {
        viewModel.loadMangaDetail(id)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Navigation Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateBack() }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
            }

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "Informasi Komik",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            // Bookmark Action
            if (detailState is UiState.Success) {
                val detail = (detailState as UiState.Success<MangaDetail>).data
                IconButton(
                    onClick = {
                        viewModel.toggleFavorite(
                            id = id,
                            title = detail.title,
                            img = detail.img ?: img,
                            type = detail.type,
                            score = detail.score,
                            status = detail.status
                        )
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Simpan",
                        tint = Color.White
                    )
                }
            }
        }

        when (val state = detailState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                }
            }
            is UiState.Success -> {
                val detail = state.data
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    item {
                        DetailHeaderSection(detail, img)
                    }

                    item {
                        val chapters = detail.data ?: emptyList()
                        val firstCh = chapters.lastOrNull()
                        val lastCh = chapters.firstOrNull()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    firstCh?.let { ch ->
                                        viewModel.navigateTo(
                                            Screen.Reader(
                                                id = ch.extractId(),
                                                title = "${detail.title} - Ch ${ch.chapter}",
                                                mangaId = id,
                                                mangaTitle = detail.title,
                                                img = detail.img ?: img
                                            )
                                        )
                                    }
                                },
                                enabled = firstCh != null,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E1E1E),
                                    disabledContainerColor = Color(0xFF0F0F0F)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Baca Chapter 1",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    lastCh?.let { ch ->
                                        viewModel.navigateTo(
                                            Screen.Reader(
                                                id = ch.extractId(),
                                                title = "${detail.title} - Ch ${ch.chapter}",
                                                mangaId = id,
                                                mangaTitle = detail.title,
                                                img = detail.img ?: img
                                            )
                                        )
                                    }
                                },
                                enabled = lastCh != null,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF4E50),
                                    disabledContainerColor = Color(0xFF0F0F0F)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Baca Terakhir",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Sinopsis",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = detail.synopsis ?: "Tidak ada sinopsis.",
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            lineHeight = 18.sp
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Daftar Chapter",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    val chapters = detail.data ?: emptyList()
                    if (chapters.isEmpty()) {
                        item {
                            Text(
                                text = "Tidak ada chapter tersedia.",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    } else {
                        items(chapters) { ch ->
                            ChapterRowItem(ch) {
                                viewModel.navigateTo(
                                    Screen.Reader(
                                        id = ch.extractId(),
                                        title = "${detail.title} - ${ch.chapter}",
                                        mangaId = id,
                                        mangaTitle = detail.title,
                                        img = detail.img ?: img
                                    )
                                )
                            }
                        }
                    }
                }
            }
            is UiState.Error -> {
                ErrorState(state.message) { viewModel.loadMangaDetail(id) }
            }
            else -> {}
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailHeaderSection(detail: MangaDetail, fallbackImg: String?) {
    Row(modifier = Modifier.fillMaxWidth()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(detail.img ?: fallbackImg)
                .crossfade(true)
                .build(),
            contentDescription = detail.title,
            modifier = Modifier
                .width(110.dp)
                .aspectRatio(0.70f)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF0A0A0A))
                .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = detail.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (!detail.score.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${detail.score} / 10", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Text(text = "Status: ${detail.status ?: "Ongoing"}", fontSize = 11.sp, color = Color.Gray)
            Text(text = "Tipe: ${detail.type ?: "Manga"}", fontSize = 11.sp, color = Color.Gray)
            val release = detail.released ?: ""
            if (release.isNotBlank()) {
                Text(text = "Rilis: $release", fontSize = 11.sp, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                detail.genre?.take(4)?.forEach { g ->
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF101010), RoundedCornerShape(3.dp))
                            .border(0.5.dp, Color(0xFF1C1C1C), RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(text = g.name, color = Color.LightGray, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ChapterRowItem(ch: ChapterItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(0.5.dp, Color(0xFF1A1A1A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Chapter " + ch.chapter,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Buka lembaran bab ini",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Mulai membaca",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// IMMERSIVE READER VIEW WITH INFORMATIVE HEADER & CONVENIENT REACTION HOTKEYS
@Composable
fun ReaderScreen(
    id: String,
    title: String,
    mangaId: String,
    mangaTitle: String,
    img: String?,
    viewModel: MangaViewModel
) {
    val readerState by viewModel.readerState.collectAsStateWithLifecycle()
    val scrollState = rememberLazyListState()
    var systemUiVisible by remember { mutableStateOf(true) }

    LaunchedEffect(id) {
        viewModel.loadChapter(id, mangaId, mangaTitle, img)
        scrollState.scrollToItem(0)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when (val state = readerState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                }
            }
            is UiState.Success -> {
                val chapter = state.data
                val pages = chapter.image ?: emptyList()

                if (pages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Lembaran komik kosong atau gagal diposkan.", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                systemUiVisible = !systemUiVisible
                            }
                    ) {
                        items(pages) { imgUrl ->
                            var pageLoading by remember { mutableStateOf(true) }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .background(Color.Black)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(imgUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Halaman Komik",
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier.fillMaxWidth(),
                                    onSuccess = { pageLoading = false },
                                    onError = { pageLoading = false }
                                )

                                if (pageLoading) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(400.dp)
                                            .background(shimmerBrush())
                                    )
                                }
                            }
                        }

                        // Bottom navigation helper buttons inside reader
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp, horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Bab Selesai Diulas",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    val previousChapterAvailable = !chapter.prev.isNullOrBlank()
                                    val nextChapterAvailable = !chapter.next.isNullOrBlank()

                                    TextButton(
                                        onClick = {
                                            chapter.prev?.let { prevUrl ->
                                                val prevId = extractIdFromUrl(prevUrl)
                                                viewModel.navigateTo(
                                                    Screen.Reader(
                                                        id = prevId,
                                                        title = "${mangaTitle} - Ch " + (chapter.chapter?.toIntOrNull()?.minus(1)?.toString() ?: "?"),
                                                        mangaId = mangaId,
                                                        mangaTitle = mangaTitle,
                                                        img = img
                                                    )
                                                )
                                            }
                                        },
                                        enabled = previousChapterAvailable,
                                        border = BorderStroke(0.5.dp, if (previousChapterAvailable) Color.White else Color.DarkGray),
                                        shape = RoundedCornerShape(24.dp),
                                        modifier = Modifier.width(135.dp)
                                    ) {
                                        Text(
                                            "Prev Chapter",
                                            fontSize = 11.sp,
                                            color = if (previousChapterAvailable) Color.White else Color.DarkGray
                                        )
                                    }

                                    TextButton(
                                        onClick = {
                                            chapter.next?.let { nextUrl ->
                                                val nextId = extractIdFromUrl(nextUrl)
                                                viewModel.navigateTo(
                                                    Screen.Reader(
                                                        id = nextId,
                                                        title = "${mangaTitle} - Ch " + (chapter.chapter?.toIntOrNull()?.plus(1)?.toString() ?: "?"),
                                                        mangaId = mangaId,
                                                        mangaTitle = mangaTitle,
                                                        img = img
                                                    )
                                                )
                                            }
                                        },
                                        enabled = nextChapterAvailable,
                                        border = BorderStroke(0.5.dp, if (nextChapterAvailable) Color.White else Color.DarkGray),
                                        shape = RoundedCornerShape(24.dp),
                                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                                        modifier = Modifier.width(135.dp)
                                    ) {
                                        Text(
                                            "Next Chapter",
                                            fontSize = 11.sp,
                                            color = if (nextChapterAvailable) Color.White else Color.DarkGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is UiState.Error -> {
                ErrorState(state.message) { viewModel.loadChapter(id, mangaId, mangaTitle, img) }
            }
            else -> {}
        }

        // Informative Header Controls with Next/Prev Shortcuts
        AnimatedVisibility(
            visible = systemUiVisible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xF2000000))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateBack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mangaTitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (readerState is UiState.Success) {
                        val activeCh = (readerState as UiState.Success<ChapterDetail>).data
                        Text(
                            text = "Chapter " + (activeCh.chapter ?: ""),
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                // Shortcuts for direct chapter switching in the top bar
                if (readerState is UiState.Success) {
                    val activeCh = (readerState as UiState.Success<ChapterDetail>).data
                    val hasPrev = !activeCh.prev.isNullOrBlank()
                    val hasNext = !activeCh.next.isNullOrBlank()

                    IconButton(
                        onClick = {
                            activeCh.prev?.let { prevUrl ->
                                val prevId = extractIdFromUrl(prevUrl)
                                viewModel.navigateTo(
                                    Screen.Reader(
                                        id = prevId,
                                        title = "${mangaTitle} - Ch " + (activeCh.chapter?.toIntOrNull()?.minus(1)?.toString() ?: "?"),
                                        mangaId = mangaId,
                                        mangaTitle = mangaTitle,
                                        img = img
                                    )
                                )
                            }
                        },
                        enabled = hasPrev
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Chapter Sebelumnya",
                            tint = if (hasPrev) Color.White else Color.DarkGray
                        )
                    }

                    IconButton(
                        onClick = {
                            activeCh.next?.let { nextUrl ->
                                val nextId = extractIdFromUrl(nextUrl)
                                viewModel.navigateTo(
                                    Screen.Reader(
                                        id = nextId,
                                        title = "${mangaTitle} - Ch " + (activeCh.chapter?.toIntOrNull()?.plus(1)?.toString() ?: "?"),
                                        mangaId = mangaId,
                                        mangaTitle = mangaTitle,
                                        img = img
                                    )
                                )
                            }
                        },
                        enabled = hasNext
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Chapter Selanjutnya",
                            tint = if (hasNext) Color.White else Color.DarkGray
                        )
                    }
                }
            }
        }
    }
}

// DEDICATED SCREEN FOR PAGINATED TOPICS & FILTER RESULTS
@Composable
fun TopicResultScreen(
    term: String,
    value: String,
    title: String,
    viewModel: MangaViewModel
) {
    val listState by viewModel.topicListState.collectAsStateWithLifecycle()

    LaunchedEffect(term, value) {
        viewModel.resetTopicPage()
        viewModel.loadTopicList(term, value)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateBack() }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when (val state = listState) {
                is UiState.Loading -> LoadingGrid()
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Tidak ada komik ditemukan", color = Color.Gray, fontSize = 13.sp)
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(state.data) { item ->
                                    MangaGridCard(item) {
                                        viewModel.navigateTo(Screen.Detail(item.extractId(), item.title, item.img))
                                    }
                                }
                            }
                            PaginationControl(
                                page = viewModel.topicPage,
                                onPrev = { viewModel.prevTopicPage(term, value) },
                                onNext = { viewModel.nextTopicPage(term, value) }
                            )
                        }
                    }
                }
                is UiState.Error -> {
                    ErrorState(state.message) { viewModel.loadTopicList(term, value) }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                fontSize = 12.sp,
                color = Color.LightGray,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = onRetry,
                border = BorderStroke(1.dp, Color.White),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Muat Ulang", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}
