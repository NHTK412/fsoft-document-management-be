package com.example.document_management.service;

import com.example.document_management.exception.StorageException;
import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @PostConstruct
    public void init() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Đã tạo mới MinIO bucket: {}", bucketName);
            }
        } catch (Exception e) {
            log.warn("Không thể kiểm tra hoặc tạo MinIO bucket '{}' lúc khởi động: {}", bucketName, e.getMessage());
        }
    }

    public void uploadFile(MultipartFile file, String s3Key) {
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(s3Key)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        } catch (Exception e) {
            log.error("Lỗi khi upload file lên MinIO: {}", e.getMessage(), e);
            throw new StorageException("Không thể upload tệp lên hệ thống lưu trữ: " + e.getMessage(), e);
        }
    }

    public InputStream getFile(String s3Key) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(s3Key)
                            .build()
            );
        } catch (Exception e) {
            log.error("Lỗi khi tải tệp từ MinIO: {}", e.getMessage(), e);
            throw new StorageException("Không thể truy xuất tệp từ hệ thống lưu trữ: " + e.getMessage(), e);
        }
    }

    public void deleteFile(String s3Key) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(s3Key)
                            .build()
            );
        } catch (Exception e) {
            log.error("Lỗi khi xóa tệp khỏi MinIO: {}", e.getMessage(), e);
            throw new StorageException("Không thể xóa tệp khỏi hệ thống lưu trữ: " + e.getMessage(), e);
        }
    }

    public String getPreSignedUrl(String s3Key, int expiryMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(s3Key)
                            .expiry(expiryMinutes, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            log.error("Lỗi khi sinh presigned URL từ MinIO: {}", e.getMessage(), e);
            throw new StorageException("Không thể sinh liên kết tải tệp: " + e.getMessage(), e);
        }
    }
}
