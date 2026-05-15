package com.skybots.kiko.actions.contacts

sealed interface ContactMatchResult {
    data class Single(val contact: ContactModel) : ContactMatchResult
    data class Multiple(val candidates: List<ContactModel>) : ContactMatchResult
    data object None : ContactMatchResult
}
