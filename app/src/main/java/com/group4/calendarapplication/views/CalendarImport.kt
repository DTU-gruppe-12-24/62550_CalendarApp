package com.group4.calendarapplication.views

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.group4.calendarapplication.models.Calendar
import com.group4.calendarapplication.models.importIcal
import com.group4.calendarapplication.models.importZippedIcal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL


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
fun FileCalendarImport(onResult: (calendar: Calendar?) -> Unit, onError: (msg: String) -> Unit) {
    val context = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(
        contract = GetCustomContents(isMultiple = true),
        onResult = { uris ->
            val cr: ContentResolver = context.contentResolver

            // Import each file
            uris.forEach { uri ->
                Log.d("MainActivity", "uri: $uri")

                // Read file from content uri
                val input = cr.openInputStream(uri) ?: return@forEach
                val fileType: String = cr.getType(uri) ?: return@forEach

                when (fileType) {
                    // If file is '.ics':
                    "text/calendar" -> {
                        try {
                            val calendar = importIcal(input)
                            onResult(calendar)
                        } catch (e: Exception) {
                            Log.e("CalendarImport", "Failed to import calendar with error: $e")
                            onError("Failed to import calendar with error: ${e.message ?: e.toString()}")
                        }
                    }
                    // If file is '.zip':
                    "application/zip" -> {
                        // Import calendars from zip
                        try {
                            val calendars = importZippedIcal(input)
                            if (calendars.isEmpty()) return@forEach

                            // Combine all calendars from zip into one
                            val calendar = combineCalendars(calendars)

                            // Update upstream calendar
                            onResult(calendar)
                        } catch (e: Exception) {
                            Log.e("CalendarImport", "Failed to import calendars with error: $e")
                            onError("Failed to import calendar(s) with error: ${e.message ?: e.toString()}")
                            return@forEach
                        }
                    }
                    else -> {
                        Log.e("CalendarImport", "Unknown file type selected for calendar import.")
                        onError("Unknown file type selected for calendar import: Only .ics or .zip files are supported.")
                        return@forEach
                    }
                }
            }
            // Always send a null calendar to make sure import popup is closed
            onResult(null)
        })

    Button(
        onClick = { filePicker.launch("*/*") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Text("Select File From Device")
    }
}

@Composable
fun UrlCalendarImport(
    onResult: (Calendar?) -> Unit,
    onClose: () -> Unit,
    onError: (msg: String) -> Unit,
) {
    var url by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxWidth()) {
        TextField(
            value = url,
            onValueChange = { url = it },
            placeholder = { Text("Paste calendar URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            enabled = url.isNotBlank(),
            onClick = {
                scope.launch {
                    try {
                        if (!url.startsWith("http") && !url.startsWith("webcal")) {
                            throw Exception("Invalid URL format. Please include 'http://', 'https://' or 'webcal://'")
                        }

                        val normalized = normalizeWebcalUrl(url)

                        val policy = ThreadPolicy.Builder().permitAll().build()
                        StrictMode.setThreadPolicy(policy)

                        // Download so we can try reading multiple times
                        val bytes = withContext(Dispatchers.IO) { downloadIcs(normalized) }
                        if (bytes.isEmpty()) throw Exception("The downloaded file is empty.")
                        val input = bytes.inputStream()

                        var resultCalendar: Calendar? = null

                        // Try reading as .ics
                        try {
                            resultCalendar = importIcal(input)
                        } catch(_: Exception) {
                        // Try reading as .zip
                        val calendars = importZippedIcal(input)
                        if (calendars.isNotEmpty()) {
                            resultCalendar = combineCalendars(calendars)
                        }
                        }
                        if (resultCalendar != null && resultCalendar.dates.isNotEmpty()) {
                            onResult(resultCalendar)
                            onClose()
                        } else {
                            throw Exception("The link does not contain a valid .ics or .zip calendar file.")
                        }
                    } catch (e: Exception) {
                        Log.e("UrlCalendarImport", "Failed to import", e)
                        onError(e.message ?: e.toString())
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Import from URL")
        }
    }
}


fun combineCalendars(calendars: ArrayList<Calendar>): Calendar {
    if (calendars.isEmpty()) throw Exception("No calendars found to combine.")
    if (calendars.size == 1) return calendars[0]

    return Calendar(
        calendars[0].name,
        calendars[0].color,
        calendars.flatMap { it.dates }.toCollection(ArrayList())
    )
}

fun normalizeWebcalUrl(url: String): String =
    if (url.startsWith("webcal://")) {
        "https://" + url.removePrefix("webcal://")
    } else {
        url
    }


suspend fun downloadIcs(url: String): ByteArray = withContext(Dispatchers.IO) {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.connectTimeout = 15_000
    connection.readTimeout = 60_000
    connection.requestMethod = "GET"
    connection.connect()

    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
        throw IOException("HTTP ${connection.responseCode}")
    }

    connection.inputStream.use { it.readBytes() }
}