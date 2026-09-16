/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package dev.brahmkshatriya.echo.dhun.betterlyrics

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

/**
 * A real, dependency-free XML parser for TTML (Timed Text Markup Language)
 * timed lyrics — the format served by the Better Lyrics / Apple Music style
 * sources.
 *
 * It understands:
 *  - `<p begin="..." end="...">` line elements nested anywhere under `<body>`
 *    (through any number of `<div>`s, as Apple Music TTML uses).
 *  - `<span begin="..." end="...">` word/syllable-level timing.
 *  - `ttm:agent` attributes (`"v2"` marks background vocals).
 *  - Both clock-time (`hh:mm:ss.mmm`) and offset-time (`1.5s`, `1500ms`)
 *    expressions, with `.` or `,` as the fraction separator.
 */
object TTMLParser {

    /** A timed word (or syllable) inside a [Line]. Times are in seconds. */
    data class Word(
        val text: String,
        val startTime: Double,
        val endTime: Double,
        val isBackground: Boolean = false,
    )

    /** A timed lyric line. Times are in seconds. */
    data class Line(
        val text: String,
        val startTime: Double,
        val endTime: Double,
        val agent: String? = null,
        val words: List<Word> = emptyList(),
    )

    private const val TTML_NS = "http://www.w3.org/ns/ttml"
    private const val TTM_NS = "http://www.w3.org/ns/ttml#metadata"
    private const val BACKGROUND_AGENT = "v2"

    /** Parses a TTML document into a chronologically ordered list of [Line]s. */
    fun parseTTML(ttml: String): List<Line> {
        val document = runCatching { parseXml(ttml) }.getOrNull() ?: return emptyList()

        val lines = mutableListOf<Line>()
        collectElements(document.documentElement) { element ->
            if (element.localName == "p") {
                parseLineElement(element)?.let { lines += it }
            }
        }
        return lines.sortedBy { it.startTime }
    }

    // ── Element parsing ─────────────────────────────────────────────────────

    private fun parseLineElement(p: Element): Line? {
        val begin = parseTime(getAttribute(p, "begin")) ?: return null
        val end = parseTime(getAttribute(p, "end"))
            ?: parseTime(getAttribute(p, "dur"))?.let { begin + it }
            ?: begin
        if (end < begin) return null

        val agent = getAgent(p)

        val words = mutableListOf<Word>()
        val textBuilder = StringBuilder()

        var node: Node? = p.firstChild
        while (node != null) {
            when (node.nodeType) {
                Node.TEXT_NODE -> textBuilder.append(node.textContent)

                Node.ELEMENT_NODE -> {
                    val child = node as Element
                    if (child.localName == "span") {
                        val wordText = child.textContent
                        textBuilder.append(wordText)

                        val wordBegin = parseTime(getAttribute(child, "begin"))
                        val wordEnd = parseTime(getAttribute(child, "end"))
                            ?: parseTime(getAttribute(child, "dur"))?.let { dur ->
                                (wordBegin ?: begin) + dur
                            }
                        if (wordText.isNotBlank() && wordBegin != null && wordEnd != null) {
                            words += Word(
                                text = wordText,
                                startTime = wordBegin,
                                endTime = wordEnd,
                                isBackground = isBackground(getAgent(child), agent),
                            )
                        }
                    } else {
                        // Metadata / unknown nodes: keep their visible text.
                        textBuilder.append(child.textContent)
                    }
                }
            }
            node = node.nextSibling
        }

        val text = textBuilder.toString().replace("\n", " ").trim()
        if (text.isEmpty() && words.isEmpty()) return null

        return Line(
            text = text,
            startTime = begin,
            endTime = end,
            agent = agent,
            words = words,
        )
    }

    /** True when the element (or its parent line) is a background vocal. */
    private fun isBackground(spanAgent: String?, lineAgent: String?): Boolean =
        spanAgent == BACKGROUND_AGENT || lineAgent == BACKGROUND_AGENT

    private fun getAgent(element: Element): String? =
        element.getAttributeNS(TTM_NS, "agent")?.takeIf { it.isNotBlank() }
            ?: runCatching {
                element.getAttribute("ttm:agent").takeIf { it.isNotBlank() }
            }.getOrNull()

    /** Reads a (possibly namespaced) attribute from [element]. */
    private fun getAttribute(element: Element, name: String): String? =
        runCatching {
            (0 until element.attributes.length)
                .asSequence()
                .map { element.attributes.item(it) }
                .firstOrNull { it.localName == name || it.nodeName == name }
                ?.nodeValue
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()

    /** Depth-first traversal of every descendant [Element]. */
    private fun collectElements(root: Element, visit: (Element) -> Unit) {
        var node: Node? = root.firstChild
        while (node != null) {
            if (node.nodeType == Node.ELEMENT_NODE) {
                val element = node as Element
                visit(element)
                collectElements(element, visit)
            }
            node = node.nextSibling
        }
    }

    // ── XML document parsing ────────────────────────────────────────────────

    private fun parseXml(xml: String): Document {
        val factory = DocumentBuilderFactory.newInstance()
        runCatching {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        }
        runCatching { factory.isExpandEntityReferences = false }
        runCatching { factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        factory.isNamespaceAware = true

        val builder = factory.newDocumentBuilder()
        builder.setErrorHandler(null)
        return builder.parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
    }

    // ── Time expressions ─────────────────────────────────────────────────────

    private val clockTimeRegex = Regex("""^(?:(\d+):)?(\d+):(\d+(?:[.,]\d+)?)$""")
    private val offsetTimeRegex = Regex("""^(\d+(?:\.\d+)?)(ms|s|h|m|t)?$""")

    /**
     * Parses a TTML time expression into seconds.
     *
     * Supported: `hh:mm:ss.fraction`, `mm:ss.fraction`, `12.34s`, `1234ms`,
     * `2h`, `3m`. Returns `null` for anything unparseable (e.g. raw ticks).
     */
    internal fun parseTime(expression: String?): Double? {
        if (expression.isNullOrBlank()) return null
        val value = expression.trim()

        clockTimeRegex.find(value)?.let { match ->
            val (hours, minutes, seconds) = match.destructured
            val h = hours.toDoubleOrNull() ?: 0.0
            val m = minutes.toDoubleOrNull() ?: 0.0
            val s = seconds.replace(',', '.').toDoubleOrNull() ?: return null
            return h * 3600.0 + m * 60.0 + s
        }

        offsetTimeRegex.find(value)?.let { match ->
            val (amount, unit) = match.destructured
            val number = amount.toDoubleOrNull() ?: return null
            return when (unit) {
                "ms" -> number / 1000.0
                "s", "" -> number
                "m" -> number * 60.0
                "h" -> number * 3600.0
                else -> null // "t" (ticks) needs the tick rate; unsupported.
            }
        }

        return null
    }
}
