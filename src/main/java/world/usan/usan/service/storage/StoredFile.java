package world.usan.usan.service.storage;

public record StoredFile(
        int sortOrder,
        String originalName,
        String storedName,
        String filePath,
        String mimeType,
        long sizeBytes
) {
}