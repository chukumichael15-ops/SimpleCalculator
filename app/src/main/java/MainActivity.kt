package com.example.simplecalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF101015)) {
                    CalculatorApp()
                }
            }
        }
    }
}

@Composable
fun CalculatorApp() {
    var expression by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }
    var errorState by remember { mutableStateOf(false) }

    fun updateResult() {
        val eval = try { evaluateExpression(expression) } catch (_: Exception) { null }
        if (eval == null) {
            resultText = ""
            errorState = expression.isNotBlank()
        } else {
            resultText = formatNumber(eval)
            errorState = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = expression.ifEmpty { "0" },
                fontSize = 32.sp,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp)
            )
            Text(
                text = if (errorState) "Error" else resultText,
                fontSize = 24.sp,
                color = if (errorState) Color(0xFFFF6B6B) else Color(0xFB9AA0A6),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
        }

        val buttons = listOf(
            listOf("C", "⌫", "%", "/"),
            listOf("7", "8", "9", "*"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("+/-", "0", ".", "=")
        )

        for (row in buttons) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (label in row) {
                    CalculatorButton(label = label, modifier = Modifier.weight(1f).height(72.dp)) {
                        when (label) {
                            "C" -> {
                                expression = ""
                                resultText = ""
                                errorState = false
                            }
                            "⌫" -> {
                                if (expression.isNotEmpty()) {
                                    expression = expression.dropLast(1)
                                    updateResult()
                                }
                            }
                            "=" -> {
                                val eval = try { evaluateExpression(expression) } catch (_: Exception) { null }
                                if (eval != null) {
                                    expression = formatNumber(eval)
                                    resultText = ""
                                    errorState = false
                                } else {
                                    resultText = ""
                                    errorState = true
                                }
                            }
                            "+/-" -> {
                                expression = toggleSign(expression)
                                updateResult()
                            }
                            else -> {
                                expression = appendTokenSafely(expression, label)
                                updateResult()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D2D2D))
    ) {
        Text(text = label, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

fun appendTokenSafely(expr: String, token: String): String {
    if (token in listOf("+", "-", "*", "/", "%")) {
        if (expr.isEmpty()) return if (token == "-") "-" else expr
        val last = expr.last()
        if (last in listOf('+', '-', '*', '/', '%', '.')) return expr.dropLast(1) + token
    }
    if (token == ".") {
        val lastNumber = expr.takeLastWhile { it !in listOf('+', '-', '*', '/', '%', '(' , ')') }
        if (lastNumber.contains('.')) return expr
        if (lastNumber.isEmpty()) return expr + "0."
    }
    return expr + token
}

fun toggleSign(expr: String): String {
    if (expr.isEmpty()) return expr
    val idx = expr.indexOfLast { it in listOf('+', '-', '*', '/', '%', '(' , ')') }
    return if (idx == -1) {
        if (expr.startsWith("-")) expr.drop(1) else "-$expr"
    } else {
        val before = expr.substring(0, idx + 1)
        val lastNumber = expr.substring(idx + 1)
        if (lastNumber.startsWith("-")) before + lastNumber.drop(1) else "$before(-$lastNumber)"
    }
}

fun formatNumber(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "Error"
    val rounded = ((value * 1e12).toLong()).toDouble() / 1e12
    return if (abs(rounded - rounded.toLong()) < 1e-12) rounded.toLong().toString() else rounded.toString()
}

fun evaluateExpression(exprRaw: String): Double? {
    if (exprRaw.isBlank()) return 0.0
    val expr = exprRaw.replace(" ", "")
    val outputQueue = mutableListOf<String>()
    val operatorStack = ArrayDeque<String>()
    fun isOperator(s: String) = s in listOf("+", "-", "*", "/", "%")
    fun precedence(op: String) = when (op) { "+", "-" -> 1; "*", "/", "%" -> 2; else -> 0 }
    fun isLeftAssociative(op: String) = op != "^"

    var i = 0
    while (i < expr.length) {
        val c = expr[i]
        when {
            c.isDigit() || c == '.' -> {
                val start = i
                i++
                while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) i++
                outputQueue.add(expr.substring(start, i))
                continue
            }
            c == '(' -> operatorStack.addFirst("(")
            c == ')' -> {
                while (operatorStack.isNotEmpty() && operatorStack.first() != "(") outputQueue.add(operatorStack.removeFirst())
                if (operatorStack.isEmpty() || operatorStack.first() != "(") return null
                operatorStack.removeFirst()
            }
            isOperator(c.toString()) -> {
                val op = c.toString()
                if (op == "-" && (i == 0 || expr[i - 1] in listOf('+', '-', '*', '/', '(', '%'))) {
                    val start = i
                    i++
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) i++
                    if (i == start + 1) return null
                    outputQueue.add(expr.substring(start, i))
                    continue
                }
                while (operatorStack.isNotEmpty() && isOperator(operatorStack.first()) &&
                    ((isLeftAssociative(op) && precedence(op) <= precedence(operatorStack.first())) ||
                            (!isLeftAssociative(op) && precedence(op) < precedence(operatorStack.first())))) {
                    outputQueue.add(operatorStack.removeFirst())
                }
                operatorStack.addFirst(op)
            }
            else -> return null
        }
        i++
    }

    while (operatorStack.isNotEmpty()) {
        val op = operatorStack.removeFirst()
        if (op == "(" || op == ")") return null
        outputQueue.add(op)
    }

    val evalStack = ArrayDeque<Double>()
    for (token in outputQueue) {
        if (token[0].isDigit() || (token.length > 1 && token[0] == '-' && token[1].isDigit()) || token[0] == '.') {
            val num = token.toDoubleOrNull() ?: return null
            evalStack.addFirst(num)
        } else if (isOperator(token)) {
            if (evalStack.size < 2) return null
            val b = evalStack.removeFirst()
            val a = evalStack.removeFirst()
            val res = when (token) {
                "+" -> a + b
                "-" -> a - b
                "*" -> a * b
                "/" -> if (b == 0.0) return null else a / b
                "%" -> if (b == 0.0) return null else a % b
                else -> return null
            }
            evalStack.addFirst(res)
        } else return null
    }
    return if (evalStack.size != 1) null else evalStack.first()
}
