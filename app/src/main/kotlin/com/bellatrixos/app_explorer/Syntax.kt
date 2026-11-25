package com.bellatrixos.app_explorer

/** How a language's text is scanned for colouring. */
enum class SyntaxStyle { NONE, CODE, JSON, MARKUP, KEY_VALUE, YAML, MARKDOWN, DIFF }

/** What a code-like language has in common: its words, comments and quotes. */
class LanguageSpec(
    val keywords: Set<String>,
    val lineComments: List<String> = emptyList(),
    val blockComment: Pair<String, String>? = null,
    /** Characters that open a string ended by the same character on the same line. */
    val quotes: String = "\"'",
    /** Characters that open a string that may run over several lines. */
    val multilineQuotes: String = "",
    /** `"""` or `'''` open a string that runs over lines (Python, Kotlin). */
    val tripleQuotes: Boolean = false,
    /** An upper-case first letter marks a type name. */
    val types: Boolean = true,
    val ignoreCase: Boolean = false,
    /** `#fff` is a number-like colour (CSS). */
    val hashColors: Boolean = false,
)

private fun words(text: String) = text.trim().split(Regex("\\s+")).toSet()

private val SLASH = listOf("//")
private val SLASH_BLOCK = "/*" to "*/"
private val C_WORDS =
    "auto break case char const continue default do double else enum extern float for goto if inline int long register " +
        "restrict return short signed sizeof static struct switch typedef union unsigned void volatile while NULL true false bool"

private val SPEC_KOTLIN =
    LanguageSpec(
        words(
            "as break class continue do else false for fun if in interface is null object package return super this throw " +
                "true try typealias typeof val var when while by catch constructor delegate dynamic field file finally get " +
                "import init param property receiver set setparam value where abstract actual annotation companion const " +
                "crossinline data enum expect external final infix inline inner internal lateinit noinline open operator out " +
                "override private protected public reified sealed suspend tailrec vararg",
        ),
        SLASH,
        SLASH_BLOCK,
        tripleQuotes = true,
    )
private val SPEC_JAVA =
    LanguageSpec(
        words(
            "abstract assert boolean break byte case catch char class const continue default do double else enum extends " +
                "final finally float for goto if implements import instanceof int interface long native new package private " +
                "protected public return short static strictfp super switch synchronized this throw throws transient try " +
                "void volatile while true false null var record sealed permits yield def",
        ),
        SLASH,
        SLASH_BLOCK,
    )
private val JS_WORDS =
    "async await break case catch class const continue debugger default delete do else export extends false finally for " +
        "function if import in instanceof let new null of return static super switch this throw true try typeof undefined " +
        "var void while with yield"
private val SPEC_JS = LanguageSpec(words(JS_WORDS), SLASH, SLASH_BLOCK, multilineQuotes = "`")
private val SPEC_TS =
    LanguageSpec(
        words("$JS_WORDS interface type enum implements namespace declare readonly abstract as from private protected public"),
        SLASH,
        SLASH_BLOCK,
        multilineQuotes = "`",
    )
private val SPEC_PYTHON =
    LanguageSpec(
        words(
            "False None True and as assert async await break class continue def del elif else except finally for from " +
                "global if import in is lambda nonlocal not or pass raise return try while with yield match case self",
        ),
        listOf("#"),
        tripleQuotes = true,
    )
private val SPEC_C = LanguageSpec(words(C_WORDS), SLASH, SLASH_BLOCK)
private val SPEC_CPP =
    LanguageSpec(
        words(
            "$C_WORDS alignas alignof and asm catch class constexpr const_cast decltype delete dynamic_cast explicit " +
                "export friend mutable namespace new noexcept nullptr operator or override private protected public " +
                "reinterpret_cast static_assert static_cast template this throw try typeid typename using virtual final",
        ),
        SLASH,
        SLASH_BLOCK,
    )
private val SPEC_CSHARP =
    LanguageSpec(
        words(
            "abstract as base bool break byte case catch char checked class const continue decimal default delegate do " +
                "double else enum event explicit extern false finally fixed float for foreach goto if implicit in int " +
                "interface internal is lock long namespace new null object operator out override params private protected " +
                "public readonly ref return sbyte sealed short sizeof stackalloc static string struct switch this throw " +
                "true try typeof uint ulong unchecked unsafe ushort using var virtual void volatile while async await record init",
        ),
        SLASH,
        SLASH_BLOCK,
    )
