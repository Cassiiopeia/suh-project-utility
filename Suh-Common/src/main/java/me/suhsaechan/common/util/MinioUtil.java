package me.suhsaechan.common.util;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveBucketArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.properties.MinioProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioUtil {

  public static final String PUBLIC_PATH = "public/";
  public static final String PUBLIC_IMAGES_PATH = "public/images/";
  public static final String PRIVATE_PATH = "private/";

  private static final int DEFAULT_PRESIGNED_EXPIRY = 1;
  private static final TimeUnit DEFAULT_PRESIGNED_TIME_UNIT = TimeUnit.HOURS;

  private final MinioProperties minioProperties;
  private MinioClient minioClient;

  @PostConstruct
  public void init() {
    try {
      this.minioClient = MinioClient.builder()
          .endpoint(minioProperties.getEndpoint())
          .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
          .build();
      log.info("MinIO 클라이언트 초기화 완료 - endpoint: {}", minioProperties.getEndpoint());
    } catch (Exception e) {
      log.error("MinIO 클라이언트 초기화 실패: {}", e.getMessage(), e);
    }
  }

  public Boolean bucketExists(String bucketName) {
    try {
      return minioClient.bucketExists(BucketExistsArgs.builder()
          .bucket(bucketName)
          .build());
    } catch (Exception e) {
      log.error("MinIO 버킷 존재 확인 실패 - bucket: {}, error: {}", bucketName, e.getMessage(), e);
      throw new RuntimeException("버킷 존재 확인 실패: " + bucketName, e);
    }
  }

  public void createBucket(String bucketName) {
    try {
      if (Boolean.FALSE.equals(bucketExists(bucketName))) {
        minioClient.makeBucket(MakeBucketArgs.builder()
            .bucket(bucketName)
            .build());
        log.info("MinIO 버킷 생성 완료 - bucket: {}", bucketName);
      }
    } catch (Exception e) {
      log.error("MinIO 버킷 생성 실패 - bucket: {}, error: {}", bucketName, e.getMessage(), e);
      throw new RuntimeException("버킷 생성 실패: " + bucketName, e);
    }
  }

  public void removeBucket(String bucketName) {
    try {
      minioClient.removeBucket(RemoveBucketArgs.builder()
          .bucket(bucketName)
          .build());
      log.info("MinIO 버킷 삭제 완료 - bucket: {}", bucketName);
    } catch (Exception e) {
      log.error("MinIO 버킷 삭제 실패 - bucket: {}, error: {}", bucketName, e.getMessage(), e);
      throw new RuntimeException("버킷 삭제 실패: " + bucketName, e);
    }
  }

  public List<Bucket> listBuckets() {
    try {
      return minioClient.listBuckets();
    } catch (Exception e) {
      log.error("MinIO 버킷 목록 조회 실패: {}", e.getMessage(), e);
      throw new RuntimeException("버킷 목록 조회 실패", e);
    }
  }

  public void uploadFile(String bucketName, String objectName, InputStream inputStream, long size, String contentType) {
    try {
      minioClient.putObject(PutObjectArgs.builder()
          .bucket(bucketName)
          .object(objectName)
          .stream(inputStream, size, -1)
          .contentType(contentType)
          .build());
      log.info("MinIO 파일 업로드 완료 - bucket: {}, object: {}", bucketName, objectName);
    } catch (Exception e) {
      log.error("MinIO 파일 업로드 실패 - bucket: {}, object: {}, error: {}", bucketName, objectName, e.getMessage(), e);
      throw new RuntimeException("파일 업로드 실패: " + objectName, e);
    }
  }

  public void uploadFile(String bucketName, String objectName, MultipartFile file) {
    try (InputStream inputStream = file.getInputStream()) {
      uploadFile(bucketName, objectName, inputStream, file.getSize(), file.getContentType());
    } catch (RuntimeException e) {
      throw e;
    } catch (IOException e) {
      log.error("MinIO MultipartFile 업로드 실패 - bucket: {}, object: {}, error: {}", bucketName, objectName, e.getMessage(), e);
      throw new RuntimeException("파일 업로드 실패: " + objectName, e);
    }
  }

  public InputStream downloadFile(String bucketName, String objectName) {
    try {
      return minioClient.getObject(GetObjectArgs.builder()
          .bucket(bucketName)
          .object(objectName)
          .build());
    } catch (Exception e) {
      log.error("MinIO 파일 다운로드 실패 - bucket: {}, object: {}, error: {}", bucketName, objectName, e.getMessage(), e);
      throw new RuntimeException("파일 다운로드 실패: " + objectName, e);
    }
  }

  public byte[] downloadFileAsBytes(String bucketName, String objectName) {
    try (InputStream inputStream = downloadFile(bucketName, objectName)) {
      return inputStream.readAllBytes();
    } catch (RuntimeException e) {
      throw e;
    } catch (IOException e) {
      log.error("MinIO 파일 바이트 변환 실패 - bucket: {}, object: {}, error: {}", bucketName, objectName, e.getMessage(), e);
      throw new RuntimeException("파일 바이트 변환 실패: " + objectName, e);
    }
  }

  public void deleteFile(String bucketName, String objectName) {
    try {
      minioClient.removeObject(RemoveObjectArgs.builder()
          .bucket(bucketName)
          .object(objectName)
          .build());
      log.info("MinIO 파일 삭제 완료 - bucket: {}, object: {}", bucketName, objectName);
    } catch (Exception e) {
      log.error("MinIO 파일 삭제 실패 - bucket: {}, object: {}, error: {}", bucketName, objectName, e.getMessage(), e);
      throw new RuntimeException("파일 삭제 실패: " + objectName, e);
    }
  }

  public StatObjectResponse getFileInfo(String bucketName, String objectName) {
    try {
      return minioClient.statObject(StatObjectArgs.builder()
          .bucket(bucketName)
          .object(objectName)
          .build());
    } catch (Exception e) {
      log.error("MinIO 파일 정보 조회 실패 - bucket: {}, object: {}, error: {}", bucketName, objectName, e.getMessage(), e);
      throw new RuntimeException("파일 정보 조회 실패: " + objectName, e);
    }
  }

  public List<Item> listObjects(String bucketName) {
    return listObjects(bucketName, null, false);
  }

  public List<Item> listObjects(String bucketName, String prefix) {
    return listObjects(bucketName, prefix, false);
  }

  public List<Item> listObjects(String bucketName, String prefix, boolean isRecursive) {
    try {
      ListObjectsArgs.Builder builder = ListObjectsArgs.builder()
          .bucket(bucketName)
          .recursive(isRecursive);

      if (prefix != null) {
        builder.prefix(prefix);
      }

      List<Item> items = new ArrayList<>();
      for (Result<Item> result : minioClient.listObjects(builder.build())) {
        items.add(result.get());
      }
      return items;
    } catch (Exception e) {
      log.error("MinIO 오브젝트 목록 조회 실패 - bucket: {}, prefix: {}, error: {}", bucketName, prefix, e.getMessage(), e);
      throw new RuntimeException("오브젝트 목록 조회 실패: " + bucketName, e);
    }
  }

  public String getPresignedDownloadUrl(String bucketName, String objectName, int expiry, TimeUnit timeUnit) {
    try {
      return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
          .method(Method.GET)
          .bucket(bucketName)
          .object(objectName)
          .expiry(expiry, timeUnit)
          .build());
    } catch (Exception e) {
      log.error("MinIO Presigned 다운로드 URL 생성 실패 - bucket: {}, object: {}, error: {}", bucketName, objectName, e.getMessage(), e);
      throw new RuntimeException("Presigned URL 생성 실패: " + objectName, e);
    }
  }

  public String getPresignedDownloadUrl(String bucketName, String objectName) {
    return getPresignedDownloadUrl(bucketName, objectName, DEFAULT_PRESIGNED_EXPIRY, DEFAULT_PRESIGNED_TIME_UNIT);
  }

  public String getPresignedUploadUrl(String bucketName, String objectName, int expiry, TimeUnit timeUnit) {
    try {
      return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
          .method(Method.PUT)
          .bucket(bucketName)
          .object(objectName)
          .expiry(expiry, timeUnit)
          .build());
    } catch (Exception e) {
      log.error("MinIO Presigned 업로드 URL 생성 실패 - bucket: {}, object: {}, error: {}", bucketName, objectName, e.getMessage(), e);
      throw new RuntimeException("Presigned 업로드 URL 생성 실패: " + objectName, e);
    }
  }

  public String getPresignedUploadUrl(String bucketName, String objectName) {
    return getPresignedUploadUrl(bucketName, objectName, DEFAULT_PRESIGNED_EXPIRY, DEFAULT_PRESIGNED_TIME_UNIT);
  }
}
