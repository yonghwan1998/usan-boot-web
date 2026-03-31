package com.usanmap.usan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.usanmap.usan.entity.ListingPhoto;
import com.usanmap.usan.repository.ListingPhotoRepository;
import com.usanmap.usan.service.storage.StoredFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListingPhotoService {

    private final ListingPhotoRepository listingPhotoRepository;

    @Transactional
    public void saveAll(Long listingId, List<StoredFile> storedFiles) {

        if (storedFiles == null || storedFiles.isEmpty()) {
            return;
        }

        List<ListingPhoto> photos = storedFiles.stream()
                .map(f -> ListingPhoto.of(
                        listingId,
                        f.sortOrder(),
                        f.originalName(),
                        f.storedName(),
                        f.filePath(),
                        f.mimeType(),
                        f.sizeBytes()
                ))
                .toList();

        listingPhotoRepository.saveAll(photos);

    }
}
