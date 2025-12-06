package com.aymanetech.runtime.errors

import com.aymanetech.lexer.Token
import com.aymanetech.lexer.TokenType

class ErrorHandler {
    var hadError = false
        private set
    var hadRuntimeError = false
        private set

    private var currentSource = ""
    private var currentFilename = "repl"

    fun setSource(source: String, filename: String = "repl") {
        this.currentSource = source
        this.currentFilename = filename
    }

    fun reset() {
        hadError = false
    }

    fun hasErrors(): Boolean = hadError || hadRuntimeError

    fun reportError(line: Int, message: String) {
        val reporter = DiagnosticReporter(currentSource, currentFilename)
        reporter.reportError(
            line = line,
            column = 0,
            message = message
        )
        hadError = true
    }

    fun reportError(line: Int, where: String, message: String) {
        val reporter = DiagnosticReporter(currentSource, currentFilename)
        reporter.reportError(
            line = line,
            column = 0,
            message = message,
            where = where
        )
        hadError = true
    }

    fun reportError(token: Token, message: String) {
        val reporter = DiagnosticReporter(currentSource, currentFilename)
        val column = findTokenColumn(token)

        if (token.type == TokenType.EOF) {
            reporter.reportError(
                line = token.line,
                column = column,
                length = 1,
                message = message,
                where = "at end"
            )
        } else {
            reporter.reportError(
                line = token.line,
                column = column,
                length = token.lexeme.length,
                message = message,
                where = "at '${token.lexeme}'"
            )
        }
        hadError = true
    }

    fun reportRuntimeError(error: RuntimeError) {
        val reporter = DiagnosticReporter(currentSource, currentFilename)
        reporter.reportRuntimeError(
            line = error.token.line,
            message = error.message ?: "Runtime error"
        )
        hadRuntimeError = true
    }

    private fun findTokenColumn(token: Token): Int {
        if (currentSource.isEmpty()) return 0
        val lines = currentSource.lines()
        if (token.line < 1 || token.line > lines.size) return 0

        val line = lines[token.line - 1]
        val index = line.indexOf(token.lexeme)
        return if (index >= 0) index + 1 else 0
    }
}
