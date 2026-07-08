package com.example.app_01

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import androidx.navigation.compose.*
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.collections.chunked
import kotlin.collections.forEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.app_01.theme.MyApplicationTheme
import com.google.firebase.auth.EmailAuthProvider
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import java.io.IOException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import org.json.JSONObject
import org.json.JSONArray
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.PUT
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Path
import kotlinx.coroutines.tasks.await
import android.provider.Settings

interface ApiService {
    @GET("user/profile/{f_uid}")
    suspend fun getUserProfile(
        @Path("f_uid") f_uid: String
    ): UserProfileResponse

    @PUT("user/update-email")
    suspend fun updateEmail(@Body request: EmailUpdateRequest): CommonResponse
}

val Context.dataStore by preferencesDataStore(name = "settings")

data class TranslationRequest(
    val isMode: String,
    val from: String,
    val to: String,
    val text: String
)

data class ChatMessage(
    val text: String,
    val isFromUser: Boolean, // trueなら自分（英語入力）、falseなら相手（翻訳結果）
    val isGreeting: Boolean = false
)

data class CommonResponse(
    val status: String,
    val updated_email: String? = null,
    val message: String? = null
)

data class UserProfileResponse(
    val email: String,
    val membership: String,
    val membership_expires_at: String?
)

data class EmailUpdateRequest(
    val f_uid: String,
    val new_email: String
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val languages = listOf(
        "English",
        "Spanish",
        "Portuguese",
        "French",
        "German",
        "Dutch",
        "Italian",
        "Danish",
        "Icelandic",
        "Norwegian",
        "Swedish",
        "Finnish",
        "Polish",
        "Czech",
        "Slovak",
        "Hungarian",
        "Slovenian",
        "Croatian",
        "Serbian",
        "Greek",
        "Turkish",
        "Hebrew",
        "Romanian",
        "Bulgarian",
        "Estonian",
        "Latvian",
        "Lithuanian",
        "Ukrainian",
        "Russian",
        "Japanese",
        "Korean",
        "Chinese",
        "Traditional Chinese",
        "Cantonese",
        "Vietnamese",
        "Khmer",
        "Lao",
        "Thai",
        "Tagalog",
        "Malay",
        "Indonesian",
        "Burmese",
        "Hindi",
        "Bengali",
        "Tamil",
        "Malayalam",
        "Kannada",
        "Telugu",
        "Marathi",
        "Oriya",
        "Gujarati",
        "Marwari",
        "Punjabi",
        "Urdu",
        "Mongolian",
        "Kazakh",
        "Kyrgyz",
        "Uzbek",
        "Persian",
        "Arabic",
        "Amharic",
        "Swahili",
        "Zulu"
    )

    var transexpandedFrom by mutableStateOf(false)
    var transexpandedTo by mutableStateOf(false)
    var chatexpandedFrom by mutableStateOf(false)
    var chatexpandedTo by mutableStateOf(false)
    var transselectedLanguageFrom by mutableStateOf(languages[0])
    var transselectedLanguageTo by mutableStateOf(languages[0])
    var chatselectedLanguageFrom by mutableStateOf(languages[0])
    private val _displayLanguage = MutableStateFlow(languages[0])
    val displayLanguage = _displayLanguage.asStateFlow()

    fun updateDisplayLanguage(newLang: String) {
        if (_displayLanguage.value != newLang) {
            _displayLanguage.value = newLang
        }
    }

    val translations = mapOf(
        "English" to mapOf("appearance" to "Appearance Settings", "dark_mode" to "Dark Mode", "lang_settings" to "Display Language"),
        "Japanese" to mapOf("appearance" to "外観設定", "dark_mode" to "ダークモード", "lang_settings" to "表示言語"),
        "Chinese" to mapOf("appearance" to "外观设置", "dark_mode" to "深色模式", "lang_settings" to "语言设置")
    )

    // チャット画面用のリスト
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    // 翻訳画面用のリスト
    private val _translateMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val translateMessages = _translateMessages.asStateFlow()

    fun resetAllStates() {
        // すべての変数を初期値に戻す
        var transexpandedFrom by mutableStateOf(false)
        var transexpandedTo by mutableStateOf(false)
        var chatexpandedFrom by mutableStateOf(false)
        var chatexpandedTo by mutableStateOf(false)
        var transselectedLanguageFrom by mutableStateOf(languages[0])
        var transselectedLanguageTo by mutableStateOf(languages[0])
        var chatselectedLanguageFrom by mutableStateOf(languages[0])
        var chatselectedLanguageTo by mutableStateOf(languages[0])
        _chatMessages.value = emptyList()
        _translateMessages.value = emptyList()

        chatWebSocketManager?.close()

        println("ViewModelの状態をリセットしました")
    }

    // 保存用のキー
    private val darkModeKey = booleanPreferencesKey("dark_mode")

    val isDarkMode: Flow<Boolean> = getApplication<Application>().dataStore.data
        .map { preferences -> preferences[darkModeKey] ?: false }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { preferences ->
                preferences[darkModeKey] = enabled
            }
        }
    }

    var currentUserId by mutableStateOf<String?>(null)
    var membershipStatus by mutableStateOf("Loading...")
    var emailAddress by mutableStateOf("Loading...")
    var passwordDisplay by mutableStateOf("********") 

    // ログイン成功メッセージを表示するかどうかの状態
    private val _showLoginSuccessMessage = mutableStateOf(false)
    val showLoginSuccessMessage: State<Boolean> = _showLoginSuccessMessage
    private var isConnecting = false
    private val auth = FirebaseAuth.getInstance()
    private val _isUserReady = MutableStateFlow(false)
    val isUserReady: StateFlow<Boolean> = _isUserReady.asStateFlow()

    var curUserId: String? = null
        private set

    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    fun isWebSocketConnected(): Boolean {
        // managerが存在し、かつisConnectedがtrueであるかを確認
        return chatWebSocketManager?.isConnected == true
    }

    fun triggerLoginSuccessMessage() {
        _showLoginSuccessMessage.value = true
    }

    fun onLoginSuccessMessageShown() {
        _showLoginSuccessMessage.value = false
    }

    private val _chatselectedLanguageState = MutableStateFlow(languages[0])
    val chatselectedLanguageState = _chatselectedLanguageState.asStateFlow()

    var chatselectedLanguageTo: String
        get() = _chatselectedLanguageState.value
        set(value) { _chatselectedLanguageState.value = value }

    private var isLastSentFromTranslate = false

    private var chatWebSocketManager: ChatWebSocketManager? = null

    init {
        // Firebaseの状態を監視
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                this.curUserId = user.uid
                _isUserReady.value = true
                prepareChat(user.uid)
            } else {
                _isUserReady.value = false
            }
        }
    }

    fun prepareChat(userId: String?) {
        this.currentUserId = userId

        if (userId.isNullOrEmpty() || userId == "null") {
            return
        }

        if (isConnecting || chatWebSocketManager?.isConnected == true) return

        isConnecting = true // 接続処理開始
        // すでに接続されている場合は一旦閉じる（再ログイン対策）
        chatWebSocketManager?.close()

        // WebSocketの初期化と接続
        chatWebSocketManager = ChatWebSocketManager(
            onConnected = {
                isConnecting = false
                if (lastGreetedLanguage == null) {
                    requestInitialGreeting()
                } else {
                    android.util.Log.d("WS_DEBUG", "接続完了: 挨拶は送信済みなのでスキップ")
                }
            },
            onMessageReceived = { response ->
                // handleMessageを呼ばず、ここで直接リストを更新する
                android.util.Log.d("WS_DEBUG", "メッセージ受信: $response")

                viewModelScope.launch {
                    val reply = handleIncomingMessage(response)

                    if (reply != null) {
                        if (reply.isGreeting) {
                            _chatMessages.update { it + reply }
                        } else {
                            if (isLastSentFromTranslate) {
                                _translateMessages.update { it + reply }
                            } else {
                                _chatMessages.update { it + reply }
                            }
                        }
                    }
                }
            }
        )

        android.util.Log.d("WS_DEBUG", "WebSocket接続を開始します")
        chatWebSocketManager?.connect()
    }

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null && user.uid != "0") {
                // UIDが取れた時だけ接続
                prepareChat(user.uid)
            }
        }
    }

    // サーバーから ID を取得して保存する関数
    fun syncUserWithServer(idToken: String) {
        // 1. OkHttpClient の準備
        val client = OkHttpClient()
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = "{}".toRequestBody(mediaType)

        // リクエストの構築
        val request = Request.Builder()
            .url("http://10.0.2.2:8000/login")
            .addHeader("Authorization", "Bearer $idToken")
            .post(requestBody)
            .build()

        // ViewModelScope で非同期実行
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // execute() で同期的にレスポンスを待機
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseData = response.body?.string() ?: return@launch
                    val jsonResponse = JSONObject(responseData)

                    // IDの取得
                    val mysqlId = jsonResponse.getString("user_id")

                    // 履歴リストの取得と解析
                    val historyArray = jsonResponse.getJSONArray("history")
                    val loadedMessages = mutableListOf<ChatMessage>()

                    for (i in 0 until historyArray.length()) {
                        val item = historyArray.getJSONObject(i)
                        val roleString = item.getString("role") // サーバーからは "user" か "assistant" が届く
                        val messageText = item.getString("content")
                        loadedMessages.add(
                            ChatMessage(
                                text = messageText,
                                // サーバーの role が "user" なら true, そうでなければ false に変換
                                isFromUser = (roleString == "user")
                            )
                        )
                    }

                    // UIスレッドで反映
                    withContext(Dispatchers.Main) {
                        currentUserId = mysqlId.toString()
                        // 取得した履歴をメッセージリストにセット
                        _chatMessages.value = loadedMessages
                    }
                } else {
                    android.util.Log.e("HTTP_DEBUG", "サーバーエラー: ${response.code}")
                }
            } catch (e: IOException) {
                android.util.Log.e("HTTP_DEBUG", "ネットワーク通信失敗: ${e.message}")
            } catch (e: Exception) {
                android.util.Log.e("HTTP_DEBUG", "解析エラー: ${e.message}")
            }
        }
    }

    fun startEmailUpdateProcess(
        newEmail: String,
        currentPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = Firebase.auth.currentUser
        val email = user?.email ?: return

        // ユーザーが入力したパスワードで認証情報を作成
        val credential = EmailAuthProvider.getCredential(email, currentPassword)

        // 再認証を実行
        user.reauthenticate(credential).addOnCompleteListener { reAuthTask ->
            if (reAuthTask.isSuccessful) {
                // 再認証に成功したら、メール更新処理を実行
                user.verifyBeforeUpdateEmail(newEmail).addOnCompleteListener { updateTask ->
                    if (updateTask.isSuccessful) {
                        onSuccess()
                    } else {
                        onError(updateTask.exception?.message ?: "検証メールの送信に失敗しました")
                    }
                }
            } else {
                // パスワード間違いやネットワークエラーの場合
                val errorMsg = reAuthTask.exception?.message ?: "認証に失敗しました。パスワードを確認してください。"
                onError(errorMsg)
            }
        }
    }

    fun reloadUserAndSync() {
        val user = Firebase.auth.currentUser
        user?.reload()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Firebase上の最新のメールアドレスを取得
                val updatedEmail = user.email ?: ""
                val fUid = user.uid

                // Firebase IDトークンを取得して、MySQL側とも同期させる
                user.getIdToken(true).addOnSuccessListener { result ->
                    val idToken = result.token
                    if (idToken != null) {
                        // 以前作成した syncUserWithServer を再利用
                        syncUserWithServer(idToken)
                    }
                }
            }
        }
    }

    fun sendInquiry(subject: String, message: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val json = JSONObject().apply {
                    put("user_id", Firebase.auth.currentUser?.uid)
                    put("app", "app01")
                    put("subject", subject)
                    put("message", message)
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("http://10.0.2.2:8000/contact") // 自身のAPI URL
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                withContext(Dispatchers.Main) {
                    onComplete(response.isSuccessful)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onComplete(false) }
            }
        }
    }

    private var lastGreetedLanguage: String? = null

    // 初回の挨拶を取得する関数
    fun updateLanguage(newLang: String) {
        if (_chatselectedLanguageState.value != newLang) {
            _chatselectedLanguageState.value = newLang
        }
    }

    private var isWaitingForInitialGreeting = false

    fun requestInitialGreeting(targetLang: String = chatselectedLanguageTo) {
        val id = currentUserId
        val manager = chatWebSocketManager

        // 基本チェック
        if (id == null || manager == null || !manager.isConnected) {
             return
        }

        // 送信する前にチェック
        if (lastGreetedLanguage == targetLang) {
            return
        }

        // 送信前に記録
        lastGreetedLanguage = targetLang

        isWaitingForInitialGreeting = true

        val jsonMessage = """
        {
            "user_id": "$id",
            "app": "app01",
            "type": "initial_greeting",
            "to": "$targetLang",
            "text": ""
        }
        """.trimIndent()

        chatWebSocketManager?.sendMessage(jsonMessage)
    }

    private fun handleIncomingMessage(jsonString: String): ChatMessage? {
        val isGreeting = isWaitingForInitialGreeting

        // 一度挨拶を受け取ったらフラグを下ろす
        if (isGreeting) {
            isWaitingForInitialGreeting = false
        }

        val text = if (jsonString.trim().startsWith("{")) {
            try {
                JSONObject(jsonString).optString("text", jsonString)
            } catch (e: Exception) { jsonString }
        } else {
            jsonString
        }

        return ChatMessage(
            text = text,
            isFromUser = false,
            isGreeting = isGreeting
        )
    }

    fun sendMessage(text: String, isTranslateMode: Boolean) {
        isLastSentFromTranslate = isTranslateMode

        // 言語設定だけを分岐で取得
        val fromLang = if (isTranslateMode) transselectedLanguageFrom else chatselectedLanguageFrom
        val toLang = if (isTranslateMode) transselectedLanguageTo else chatselectedLanguageTo

        // JSON文字列を生成
        val jsonMessage = """
        {
            "user_id": "$currentUserId",
            "app": "app01",
            "isMode": "$isLastSentFromTranslate",
            "from": "$fromLang",
            "to": "$toLang",
            "text": "$text"
        }
        """.trimIndent()

        // ここで呼び出す
        chatWebSocketManager?.sendMessage(jsonMessage)

        val newMessage = ChatMessage(text = text, isFromUser = true)
        if (isTranslateMode) {
            _translateMessages.update { it + newMessage }
        } else {
            _chatMessages.update { it + newMessage }
        }
    }

    init {
        // ViewModelが作られた瞬間に接続を開始する
        connectToWebsocket()
    }

    fun connectToWebsocket() {
        chatWebSocketManager?.connect()
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:8000/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(ApiService::class.java)

    fun fetchUserProfile(f_uid: String) {
        viewModelScope.launch {
            try {
                val response = apiService.getUserProfile(f_uid)
                membershipStatus = response.membership

                // Firebaseの最新情報を取得
                val firebaseUser = FirebaseAuth.getInstance().currentUser
                firebaseUser?.reload()?.await()
                val latestFirebaseEmail = firebaseUser?.email

                // MySQLから現在の情報を取得
                val mysqlProfile = apiService.getUserProfile(f_uid)

                // 比較して差分があれば更新
                if (latestFirebaseEmail != null && latestFirebaseEmail != mysqlProfile.email) {
                    updateMysqlEmail(f_uid, latestFirebaseEmail)
                } else {
                    emailAddress = mysqlProfile.email
                }
            } catch (e: Exception) {
                emailAddress = "Loading error"
                membershipStatus = "Loading error"
            }
        }
    }

    private fun updateMysqlEmail(fUid: String, newEmail: String) {
        viewModelScope.launch {
            try {
                val request = EmailUpdateRequest(f_uid = fUid, new_email = newEmail)

                val response = apiService.updateEmail(request)

                if (response.status == "success") {
                    emailAddress = newEmail
                }
            } catch (e: Exception) {}
        }
    }

    fun onEmailVerified(newEmail: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                apiService.updateEmail(EmailUpdateRequest(uid, newEmail))
                emailAddress = newEmail
            } catch (e: Exception) {}
        }
    }

    fun loginToServer(firebaseUid: String, idToken: String, onFinish: (Boolean) -> Unit) {
        // デバイスIDの取得
        val contentResolver = getApplication<Application>().contentResolver
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        val url = "http://10.0.2.2:8000/login"

        // JSONオブジェクトの作成
        val requestJson = JSONObject().apply {
            put("user_id", firebaseUid)
            put("device_id", deviceId)
            put("membership", "free")
        }

        // OkHttpクライアントの準備
        val client = OkHttpClient()
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestJson.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $idToken")
            .post(body)
            .build()

        // 非同期で送信
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("LoginError", "サーバー接続失敗: ${e.message}")
                onFinish(false)
            }

            override fun onResponse(call: Call, response: Response) {
                val isSuccessful = response.isSuccessful

                response.use {
                    if (!response.isSuccessful) {
                        android.util.Log.e("LoginError", "エラーコード: ${response.code}")
                        onFinish(false)
                    } else {
                        val responseData = response.body?.string()
                        android.util.Log.i("LoginSuccess", "ログイン成功: $responseData")
                    }
                }
                response.close()

                Handler(Looper.getMainLooper()).post {
                    onFinish(isSuccessful)
                }
            }
        })
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: MainViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState(initial = false)
            MyApplicationTheme(darkTheme = isDarkMode) {
                AuthNavigation(
                    isDarkMode = isDarkMode,
                    onThemeChange = { viewModel.toggleDarkMode(it) }
                )
            }
        }
    }
}

