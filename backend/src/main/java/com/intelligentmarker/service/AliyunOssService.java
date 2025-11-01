package com.intelligentmarker.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import com.intelligentmarker.config.OssProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

/**
 * Aliyun OSS File Storage Service
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AliyunOssService {
    
    private final OssProperties ossProperties;
    private OSS ossClient;
    
    @PostConstruct
    public void init() {
        try {
            // Try to initialize OSS client
            this.ossClient = new OSSClientBuilder().build(
                ossProperties.getEndpoint(),
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret()
            );
            log.info("Aliyun OSS client initialized");
        } catch (Exception e) {
            log.warn("Aliyun OSS not configured, will use local storage mode");
            this.ossClient = null;
        }
    }
    
    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }
    
    /**
     * Upload file to OSS (or local storage)
     * @param file File
     * @param folder Folder path
     * @return OSS file URL or local path
     */
    public String uploadFile(MultipartFile file, String folder) {
        try {
            String fileName = folder + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

            // If OSS is available, upload to OSS
            if (ossClient != null) {
                try {
                    ossClient.putObject(
                        ossProperties.getBucketName(),
                        fileName,
                        file.getInputStream()
                    );
                    
                    String url = "https://" + ossProperties.getBucketName() + "." + 
                                 ossProperties.getEndpoint() + "/" + fileName;
                    
                    log.info("File uploaded to OSS: {}", url);
                    return url;
                } catch (Exception ossEx) {
                    log.warn("OSS upload failed, falling back to local storage: {}", ossEx.getMessage());
                }
            }

            // Fallback to local storage (Demo mode)
            String localPath = "/tmp/uploads/" + fileName;
            java.io.File localFile = new java.io.File(localPath);
            localFile.getParentFile().mkdirs();
            file.transferTo(localFile);
            
            log.info("File saved to local storage (Demo mode): {}", localPath);
            return "local://" + localPath;
            
        } catch (Exception e) {
            log.error("Failed to upload file", e);
            throw new RuntimeException("File upload failed: " + e.getMessage());
        }
    }
    
    /**
     * Upload text content to OSS (or local storage)
     * @param content Text content
     * @param folder Folder path
     * @param fileName File name
     * @return OSS file URL or local path
     */
    public String uploadText(String content, String folder, String fileName) {
        try {
            String fullPath = folder + "/" + UUID.randomUUID() + "_" + fileName;

            // If OSS is available, upload to OSS
            if (ossClient != null) {
                try {
                    InputStream inputStream = new ByteArrayInputStream(content.getBytes("UTF-8"));
                    ossClient.putObject(ossProperties.getBucketName(), fullPath, inputStream);
                    
                    String url = "https://" + ossProperties.getBucketName() + "." + 
                                 ossProperties.getEndpoint() + "/" + fullPath;
                    
                    log.info("Text uploaded to OSS: {}", url);
                    return url;
                } catch (Exception ossEx) {
                    log.warn("OSS upload failed, falling back to local storage: {}", ossEx.getMessage());
                }
            }

            // Fallback to local storage (Demo mode)
            String localPath = "/tmp/uploads/" + fullPath;
            java.io.File localFile = new java.io.File(localPath);
            localFile.getParentFile().mkdirs();
            java.nio.file.Files.write(localFile.toPath(), content.getBytes("UTF-8"));
            
            log.info("Text saved to local storage (Demo mode): {}", localPath);
            return "local://" + localPath;
            
        } catch (Exception e) {
            log.error("Failed to upload text", e);
            throw new RuntimeException("Text upload failed: " + e.getMessage());
        }
    }
    
    /**
     * Download file content from OSS (or read from local)
     * @param fileUrl OSS file URL or local path
     * @return File content (byte array)
     */
    public byte[] downloadFile(String fileUrl) {
        try {
            // If it's a local path
            if (fileUrl.startsWith("local://")) {
                String localPath = fileUrl.substring(8);
                return java.nio.file.Files.readAllBytes(new java.io.File(localPath).toPath());
            }

            // Download from OSS
            if (ossClient != null) {
                String objectKey = extractObjectKey(fileUrl);
                InputStream inputStream = ossClient.getObject(ossProperties.getBucketName(), objectKey).getObjectContent();
                return inputStream.readAllBytes();
            }
            
            throw new RuntimeException("OSS not configured and file is not local");
            
        } catch (Exception e) {
            log.error("Failed to download file: {}", fileUrl, e);
            throw new RuntimeException("File download failed: " + e.getMessage());
        }
    }
    
    /**
     * Delete OSS file
     * @param fileUrl OSS file URL
     */
    public void deleteFile(String fileUrl) {
        try {
            String objectKey = extractObjectKey(fileUrl);
            ossClient.deleteObject(ossProperties.getBucketName(), objectKey);
            
            log.info("File deleted from OSS: {}", fileUrl);
            
        } catch (Exception e) {
            log.error("Failed to delete file from OSS: {}", fileUrl, e);
        }
    }
    
    /**
     * Extract OSS object key from complete URL
     */
    private String extractObjectKey(String url) {
        // Extract folder/file.pdf from https://bucket.endpoint.com/folder/file.pdf
        String[] parts = url.split(ossProperties.getEndpoint() + "/");
        return parts.length > 1 ? parts[1] : url;
    }
}

