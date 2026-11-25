package com.bellatrixos.app_explorer

import com.bellatrixos.app_explorer.SyntaxHighlighter.Kind
import org.junit.Assert.assertEquals
import org.junit.Test

class SyntaxTest {
    private fun kinds(
        text: String,
        language: Language,
    ) = SyntaxHighlighter.tokens(text, language).map { text.substring(it.start, it.end) to it.kind }

    @Test fun extensionsPickTheLanguage() {
        assertEquals(Language.KOTLIN, FileKinds.languageOf("Main.kt"))
        assertEquals(Language.PYTHON, FileKinds.languageOf("run.PY"))
        assertEquals(Language.SHELL, FileKinds.languageOf(".bashrc"))
        assertEquals(Language.SHELL, FileKinds.languageOf("Makefile"))
    }

    @Test fun anUnknownNameIsRecognisedByItsFirstLine() {
        assertEquals(Language.PYTHON, FileKinds.languageOf("tool", "#!/usr/bin/env python3"))
        assertEquals(Language.SHELL, FileKinds.languageOf("tool", "#!/bin/sh"))
        assertEquals(Language.MARKUP, FileKinds.languageOf("page", "<?xml version=\"1.0\"?>"))
        assertEquals(Language.JSON, FileKinds.languageOf("data", "{\"a\": 1}"))
        assertEquals(Language.PLAIN, FileKinds.languageOf("notes", "hello"))
    }

    @Test fun routesFollowTheKindOfFile() {
        assertEquals(FileKinds.Route.EDITOR, FileKinds.route("a.kt"))
        assertEquals(FileKinds.Route.EDITOR, FileKinds.route("notes.txt"))
        assertEquals(FileKinds.Route.EDITOR, FileKinds.route("Dockerfile"))
        assertEquals(FileKinds.Route.EXTERNAL, FileKinds.route("doc.pdf"))
        assertEquals(FileKinds.Route.EXTERNAL, FileKinds.route("backup.zip"))
        assertEquals(FileKinds.Route.SNIFF, FileKinds.route("data.xyz"))
    }

    @Test fun codeHasKeywordsStringsNumbersAndComments() {
        val tokens = kinds("val x = \"hi\" // note\nfun f() = 42", Language.KOTLIN)
        assertEquals(
            listOf(
                "val" to Kind.KEYWORD,
                "\"hi\"" to Kind.STRING,
                "// note" to Kind.COMMENT,
                "fun" to Kind.KEYWORD,
                "42" to Kind.NUMBER,
            ),
            tokens,
        )
    }

    @Test fun anUnterminatedStringStopsAtTheEndOfItsLine() {
        val tokens = kinds("a = \"open\nb = 1", Language.PYTHON)
        assertEquals(listOf("\"open" to Kind.STRING, "1" to Kind.NUMBER), tokens)
    }

    @Test fun pythonTripleQuotesRunOverLines() {
        val tokens = kinds("\"\"\"a\nb\"\"\"\nx", Language.PYTHON)
        assertEquals(listOf("\"\"\"a\nb\"\"\"" to Kind.STRING), tokens)
    }

    @Test fun luaBlockCommentIsNotALineComment() {
        val tokens = kinds("--[[ a\nb ]] local", Language.LUA)
        assertEquals(listOf("--[[ a\nb ]]" to Kind.COMMENT, "local" to Kind.KEYWORD), tokens)
    }

    @Test fun jsonKeysAreToldFromValues() {
        val tokens = kinds("{\"a\": \"b\", \"n\": 1, \"t\": true}", Language.JSON)
        assertEquals(
            listOf(
                "\"a\"" to Kind.KEY,
                "\"b\"" to Kind.STRING,
                "\"n\"" to Kind.KEY,
                "1" to Kind.NUMBER,
                "\"t\"" to Kind.KEY,
                "true" to Kind.KEYWORD,
            ),
            tokens,
        )
    }

    @Test fun markupHasTagsAttributesAndStrings() {
        val tokens = kinds("<a href=\"x\">hi</a>", Language.MARKUP)
        assertEquals(
            listOf(
                "<a" to Kind.TAG,
                "href" to Kind.ATTRIBUTE,
                "\"x\"" to Kind.STRING,
                ">" to Kind.TAG,
                "</a" to Kind.TAG,
                ">" to Kind.TAG,
            ),
            tokens,
        )
    }

    @Test fun markdownHeadingsAndInlineCode() {
        val tokens = kinds("# Title\nuse `code` here", Language.MARKDOWN)
        assertEquals(listOf("# Title" to Kind.HEADING, "`code`" to Kind.STRING), tokens)
    }

    @Test fun configSectionsKeysAndComments() {
        val tokens = kinds("[main]\n# c\nname = \"v\"\nn=3", Language.INI)
        assertEquals(
            listOf(
                "[main]" to Kind.HEADING,
                "# c" to Kind.COMMENT,
                "name" to Kind.KEY,
                "\"v\"" to Kind.STRING,
                "n" to Kind.KEY,
                "3" to Kind.NUMBER,
            ),
            tokens,
        )
    }

    @Test fun diffLinesAreColouredByTheirFirstCharacter() {
        val tokens = kinds("@@ -1 +1 @@\n-old\n+new\n ctx", Language.DIFF)
        assertEquals(
            listOf("@@ -1 +1 @@" to Kind.HEADING, "-old" to Kind.REMOVED, "+new" to Kind.ADDED),
            tokens,
        )
    }
}
