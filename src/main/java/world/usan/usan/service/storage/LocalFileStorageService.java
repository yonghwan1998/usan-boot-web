package world.usan.usan.service.storage;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {

    private final FileProperties props;

    private Path root() {
        return Paths.get(props.getUploadDir()).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(root());
        } catch (IOException e) {
            throw new IllegalStateException("업로드 디렉토리 생성 실패: " + root(), e);
        }
    }

    @Override
    public List<StoredFile> storeListingPhotos(String listingPublicId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        Path dir = root().resolve("listings").resolve(listingPublicId);

        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("업로드 디렉터리 생성 실패: " + dir, e);
        }

        List<StoredFile> result = new ArrayList<>();

        int order = 1;
        for (MultipartFile f : files) {
            if (f == null || f.isEmpty()) continue;

            String original = safeName(f.getOriginalFilename());
            String ext = normalizeExt(original);

            String storedName = String.format("%04d_%s%s",
                    order,
                    UUID.randomUUID().toString().replace("-", ""),
                    ext.isBlank() ? "" : "." + ext
            );

            Path target = dir.resolve(storedName);

            try {
                Files.copy(f.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new IllegalStateException("파일 저장 실패: " + target, e);
            }

            String publicUrl = props.getPublicPrefix() + "/listings/" + listingPublicId + "/" + storedName;

            result.add(new StoredFile(
                    order,
                    original,
                    storedName,
                    publicUrl,
                    f.getSize()
            ));

            order++;
        }

        return result;
    }

    private String safeName(String name) {
        if (name == null) return "";
        return name.replace("\\", "_").replace("/", "_");
    }

    private String normalizeExt(String originalName) {
        int idx = originalName.lastIndexOf(".");
        if (idx < 0 || idx == originalName.length() - 1) return "";
        String ext = originalName.substring(idx + 1).toLowerCase(Locale.ROOT).trim();

        return switch (ext) {
            case "png", "jpg", "jpeg", "webp", "pdf" -> ext;
            default -> "";
        };
    }
}
