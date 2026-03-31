package com.usanmap.usan.service.storage;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileStorageService {
    List<StoredFile> storeListingPhotos(String listingPublicId, List<MultipartFile> files);
}
