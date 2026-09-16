/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package dev.brahmkshatriya.echo.dhun.kugou

import android.util.Base64
import dev.brahmkshatriya.echo.common.models.Lyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.Inflater
import kotlin.math.abs

/**
 * KuGou lyrics client.
 *
 * Uses the well-known public KuGou lyrics endpoints:
 *  1. `GET http://krcs.kugou.com/search?ver=1&man=yes&client=mobi&keyword=<title - artist>&duration=<ms>`
 *     -> returns a list of lyric candidates (id + accesskey).
 *  2. `GET http://krcs.kugou.com/download?ver=1&client=mobi&id=<id>&accesskey=<accesskey>&fmt=<krc|lrc>&charset=utf8`
 *     -> returns the lyrics payload, base64 encoded.
 *
 * For `fmt=krc` the payload additionally needs to be XOR-decoded (key 0x40) and
 * zlib-inflated before it can be read; the resulting KRC karaoke text is then
 * converted to standard LRC. For `fmt=lrc` the base64 payload decodes straight
 * to plain LRC.
 */
object KuGou {

    /** When true, returned lyrics are converted from Simplified to Traditional Chinese. */
    var useTraditionalChinese: Boolean = false

    private const val SEARCH_URL = "http://krcs.kugou.com/search"
    private const val DOWNLOAD_URL = "http://krcs.kugou.com/download"
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Dhun/1.0"

    /** Allowed mismatch (in seconds) between the requested and candidate duration. */
    private const val DURATION_TOLERANCE = 8

    private const val MAX_OPTIONS = 6

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    // ── DTOs ────────────────────────────────────────────────────────────────

    @Serializable
    data class SearchResponse(
        val status: Int = 0,
        val candidates: List<Candidate> = emptyList()
    )

    @Serializable
    data class Candidate(
        val id: Long = 0,
        val accesskey: String = "",
        val duration: Long = 0,
        val title: String? = null,
        val singer: String? = null,
        val song: String? = null
    )

    @Serializable
    private data class DownloadResponse(
        val status: Int = 0,
        val content: String = "",
        val fmt: String? = null,
        val charset: String? = null
    )

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Fetches the best-matching LRC lyrics for [title]/[artist].
     *
     * @param duration track duration in seconds; `-1` disables duration matching.
     */
    suspend fun getLyrics(title: String, artist: String, duration: Int): Result<String> =
        runCatching {
            val candidate = bestCandidate(title, artist, duration)
                ?: throw IllegalStateException("No lyrics candidate found")
            fetchCandidate(candidate).normalize()
        }

    /**
     * Calls [callback] with every distinct LRC variant found for [title]/[artist].
     */
    suspend fun getAllPossibleLyricsOptions(
        title: String,
        artist: String,
        duration: Int,
        callback: (String) -> Unit
    ) {
        val keyword = "$title - $artist"
        val candidates = search(keyword, duration)
            .sortedBy { if (duration > 0) abs(it.duration - duration * 1000L) else 0L }

        val seen = mutableSetOf<String>()
        for (candidate in candidates.take(MAX_OPTIONS)) {
            val lyrics = runCatching { fetchCandidate(candidate).normalize() }.getOrNull() ?: continue
            if (lyrics.isNotBlank() && seen.add(lyrics)) callback(lyrics)
        }
    }

    /**
     * Searches lyric candidates and maps them onto the common [Lyrics] model
     * (without downloading the actual lyrics bodies).
     */
    suspend fun searchLyrics(title: String, artist: String, duration: Int): List<Lyrics> {
        val candidates = search("$title - $artist", duration)
        return candidates.map { candidate ->
            Lyrics(
                id = candidate.id.toString(),
                title = candidate.song ?: candidate.title ?: title,
                subtitle = candidate.singer ?: artist,
                extras = buildMap {
                    put("accesskey", candidate.accesskey)
                    put("duration", candidate.duration.toString())
                }
            )
        }
    }

    // ── Internals ───────────────────────────────────────────────────────────