private val SPEC_GO =
    LanguageSpec(
        words(
            "break case chan const continue default defer else fallthrough for func go goto if import interface map " +
                "package range return select struct switch type var true false nil iota",
        ),
        SLASH,
        SLASH_BLOCK,
        multilineQuotes = "`",
    )
private val SPEC_RUST =
    LanguageSpec(
        words(
            "as async await break const continue crate dyn else enum extern false fn for if impl in let loop match mod " +
                "move mut pub ref return self Self static struct super trait true type unsafe use where while",
        ),
        SLASH,
        SLASH_BLOCK,
        quotes = "\"",
    )
private val SPEC_PHP =
    LanguageSpec(
        words(
            "abstract and array as break callable case catch class clone const continue declare default do echo else " +
                "elseif empty extends final finally fn for foreach function global if implements include include_once " +
                "instanceof interface isset list namespace new or print private protected public require require_once " +
                "return static switch throw trait try unset use var while yield true false null",
        ),
        listOf("//", "#"),
        SLASH_BLOCK,
    )
private val SPEC_RUBY =
    LanguageSpec(
        words(
            "alias and begin break case class def do else elsif end ensure false for if in module next nil not or redo " +
                "rescue retry return self super then true undef unless until when while yield",
        ),
        listOf("#"),
    )
private val SPEC_SHELL =
    LanguageSpec(
        words(
            "if then else elif fi case esac for select while until do done in function time echo cd exit export local " +
                "readonly return set shift source unset alias eval exec test true false",
        ),
        listOf("#"),
        types = false,
    )
private val SPEC_SQL =
    LanguageSpec(
        words(
            "select from where insert into values update set delete create table drop alter add column primary key " +
                "foreign references index unique not null default and or in is like between join inner left right outer " +
                "on group by order having limit offset as distinct union all case when then else end exists count sum " +
                "avg min max asc desc view begin commit rollback",
        ),
        listOf("--"),
        SLASH_BLOCK,
        types = false,
        ignoreCase = true,
    )
private val SPEC_LUA =
    LanguageSpec(
        words("and break do else elseif end false for function goto if in local nil not or repeat return then true until while"),
        listOf("--"),
        "--[[" to "]]",
        types = false,
    )
private val SPEC_SWIFT =
    LanguageSpec(
        words(
            "associatedtype class deinit enum extension fileprivate func import init inout internal let open operator " +
                "private protocol public rethrows static struct subscript typealias var break case continue default defer " +
                "do else fallthrough for guard if in repeat return switch where while as catch false is nil super self " +
                "Self throw throws true try",
        ),
        SLASH,
        SLASH_BLOCK,
    )
private val SPEC_DART =
    LanguageSpec(
        words(
            "abstract as assert async await break case catch class const continue covariant default deferred do dynamic " +
                "else enum export extends extension external factory false final finally for Function get hide if " +
                "implements import in interface is late library mixin new null on operator part required rethrow return " +
                "set show static super switch sync this throw true try typedef var void while with yield",
        ),
        SLASH,
        SLASH_BLOCK,
    )
private val SPEC_CSS = LanguageSpec(emptySet(), emptyList(), SLASH_BLOCK, types = false, hashColors = true)
private val SPEC_JSON = LanguageSpec(words("true false null"), emptyList(), types = false, quotes = "\"")

