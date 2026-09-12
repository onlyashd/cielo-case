package com.example.cielocase.ui.component

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** Message produced by a ViewModel, resolved to text only in the composition. */
data class UiMessage(
    @StringRes val resId: Int,
    val formatArgs: List<Any> = emptyList(),
) {
    constructor(@StringRes resId: Int, arg: Any) : this(resId, listOf(arg))
}

@Composable
fun UiMessage.text(): String = stringResource(resId, *formatArgs.toTypedArray())