    private suspend fun bestCandidate(title: String, artist: String, duration: Int): Candidate? {
        val candidates = search("$title - $artist", duration)
        if (candidates.isEmpty()) return null
        if (duration <= 0) return candidates.first()
        val durationMs = duration * 1000L
        return candidates
            .filter { abs(it.duration - durationMs) <= DURATION_TOLERANCE * 1000L }
            .minByOrNull { abs(it.duration - durationMs) }
    }

    private suspend fun search(keyword: String, duration: Int): List<Candidate> {
        val url = SEARCH_URL.toHttpUrl().newBuilder().apply {
            addQueryParameter("ver", "1")
            addQueryParameter("man", "yes")
            addQueryParameter("client", "mobi")
            addQueryParameter("keyword", keyword)
            if (duration > 0) addQueryParameter("duration", (duration * 1000L).toString())
            addQueryParameter("page", "1")
            addQueryParameter("pagesize", "10")
        }.build()

        val body = execute(url) ?: return emptyList()
        val response = runCatching { json.decodeFromString<SearchResponse>(body) }.getOrNull()
            ?: return emptyList()
        if (response.status != 200) return emptyList()
        return response.candidates.filter { it.accesskey.isNotBlank() && it.id != 0L }
    }

    /**
     * Downloads the lyrics for [candidate]. Tries the KRC (karaoke) format first,
     * falling back to plain LRC.
     */
    private suspend fun fetchCandidate(candidate: Candidate): String {
        // 1) KRC: base64 -> XOR 0x40 -> zlib inflate -> karaoke text -> LRC.
        val krc = runCatching {
            val content = download(candidate, "krc")
            decodeKrcToLrc(content)
        }.getOrNull()
        if (!krc.isNullOrBlank()) return krc

        // 2) Plain LRC: base64 -> LRC text.
        val lrc = download(candidate, "lrc")
        return Base64.decode(lrc, Base64.DEFAULT).toString(Charsets.UTF_8)
    }

    private suspend fun download(candidate: Candidate, format: String): String {
        val url = DOWNLOAD_URL.toHttpUrl().newBuilder().apply {
            addQueryParameter("ver", "1")
            addQueryParameter("client", "mobi")
            addQueryParameter("id", candidate.id.toString())
            addQueryParameter("accesskey", candidate.accesskey)
            addQueryParameter("fmt", format)
            addQueryParameter("charset", "utf8")
        }.build()

        val body = execute(url)
            ?: throw IllegalStateException("Failed to download lyrics from KuGou")
        val response = json.decodeFromString<DownloadResponse>(body)
        if (response.status != 200 || response.content.isBlank()) {
            throw IllegalStateException("KuGou download failed: status=${response.status}")
        }
        return response.content
    }