/** The languages Explorer's editor colours, with the extensions that mean each one. */
enum class Language(
    val label: String,
    val style: SyntaxStyle,
    val spec: LanguageSpec?,
    val extensions: Set<String>,
) {
    PLAIN(
        "Text",
        SyntaxStyle.NONE,
        null,
        words(
            "txt log text csv tsv srt vtt lrc tex list rules gitattributes dockerignore vim el lock owl nfo asc pem " +
                "sum mod cmake in out",
        ),
    ),
    KOTLIN("Kotlin", SyntaxStyle.CODE, SPEC_KOTLIN, words("kt kts")),
    JAVA("Java", SyntaxStyle.CODE, SPEC_JAVA, words("java")),
    GRADLE("Gradle", SyntaxStyle.CODE, SPEC_JAVA, words("gradle groovy")),
    JAVASCRIPT("JavaScript", SyntaxStyle.CODE, SPEC_JS, words("js mjs cjs jsx")),
    TYPESCRIPT("TypeScript", SyntaxStyle.CODE, SPEC_TS, words("ts tsx")),
    PYTHON("Python", SyntaxStyle.CODE, SPEC_PYTHON, words("py pyw")),
    C("C", SyntaxStyle.CODE, SPEC_C, words("c h")),
    CPP("C++", SyntaxStyle.CODE, SPEC_CPP, words("cpp cc cxx hpp hh hxx ino")),
    CSHARP("C#", SyntaxStyle.CODE, SPEC_CSHARP, words("cs")),
    GO("Go", SyntaxStyle.CODE, SPEC_GO, words("go")),
    RUST("Rust", SyntaxStyle.CODE, SPEC_RUST, words("rs")),
    PHP("PHP", SyntaxStyle.CODE, SPEC_PHP, words("php")),
    RUBY("Ruby", SyntaxStyle.CODE, SPEC_RUBY, words("rb")),
    SHELL(
        "Shell",
        SyntaxStyle.CODE,
        SPEC_SHELL,
        words("sh bash zsh fish bashrc zshrc profile bash_profile zprofile mk"),
    ),
    SQL("SQL", SyntaxStyle.CODE, SPEC_SQL, words("sql")),
    LUA("Lua", SyntaxStyle.CODE, SPEC_LUA, words("lua")),
    SWIFT("Swift", SyntaxStyle.CODE, SPEC_SWIFT, words("swift")),
    DART("Dart", SyntaxStyle.CODE, SPEC_DART, words("dart")),
    CSS("CSS", SyntaxStyle.CODE, SPEC_CSS, words("css scss less")),
    JSON("JSON", SyntaxStyle.JSON, SPEC_JSON, words("json jsonc json5 geojson")),
    MARKUP("HTML / XML", SyntaxStyle.MARKUP, null, words("xml html htm xhtml svg xsl xaml plist")),
    YAML("YAML", SyntaxStyle.YAML, null, words("yml yaml")),
    INI(
        "Config",
        SyntaxStyle.KEY_VALUE,
        null,
        words("ini toml conf cfg properties desktop service env editorconfig gitconfig gitignore"),
    ),
    MARKDOWN("Markdown", SyntaxStyle.MARKDOWN, null, words("md markdown")),
    DIFF("Diff", SyntaxStyle.DIFF, null, words("diff patch")),
    ;

    /** Code-like text is not wrapped and gets line numbers; prose is wrapped. */
    val code: Boolean get() = this != PLAIN && this != MARKDOWN
}

/** What Explorer does with a file when it is tapped. */
object FileKinds {
    enum class Route {
        /** Text or code: the editor in the Explorer window. */
        EDITOR,

        /** Not text: another app (documents, archives, packages, fonts, databases, binaries). */
        EXTERNAL,

        /** An extension nobody knows: look inside, text goes to the editor and the rest to another app. */
        SNIFF,
    }

    private val EXTERNAL =
        words(
            "pdf doc docx xls xlsx ppt pptx odt ods odp rtf epub mobi zip rar 7z tar gz tgz bz2 xz zst lz4 cab jar war " +
                "apk apks xapk aab iso img dmg bin exe dll so dex class o a msi deb rpm ttf otf woff woff2 db sqlite " +
                "sqlite3 mdb realm psd ai flac opus wma aac m4a mp3 ogg wav mid midi avi flv wmv m4v mkv mp4 mov webm " +
                "3gp mpg mpeg bmp gif jpg jpeg png webp heic heif avif ico tif tiff raw",
        )

    private val byExtension: Map<String, Language> by lazy {
        val map = HashMap<String, Language>()
        for (language in Language.values()) for (ext in language.extensions) map[ext] = language
        map
    }

    private val byName =
        mapOf(
            "makefile" to Language.SHELL,
            "dockerfile" to Language.SHELL,
            "gemfile" to Language.RUBY,
            "rakefile" to Language.RUBY,
            "cmakelists.txt" to Language.PLAIN,
            "readme" to Language.PLAIN,
            "license" to Language.PLAIN,
        )

    private fun extensionOf(name: String) = name.substringAfterLast('.', "").lowercase()

    fun route(name: String): Route {
        val ext = extensionOf(name)
        return when {
            ext in byExtension -> Route.EDITOR
            ext in EXTERNAL -> Route.EXTERNAL
            name.lowercase() in byName -> Route.EDITOR
            else -> Route.SNIFF
        }
    }

