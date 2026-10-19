package com.aweagle.arrowbloom.ui

import android.content.Context
import android.content.Intent
import com.aweagle.arrowbloom.R

/** Opens the system share sheet with [text]. Never throws: a device with no share target just does nothing. */
fun shareText(context: Context, text: String) {
    runCatching {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(send, context.getString(R.string.share_chooser)))
    }
}
