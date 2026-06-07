package com.example.presentation.viewmodel

/**
 * 艺术长图生成状态
 */
sealed class ArtImageState {
    object Idle : ArtImageState()
    object Loading : ArtImageState()
    data class Success(val imageUrl: String) : ArtImageState()
    data class Error(val message: String) : ArtImageState()
}