    /** The language for [name], or for an unknown name the one its first line ([head]) gives away. */
    fun languageOf(
        name: String,
        head: String? = null,
    ): Language {
        byExtension[extensionOf(name)]?.let { return it }
        byName[name.lowercase()]?.let { return it }
        val line = head?.trimStart().orEmpty()
        return when {
            line.startsWith("#!") ->
                when {
                    "python" in line -> Language.PYTHON
                    "node" in line || "deno" in line -> Language.JAVASCRIPT
                    "ruby" in line -> Language.RUBY
                    "php" in line -> Language.PHP
                    "lua" in line -> Language.LUA
                    else -> Language.SHELL
                }
            line.startsWith("<?xml") || line.startsWith("<!doctype html", ignoreCase = true) ||
                line.startsWith("<html", ignoreCase = true) -> Language.MARKUP
            line.startsWith("{") || line.startsWith("[{") -> Language.JSON
            else -> Language.PLAIN
        }
    }
}

/** Colours text by splitting it into tokens; no Android types, so it can be tested. */
object SyntaxHighlighter {
    enum class Kind { KEYWORD, STRING, COMMENT, NUMBER, TYPE, KEY, TAG, ATTRIBUTE, HEADING, ADDED, REMOVED }

    class Token(
        val start: Int,
        val end: Int,
        val kind: Kind,
    )

    fun tokens(
        text: CharSequence,
        language: Language,
    ): List<Token> {
        val out = ArrayList<Token>()
        when (language.style) {
            SyntaxStyle.NONE -> {}
            SyntaxStyle.CODE -> language.spec?.let { scanCode(text, it, false, out) }
            SyntaxStyle.JSON -> language.spec?.let { scanCode(text, it, true, out) }
            SyntaxStyle.MARKUP -> scanMarkup(text, out)
            SyntaxStyle.KEY_VALUE -> scanKeyValue(text, yaml = false, out)
            SyntaxStyle.YAML -> scanKeyValue(text, yaml = true, out)
            SyntaxStyle.MARKDOWN -> scanMarkdown(text, out)
            SyntaxStyle.DIFF -> scanDiff(text, out)
        }
        return out
    }

    private fun startsWith(
        t: CharSequence,
        at: Int,
        prefix: String,
    ): Boolean {
        if (at + prefix.length > t.length) return false
        for (k in prefix.indices) if (t[at + k] != prefix[k]) return false
        return true
    }

    private fun lineEnd(
        t: CharSequence,
        from: Int,
    ): Int {
        var i = from
        while (i < t.length && t[i] != '\n') i++
        return i
    }

    private fun indexOf(
        t: CharSequence,
        needle: String,
        from: Int,
    ): Int {
        var i = from
        while (i + needle.length <= t.length) {
            if (startsWith(t, i, needle)) return i
            i++
        }
        return -1
    }

    /** The end of the string that opens at [at], [quote] being its opening character. */
    private fun stringEnd(
        t: CharSequence,
        at: Int,
        quote: Char,
        spec: LanguageSpec,
    ): Int {
        val n = t.length
        if (spec.tripleQuotes && startsWith(t, at, "$quote$quote$quote")) {
            val close = indexOf(t, "$quote$quote$quote", at + 3)
            return if (close < 0) n else close + 3
        }
        val multiline = spec.multilineQuotes.indexOf(quote) >= 0
        var j = at + 1
        while (j < n) {
            val ch = t[j]
            if (ch == '\\') {
                j += 2
                continue
            }
            if (ch == quote) return j + 1
            if (ch == '\n' && !multiline) return j
            j++
        }
        return n
    }

    private fun isWordStart(c: Char) = c.isLetter() || c == '_' || c == '$'

    private fun isWordPart(c: Char) = c.isLetterOrDigit() || c == '_' || c == '$'