    private suspend fun execute(url: okhttp3.HttpUrl): String? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body?.string()
            }
        }.getOrNull()
    }

    // ── KRC decoding ────────────────────────────────────────────────────────

    private val krcLineRegex = Regex("""\[(\d+),(\d+)\](.*)""")
    private val krcWordRegex = Regex("""<(\d+),(\d+),(\d+)>([^<\[]*)""")

    /**
     * Decodes a base64 KRC payload: base64 -> XOR with 0x40 -> zlib inflate,
     * then converts the karaoke timing markup into standard LRC.
     */
    internal fun decodeKrcToLrc(base64Content: String): String {
        val encrypted = Base64.decode(base64Content, Base64.DEFAULT)
        val xored = ByteArray(encrypted.size) { encrypted[it] xor 0x40 }
        val decompressed = inflate(xored)
        val krc = decompressed.toString(Charsets.UTF_8)

        val lrc = StringBuilder()
        for (line in krc.lines()) {
            val match = krcLineRegex.find(line) ?: continue
            val (startMs, _) = match.destructured
            val payload = match.groupValues[3]

            val textBuilder = StringBuilder()
            for (wordMatch in krcWordRegex.findAll(payload)) {
                textBuilder.append(wordMatch.groupValues[4])
            }
            val text = if (textBuilder.isEmpty()) payload else textBuilder.toString()
            if (text.isBlank()) continue

            lrc.append('[').append(formatLrcTime(startMs.toLong())).append(']')
                .append(text.trim())
                .append('\n')
        }
        return lrc.toString().trim()
    }

    private fun inflate(data: ByteArray): ByteArray {
        val inflater = Inflater()
        inflater.setInput(data)
        return try {
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count > 0) {
                    output.write(buffer, 0, count)
                    total += count
                } else if (inflater.needsInput() || inflater.needsDictionary()) {
                    break
                }
                if (total > 64 * 1024 * 1024) break // safety valve
            }
            output.toByteArray()
        } finally {
            inflater.end()
        }
    }

    private fun formatLrcTime(timeMs: Long): String {
        val clamped = timeMs.coerceAtLeast(0)
        val minutes = clamped / 60000
        val seconds = (clamped % 60000) / 1000
        val centis = (clamped % 1000) / 10
        return "%02d:%02d.%02d".format(minutes, seconds, centis)
    }

    // ── Post-processing ─────────────────────────────────────────────────────

    @Suppress("RegExpRedundantEscape")
    private val acceptedLineRegex = """\[\d{2}:\d{2}\.\d{2,3}\].*""".toRegex()

    /** Keeps only timestamped lyric lines and trims metadata/credit noise. */
    private fun String.normalize(): String {
        val lines = lines().filter { it.matches(acceptedLineRegex) }
        val converted = if (useTraditionalChinese) lines.map(::toTraditional) else lines
        return converted.joinToString("\n").trim()
    }

    // ── Simplified -> Traditional Chinese (best-effort, common characters) ───

    private val simplifiedToTraditional: Map<String, String> = mapOf(
        "爱" to "愛", "别" to "別", "宝" to "寶", "报" to "報", "贝" to "貝",
        "备" to "備", "笔" to "筆", "毕" to "畢", "边" to "邊", "变" to "變",
        "标" to "標", "宾" to "賓", "补" to "補", "参" to "參",
        "残" to "殘", "蚕" to "蠶", "灿" to "燦", "层" to "層", "尝" to "嘗",
        "肠" to "腸", "车" to "車", "彻" to "徹", "尘" to "塵", "陈" to "陳",
        "称" to "稱", "惩" to "懲", "迟" to "遲", "冲" to "衝", "丑" to "醜",
        "词" to "詞", "聪" to "聰", "从" to "從", "丛" to "叢", "凑" to "湊",
        "窜" to "竄", "达" to "達", "带" to "帶", "贷" to "貸", "单" to "單",
        "担" to "擔", "胆" to "膽", "弹" to "彈", "当" to "當", "党" to "黨",
        "荡" to "盪", "导" to "導", "岛" to "島", "祷" to "禱", "灯" to "燈",
        "邓" to "鄧", "敌" to "敵", "递" to "遞", "点" to "點", "淀" to "澱",
        "调" to "調", "订" to "訂", "东" to "東", "动" to "動",
        "栋" to "棟", "斗" to "鬥", "独" to "獨", "读" to "讀", "赌" to "賭",
        "镀" to "鍍", "断" to "斷", "缎" to "緞", "队" to "隊", "对" to "對",
        "吨" to "噸", "顿" to "頓", "夺" to "奪", "堕" to "墮", "鹅" to "鵝",
        "额" to "額", "恶" to "惡", "儿" to "兒", "尔" to "爾", "发" to "發",
        "罚" to "罰", "阀" to "閥", "机" to "機", "丰" to "豐",
        "风" to "風", "凤" to "鳳", "肤" to "膚", "妇" to "婦", "复" to "復",
        "负" to "負", "讣" to "訃", "该" to "該", "盖" to "蓋", "纲" to "綱", "镐" to "鎬", "个" to "個", "给" to "給", "巩" to "鞏",
        "贡" to "貢", "沟" to "溝", "构" to "構", "购" to "購", "顾" to "顧", "雇" to "僱", "刮" to "颳", "挂" to "掛", "关" to "關",
        "观" to "觀", "馆" to "館", "惯" to "慣", "贯" to "貫", "广" to "廣",
        "归" to "歸", "龟" to "龜", "规" to "規", "贵" to "貴", "国" to "國",
        "过" to "過", "汉" to "漢", "号" to "號", "轰" to "轟",
        "红" to "紅", "后" to "後", "壶" to "壺", "护" to "護", "沪" to "滬",
        "华" to "華", "画" to "畫", "话" to "話", "怀" to "懷",
        "坏" to "壞", "欢" to "歡", "环" to "環", "还" to "還", "换" to "換",
        "唤" to "喚", "黄" to "黃", "挥" to "揮", "辉" to "輝",
        "汇" to "匯", "会" to "會", "浑" to "渾", "伙" to "夥", "获" to "獲", "货" to "貨",
        "祸" to "禍", "击" to "擊", "饥" to "饑", "积" to "積", "鸡" to "雞",
        "极" to "極", "际" to "際", "剂" to "劑", "济" to "濟", "继" to "繼",
        "计" to "計", "记" to "記", "纪" to "紀", "价" to "價", "驾" to "駕",
        "歼" to "殲", "监" to "監", "检" to "檢", "简" to "簡", "见" to "見",
        "舰" to "艦", "剑" to "劍", "渐" to "漸", "践" to "踐", "将" to "將",
        "浆" to "漿", "奖" to "獎", "讲" to "講", "酱" to "醬", "胶" to "膠",
        "届" to "屆", "紧" to "緊", "谨" to "謹", "进" to "進", "尽" to "盡",
        "劲" to "勁", "惊" to "驚", "经" to "經", "静" to "靜", "镜" to "鏡",
        "竞" to "競", "纠" to "糾", "旧" to "舊", "剧" to "劇", "惧" to "懼",
        "据" to "據", "鹃" to "鵑", "卷" to "捲", "觉" to "覺", "决" to "決",
        "绝" to "絕", "军" to "軍", "骏" to "駿", "开" to "開", "凯" to "凱",
        "颗" to "顆", "壳" to "殼", "课" to "課", "恳" to "懇", "夸" to "誇",
        "块" to "塊", "亏" to "虧", "扩" to "擴", "阔" to "闊",
        "腊" to "臘", "蜡" to "蠟", "来" to "來", "赖" to "賴", "蓝" to "藍",
        "兰" to "蘭", "拦" to "攔", "栏" to "欄", "烂" to "爛", "劳" to "勞",
        "涝" to "澇", "乐" to "樂", "类" to "類", "泪" to "淚", "厘" to "釐",
        "离" to "離", "里" to "裡", "礼" to "禮", "丽" to "麗", "历" to "歷",
        "厉" to "厲", "励" to "勵", "联" to "聯", "连" to "連", "怜" to "憐",
        "帘" to "簾", "莲" to "蓮", "练" to "練", "炼" to "煉", "两" to "兩",
        "辆" to "輛", "疗" to "療", "辽" to "遼", "猎" to "獵", "临" to "臨",
        "邻" to "鄰", "鳞" to "鱗", "灵" to "靈", "岭" to "嶺", "领" to "領",
        "刘" to "劉", "龙" to "龍", "笼" to "籠", "楼" to "樓", "娄" to "婁",
        "芦" to "蘆", "卢" to "盧", "炉" to "爐", "陆" to "陸", "录" to "錄",
        "虑" to "慮", "论" to "論", "轮" to "輪", "罗" to "羅", "络" to "絡",
        "妈" to "媽", "马" to "馬", "骂" to "罵", "吗" to "嗎", "买" to "買",
        "卖" to "賣", "脉" to "脈", "满" to "滿", "猫" to "貓", "贸" to "貿",
        "门" to "門", "闷" to "悶", "们" to "們", "梦" to "夢", "谜" to "謎",
        "弥" to "彌", "庙" to "廟", "灭" to "滅",
        "悯" to "憫", "鸣" to "鳴", "谬" to "謬", "谋" to "謀", "亩" to "畝",
        "纳" to "納", "难" to "難", "脑" to "腦", "闹" to "鬧", "内" to "內",
        "拟" to "擬", "酿" to "釀", "鸟" to "鳥", "聂" to "聶", "宁" to "寧",
        "柠" to "檸", "钮" to "鈕", "农" to "農", "浓" to "濃",
        "欧" to "歐", "殴" to "毆", "呕" to "嘔", "盘" to "盤", "庞" to "龐",
        "赔" to "賠", "喷" to "噴", "鹏" to "鵬", "骗" to "騙", "飘" to "飄",
        "频" to "頻", "贫" to "貧", "苹" to "蘋", "凭" to "憑", "评" to "評",
        "泼" to "潑", "扑" to "撲", "铺" to "鋪", "仆" to "僕", "朴" to "樸",
        "谱" to "譜", "栖" to "棲", "齐" to "齊", "骑" to "騎", "启" to "啟",
        "气" to "氣", "弃" to "棄",        "签" to "簽", "钱" to "錢",
        "潜" to "潛", "浅" to "淺", "枪" to "槍", "强" to "強", "墙" to "牆",
        "抢" to "搶", "桥" to "橋", "侨" to "僑", "翘" to "翹", "窃" to "竊",
        "亲" to "親", "钦" to "欽", "寝" to "寢", "庆" to "慶", "琼" to "瓊",
        "区" to "區", "驱" to "驅", "趋" to "趨", "权" to "權", "劝" to "勸", "却" to "卻", "让" to "讓",
        "扰" to "擾", "热" to "熱", "认" to "認", "韧" to "韌", "荣" to "榮",
        "绒" to "絨", "软" to "軟", "锐" to "銳", "闰" to "閏", "润" to "潤",
        "洒" to "灑", "萨" to "薩", "赛" to "賽", "伞" to "傘", "丧" to "喪",
        "扫" to "掃", "涩" to "澀", "杀" to "殺", "晒" to "曬", "闪" to "閃", "陕" to "陝", "赡" to "贍", "缮" to "繕", "伤" to "傷",
        "赏" to "賞", "烧" to "燒", "绍" to "紹", "设" to "設", "绅" to "紳",
        "审" to "審", "肾" to "腎", "声" to "聲", "胜" to "勝", "圣" to "聖",
        "师" to "師", "时" to "時", "湿" to "濕", "实" to "實", "诗" to "詩",
        "势" to "勢", "适" to "適", "释" to "釋", "视" to "視",
        "试" to "試", "寿" to "壽", "兽" to "獸", "书" to "書", "属" to "屬",
        "术" to "術", "树" to "樹", "竖" to "豎", "帅" to "帥", "双" to "雙",
        "谁" to "誰", "顺" to "順", "说" to "說", "硕" to "碩", "丝" to "絲",
        "肃" to "肅", "虽" to "雖", "随" to "隨", "岁" to "歲", "孙" to "孫",
        "损" to "損", "缩" to "縮", "锁" to "鎖", "态" to "態", "摊" to "攤",
        "瘫" to "癱", "贪" to "貪", "坛" to "壇", "汤" to "湯", "涛" to "濤", "讨" to "討", "腾" to "騰", "题" to "題",
        "体" to "體", "条" to "條", "铁" to "鐵", "厅" to "廳", "听" to "聽",
        "统" to "統", "头" to "頭", "图" to "圖", "涂" to "塗", "团" to "團",
        "颓" to "頹", "蜕" to "蛻", "脱" to "脫", "驮" to "馱", "弯" to "彎",
        "湾" to "灣", "万" to "萬", "网" to "網", "韦" to "韋", "违" to "違",
        "围" to "圍", "为" to "為", "维" to "維", "伟" to "偉", "纬" to "緯",
        "卫" to "衛", "温" to "溫", "闻" to "聞", "稳" to "穩", "问" to "問",
        "涡" to "渦", "无" to "無", "牺" to "犧", "习" to "習",
        "侠" to "俠", "狭" to "狹", "吓" to "嚇", "纤" to "纖", "鲜" to "鮮",
        "贤" to "賢", "显" to "顯", "险" to "險", "现" to "現", "献" to "獻",
        "县" to "縣", "宪" to "憲", "线" to "線", "乡" to "鄉",
        "详" to "詳", "响" to "響", "项" to "項", "萧" to "蕭",
        "协" to "協", "挟" to "挾", "写" to "寫", "泻" to "瀉",
        "谢" to "謝", "兴" to "興", "须" to "須", "虚" to "虛", "续" to "續",
        "选" to "選", "旋" to "鏇", "学" to "學", "询" to "詢",
        "训" to "訓", "讯" to "訊", "逊" to "遜", "压" to "壓", "鸦" to "鴉",
        "盐" to "鹽", "严" to "嚴", "颜" to "顏", "阎" to "閻", "艳" to "艷",
        "厌" to "厭", "验" to "驗", "阳" to "陽", "养" to "養", "样" to "樣",
        "尧" to "堯", "爷" to "爺", "业" to "業", "叶" to "葉", "页" to "頁",
        "医" to "醫", "仪" to "儀", "义" to "義", "议" to "議", "亿" to "億",
        "艺" to "藝", "忆" to "憶", "异" to "異", "阴" to "陰", "银" to "銀",
        "隐" to "隱", "应" to "應", "婴" to "嬰", "鹰" to "鷹", "营" to "營",
        "蝇" to "蠅", "赢" to "贏", "拥" to "擁", "涌" to "湧",
        "佣" to "傭", "忧" to "憂", "优" to "優", "邮" to "郵", "犹" to "猶",
        "游" to "遊", "诱" to "誘", "舆" to "輿", "与" to "與", "屿" to "嶼",
        "语" to "語", "誉" to "譽", "预" to "預", "驭" to "馭", "鸳" to "鴛",
        "渊" to "淵", "园" to "園", "员" to "員", "圆" to "圓", "缘" to "緣",
        "远" to "遠", "愿" to "願", "约" to "約", "跃" to "躍", "阅" to "閱",
        "云" to "雲", "运" to "運", "酝" to "醞", "杂" to "雜", "灾" to "災",
        "载" to "載", "赞" to "讚", "赃" to "贓", "脏" to "髒", "凿" to "鑿",
        "枣" to "棗", "灶" to "竈", "择" to "擇", "泽" to "澤", "贼" to "賊",
        "赠" to "贈", "扎" to "紮", "札" to "劄", "铡" to "鍘", "债" to "債",
        "战" to "戰", "张" to "張", "账" to "賬", "赵" to "趙", "这" to "這",
        "针" to "針", "侦" to "偵", "诊" to "診", "镇" to "鎮", "阵" to "陣",
        "争" to "爭", "征" to "徵", "挣" to "掙", "睁" to "睜", "证" to "證",
        "郑" to "鄭", "织" to "織", "职" to "職", "执" to "執", "纸" to "紙",
        "志" to "誌", "制" to "製", "质" to "質", "钟" to "鐘", "终" to "終",
        "种" to "種", "众" to "眾", "昼" to "晝", "猪" to "豬",
        "铸" to "鑄", "筑" to "築", "专" to "專", "转" to "轉",
        "赚" to "賺", "妆" to "妝", "庄" to "莊", "装" to "裝", "壮" to "壯",
        "状" to "狀", "坠" to "墜", "谆" to "諄", "浊" to "濁", "总" to "總",
        "纵" to "縱", "钻" to "鑽",
    )

    internal fun toTraditional(text: String): String = buildString(text.length) {
        for (char in text) {
            val mapped = simplifiedToTraditional[char.toString()]
            if (mapped != null) append(mapped) else append(char)
        }
    }
}
