package com.example.smartread.data.repository

import com.example.smartread.data.local.dao.ChatDao
import com.example.smartread.data.local.dao.HighlightDao
import com.example.smartread.data.local.dao.NoteDao
import com.example.smartread.data.local.dao.ReportDao
import com.example.smartread.data.mapper.toDomain
import com.example.smartread.data.mapper.toEntity
import com.example.smartread.data.remote.AigcRemoteDataSource
import com.example.smartread.data.remote.ReportGenerateRequest
import com.example.smartread.domain.model.ReadingReport
import com.example.smartread.domain.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ReportRepositoryImpl @Inject constructor(
    private val reportDao: ReportDao,
    private val highlightDao: HighlightDao,
    private val noteDao: NoteDao,
    private val chatDao: ChatDao,
    private val aigcRemoteDataSource: AigcRemoteDataSource
) : ReportRepository {
    override suspend fun generateBlindBoxReport(bookId: String): ReadingReport {
        // MVP: use cached report when aggregation needs UI-collected Flow data.
        reportDao.getReport(bookId)?.let { return it.toDomain() }
        val report = aigcRemoteDataSource.generateReport(
            ReportGenerateRequest(
                bookId = bookId,
                highlights = emptyList(),
                notes = emptyList(),
                conversations = emptyList()
            )
        )
        reportDao.upsertReport(report.toEntity())
        return report
    }

    override fun observeReport(bookId: String): Flow<ReadingReport?> =
        reportDao.observeReport(bookId).map { it?.toDomain() }
}
