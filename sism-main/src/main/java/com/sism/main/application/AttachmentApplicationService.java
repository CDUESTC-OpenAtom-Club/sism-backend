package com.sism.main.application;

import com.sism.main.interfaces.dto.AttachmentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttachmentApplicationService {

    private final JdbcTemplate jdbcTemplate;

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @Transactional
    public AttachmentResponse upload(MultipartFile file, Long uploadedBy) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        if (uploadedBy == null || uploadedBy <= 0) {
            throw new IllegalArgumentException("uploadedBy 必填");
        }

        Path uploadRoot = Paths.get(uploadPath).toAbsolutePath().normalize();
        Files.createDirectories(uploadRoot);

        String originalName = file.getOriginalFilename() == null ? "unknown" : Paths.get(file.getOriginalFilename()).getFileName().toString();
        String extension = "";
        int extensionIndex = originalName.lastIndexOf('.');
        if (extensionIndex >= 0 && extensionIndex < originalName.length() - 1) {
            extension = originalName.substring(extensionIndex + 1);
        }

        String objectKey = UUID.randomUUID() + (extension.isBlank() ? "" : "." + extension);
        Path targetPath = uploadRoot.resolve(objectKey);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        Long id = jdbcTemplate.queryForObject(
                """
                INSERT INTO public.attachment (
                    storage_driver, bucket, object_key, public_url, original_name,
                    content_type, file_ext, size_bytes, uploaded_by, uploaded_at, remark, is_deleted
                )
                VALUES ('FILE', NULL, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, NULL, false)
                RETURNING id
                """,
                Long.class,
                objectKey,
                "/api/v1/attachments/__TEMP__/download",
                originalName,
                file.getContentType(),
                extension.isBlank() ? null : extension,
                file.getSize(),
                uploadedBy
        );

        String publicUrl = "/api/v1/attachments/" + id + "/download";
        jdbcTemplate.update(
                "UPDATE public.attachment SET public_url = ? WHERE id = ?",
                publicUrl,
                id
        );

        return getMetadata(id);
    }

    public AttachmentResponse getMetadata(Long id) {
        List<AttachmentResponse> rows = jdbcTemplate.query(
                """
                SELECT id,
                       original_name,
                       size_bytes,
                       content_type,
                       COALESCE(NULLIF(public_url, ''), CONCAT('/api/v1/attachments/', id, '/download')) AS url,
                       uploaded_by,
                       uploaded_at
                FROM public.attachment
                WHERE id = ?
                  AND COALESCE(is_deleted, false) = false
                """,
                (rs, rowNum) -> AttachmentResponse.builder()
                        .id(rs.getLong("id"))
                        .fileName(rs.getString("original_name"))
                        .fileSize(rs.getLong("size_bytes"))
                        .fileType(rs.getString("content_type"))
                        .url(rs.getString("url"))
                        .uploadedBy(rs.getLong("uploaded_by"))
                        .uploadedAt(rs.getObject("uploaded_at", OffsetDateTime.class))
                        .build(),
                id
        );

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Attachment not found: " + id);
        }

        return rows.get(0);
    }

    public Resource loadAsResource(Long id) throws MalformedURLException {
        String objectKey = jdbcTemplate.queryForObject(
                """
                SELECT object_key
                FROM public.attachment
                WHERE id = ?
                  AND COALESCE(is_deleted, false) = false
                """,
                String.class,
                id
        );
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("Attachment object key not found: " + id);
        }

        Path uploadRoot = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path filePath = uploadRoot.resolve(objectKey).normalize();
        return new UrlResource(filePath.toUri());
    }
}
