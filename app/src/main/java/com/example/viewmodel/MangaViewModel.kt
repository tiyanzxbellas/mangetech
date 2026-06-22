package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.data.db.AppDatabase
import com.example.data.db.FavoriteMangaEntity
import com.example.data.db.HistoryMangaEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    object Loading : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface Screen {
    object Home : Screen
    data class TopicResult(val term: String, val value: String, val title: String) : Screen
    data class Detail(val id: String, val title: String, val img: String?) : Screen
    data class Reader(val id: String, val title: String, val mangaId: String, val mangaTitle: String, val img: String?) : Screen
}

class MangaViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.mangaDao()

    // Screen State
    var currentScreen by mutableStateOf<Screen>(Screen.Home)
        private set

    private val screenStack = mutableListOf<Screen>()

    fun navigateTo(screen: Screen) {
        screenStack.add(currentScreen)
        currentScreen = screen
    }

    fun navigateBack(): Boolean {
        if (screenStack.isNotEmpty()) {
            currentScreen = screenStack.removeAt(screenStack.size - 1)
            return true
        }
        return false
    }

    // Bottom Navigation Tab State (0: Beranda, 1: Jelajah/Genre, 2: Favorit, 3: Riwayat)
    var selectedBottomTab by mutableStateOf(0)
        private set

    fun setBottomTab(index: Int) {
        selectedBottomTab = index
    }

    // Data lists for the Home/Beranda Screen
    private val _latestListState = MutableStateFlow<UiState<List<MangaItem>>>(UiState.Idle)
    val latestListState: StateFlow<UiState<List<MangaItem>>> = _latestListState.asStateFlow()

    private val _popularListState = MutableStateFlow<UiState<List<MangaItem>>>(UiState.Idle)
    val popularListState: StateFlow<UiState<List<MangaItem>>> = _popularListState.asStateFlow()

    // Dedicated Paginated/Topic Screen State
    var topicPage by mutableStateOf(1)
        private set
    private val _topicListState = MutableStateFlow<UiState<List<MangaItem>>>(UiState.Idle)
    val topicListState: StateFlow<UiState<List<MangaItem>>> = _topicListState.asStateFlow()

    fun loadTopicList(term: String, value: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _topicListState.value = UiState.Loading
            try {
                val startPage = (topicPage - 1) * 4 + 1
                val deferreds = (0..3).map { offset ->
                    val apiPage = (startPage + offset).toString()
                    async {
                        try {
                            val responseBody = when (term) {
                                "latest" -> RetrofitClient.api.getHomepage(page = "latest", paged = apiPage)
                                "terpopuler" -> RetrofitClient.api.getHomepage(page = "terpopuler", paged = apiPage)
                                "genres" -> RetrofitClient.api.getTermResult(term = "genres", value = value, paged = apiPage)
                                "demographic" -> RetrofitClient.api.getTermResult(term = "demographic", value = value, paged = apiPage)
                                "theme" -> RetrofitClient.api.getTermResult(term = "theme", value = value, paged = apiPage)
                                "content" -> RetrofitClient.api.getTermResult(term = "content", value = value, paged = apiPage)
                                "type" -> RetrofitClient.api.getTypeList(type = value, paged = apiPage)
                                "colorized" -> RetrofitClient.api.getColorizedList(colorized = if (value == "colorized") "1" else "0", paged = apiPage)
                                else -> null
                            }
                            responseBody?.string()?.let { RetrofitClient.parseMangaList(it) } ?: emptyList()
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }
                }
                val results = deferreds.awaitAll()
                val list = results.flatten().distinctBy { it.extractId() }
                _topicListState.value = UiState.Success(list)
            } catch (e: Exception) {
                _topicListState.value = UiState.Error(e.message ?: "Gagal memuat daftar komik.")
            }
        }
    }

    fun nextTopicPage(term: String, value: String) {
        topicPage++
        loadTopicList(term, value)
    }

    fun prevTopicPage(term: String, value: String) {
        if (topicPage > 1) {
            topicPage--
            loadTopicList(term, value)
        }
    }

    fun resetTopicPage() {
        topicPage = 1
    }

    // Live Search with Debounce State
    var searchQuery by mutableStateOf("")
        private set
    var searchActive by mutableStateOf(false)
        private set
    private val _searchListState = MutableStateFlow<UiState<List<MangaItem>>>(UiState.Idle)
    val searchListState: StateFlow<UiState<List<MangaItem>>> = _searchListState.asStateFlow()
    var searchPage by mutableStateOf(1)
        private set

    private var searchJob: kotlinx.coroutines.Job? = null

    fun onSearchQueryChange(query: String) {
        searchQuery = query
        searchJob?.cancel()
        if (query.isNotBlank()) {
            searchJob = viewModelScope.launch {
                delay(400) // 400ms debounce
                searchPage = 1
                loadSearchResults()
            }
        } else {
            _searchListState.value = UiState.Idle
        }
    }

    fun toggleSearch(active: Boolean) {
        searchActive = active
        if (!active) {
            searchQuery = ""
            _searchListState.value = UiState.Idle
        }
    }

    fun executeSearch() {
        if (searchQuery.isBlank()) return
        searchPage = 1
        loadSearchResults()
    }

    fun loadSearchResults() {
        viewModelScope.launch(Dispatchers.IO) {
            _searchListState.value = UiState.Loading
            try {
                val startPage = (searchPage - 1) * 4 + 1
                val deferreds = (0..3).map { offset ->
                    val apiPage = (startPage + offset).toString()
                    async {
                        try {
                            val responseBody = RetrofitClient.api.searchManga(query = searchQuery, paged = apiPage)
                            RetrofitClient.parseMangaList(responseBody.string())
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }
                }
                val results = deferreds.awaitAll()
                val list = results.flatten().distinctBy { it.extractId() }
                _searchListState.value = UiState.Success(list)
            } catch (e: Exception) {
                _searchListState.value = UiState.Error(e.message ?: "Pencarian gagal.")
            }
        }
    }

    fun nextSearchPage() {
        searchPage++
        loadSearchResults()
    }

    fun prevSearchPage() {
        if (searchPage > 1) {
            searchPage--
            loadSearchResults()
        }
    }

    // Detail State
    private val _detailState = MutableStateFlow<UiState<MangaDetail>>(UiState.Idle)
    val detailState: StateFlow<UiState<MangaDetail>> = _detailState.asStateFlow()

    fun loadMangaDetail(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _detailState.value = UiState.Loading
            try {
                val responseBody = RetrofitClient.api.getMangaDetail(id = id)
                val list = RetrofitClient.parseMangaDetail(responseBody.string())
                val detail = list.firstOrNull()
                if (detail != null) {
                    _detailState.value = UiState.Success(detail)
                } else {
                    _detailState.value = UiState.Error("Detail komik tidak ditemukan.")
                }
            } catch (e: Exception) {
                _detailState.value = UiState.Error(e.message ?: "Gagal memuat detail komik.")
            }
        }
    }

    // Reader State
    private val _readerState = MutableStateFlow<UiState<ChapterDetail>>(UiState.Idle)
    val readerState: StateFlow<UiState<ChapterDetail>> = _readerState.asStateFlow()

    fun loadChapter(chapterId: String, mangaId: String, mangaTitle: String, img: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            _readerState.value = UiState.Loading
            try {
                val chapterDetail = RetrofitClient.api.getChapterDetail(id = chapterId)
                _readerState.value = UiState.Success(chapterDetail)

                // Save to history on Room thread as soon as details are successful
                dao.insertHistory(
                    HistoryMangaEntity(
                        id = mangaId,
                        mangaTitle = mangaTitle,
                        img = img,
                        chapterTitle = chapterDetail.title,
                        chapterId = chapterId,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                _readerState.value = UiState.Error(e.message ?: "Gagal memuat halaman baca.")
            }
        }
    }

    // Room Favorites
    val favorites: Flow<List<FavoriteMangaEntity>> = dao.getAllFavorites()

    fun isFavorite(id: String): Flow<Boolean> = dao.isFavoriteFlow(id)

    fun toggleFavorite(id: String, title: String, img: String?, type: String?, score: String?, status: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (dao.isFavorite(id)) {
                dao.deleteFavoriteById(id)
            } else {
                dao.insertFavorite(
                    FavoriteMangaEntity(
                        id = id,
                        title = title,
                        img = img,
                        type = type,
                        score = score,
                        status = status,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    // Room History
    val history: Flow<List<HistoryMangaEntity>> = dao.getAllHistory()

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearAllHistory()
        }
    }

    fun deleteHistoryItem(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteHistoryById(id)
        }
    }

    // Initializer
    init {
        loadLatestList()
        loadPopularList()
    }

    fun loadLatestList() {
        viewModelScope.launch(Dispatchers.IO) {
            _latestListState.value = UiState.Loading
            try {
                val responseBody = RetrofitClient.api.getHomepage(page = "latest", paged = "1")
                val list = RetrofitClient.parseMangaList(responseBody.string())
                _latestListState.value = UiState.Success(list)
            } catch (e: Exception) {
                _latestListState.value = UiState.Error(e.message ?: "Gagal memuat komik terbaru.")
            }
        }
    }

    fun loadPopularList() {
        viewModelScope.launch(Dispatchers.IO) {
            _popularListState.value = UiState.Loading
            try {
                val responseBody = RetrofitClient.api.getHomepage(page = "terpopuler", paged = "1")
                val list = RetrofitClient.parseMangaList(responseBody.string())
                _popularListState.value = UiState.Success(list)
            } catch (e: Exception) {
                _popularListState.value = UiState.Error(e.message ?: "Gagal memuat komik populer.")
            }
        }
    }
}
