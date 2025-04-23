package cn.superlu.onlyoffice.service;


import cn.superlu.onlyoffice.config.MinioConfig;
import cn.superlu.onlyoffice.dto.FileInfoDTO;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {
    private final MinioClient minioClient;

    private final MinioConfig minioConfig;

    /**
     * 上传文件到MinIO
     *
     * @param file        上传的文件
     * @param objectName  对象名称
     * @param contentType 内容类型
     * @return 是否上传成功
     */
    public boolean uploadFile(MultipartFile file, String objectName, String contentType) {
        try {
            // 检查存储桶是否存在，不存在则创建
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioConfig.getBucketName()).build());

            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioConfig.getBucketName()).build());
            }

            // 上传文件
            minioClient.putObject(PutObjectArgs.builder().bucket(minioConfig.getBucketName()).object(objectName).contentType(contentType).stream(file.getInputStream(), file.getSize(), -1).build());

            return true;
        } catch (Exception e) {
            log.error("上传文件到MinIO失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 上传本地文件到MinIO 和上一个方法io流不同
     *
     * @param file        要上传的本地文件
     * @param objectName  文件在MinIO中的名称
     * @param contentType 文件的内容类型
     * @return 如果上传成功返回true，否则返回false
     */
    public boolean uploadFile(File file, String objectName, String contentType) {
        try (InputStream inputStream = new FileInputStream(file)) {
            // 检查存储桶是否存在，不存在则创建
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioConfig.getBucketName()).build());

            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioConfig.getBucketName()).build());
            }

            // 上传文件
            minioClient.putObject(PutObjectArgs.builder().bucket(minioConfig.getBucketName()).object(objectName).contentType(contentType).stream(inputStream, file.length(), -1).build());

            return true;
        } catch (IOException | MinioException e) {
            // 使用日志记录错误信息
            log.error("上传文件到MinIO失败: {}", e.getMessage(), e);
            return false;
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取文件访问URL
     *
     * @param objectName 对象名称
     * @return 文件访问URL
     */
    public String getFileUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().bucket(minioConfig.getBucketName()).object(objectName).method(Method.GET).expiry(7, TimeUnit.DAYS) // URL有效期7天
                    .build());
        } catch (Exception e) {
            log.error("获取文件URL失败: {}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * 从MinIO下载文件
     *
     * @param objectName 对象名称
     * @return 文件输入流
     */
    public InputStream downloadFile(String objectName) {
        try {
            return minioClient.getObject(GetObjectArgs.builder().bucket(minioConfig.getBucketName()).object(objectName).build());
        } catch (Exception e) {
            log.error("从MinIO下载文件失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 删除MinIO中的文件
     *
     * @param objectName 对象名称
     * @return 是否删除成功
     */
    public boolean deleteFile(String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(minioConfig.getBucketName()).object(objectName).build());
            return true;
        } catch (Exception e) {
            log.error("删除MinIO文件失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取MinIO中所有文件的列表
     *
     * @return 文件信息列表
     */
    public List<FileInfoDTO> listFiles() {
        List<FileInfoDTO> fileList = new ArrayList<>();
        try {
            // 列出存储桶中的所有对象
            Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder().bucket(minioConfig.getBucketName()).build());

            for (Result<Item> result : results) {
                Item item = result.get();
                String objectName = item.objectName();

                // 跳过目录对象
                if (objectName.endsWith("/")) {
                    continue;
                }

                // 获取文件名和扩展名
                String fileName = objectName;
                String fileExtension = "";
                int dotIndex = objectName.lastIndexOf('.');
                if (dotIndex > 0) {
                    fileExtension = objectName.substring(dotIndex + 1);
                }

                // 获取文件键（去除扩展名）
                String fileKey = dotIndex > 0 ? objectName.substring(0, dotIndex) : objectName;

                // 获取文件URL
                String fileUrl = getFileUrl(objectName);

                // 创建文件信息对象
                FileInfoDTO fileInfo = new FileInfoDTO();
                fileInfo.setFileName(fileName);
                fileInfo.setFileUrl(fileUrl);
                fileInfo.setFileType(fileExtension);
                fileInfo.setFileKey(fileKey);

                // 将上传时间设置为最后修改时间
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                fileInfo.setUploadTime(Long.parseLong(dateFormat.format(new Date(item.lastModified().toEpochSecond() * 1000))));

                fileList.add(fileInfo);
            }
        } catch (Exception e) {
            log.error("获取文件列表失败: {}", e.getMessage(), e);
        }

        // 按最后修改时间降序排序
        fileList.sort((a, b) -> {
            try {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date dateA = format.parse(String.valueOf(a.getUploadTime()));
                Date dateB = format.parse(String.valueOf(b.getUploadTime()));
                return dateB.compareTo(dateA);
            } catch (Exception e) {
                return 0;
            }
        });

        return fileList;
    }
}
