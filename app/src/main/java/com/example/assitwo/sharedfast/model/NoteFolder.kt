package com.example.assitwo.sharedfast.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class NoteFolder(
    val title: String,
    val createdTime: String
) : Parcelable