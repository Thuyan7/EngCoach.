package com.engcoach.tests;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "question_groups")
public class QuestionGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 16)
    private String part;

    @Column(name = "group_type", nullable = false, length = 32)
    private String groupType;

    @Lob
    @Column(name = "shared_content", length = 16777215)
    private String sharedContent;

    @Column(length = 2048)
    private String imageUrl;

    @Column(length = 2048)
    private String audioUrl;

    protected QuestionGroup() { }

    public QuestionGroup(String part, String groupType, String sharedContent,
                         String imageUrl, String audioUrl) {
        this.part = part;
        this.groupType = groupType;
        this.sharedContent = sharedContent;
        this.imageUrl = imageUrl;
        this.audioUrl = audioUrl;
    }

    public void refreshFrom(String sharedContent, String imageUrl, String audioUrl) {
        this.sharedContent = sharedContent;
        this.imageUrl = imageUrl;
        this.audioUrl = audioUrl;
    }

    public Long getId() { return id; }
    public String getPart() { return part; }
    public String getGroupType() { return groupType; }
    public String getSharedContent() { return sharedContent; }
    public String getImageUrl() { return imageUrl; }
    public String getAudioUrl() { return audioUrl; }
}
