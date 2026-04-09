package com.int4074.wordduo.ui

import com.int4074.wordduo.data.EssayIssue
import com.int4074.wordduo.data.EssayReviewResult

object EssayReviewEngine {
    private data class Rule(
        val pattern: Regex,
        val category: String,
        val diagnosis: String,
        val suggestion: String,
        val replacement: String,
        val penalty: Int
    )

    private val rules = listOf(
        Rule(Regex("\\bi am agree\\b", RegexOption.IGNORE_CASE), "语法", "agree 不能和 am 连用。", "改成 I agree。", "I agree", 7),
        Rule(Regex("\\bmore better\\b", RegexOption.IGNORE_CASE), "用词", "better 已经是比较级。", "改成 better。", "better", 6),
        Rule(Regex("\\bvery unique\\b", RegexOption.IGNORE_CASE), "用词", "unique 通常不再和 very 连用。", "改成 unique。", "unique", 4),
        Rule(Regex("\\bdiscuss about\\b", RegexOption.IGNORE_CASE), "搭配", "discuss 后面一般不接 about。", "改成 discuss。", "discuss", 5),
        Rule(Regex("\\baccording to me\\b", RegexOption.IGNORE_CASE), "表达", "according to me 不够自然。", "改成 in my opinion。", "in my opinion", 5),
        Rule(Regex("\\bpeople is\\b", RegexOption.IGNORE_CASE), "主谓一致", "people 是复数。", "改成 people are。", "people are", 6),
        Rule(Regex("\\bthere have\\b", RegexOption.IGNORE_CASE), "句式", "there be 句型误用。", "改成 there are 或 there is。", "there are", 6),
        Rule(Regex("\\ban advice\\b", RegexOption.IGNORE_CASE), "可数性", "advice 通常是不可数名词。", "改成 some advice。", "some advice", 6),
        Rule(Regex("\\ba information\\b", RegexOption.IGNORE_CASE), "可数性", "information 通常是不可数名词。", "改成 some information。", "some information", 6),
        Rule(Regex("(?<![A-Za-z])i(?![A-Za-z])"), "大小写", "第一人称代词 i 需要大写。", "改成 I。", "I", 3),
        Rule(Regex("\\bchildrens\\b", RegexOption.IGNORE_CASE), "拼写", "children 本身已经是复数。", "改成 children。", "children", 6),
        Rule(Regex("\\benvironment protect\\b", RegexOption.IGNORE_CASE), "搭配", "表达不完整。", "改成 environmental protection。", "environmental protection", 5)
    )

    fun review(text: String): EssayReviewResult {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            return EssayReviewResult(summary = "请先输入文章后再进行检测。")
        }

        var score = 92
        val issues = mutableListOf<EssayIssue>()
        var improvedText = trimmed

        val wordCount = Regex("[A-Za-z']+").findAll(trimmed).count()
        val sentenceCount = Regex("[.!?]+").findAll(trimmed).count().coerceAtLeast(1)

        if (wordCount < 60) {
            score -= 10
            issues += EssayIssue(
                excerpt = "全文长度",
                category = "结构",
                diagnosis = "文章偏短，论证展开不够充分。",
                suggestion = "补充理由、例子或结论，让内容更完整。"
            )
        }

        if (!trimmed.endsWith('.') && !trimmed.endsWith('!') && !trimmed.endsWith('?')) {
            score -= 4
            issues += EssayIssue(
                excerpt = "结尾标点",
                category = "标点",
                diagnosis = "文章结尾缺少完整句号或问号。",
                suggestion = "为最后一句补上合适的标点。"
            )
        }

        if (Regex("\\bvery\\b", RegexOption.IGNORE_CASE).findAll(trimmed).count() >= 3) {
            score -= 4
            issues += EssayIssue(
                excerpt = "very",
                category = "用词",
                diagnosis = "very 重复过多，表达显得单一。",
                suggestion = "用 more precise adjectives 替换部分 very + adjective 结构。"
            )
        }

        rules.forEach { rule ->
            rule.pattern.findAll(trimmed).forEach { match ->
                score -= rule.penalty
                issues += EssayIssue(
                    excerpt = match.value,
                    category = rule.category,
                    diagnosis = rule.diagnosis,
                    suggestion = rule.suggestion
                )
            }
            improvedText = rule.pattern.replace(improvedText, rule.replacement)
        }

        score = score.coerceIn(35, 98)

        val summary = when {
            score >= 85 -> "整体表达比较清晰，语法和用词控制较好，适合在细节上继续润色。"
            score >= 70 -> "文章主旨明确，但还有一些常见语法和搭配问题，修改后会更自然。"
            else -> "文章基本意思可以理解，但语法、拼写或搭配错误较多，建议先修正基础句式。"
        }

        return EssayReviewResult(
            score = score,
            wordCount = wordCount,
            sentenceCount = sentenceCount,
            summary = summary,
            issues = issues.distinctBy { listOf(it.excerpt, it.category, it.diagnosis) },
            improvedText = improvedText
        )
    }
}
