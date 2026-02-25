package com.forge.webide.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "knowledge_tags")
class KnowledgeTagEntity(
    @Id
    val id: String,

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(nullable = false, length = 500)
    var description: String = "",

    @Column(name = "chapter_heading", nullable = false, length = 200)
    var chapterHeading: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

    @Column(nullable = false, length = 20)
    var status: String = "active",

    @Column(name = "source_file", length = 500)
    var sourceFile: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