// 画面の定義
sealed class Screen(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    object Translate : Screen("translate", "Translate", Icons.Default.Translate)
    object Chat : Screen("chat", "Chat", Icons.Default.ChatBubbleOutline)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object UpdateEmail : Screen("email_update", "Update Email", Icons.Default.Settings)
    object ChangePassword : Screen("change_password", "Change Password", Icons.Default.Settings)
    object TermsOfService : Screen("terms_of_service", "Terms of Service", Icons.Default.Settings)
    object PrivacyPolicy : Screen("privacy_policy", "Privacy Policy", Icons.Default.Settings)
    object Commercial : Screen(
        "commercial_transactions",
        "Specified Commercial Transactions Act",
        Icons.Default.Settings
    )
    object ContactUs : Screen("contact_us", "Contact Us", Icons.Default.Settings)
}

fun sendRegisterRequestToServer(idToken: String) {
    val client = OkHttpClient()

    // サーバーのエンドポイントを指定
    val url = "http://10.0.2.2:8000/register"

    val mediaType = "application/json; charset=utf-8".toMediaType()
    val requestBody = "{}".toRequestBody(mediaType)

    val request = Request.Builder()
        .url(url)
        .addHeader("Authorization", "Bearer $idToken")
        .post(requestBody)
        .build()

    CoroutineScope(Dispatchers.IO).launch {
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("SERVER_REGISTER", "送信失敗: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    android.util.Log.d("SERVER_REGISTER", "サーバー側の保存成功")
                } else {
                    android.util.Log.e("SERVER_REGISTER", "サーバーエラー: ${response.code}")
                }
            }
        })
    }
}

