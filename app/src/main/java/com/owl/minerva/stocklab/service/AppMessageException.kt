package com.owl.minerva.stocklab.service

import androidx.annotation.StringRes

class AppMessageException(
    @get:StringRes val messageResId: Int,
) : IllegalArgumentException()

fun requireAppMessage(value: Boolean, @StringRes messageResId: Int) {
    if (!value) {
        throw AppMessageException(messageResId)
    }
}
