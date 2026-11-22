package tech.huangsh.firstsight.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun CalculatorView(modifier: Modifier = Modifier) {
    var display by remember { mutableStateOf("0") }
    var storedValue by remember { mutableStateOf<Double?>(null) }
    var operator by remember { mutableStateOf<String?>(null) }
    var resetOnNextInput by remember { mutableStateOf(false) }

    fun clear() {
        display = "0"
        storedValue = null
        operator = null
        resetOnNextInput = false
    }

    fun inputDigit(digit: String) {
        display = when {
            resetOnNextInput || display == "0" -> digit
            display.length >= 12 -> display
            else -> display + digit
        }
        resetOnNextInput = false
    }

    fun inputDot() {
        display = when {
            resetOnNextInput -> "0."
            display.contains(".") -> display
            else -> "$display."
        }
        resetOnNextInput = false
    }

    fun backspace() {
        display = if (display.length <= 1) "0" else display.dropLast(1)
    }

    fun inputOperator(nextOperator: String) {
        val current = display.toDoubleOrNull() ?: 0.0
        if (operator != null && !resetOnNextInput) {
            val result = calculate(storedValue ?: 0.0, current, operator ?: "+")
            display = formatNumber(result)
            storedValue = if (result.isNaN() || result.isInfinite()) null else result
        } else {
            storedValue = current
        }
        operator = nextOperator
        resetOnNextInput = true
    }

    fun equals() {
        val currentOperator = operator ?: return
        val current = display.toDoubleOrNull() ?: 0.0
        val result = calculate(storedValue ?: 0.0, current, currentOperator)
        display = formatNumber(result)
        storedValue = null
        operator = null
        resetOnNextInput = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Text(
                    text = display,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    maxLines = 2
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CalculatorKey("C", Modifier.weight(1f), isAction = true) { clear() }
            CalculatorKey("⌫", Modifier.weight(1f), isAction = true) { backspace() }
            CalculatorKey("÷", Modifier.weight(1f), isOperator = true) { inputOperator("/") }
            CalculatorKey("×", Modifier.weight(1f), isOperator = true) { inputOperator("*") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CalculatorKey("7", Modifier.weight(1f)) { inputDigit("7") }
            CalculatorKey("8", Modifier.weight(1f)) { inputDigit("8") }
            CalculatorKey("9", Modifier.weight(1f)) { inputDigit("9") }
            CalculatorKey("−", Modifier.weight(1f), isOperator = true) { inputOperator("-") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CalculatorKey("4", Modifier.weight(1f)) { inputDigit("4") }
            CalculatorKey("5", Modifier.weight(1f)) { inputDigit("5") }
            CalculatorKey("6", Modifier.weight(1f)) { inputDigit("6") }
            CalculatorKey("+", Modifier.weight(1f), isOperator = true) { inputOperator("+") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CalculatorKey("1", Modifier.weight(1f)) { inputDigit("1") }
            CalculatorKey("2", Modifier.weight(1f)) { inputDigit("2") }
            CalculatorKey("3", Modifier.weight(1f)) { inputDigit("3") }
            CalculatorKey("=", Modifier.weight(1f), isEquals = true) { equals() }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CalculatorKey("0", Modifier.weight(3f)) { inputDigit("0") }
            CalculatorKey(".", Modifier.weight(1f)) { inputDot() }
        }
    }
}

@Composable
private fun CalculatorKey(
    label: String,
    modifier: Modifier = Modifier,
    isOperator: Boolean = false,
    isAction: Boolean = false,
    isEquals: Boolean = false,
    onClick: () -> Unit
) {
    val background = when {
        isEquals -> MaterialTheme.colorScheme.primary
        isOperator -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        isAction -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = when {
        isEquals -> MaterialTheme.colorScheme.onPrimary
        isOperator -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .height(58.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

private fun calculate(left: Double, right: Double, operator: String): Double {
    return when (operator) {
        "+" -> left + right
        "-" -> left - right
        "*" -> left * right
        "/" -> if (right == 0.0) Double.NaN else left / right
        else -> right
    }
}

private fun formatNumber(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "错误"
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format(Locale.US, "%.6f", value).trimEnd('0').trimEnd('.')
    }
}
