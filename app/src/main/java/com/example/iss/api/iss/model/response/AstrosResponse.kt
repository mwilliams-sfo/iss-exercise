package com.example.iss.api.iss.model.response

import androidx.annotation.Keep
import com.example.iss.api.iss.model.Person

@Keep
data class AstrosResponse(
    val message: String,
    val number: Int,
    val people: List<Person>
)
