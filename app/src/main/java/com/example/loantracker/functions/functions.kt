package com.example.loantracker.functions

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.example.loantracker.data.local.data_classes.ContactPhonebook
import kotlinx.coroutines.launch

fun getContact(
    context: Context,
    uri: Uri
): ContactPhonebook? {
    val projection = arrayOf(
        ContactsContract.Contacts.DISPLAY_NAME,
        ContactsContract.Contacts._ID
    )
    context.contentResolver.query(
        uri,
        projection,
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()){
            val name = cursor.getString(
                cursor.getColumnIndexOrThrow(
                    ContactsContract.Contacts.DISPLAY_NAME
                )
            )

            val contactId = cursor.getString(
                cursor.getColumnIndexOrThrow(
                    ContactsContract.Contacts._ID
                )
            )

            val phoneNumber = getPhoneNumber(
                context,
                contactId
            )

            return ContactPhonebook(
                name = name,
                phoneNumber = phoneNumber ?: ""
            )
        }
    }

    return  null
}

fun getPhoneNumber(
    context: Context,
    contactId: String
): String? {
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.NUMBER
    )

    context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        projection,
        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
        arrayOf(contactId),
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()){
            return cursor.getString(
                cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )
            )
        }
    }

    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun getTopAppBarColors(): TopAppBarColors{
    val colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = Color(0xFF00695C), // Greenish Teal
        titleContentColor = Color.White,
        navigationIconContentColor = Color.White,
        actionIconContentColor = Color.White
    )

    return colors
}

@SuppressLint("DefaultLocale")
fun formatMoney(
    amount: Double,
    currency: String = "MK"
): String {
    val symbol = currency.trim().ifEmpty { "MK" }
    return "$symbol " + String.format(
        "%,.0f",
        amount
    )
}
