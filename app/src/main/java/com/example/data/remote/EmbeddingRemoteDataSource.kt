package com.example.data.remote

import kotlin.math.abs

class EmbeddingRemoteDataSource {
    fun embedLocal(text: String, dimensions: Int = 32): List<Float> {
        val buckets = FloatArray(dimensions)
        text.forEachIndexed { index, char ->
            val bucket = abs((char.code + index * 31) % dimensions)
            buckets[bucket] += ((char.code % 97) + 1) / 97f
        }
        val norm = kotlin.math.sqrt(buckets.sumOf { (it * it).toDouble() }).toFloat()
        return if (norm == 0f) buckets.toList() else buckets.map { it / norm }
    }
}
