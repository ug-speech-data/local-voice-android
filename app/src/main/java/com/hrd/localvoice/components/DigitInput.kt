package com.hrd.localvoice.components


import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.lang.Float.min

@Preview(showBackground = true)
@Composable
fun DigitInput(digitCount: Int = 10, title: String = "", setValue: (input: String) -> Unit = {}) {
    var values by remember {
        mutableStateOf("")
    }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val inputWidth = min(((screenWidth - (2 * digitCount)) / digitCount.toFloat()), 50f)

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (title.isNotEmpty()) Text(
            text = title,
        )
        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center
        ) {
            BasicTextField(value = values, onValueChange = {
                values = if (it.length <= digitCount) it else values
                setValue(values)
            }, singleLine = true, keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
            ), decorationBox = {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(digitCount) { index ->
                        val char = when {
                            index >= values.length -> ""
                            else -> values[index].toString()
                        }
                        val isFocused = values.length == index
                        Text(
                            modifier = Modifier
                                .width(inputWidth.dp)
                                .height(inputWidth.dp)
                                .border(
                                    if (isFocused) 2.dp else 1.dp,
                                    if (isFocused) Color.DarkGray else Color.LightGray,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(1.dp),
                            text = char,
                            style = MaterialTheme.typography.h5,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                }
            })
        }
    }
}