fun requestAccountDeletion(idToken: String, onComplete: () -> Unit) {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("http://10.0.2.2:8000/delete-account")
        .addHeader("Authorization", "Bearer $idToken")
        .post("{}".toRequestBody("application/json".toMediaType()))
        .build()

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                withContext(Dispatchers.Main) {
                    Firebase.auth.signOut()
                    onComplete()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DELETE", "削除リクエスト失敗", e)
        }
    }
}

@Composable
fun AuthNavigation(
    isDarkMode: Boolean,
    onThemeChange: (Boolean) -> Unit,
) {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()
    val auth = FirebaseAuth.getInstance()


    // 現在ログインしているユーザーがいるか確認
    val currentUser = auth.currentUser
    val startRoute = if (currentUser != null && currentUser.isEmailVerified) {
        "translate"
    } else {
        "welcome"
    }

    NavHost(
        navController = navController,
        startDestination = startRoute,

        composable("welcome") { WelcomeScreen(navController) }
        composable("register") {
            RegisterScreen(navController, viewModel)
        }
        composable("login") {
            LoginScreen(navController, viewModel)
        }
        composable("reset_password") { ResetPasswordScreen(navController) }
        composable("email_verification") {
            EmailVerificationScreen(navController, viewModel)
        }
        composable("translate") {
            MainScreen(navController, viewModel, "translate") {
                TranslateScreen(viewModel = viewModel)
            }
        }
        composable("chat") {
            MainScreen(navController, viewModel, "chat") {
                ChatScreen(viewModel = viewModel)
            }
        }
        composable("settings") {
            SubScreen(navController, "settings") {
                SettingsScreen(
                    isDarkMode = isDarkMode,
                    onThemeChange = onThemeChange,
                    viewModel = viewModel,
                    navController = navController,
                    onNavigateToTerms = { navController.navigate("terms_of_service") },
                    onNavigateToPrivacy = { navController.navigate("privacy_policy") },
                    onNavigateToCommercial = { navController.navigate("commercial_transactions") }
                )
            }
        }
        composable("email_update") {
            SubScreen(navController, "email_update") {
                EmailUpdateScreen(
                    navController = navController,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable(Screen.ChangePassword.route) {
            SubScreen(navController, "change_password") {
                ChangePasswordScreen(navController = navController)
            }
        }
        composable(Screen.TermsOfService.route) {
            SubScreen(navController, "terms_of_service") {
                TermsOfServiceScreen()
            }
        }
        composable(Screen.PrivacyPolicy.route) {
            SubScreen(navController, "privacy_policy") {
                PrivacyPolicyScreen()
            }
        }
        composable(Screen.Commercial.route) {
            SubScreen(navController, "commercial_transactions") {
                CommercialScreen()
            }
        }
        composable(Screen.ContactUs.route) {
            SubScreen(navController, "contact_us") {
                ContactUsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    viewModel: MainViewModel,
    currentRoute: String,
    content: @Composable () -> Unit
) {
    val bottomNavItems = listOf(Screen.Translate, Screen.Chat)

    val currentTitle = when (currentRoute) {
        Screen.Settings.route -> Screen.Settings.label
        Screen.Translate.route -> Screen.Translate.label
        Screen.Chat.route -> Screen.Chat.label
        Screen.UpdateEmail.route -> Screen.UpdateEmail.label
        Screen.ChangePassword.route -> Screen.ChangePassword.label
        Screen.TermsOfService.route -> Screen.TermsOfService.label
        Screen.PrivacyPolicy.route -> Screen.PrivacyPolicy.label
        Screen.Commercial.route -> Screen.Commercial.label
        Screen.ContactUs.route -> Screen.ContactUs.label
        else -> "App"
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val showMessage by viewModel.showLoginSuccessMessage

    // showMessage が true になった時だけ実行
    LaunchedEffect(showMessage) {
        if (showMessage) {
            snackbarHostState.showSnackbar(
                message = "Log in succeeded",
                duration = SnackbarDuration.Short
            )
            // 表示し終わったらフラグを戻す
            viewModel.onLoginSuccessMessageShown()
        }
    }

    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val titles = listOf("Translate", "Chat")

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = {
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(pagerState.currentPage)
                )
            }
        ) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(index) }
                    },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                )
            }
        }
    }

    // 設定画面以外にいる時は true
    val isMainDestination = currentRoute in bottomNavItems.map { it.route }
    val isCommercial = currentRoute == Screen.Commercial.route
    val pagerPage = pagerState.currentPage

    val displayTitle = if (isMainDestination) {
        when (pagerPage) {
            0 -> "Translate"
            1 -> "Chat"
            else -> currentTitle
        }
    } else {
        currentTitle
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            TopAppBar(
                title = {
                    if (isCommercial) {
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleMedium
                        )
                    } else {
                        Text(
                            text = displayTitle
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                navigationIcon = {
                    // メイン画面以外にいる時は「戻る」を表示
                    if (!isMainDestination) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    // 基本画面にいる時だけ設定アイコンを表示する
                    if (isMainDestination) {
                        IconButton(onClick = { navController.navigate("settings") }) {
                            Icon(Icons.Default.Settings, contentDescription = "To Settings")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (isMainDestination) {
                NavigationBar {
                    // 現在の画面のパスを取得
                    val coroutineScope = rememberCoroutineScope()

                    // 翻訳ボタン
                    NavigationBarItem(
                        selected = pagerState.currentPage == 0,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(0)
                            }
                        },
                        label = { Text("translate") },
                        icon = { Icon(Icons.Filled.Translate, contentDescription = null) }
                    )

                    // チャットボタン
                    NavigationBarItem(
                        selected = pagerState.currentPage == 1,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        },
                        label = { Text("chat") },
                        icon = { Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null) }
                    )
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            // 画面の端で止まるように設定
            beyondViewportPageCount = 1
        ) { page ->
            when (page) {
                0 -> TranslateScreen(viewModel = viewModel)
                1 -> ChatScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubScreen(
    navController: NavController,
    currentRoute: String,
    content: @Composable () -> Unit
) {
    val bottomNavItems = listOf(Screen.Translate, Screen.Chat)

    val currentTitle = when (currentRoute) {
        Screen.Settings.route -> Screen.Settings.label
        Screen.UpdateEmail.route -> Screen.UpdateEmail.label
        Screen.ChangePassword.route -> Screen.ChangePassword.label
        Screen.TermsOfService.route -> Screen.TermsOfService.label
        Screen.PrivacyPolicy.route -> Screen.PrivacyPolicy.label
        Screen.Commercial.route -> Screen.Commercial.label
        Screen.ContactUs.route -> Screen.ContactUs.label
        else -> "アプリ"
    }

    // 設定画面以外にいる時は true
    val isMainDestination = currentRoute in bottomNavItems.map { it.route }
    val isCommercial = currentRoute == Screen.Commercial.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isCommercial) {
                        Text(
                            text = currentTitle,
                            style = MaterialTheme.typography.titleMedium
                        )
                    } else {
                        Text(
                            text = currentTitle
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                navigationIcon = {
                    // メイン画面以外にいる時は「戻る」を表示
                    if (!isMainDestination) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            content()
        }
    }
}

// --- 各画面の定義 ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateScreen(viewModel: MainViewModel) {
    var inputText by remember { mutableStateOf("") }

    // WebSocketから届いたメッセージリスト
    val messageList by viewModel.translateMessages.collectAsState()

    // 最新の受信メッセージを取得する
    val latestMessage = messageList.lastOrNull { !it.isFromUser && !it.isGreeting }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // プルダウンメニュー部分
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 左側: From (入力言語)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "From",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small
                        )
                        .clickable { viewModel.transexpandedFrom = true }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = viewModel.transselectedLanguageFrom,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            modifier = Modifier.weight(1f)
                        )
                        // 下向き矢印アイコンを配置
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    // From用メニュー
                    DropdownMenu(
                        expanded = viewModel.transexpandedFrom,
                        onDismissRequest = { viewModel.transexpandedFrom = false },
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .heightIn(max = 600.dp)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        viewModel.languages.chunked(3).forEach { rowLanguages ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                rowLanguages.forEach { lang ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                viewModel.transselectedLanguageFrom = lang
                                                viewModel.transexpandedFrom = false
                                            }
                                            .padding(
                                                vertical = 2.dp,
                                                horizontal = 1.dp
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = lang,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                // 3列に満たない行の隙間埋め
                                repeat(3 - rowLanguages.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // 右側: To (翻訳後言語)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "To",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small
                        )
                        .clickable { viewModel.transexpandedTo = true }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = viewModel.transselectedLanguageTo,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    // To用メニュー
                    DropdownMenu(
                        expanded = viewModel.transexpandedTo,
                        onDismissRequest = { viewModel.transexpandedTo = false },
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .heightIn(max = 600.dp)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        viewModel.languages.chunked(3).forEach { rowLanguages ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                rowLanguages.forEach { lang ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                viewModel.transselectedLanguageTo = lang
                                                viewModel.transexpandedTo = false
                                            }
                                            .padding(
                                                vertical = 2.dp,
                                                horizontal = 1.dp
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = lang,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                // 3列に満たない行の隙間埋め
                                repeat(3 - rowLanguages.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }


        // 英語の入力画面
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Type your message.") },
            placeholder = { Text("Example: Hello, how are you?") },
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 日本語の表示画面
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = viewModel.transselectedLanguageTo + ":",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (latestMessage != null) {
                    Text(
                        text = latestMessage.text,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 翻訳ボタン
        Button(
            onClick = {
                viewModel.sendMessage(inputText, isTranslateMode = true) // サーバーへ送信
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.Default.Translate, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Translate", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: MainViewModel) {
    val isUserReady by viewModel.isUserReady.collectAsState()

    if (!isUserReady) {
        // IDが取れるまで待機画面を表示
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        // IDが確定してからチャット画面を表示
        ChatContent(viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatContent(viewModel: MainViewModel) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

    // 言語設定を監視するためにStateとして取得
    val selectedLanguage by viewModel.chatselectedLanguageState.collectAsState()

    // 画面表示時に1回だけ実行
    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) viewModel.prepareChat(uid)
    }

    // 言語切り替えの監視
    LaunchedEffect(selectedLanguage) {
        // 接続されるまで待機
        while (!viewModel.isWebSocketConnected()) {
            kotlinx.coroutines.delay(300)
        }

        viewModel.requestInitialGreeting(selectedLanguage)
    }

    // メッセージが増えた時だけ実行
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // プルダウンメニュー
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 左側: From (入力言語)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "From",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small
                        )
                        .clickable { viewModel.chatexpandedFrom = true }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = viewModel.chatselectedLanguageFrom,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    // From用メニュー
                    DropdownMenu(
                        expanded = viewModel.chatexpandedFrom,
                        onDismissRequest = { viewModel.chatexpandedFrom = false },
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .heightIn(max = 600.dp)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        viewModel.languages.chunked(3).forEach { rowLanguages ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                rowLanguages.forEach { lang ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                viewModel.chatselectedLanguageFrom = lang
                                                viewModel.chatexpandedFrom = false
                                            }
                                            .padding(
                                                vertical = 2.dp,
                                                horizontal = 1.dp
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = lang,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                repeat(3 - rowLanguages.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // 右側: To (翻訳後言語)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "To",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small
                        )
                        .clickable { viewModel.chatexpandedTo = true }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = viewModel.chatselectedLanguageTo,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    // To用メニュー
                    DropdownMenu(
                        expanded = viewModel.chatexpandedTo,
                        onDismissRequest = { viewModel.chatexpandedTo = false },
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .heightIn(max = 600.dp)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        viewModel.languages.chunked(3).forEach { rowLanguages ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                rowLanguages.forEach { lang ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                viewModel.chatselectedLanguageTo = lang
                                                viewModel.updateLanguage(lang)
                                                viewModel.chatexpandedTo = false
                                            }
                                            .padding(
                                                vertical = 2.dp,
                                                horizontal = 1.dp
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = lang,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                repeat(3 - rowLanguages.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // メッセージを表示するリスト
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChatInputBar(
                inputText = inputText,
                onValueChange = { inputText = it },
                onSendClick = {
                    viewModel.sendMessage(inputText, isTranslateMode = false)
                    inputText = ""
                }
            )
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isFromUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor =
        if (message.isFromUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val shape = if (message.isFromUser) {
        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Surface(
            color = bubbleColor,
            shape = shape,
            tonalElevation = 2.dp
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInputBar(
    inputText: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = inputText,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    if (inputText.isEmpty()) {
                        Text(
                            text = "Type your message...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    innerTextField()
                }
            )

            // 送信ボタン
            IconButton(
                onClick = onSendClick,
                enabled = inputText.isNotBlank(),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = "送信",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

class ChatWebSocketManager(
    private val onMessageReceived: (String) -> Unit,
    private val onConnected: () -> Unit
) {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    var isConnected: Boolean = false
        private set

    // 再接続を管理するためのHandler
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isManualClose = false

    fun connect() {
        if (isConnected) return

        isManualClose = false
        val request = Request.Builder()
            .url("ws://10.0.2.2:8000/ws/chat")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                // メッセージを受信した時の処理
                onMessageReceived(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                attemptReconnect()
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                if (!isManualClose) attemptReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                if (!isManualClose) attemptReconnect()
            }
        })
    }

    private fun attemptReconnect() {
        if (isManualClose) return

        mainHandler.postDelayed({
            connect()
        }, 3000)
    }

    fun sendMessage(message: String): Boolean {
        val success = webSocket?.send(message) ?: false
        return success
    }

    fun close() {
        isManualClose = true
        webSocket?.close(1000, "App closed")
        isConnected = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkMode: Boolean,
    onThemeChange: (Boolean) -> Unit,
    viewModel: MainViewModel,
    navController: NavController,
    onNavigateToTerms: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToCommercial: () -> Unit,
) {
    val showLogoutDialog = remember { mutableStateOf(false) }
    val showAccountDeleteDialog = remember { mutableStateOf(false) }
    // 表示言語の状態を監視
    val currentDisplayLang by viewModel.displayLanguage.collectAsState()

    // 現在の言語に基づいたテキストを取得
    val uiText = viewModel.translations[currentDisplayLang] ?: viewModel.translations["English"]!!

    var expanded by remember { mutableStateOf(false) }
    val displayLanguages = viewModel.languages
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                // Firebase から現在のユーザーの UID を取得する
                val currentUid = FirebaseAuth.getInstance().currentUser?.uid
                if (currentUid != null) {
                    viewModel.fetchUserProfile(currentUid)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 画面が表示されたときに実行
    LaunchedEffect(Unit) {
        viewModel.reloadUserAndSync()
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.uid?.let { uid ->
            viewModel.fetchUserProfile(uid)
        }

        val user = FirebaseAuth.getInstance().currentUser

        // ユーザー情報を最新の状態に更新
        user?.reload()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val firebaseEmail = user.email
                // サーバーのメアドとFirebaseのメアドを比較
                if (firebaseEmail != null && firebaseEmail != viewModel.emailAddress) {
                    viewModel.onEmailVerified(firebaseEmail)
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = uiText["appearance"] ?: "Appearance Settings",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 8.dp)
            )

            // 表示言語選択プルダウン
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = uiText["lang_settings"] ?: "Display Language",
                    style = MaterialTheme.typography.bodyLarge
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier
                        .width(140.dp)
                        .height(36.dp)
                ) {
                    OutlinedCard(
                        onClick = { expanded = true },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .height(36.dp),
                        shape = MaterialTheme.shapes.extraSmall,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = currentDisplayLang, fontSize = 13.sp)
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        }
                    }

                    // メニュー部分
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .heightIn(max = 600.dp)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        // displayLanguages を 3列ずつ表示
                        displayLanguages.chunked(3).forEach { rowLanguages ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                rowLanguages.forEach { lang ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                viewModel.updateDisplayLanguage(lang)
                                                expanded = false
                                            }
                                            .padding(vertical = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = lang,
                                            fontSize = 11.sp,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                // 3列に満たない場合の調整
                                repeat(3 - rowLanguages.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // 英語に戻すボタン
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        viewModel.updateDisplayLanguage("English")
                        viewModel.requestInitialGreeting("English")
                    },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Reset to English",
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 12.sp
                    )
                }
            }

            // ダークモード切り替え部分の余白調整
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = uiText["dark_mode"] ?: "Dark Mode",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { onThemeChange(it) }
                )
            }

        }

        item {
            Text(
                "Account Information",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // メールアドレス
        item {
            SettingItem(
                label = "Account Email",
                value = viewModel.emailAddress,
                isEditable = true,
                onClick = { navController.navigate("email_update") })
        }

        // 会員資格
        item {
            SettingItem(
                label = "Membership",
                value = viewModel.membershipStatus,
                isEditable = false
            )
        }

        // パスワード
        item {
            SettingItem(
                label = "Password",
                value = viewModel.passwordDisplay,
                isEditable = true,
                onClick = { navController.navigate("change_password") })
        }

        item {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Upgrade to Premium",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "$5.00 / month", // 英語表記の月額
                        style = MaterialTheme.typography.labelLarge, // 2行目は少し小さく
                    )
                }
            }
        }

        item {
            horizontalDivider()
        }

        item {
            Text(
                text = "Support & Legal",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        item {
            SettingItem(
                label = "Downgrade",
                value = "Stop Subscription",
                isEditable = true,
            )
        }

        // 利用規約
        item {
            SettingItem(
                label = "Legal",
                value = "Terms of Service",
                isEditable = true,
                onClick = onNavigateToTerms
            )
        }

        // プライバシーポリシー
        item {
            SettingItem(
                label = "Privacy",
                value = "Privacy Policy",
                isEditable = true,
                onClick = onNavigateToPrivacy
            )
        }

        // 特定商取引法に基づく表記
        item {
            SettingItem(
                label = "About",
                value = "Specified Commercial Transactions Act",
                isEditable = true,
                onClick = onNavigateToCommercial
            )
        }

        // 問合せ
        item {
            SettingItem(
                label = "Support",
                value = "Contact Us",
                isEditable = true,
                onClick = { navController.navigate("contact_us") }
            )
        }

        // ログアウト
        item {
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { showLogoutDialog.value = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out")
            }

            if (showLogoutDialog.value) {
                AlertDialog(
                    onDismissRequest = { showLogoutDialog.value = false },
                    title = {
                        Text(text = "Log Out")
                    },
                    text = {
                        Text(text = "Are you sure you want to log out?")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showLogoutDialog.value = false

                                val user = FirebaseAuth.getInstance().currentUser
                                // まず現在のトークンを取得
                                user?.getIdToken(false)?.addOnSuccessListener { result ->
                                    val idToken = result.token
                                    if (idToken != null) {
                                        // サーバーへ通知し、完了後にFirebaseからサインアウト
                                        notifyLogoutToServer(idToken) {
                                            FirebaseAuth.getInstance().signOut()
                                            viewModel.resetAllStates()
                                            navController.navigate("welcome") {
                                                popUpTo(navController.graph.id) {
                                                    inclusive = true
                                                }
                                            }
                                        }
                                    }
                                } ?: run {
                                    // ユーザー情報が取れない場合ログアウト
                                    FirebaseAuth.getInstance().signOut()
                                    viewModel.resetAllStates()
                                    navController.navigate("welcome") {
                                        popUpTo(navController.graph.id) {
                                            inclusive = true
                                        }
                                    }
                                }
                                println("Logged out!")
                            }
                        ) {
                            Text(text = "Log Out", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showLogoutDialog.value = false }
                        ) {
                            Text(text = "Cancel")
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    icon = {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                    }
                )
            }
        }

        // アカウント削除
        item {
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showAccountDeleteDialog.value = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFB71C1C),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Account")
            }

            if (showAccountDeleteDialog.value) {
                AlertDialog(
                    onDismissRequest = { showAccountDeleteDialog.value = false },
                    title = {
                        Text(text = "Delete Account")
                    },
                    text = {
                        Text(text = "Are you sure you want to delete account?\nDue to the nature of digital content, no returns or refunds are accepted after purchase.\nYour subscription will be canceled from next subscription update in next month.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showAccountDeleteDialog.value = false

                                val user = FirebaseAuth.getInstance().currentUser
                                user?.getIdToken(false)?.addOnSuccessListener { result ->
                                    val idToken = result.token
                                    if (idToken != null) {
                                        notifyAccountDeleteToServer(idToken) {
                                            user.delete().addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    android.util.Log.d(
                                                        "AUTH",
                                                        "アカウントを削除しました"
                                                    )
                                                } else {
                                                    requestAccountDeletion(idToken) {
                                                        viewModel.resetAllStates()
                                                    }
                                                }
                                                viewModel.resetAllStates()
                                                navController.navigate("welcome") {
                                                    popUpTo(navController.graph.id) {
                                                        inclusive = true
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                println("Account Deleted!")
                            }
                        ) {
                            Text(text = "Delete Account", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showAccountDeleteDialog.value = false }
                        ) {
                            Text(text = "Cancel")
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    icon = {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                    }
                )
            }

            // バージョン表示
            Text(
                text = "Version 1.2.1",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SettingItem(
    label: String,
    value: String,
    isEditable: Boolean,
    onClick: () -> Unit = {}
) {
    ListItem(
        modifier = if (isEditable) Modifier.clickable(onClick = onClick) else Modifier,
        headlineContent = { Text(label, style = MaterialTheme.typography.labelMedium) },
        supportingContent = {
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isEditable) Color.Unspecified else Color.Gray
            )
        },
        trailingContent = {
            if (isEditable) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Edit",
                    tint = Color.Gray
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun horizontalDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        thickness = 0.5.dp,
        color = Color.LightGray
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(navController: NavController) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val context = LocalContext.current
    val auth = Firebase.auth

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Set New Password",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 現在のパスワード入力
            OutlinedTextField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                label = { Text("Current Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm New Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    val user = auth.currentUser
                    val email = user?.email

                    if (user != null && email != null) {
                        // 現在のパスワードで再認証
                        val credential = EmailAuthProvider.getCredential(email, currentPassword)

                        user.reauthenticate(credential).addOnCompleteListener { reAuthTask ->
                            if (reAuthTask.isSuccessful) {
                                // 再認証に成功したら、新しいパスワードに更新
                                user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                                    if (updateTask.isSuccessful) {
                                        Toast.makeText(context, "Password Updated!", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    } else {
                                        Toast.makeText(context, "Update failed: ${updateTask.exception?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Authentication failed. Check current password.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            ) {
                Text("Update Password")
            }
        }
    }
}

// セクション用共通データクラス
data class TermsSectionData(
    val title: String,
    val parts: List<Pair<String, Boolean>>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfServiceScreen() {
    // 2. 利用規約データの定義
    val sections = listOf(
        TermsSectionData(
            "1. Provision of Service",
            listOf("This app provides translation and chat functions. We do not guarantee the accuracy or completeness of the translation results." to false)
        ),
        TermsSectionData(
            "2. Subscriptions",
            listOf("The Premium Plan is a monthly paid service. Payment will be charged to your Google Play account at the time of purchase confirmation." to false)
        ),
        TermsSectionData(
            "3. Cancellation Policy",
            listOf(
                "• How to Cancel: " to true,
                "You must manage and cancel your subscription through the Google Play Store settings. Uninstalling the app does not automatically cancel your subscription.\n" to false,
                "• Renewal: " to true,
                "To avoid being charged for the next period, you must cancel at least 24 hours before the end of the current billing cycle.\n" to false,
                "• Refunds: " to true,
                "Due to the nature of digital services, we do not offer pro-rated refunds for partial months or any refunds after a purchase has been processed." to false
            )
        ),
        TermsSectionData(
            "4. Prohibited Actions",
            listOf("Users must not use this app for illegal purposes, defamation of others, or any action that places an excessive load on our servers." to false)
        ),
        TermsSectionData(
            "5. Intellectual Property",
            listOf("All rights (copyrights, trademarks, etc.) related to this app belong to the developer." to false)
        ),
        TermsSectionData(
            "6. Disclaimer",
            listOf("We are not liable for any damages (loss of data, disadvantages due to translation errors, etc.) arising from the use of this app." to false)
        ),
        TermsSectionData(
            "7. Changes to Terms",
            listOf("We reserve the right to modify these terms at any time. Modified terms become effective once posted within the app." to false)
        )
    )

    Scaffold { padding ->
        SelectionContainer {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "Terms of Service",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Last updated: Jan 20, 2026",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(24.dp))

                sections.forEach { section ->
                    TermsSectionItem(section)
                }

                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "If you have any questions, please contact us via the Support section in the settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun TermsSectionItem(data: TermsSectionData) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = data.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = buildAnnotatedString {
                data.parts.forEach { (text, isBold) ->
                    if (isBold) {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text)
                        }
                    } else {
                        append(text)
                    }
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// 各セクションのデータを保持するデータクラス
data class PrivacySectionData(
    val title: String,
    val parts: List<Pair<String, Boolean>>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen() {
    val sections = listOf(
        PrivacySectionData(
            "1. Information We Collect",
            listOf(
                "• Device Information: " to true,
                "Usage statistics, crash reports, and device type.\n" to false,
                "• Payment Information: " to true,
                "We receive payment status via Google Play for subscription management; however, we do not store credit card numbers or other sensitive financial details." to false
            )
        ),
        PrivacySectionData(
            "2. How We Use Your Information",
            listOf("Collected information is used to provide services, provide customer support, and prevent fraudulent activity." to false)
        ),
        PrivacySectionData(
            "3. Data Retention and Third-Party Sharing",
            listOf("We do not share personal information with third parties without user consent, except as required by law." to false)
        ),
        PrivacySectionData(
            "4. Security",
            listOf("We implement appropriate security measures to protect user information from unauthorized access or leakage." to false)
        ),
        PrivacySectionData(
            "5. User Rights",
            listOf("Users may request the deletion of their data through the app settings or by contacting us via the support email." to false)
        ),
        PrivacySectionData(
            "6. Contact Us",
            listOf("For inquiries regarding our Privacy Policy, please contact us via the \"Contact Us\" section in the app settings." to false)
        )
    )

    Scaffold { padding ->
        SelectionContainer {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "Privacy Policy",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Last updated: Jan 20, 2026",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(24.dp))

                sections.forEach { section ->
                    PrivacySectionItem(section)
                }

                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "If you have any questions, please contact us via the Support section in the settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun PrivacySectionItem(data: PrivacySectionData) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = data.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = buildAnnotatedString {
                data.parts.forEach { (text, isBold) ->
                    if (isBold) {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text)
                        }
                    } else {
                        append(text)
                    }
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

data class CommercialSectionData(
    val title: String,
    val parts: List<Pair<String, Boolean>>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommercialScreen() {
    val sections = listOf(
        CommercialSectionData(
            "1. Distributor",
            listOf("Takeaki Nadabe" to false)
        ),
        CommercialSectionData(
            "2. Representative",
            listOf("Takeaki Nadabe" to false)
        ),
        CommercialSectionData(
            "3. Location",
            listOf("Company Address" to false)
        ),
        CommercialSectionData(
            "4. Contact Information",
            listOf(
                "• Email: " to true,
                "Support Email Address\n" to false,
                "• TEL: " to true,
                "Support TEL" to false
            )
        ),
        CommercialSectionData(
            "5. Selling Price",
            listOf("$5.00 USD per month (Price may vary based on exchange rates. Taxes will be applied as required by law.)" to false)
        ),
        CommercialSectionData(
            "6. Additional Charges",
            listOf("Internet connection fees required to use the app." to false)
        ),
        CommercialSectionData(
            "7. Payment Method & Timing",
            listOf("Payments are processed through Google Play Store. Payment occurs at the time of purchase confirmation." to false)
        ),
        CommercialSectionData(
            "8. Delivery Time",
            listOf("Premium features are available immediately after the payment process is completed." to false)
        ),
        CommercialSectionData(
            "9. Returns and Cancellations",
            listOf("Due to the nature of digital content, no returns or refunds are accepted after purchase. You can cancel your subscription at any time through your Google Play subscription settings." to false)
        )
    )

    Scaffold { padding ->
        SelectionContainer {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "Specified Commercial Transactions Act",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Last updated: Jan 20, 2026",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(24.dp))

                sections.forEach { section ->
                    CommercialSectionItem(section)
                }

                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "If you have any questions, please contact us via the Support section in the settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun CommercialSectionItem(data: CommercialSectionData) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = data.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = buildAnnotatedString {
                data.parts.forEach { (text, isBold) ->
                    if (isBold) {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text)
                        }
                    } else {
                        append(text)
                    }
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactUsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Send Message to Us",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Subject") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Message") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                maxLines = 10
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    isLoading = true
                    // ViewModelに作成する送信関数を呼ぶ
                    viewModel.sendInquiry(subject, message) { success ->
                        isLoading = false
                        if (success) {
                            Toast.makeText(context, "Sent successfully!", Toast.LENGTH_SHORT).show()
                            onBack()
                        } else {
                            Toast.makeText(context, "Failed to send.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && subject.isNotBlank() && message.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Submit")
                }
            }
        }
    }
}

fun notifyLogoutToServer(idToken: String, onComplete: () -> Unit) {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("http://10.0.2.2:8000/logout")
        .addHeader("Authorization", "Bearer $idToken")
        .post("{}".toRequestBody("application/json".toMediaType()))
        .build()

    CoroutineScope(Dispatchers.IO).launch {
        try {
            // サーバーに通知
            val response = client.newCall(request).execute()
            android.util.Log.d("DELETE ACCOUNT", "Server notified: ${response.isSuccessful}")
        } catch (e: Exception) {
            android.util.Log.e("DELETE ACCOUNT", "Server notification failed", e)
        } finally {
            // アプリのログアウト処理を呼ぶ
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }
}

fun notifyAccountDeleteToServer(idToken: String, onComplete: () -> Unit) {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("http://10.0.2.2:8000/delete-account")
        .addHeader("Authorization", "Bearer $idToken")
        .post("{}".toRequestBody("application/json".toMediaType()))
        .build()

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = client.newCall(request).execute()
            android.util.Log.d("LOGOUT", "Server notified: ${response.isSuccessful}")
        } catch (e: Exception) {
            android.util.Log.e("LOGOUT", "Server notification failed", e)
        } finally {
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }
}

// ウェルカム画面
@Composable
fun WelcomeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Talk in Multi-Language",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { navController.navigate("register") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) { Text("Create Account") }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = { navController.navigate("login") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) { Text("Login") }
    }
}

// アカウント登録画面
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val isPasswordValid =
        password.length >= 8 && password.any { it.isDigit() } && password.any { it.isLetter() }
    var expanded by remember { mutableStateOf(false) }
    val currentDisplayLang by viewModel.displayLanguage.collectAsState()
    val displayLanguages = viewModel.languages
    val mContext = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(24.dp)) {
        Text(
            "Create Account",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password (8+ chars, Alphanumeric)") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Select Display Language", style = MaterialTheme.typography.bodyLarge)

            OutlinedCard(
                onClick = { showDialog = true },
                modifier = Modifier.width(150.dp).height(50.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = currentDisplayLang)
                }
            }
        }

        if (showDialog) {
            Dialog(
                onDismissRequest = { showDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .heightIn(max = 600.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Select Language",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            displayLanguages.chunked(3).forEach { row ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    row.forEach { lang ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    viewModel.updateDisplayLanguage(lang)
                                                    showDialog = false
                                                }
                                                .padding(vertical = 2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = lang,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    repeat(3 - row.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                 val auth = Firebase.auth

                // Firebaseにユーザー作成
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = task.result?.user

                            user?.sendEmailVerification()?.addOnCompleteListener { emailTask ->
                                if (emailTask.isSuccessful) {
                                    android.util.Log.d("AUTH", "認証メール送信完了")
                                }
                            }

                            user?.getIdToken(true)?.addOnSuccessListener { tokenResult ->
                                val idToken = tokenResult.token
                                if (idToken != null) {
                                    navController.navigate("email_verification") {
                                        popUpTo("signup") { inclusive = true }
                                    }
                                } else {
                                    android.util.Log.e("AUTH", "トークンの取得に失敗しました")
                                }
                            }
                        } else {
                             val exception = task.exception
                            val errorMessage = exception?.message ?: "Unknown Error"

                            android.util.Log.e("AUTH", "登録失敗: $errorMessage", exception)

                            Toast.makeText(mContext, "Error: $errorMessage", Toast.LENGTH_LONG)
                                .show()
                        }
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("AUTH", "サインアップ失敗: ${e.message}")
                    }
            },
            enabled = isPasswordValid && password == confirmPassword && email.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Sign Up") }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Already have an account? ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Log In",
                modifier = Modifier
                    .clickable {
                        navController.navigate("login") {
                            popUpTo("signup") { inclusive = true }
                        }
                    }
                    .padding(4.dp),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

// ログイン画面
@Composable
fun LoginScreen(navController: NavController, viewModel: MainViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val mContext = LocalContext.current

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(24.dp)) {
        Text("Login", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        TextButton(onClick = { navController.navigate("reset_password") }) {
            Text("Forgot Password?")
        }

        TextButton(onClick = { navController.navigate("register") }) {
            Text("Create account?")
        }

        Button(
            onClick = {
                val auth = Firebase.auth

                // Firebaseでサインインを実行
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = task.result?.user

                            if (user != null && user.isEmailVerified) {
                                user.getIdToken(true).addOnSuccessListener { result ->
                                    val idToken = result.token
                                    if (idToken != null) {
                                        viewModel.syncUserWithServer(idToken)
                                        viewModel.loginToServer(user.uid, idToken) { success ->
                                            if (success) {
                                                viewModel.triggerLoginSuccessMessage()
                                                navController.navigate("translate") {
                                                    popUpTo("login") { inclusive = true }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Toast.makeText(
                                    mContext,
                                    "Please verify your email first.",
                                    Toast.LENGTH_LONG
                                ).show()
                                navController.navigate("email_verification")
                            }
                        } else {
                            val errorMsg = task.exception?.message ?: "Login failed"
                            Toast.makeText(mContext, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = email.isNotEmpty() && password.isNotEmpty()
        ) {
            Text("Login")
        }
    }
}

// パスワード再設定画面
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    val mContext = LocalContext.current
    val auth = Firebase.auth

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reset Password") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            Text(
                "Forgot your password?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Enter your registered email and we'll send you a link to reset your password.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (email.isNotEmpty()) {
                        auth.sendPasswordResetEmail(email)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(mContext, "Reset email sent!", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                } else {
                                    val error = task.exception?.message ?: "Failed to send email"
                                    Toast.makeText(mContext, error, Toast.LENGTH_LONG).show()
                                }
                            }
                    } else {
                        Toast.makeText(mContext, "Please enter your email", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = email.isNotEmpty()
            ) {
                Text("Send Reset Email")
            }
        }
    }
}

// メール認証待機画面
@Composable
fun EmailVerificationScreen(navController: NavController, viewModel: MainViewModel) {
    val auth = Firebase.auth
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        while (true) {
            auth.currentUser?.reload()?.addOnCompleteListener {
                val user = auth.currentUser
                if (user != null && user.isEmailVerified) {
                    user.getIdToken(true).addOnSuccessListener { result ->
                        val idToken = result.token
                        if (idToken != null) {
                            sendRegisterRequestToServer(idToken)
                            viewModel.loginToServer(user.uid, idToken) { success ->
                                if (success) {
                                    viewModel.triggerLoginSuccessMessage()
                                    navController.navigate("translate") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            kotlinx.coroutines.delay(3000)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("We've sent a verification email.")
        Text("Please check your inbox and click the link.")

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            auth.currentUser?.sendEmailVerification()
            Toast.makeText(context, "Resending email!", Toast.LENGTH_SHORT).show()
        }) {
            Text("Resend verification email")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailUpdateScreen(navController: NavController, viewModel: MainViewModel, onBack: () -> Unit) {
    var newEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Change Email Address",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = newEmail,
                onValueChange = { newEmail = it },
                label = { Text("New Email Address") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // セキュリティのためにパスワード入力必須
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Confirm Current Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    isLoading = true
                    viewModel.startEmailUpdateProcess(
                        newEmail, password,
                        onSuccess = {
                            isLoading = false
                            Toast.makeText(context, "Verification email sent. Please check your inbox.", Toast.LENGTH_LONG).show()

                            val user = FirebaseAuth.getInstance().currentUser
                            user?.getIdToken(false)?.addOnSuccessListener { result ->
                                val idToken = result.token
                                if (idToken != null) {
                                    // サーバーへ通知し、完了後にFirebaseからサインアウト
                                    notifyLogoutToServer(idToken) {
                                        FirebaseAuth.getInstance().signOut()
                                        viewModel.resetAllStates()
                                        navController.navigate("welcome") {
                                            popUpTo(navController.graph.id) {
                                                inclusive = true
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        onError = { error ->
                            isLoading = false
                            Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                        }
                    )
                },
                enabled = !isLoading && newEmail.isNotEmpty() && password.isNotEmpty()
            ) {
                if (isLoading) CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                ) else Text("Send verification email")
            }
        }
    }
}