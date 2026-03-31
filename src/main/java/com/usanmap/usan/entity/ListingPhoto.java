package com.usanmap.usan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "listing_photo",
        indexes = @Index(name = "idx_listing_photo_listing_sort", columnList = "listing_id, sort_order"))
public class ListingPhoto {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "listing_id", nullable = false)
    private Long listingId;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false)
    private String storedFilename;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public static ListingPhoto of(Long listingId,
                                  int sortOrder,
                                  String originalFilename,
                                  String storedFilename,
                                  String filePath,
                                  String mimeType,
                                  long fileSize) {
        ListingPhoto p = new ListingPhoto();
        p.listingId = listingId;
        p.sortOrder = sortOrder;
        p.originalFilename = originalFilename;
        p.storedFilename = storedFilename;
        p.filePath = filePath;
        p.mimeType = mimeType;
        p.fileSize = fileSize;
        return p;
    }
}