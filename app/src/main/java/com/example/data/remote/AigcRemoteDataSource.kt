package com.example.data.remote

import org.json.JSONObject
import java.util.UUID

class AigcRemoteDataSource(
    private val blueLmApi: BlueLmApi = BlueLmClient().api
) {
    // 根据 AIGC 比赛官方文档配置
    private val appKey = "sk-xuanji-2026316046-SVRqbXNtWmdlaU1oUWlZSQ="
    private val modelName = "Doubao-Seed-2.0-mini" // 官方文档推荐模型

    suspend fun generate(prompt: String, systemPrompt: String? = null): String {
        val messages = mutableListOf<BlueLmMessage>()
        if (systemPrompt != null) {
            messages.add(BlueLmMessage(role = "system", content = systemPrompt))
        }
        messages.add(BlueLmMessage(role = "user", content = prompt))

        val request = BlueLmChatRequest(
            model = modelName,
            messages = messages,
            temperature = 0.7f,
            max_tokens = 1024
        )

        val requestId = UUID.randomUUID().toString()

        return try {
            // 按照官方示例使用 Bearer 认证和 request_id 查询参数
            val response = blueLmApi.chatCompletions("Bearer $appKey", requestId, request)
            
            if (response.error != null) {
                "对话失败: ${response.error.message ?: "未知错误"}"
            } else {
                response.choices?.firstOrNull()?.message?.content
                    ?: "大模型未返回有效信息。"
            }
        } catch (e: Exception) {
            "对话失败: ${e.localizedMessage}。请检查 API 配置和网络状态。"
        }
    }

    // --- 伴读业务逻辑 ---

    suspend fun socraticReply(bookTitle: String, passage: String, userText: String, historyPrompt: String): String {
        val systemPrompt = Prompts.socraticSystemPrompt
        val prompt = Prompts.socraticUserPrompt(bookTitle, passage, userText, historyPrompt)
        return generate(prompt, systemPrompt)
    }

    suspend fun noteInsight(originalText: String, userNote: String): String =
        generate(Prompts.noteInsightPrompt(originalText, userNote), Prompts.noteInsightSystemPrompt)

    suspend fun noteTags(originalText: String, userNote: String): String =
        generate(Prompts.noteTagsPrompt(originalText, userNote), Prompts.noteTagsSystemPrompt)
            .replace(".", "")
            .trim()

    suspend fun blindBoxReportJson(
        bookTitle: String,
        notesSummaryText: String,
        highlightsText: String,
        chatsText: String
    ): JSONObject? {
        val raw = generate(
            prompt = Prompts.reportPrompt(bookTitle, highlightsText, notesSummaryText, chatsText),
            systemPrompt = Prompts.reportSystemPrompt
        )
        // 增强的 JSON 提取逻辑，处理 AI 可能返回的 Markdown 代码块或文字干扰
        val jsonString = try {
            val startIndex = raw.indexOf("{")
            val endIndex = raw.lastIndexOf("}")
            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                raw.substring(startIndex, endIndex + 1)
            } else {
                raw.trim()
            }
        } catch (e: Exception) {
            raw.trim()
        }

        return runCatching { JSONObject(jsonString) }.getOrNull()
    }

    /**
     * 智能搜索/推荐书籍
     * 返回格式：JSON 数组 [{"title": "...", "author": "...", "summary": "...", "category": "..."}]
     */
    suspend fun searchBooks(query: String): List<JSONObject> {
        val raw = generate(
            prompt = Prompts.searchBooksPrompt(query),
            systemPrompt = Prompts.searchBooksSystemPrompt
        )
        
        return try {
            val startIndex = raw.indexOf("[")
            val endIndex = raw.lastIndexOf("]")
            if (startIndex != -1 && endIndex != -1) {
                val jsonArray = org.json.JSONArray(raw.substring(startIndex, endIndex + 1))
                List(jsonArray.length()) { i -> jsonArray.getJSONObject(i) }
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 推理思想间的深层基因关系
     * 返回格式：JSON 数组 [{"target": "...", "type": "...", "reason": "..."}]
     */
    suspend fun inferRelationships(
        currentNote: String,
        relatedNotes: List<String>
    ): List<JSONObject> {
        if (relatedNotes.isEmpty()) return emptyList()
        
        val raw = generate(
            prompt = Prompts.geneRelationshipPrompt(currentNote, relatedNotes),
            systemPrompt = Prompts.geneRelationshipSystemPrompt
        )
        
        return try {
            val startIndex = raw.indexOf("[")
            val endIndex = raw.lastIndexOf("]")
            if (startIndex != -1 && endIndex != -1) {
                val jsonArray = org.json.JSONArray(raw.substring(startIndex, endIndex + 1))
                List(jsonArray.length()) { i -> jsonArray.getJSONObject(i) }
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

object Prompts {
    const val socraticSystemPrompt =
        "你是 SmartRead 的智能伴读专家。你不仅精通苏格拉底式启发教学，还能提供深刻的知识洞察。你的角色是读者的“思想陪跑者”。当读者感到困惑时，你要提供多维度的引导和参考答案；当读者表达观点时，你要通过挑战性提问深化其思考。回复要博学、温和且具有穿透力。"

    fun socraticUserPrompt(bookTitle: String, passage: String, userText: String, historyPrompt: String) = """
        当前书籍：《$bookTitle》
        当前选中片段：
        "$passage"

        读者的思考/问题：
        "$userText"

        最近对话历史：
        $historyPrompt

        请按照以下逻辑给予回应：
        1. 简要肯定或梳理读者的观点。
        2. 提供一个基于文本背景的深度解读或“参考视角”。
        3. 提出 1-2 个能够引导读者继续向更深层次挖掘的启发性问题。
    """.trimIndent()

    const val noteInsightSystemPrompt =
        "你是一个深度阅读思想剖析专家。你的任务是针对读者的摘录和感悟，生成一段具有哲学高度或思维深度的'AI智慧深透剖析'。你的回复应当犀利、深邃，能够揭示文字背后的底层逻辑或认知价值。"

    fun noteInsightPrompt(originalText: String, userNote: String) = """
        原文摘录：
        "$originalText"

        读者的感悟：
        "$userNote"

        请基于以上内容，生成一段 100 字以内的'AI智慧深透剖析'，帮助读者提升认知维度。
    """.trimIndent()

    const val noteTagsSystemPrompt = "你是一个极简标签分类器，只返回标签。"

    fun noteTagsPrompt(originalText: String, userNote: String) =
        "根据以下原文和笔记生成 2 个简短标签，用英文逗号分隔，只返回标签：\n$originalText\n$userNote"

    const val reportSystemPrompt =
        "你是一个高级阅读报告分析师。你的任务是根据读者的阅读数据（划线、笔记、对话）生成一份多维度的'阅读盲盒报告'。请务必返回可被 JSONObject 解析的纯 JSON 格式内容，不要包含任何 Markdown 标记。"

    fun reportPrompt(bookTitle: String, highlightsText: String, notesSummaryText: String, chatsText: String) = """
        书籍：《$bookTitle》
        阅读高亮（体现关注点）：
        $highlightsText

        阅读笔记（包括AI深透剖析）：
        $notesSummaryText

        用户提问（体现怀疑精神）：
        $chatsText

        请综合分析以上数据，评估读者的阅读表现并返回 JSON：
        {
          "logic": 逻辑评分(50-100),
          "empathy": 共情评分(50-100),
          "critical": 批判思维评分(50-100),
          "width": 知识广度评分(50-100),
          "innovation": 创新启发评分(50-100),
          "cognitiveIncrement": "结合用户的划线和笔记，总结本次阅读带来的认知增量",
          "motto": "一句深度契合读者本次思考风格的阅读金句"
        }
    """.trimIndent()

    const val geneRelationshipSystemPrompt = 
        "你是一个思想基因分析专家。你的任务是发现不同思想/观点之间的深层逻辑联系，并以纯 JSON 数组形式返回关系。关系类型包括：支撑, 矛盾, 类比, 补充, 演化。"

    fun geneRelationshipPrompt(current: String, others: List<String>) = """
        新思想：
        "$current"

        已有思想库：
        ${others.joinToString("\n") { "- $it" }}

        请找出“新思想”与“已有思想库”中哪些观点存在逻辑关联，并返回 JSON 数组（不要 Markdown）：
        [
          { "target": "已有思想的内容简述", "type": "支撑/矛盾/类比/补充/演化", "reason": "一句话解释原因" }
        ]
    """.trimIndent()

    const val searchBooksSystemPrompt =
        "你是一个博学的图书检索助手。请根据用户关键词搜索或推荐相关书籍，并以纯 JSON 数组形式返回结果，不要包含 Markdown 格式。"

    fun searchBooksPrompt(query: String) = """
        关键词：$query
        请搜索或推荐 5 本相关的经典书籍。
        返回格式：
        [
          { "title": "书名", "author": "作者", "summary": "一句话简介", "category": "分类" }
        ]
    """.trimIndent()
}
