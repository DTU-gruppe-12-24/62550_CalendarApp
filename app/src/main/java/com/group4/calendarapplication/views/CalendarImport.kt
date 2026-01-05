package com.group4.calendarapplication.views

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.group4.calendarapplication.models.Calendar
import com.group4.calendarapplication.models.importZippedIcal
import java.io.File
import java.net.URI

class GetCustomContents(
    private val isMultiple: Boolean = false, //This input check if the select file option is multiple or not
): ActivityResultContract<String, List<@JvmSuppressWildcards Uri>>() {

    override fun createIntent(context: Context, input: String): Intent {
        return Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = input //The input option es the MIME Type that you need to use
            putExtra(Intent.EXTRA_LOCAL_ONLY, true) //Return data on the local device
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, isMultiple) //If select one or more files
                .addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        return intent.takeIf {
            resultCode == Activity.RESULT_OK
        }?.getClipDataUris() ?: emptyList()
    }

    internal companion object {

        //Collect all Uris from files selected
        internal fun Intent.getClipDataUris(): List<Uri> {
            // Use a LinkedHashSet to maintain any ordering that may be
            // present in the ClipData
            val resultSet = LinkedHashSet<Uri>()
            data?.let { data ->
                resultSet.add(data)
            }
            val clipData = clipData
            if (clipData == null && resultSet.isEmpty()) {
                return emptyList()
            } else if (clipData != null) {
                for (i in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(i).uri
                    if (uri != null) {
                        resultSet.add(uri)
                    }
                }
            }
            return ArrayList(resultSet)
        }
    }
}

@Composable
public fun CalendarImport(onResult: (calendar: Calendar?) -> Unit) {
    val context = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(
        contract = GetCustomContents(isMultiple = true),
        onResult = { uris ->
            // Import each file
            uris.forEach { uri ->
                Log.d("MainActivity", "uri: $uri")

                // Read file from content uri
                val cr: ContentResolver = context.contentResolver
                val input = cr.openInputStream(uri)

                if (input != null) {
                    // Import calendars from zip
                    val calendars = importZippedIcal(input)
                    if (calendars.isNotEmpty()) {
                        // Combine all calendars from zip into one
                        val calendar = Calendar(calendars[0].name, calendars[0].color,
                            calendars.reduce { combined, cal ->
                                combined.dates.addAll(cal.dates)
                                combined
                            }.dates
                        )
                        // Update upstream calendar
                        onResult(calendar)
                    }
                }
            }
            // Always send a null calendar to make sure import popup is closed
            onResult(null)
        })

    SideEffect {
        filePicker.launch("*/*")
    }
}