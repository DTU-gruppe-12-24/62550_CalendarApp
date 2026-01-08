package com.group4.calendarapplication.views

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.group4.calendarapplication.models.Calendar
import com.group4.calendarapplication.models.importIcal
import com.group4.calendarapplication.models.importZippedIcal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
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
fun FileCalendarImport(onResult: (calendar: Calendar?) -> Unit) {
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
                        val calendar = importIcal(input)
                        onResult(calendar)
                    }
                    // If file is '.zip':
                    "application/zip" -> {
                        // Import calendars from zip
                        try {
                            val calendars = importZippedIcal(input)
                            if (calendars.isEmpty()) return@forEach

                            // Combine all calendars from zip into one
                            val calendar = Calendar(
                                calendars[0].name, calendars[0].color,
                                calendars.reduce { combined, cal ->
                                    combined.dates.addAll(cal.dates)
                                    combined
                                }.dates
                            )
                            // Update upstream calendar
                            onResult(calendar)
                        } catch (e: Exception) {
                            Log.e("CalendarImport", "Failed to import calendar(s) with error: $e")
                            throw e
                        }
                    }
                }
            }
            // Always send a null calendar to make sure import popup is closed
            onResult(null)
        })

    Button(onClick = { filePicker.launch("*/*")}) {
        Text("Add calendar from file")
    }
}

@Composable
fun UrlCalendarImport(
    onResult: (Calendar?) -> Unit,
    onClose: () -> Unit
) {
    var url by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier

    ) {
        CloseIconButton(
            modifier = Modifier.size(32.dp).offset((-8).dp, (-8).dp),
            onClick = onClose

        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally

        ) {
            Text(
                text = "Add new Calendar with URL or file",
                color = MaterialTheme.colorScheme.primary,
                fontSize = TextUnit(6.0f, TextUnitType.Em),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            TextField(
                value = url,
                onValueChange = { url = it },
                placeholder = { Text("Paste calendar URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            Button(
                enabled = url.isNotBlank(),
                onClick = {
                    scope.launch {
                        try {
                            val normalized = normalizeWebcalUrl(url)
                            val input = withContext(Dispatchers.IO) {
                                downloadIcs(normalized)
                            }
                            val calendar = importIcal(input)
                            onResult(calendar)
                            onClose()
                        } catch (e: Exception) {
                            Log.e("UrlCalendarImport", "Failed to import", e)
                            onResult(null)
                        }
                    }
                }
            ) {
                Text("Import")
            }
        }
    }
}

fun normalizeWebcalUrl(url: String): String =
    if (url.startsWith("webcal://")) {
        "https://" + url.removePrefix("webcal://")
    } else {
        url
    }


fun downloadIcs(url: String): InputStream {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.connectTimeout = 15_000
    connection.readTimeout = 15_000
    connection.requestMethod = "GET"
    connection.connect()

    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
        throw IOException("HTTP ${connection.responseCode}")
    }

    return connection.inputStream
}