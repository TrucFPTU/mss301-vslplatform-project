package com.vsl.learning.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vocabularies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vocabulary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Category cung nam trong learning-service nen van giu quan he JPA.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(length = 255)
    private String word;

    @Column(length = 255)
    private String description;

    @Column(name = "video_tutorial_url", length = 500)
    private String videoTutorialUrl;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** Chi so class trong model AI tuong ung voi tu vung nay. */
    @Column(name = "expected_id")
    private Integer expectedId;
}