    private fun scanCode(
        t: CharSequence,
        spec: LanguageSpec,
        jsonKeys: Boolean,
        out: MutableList<Token>,
    ) {
        val n = t.length
        var i = 0
        while (i < n) {
            val c = t[i]
            // A block comment is looked for first: Lua's `--[[` also starts with its line comment `--`.
            val block = spec.blockComment
            if (block != null && startsWith(t, i, block.first)) {
                val close = indexOf(t, block.second, i + block.first.length)
                val end = if (close < 0) n else close + block.second.length
                out.add(Token(i, end, Kind.COMMENT))
                i = end
                continue
            }
            val comment = spec.lineComments.firstOrNull { startsWith(t, i, it) }
            if (comment != null) {
                val end = lineEnd(t, i)
                out.add(Token(i, end, Kind.COMMENT))
                i = end
                continue
            }
            if (spec.quotes.indexOf(c) >= 0 || spec.multilineQuotes.indexOf(c) >= 0) {
                val end = stringEnd(t, i, c, spec)
                var after = end
                while (after < n && (t[after] == ' ' || t[after] == '\t')) after++
                val isKey = jsonKeys && after < n && t[after] == ':'
                out.add(Token(i, end, if (isKey) Kind.KEY else Kind.STRING))
                i = end
                continue
            }
            if (spec.hashColors && c == '#' && i + 1 < n && t[i + 1].isLetterOrDigit()) {
                var end = i + 1
                while (end < n && t[end].isLetterOrDigit()) end++
                out.add(Token(i, end, Kind.NUMBER))
                i = end
                continue
            }
            if (c.isDigit() || (c == '.' && i + 1 < n && t[i + 1].isDigit())) {
                var end = i + 1
                while (end < n && (t[end].isLetterOrDigit() || t[end] == '.' || t[end] == '_')) end++
                out.add(Token(i, end, Kind.NUMBER))
                i = end
                continue
            }
            if (isWordStart(c)) {
                var end = i + 1
                while (end < n && isWordPart(t[end])) end++
                val word = t.subSequence(i, end).toString()
                val key = if (spec.ignoreCase) word.lowercase() else word
                when {
                    key in spec.keywords -> out.add(Token(i, end, Kind.KEYWORD))
                    spec.types && word.length > 1 && word[0].isUpperCase() -> out.add(Token(i, end, Kind.TYPE))
                }
                i = end
                continue
            }
            i++
        }
    }

    private fun scanMarkup(
        t: CharSequence,
        out: MutableList<Token>,
    ) {
        val n = t.length
        var i = 0
        while (i < n) {
            if (startsWith(t, i, "<!--")) {
                val close = indexOf(t, "-->", i + 4)
                val end = if (close < 0) n else close + 3
                out.add(Token(i, end, Kind.COMMENT))
                i = end
                continue
            }
            val opensTag = t[i] == '<' && i + 1 < n && (t[i + 1].isLetter() || t[i + 1] == '/' || t[i + 1] == '!' || t[i + 1] == '?')
            if (!opensTag) {
                i++
                continue
            }
            var j = i + 1
            while (j < n && (t[j] == '/' || t[j] == '!' || t[j] == '?')) j++
            while (j < n && (t[j].isLetterOrDigit() || t[j] == '-' || t[j] == '_' || t[j] == ':' || t[j] == '.')) j++
            out.add(Token(i, j, Kind.TAG))
            // Attributes, up to the tag's closing bracket.
            while (j < n && t[j] != '>') {
                val ch = t[j]
                when {
                    ch == '"' || ch == '\'' -> {
                        var end = j + 1
                        while (end < n && t[end] != ch) end++
                        end = minOf(end + 1, n)
                        out.add(Token(j, end, Kind.STRING))
                        j = end
                    }
                    ch.isLetter() || ch == '_' || ch == ':' -> {
                        var end = j + 1
                        while (end < n && (t[end].isLetterOrDigit() || t[end] == '-' || t[end] == '_' || t[end] == ':' || t[end] == '.')) end++
                        out.add(Token(j, end, Kind.ATTRIBUTE))
                        j = end
                    }
                    else -> j++
                }
            }
            if (j < n) {
                val from = if (j > i && t[j - 1] == '/') j - 1 else j
                out.add(Token(from, j + 1, Kind.TAG))
                j++
            }
            i = j
        }
    }

    private fun scanKeyValue(
        t: CharSequence,
        yaml: Boolean,
        out: MutableList<Token>,
    ) {
        val n = t.length
        var lineStart = 0
        while (lineStart < n) {
            val end = lineEnd(t, lineStart)
            var i = lineStart
            while (i < end && (t[i] == ' ' || t[i] == '\t')) i++
            if (i < end) {
                val c = t[i]
                when {
                    c == '#' || (!yaml && c == ';') -> out.add(Token(i, end, Kind.COMMENT))
                    !yaml && c == '[' -> out.add(Token(i, end, Kind.HEADING))
                    else -> {
                        if (yaml && c == '-' && i + 1 < end && t[i + 1] == ' ') {
                            i += 2
                            while (i < end && t[i] == ' ') i++
                        }
                        var sep = i
                        val mark = if (yaml) ':' else '='
                        while (sep < end && t[sep] != mark && !(yaml && t[sep] == '#')) sep++
                        var valueFrom = i
                        if (sep < end && t[sep] == mark && sep > i) {
                            var keyEnd = sep
                            while (keyEnd > i && (t[keyEnd - 1] == ' ' || t[keyEnd - 1] == '\t')) keyEnd--
                            out.add(Token(i, keyEnd, Kind.KEY))
                            valueFrom = sep + 1
                        }
                        scanValue(t, valueFrom, end, out)
                    }
                }
            }
            lineStart = end + 1
        }
    }

    /** One line's value: quoted strings, numbers and the usual literals. */
    private fun scanValue(
        t: CharSequence,
        from: Int,
        end: Int,
        out: MutableList<Token>,
    ) {
        var i = from
        while (i < end) {
            val c = t[i]
            when {
                c == '"' || c == '\'' -> {
                    var close = i + 1
                    while (close < end && t[close] != c) close++
                    close = minOf(close + 1, end)
                    out.add(Token(i, close, Kind.STRING))
                    i = close
                }
                c == '#' && (i == from || t[i - 1] == ' ') -> {
                    out.add(Token(i, end, Kind.COMMENT))
                    i = end
                }
                c.isDigit() && (i == from || !t[i - 1].isLetterOrDigit()) -> {
                    var close = i + 1
                    while (close < end && (t[close].isLetterOrDigit() || t[close] == '.')) close++
                    out.add(Token(i, close, Kind.NUMBER))
                    i = close
                }
                c.isLetter() -> {
                    var close = i + 1
                    while (close < end && (t[close].isLetterOrDigit() || t[close] == '_')) close++
                    if (t.subSequence(i, close).toString().lowercase() in LITERALS) out.add(Token(i, close, Kind.KEYWORD))
                    i = close
                }
                else -> i++
            }
        }
    }

    private val LITERALS = setOf("true", "false", "null", "yes", "no", "on", "off", "none")

    private fun scanMarkdown(
        t: CharSequence,
        out: MutableList<Token>,
    ) {
        val n = t.length
        var lineStart = 0
        var fenced = false
        while (lineStart < n) {
            val end = lineEnd(t, lineStart)
            if (startsWith(t, lineStart, "```") || startsWith(t, lineStart, "~~~")) {
                out.add(Token(lineStart, end, Kind.STRING))
                fenced = !fenced
            } else if (fenced) {
                out.add(Token(lineStart, end, Kind.STRING))
            } else if (t[lineStart] == '#') {
                out.add(Token(lineStart, end, Kind.HEADING))
            } else if (t[lineStart] == '>') {
                out.add(Token(lineStart, end, Kind.COMMENT))
            } else {
                var i = lineStart
                while (i < end) {
                    if (t[i] == '`') {
                        val close = (i + 1 until end).firstOrNull { t[it] == '`' }
                        if (close != null) {
                            out.add(Token(i, close + 1, Kind.STRING))
                            i = close + 1
                            continue
                        }
                    }
                    i++
                }
            }
            lineStart = end + 1
        }
    }

    private fun scanDiff(
        t: CharSequence,
        out: MutableList<Token>,
    ) {
        val n = t.length
        var lineStart = 0
        while (lineStart < n) {
            val end = lineEnd(t, lineStart)
            val kind =
                when {
                    startsWith(t, lineStart, "+++") || startsWith(t, lineStart, "---") ||
                        startsWith(t, lineStart, "@@") || startsWith(t, lineStart, "diff ") ||
                        startsWith(t, lineStart, "index ") -> Kind.HEADING
                    t[lineStart] == '+' -> Kind.ADDED
                    t[lineStart] == '-' -> Kind.REMOVED
                    else -> null
                }
            if (kind != null) out.add(Token(lineStart, end, kind))
            lineStart = end + 1
        }
    }
}
