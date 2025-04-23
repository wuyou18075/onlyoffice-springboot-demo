package cn.superlu.onlyoffice.service;

import cn.superlu.onlyoffice.config.OnlyOfficeConfig;
import cn.superlu.onlyoffice.dto.FileInfoDTO;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnlyOfficeService {

    private final MinioService minioService;
    /**
     * //     * 1 - 正在编辑文档，
     * //     * 2 - 文档已准备好保存，
     * //     * 3 - 发生文档保存错误，
     * //     * 4 - 文档已关闭，没有任何更改，
     * //     * 6 - 正在编辑文档，但保存了当前文档状态，
     * //     * 7 - 强制保存文档时发生错误。
     * //
     */
    private static final Map<Integer, String> documentStatusMap;

    static {
        Map<Integer, String> aMap = new HashMap<>();
        aMap.put(1, "status: 1 正在编辑文档");
        aMap.put(2, "status: 2 文档已准备好保存");
        aMap.put(3, "status: 3 发生文档保存错误");
        aMap.put(4, "status: 4 文档已关闭，没有任何更改");
        aMap.put(6, "status: 6 正在编辑文档，但保存了当前文档状态");
        aMap.put(7, "status: 7 强制保存文档时发生错误");
        documentStatusMap = Collections.unmodifiableMap(aMap);
    }

    /**
     * 上传文件
     *
     * @param file 上传的文件
     * @return 文件信息
     */
    public FileInfoDTO uploadFile(MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String fileKey = UUID.randomUUID().toString().replace("-", "");
            String objectName = fileKey + "." + fileExtension;

            // 上传到MinIO
            boolean uploadResult = minioService.uploadFile(file, objectName, file.getContentType());
            if (!uploadResult) {
                return null;
            }

            // 获取文件访问URL
            String fileUrl = minioService.getFileUrl(objectName);
            log.info("文件访问URL: {}", fileUrl);
            FileInfoDTO fileInfo = new FileInfoDTO();
            fileInfo.setFileName(originalFilename);
            fileInfo.setFileUrl(fileUrl);
            fileInfo.setFileType(fileExtension);
            fileInfo.setFileKey(fileKey);

            return fileInfo;
        } catch (Exception e) {
            log.error("上传文件失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 处理OnlyOffice回调
     *
     * @param body 回调请求体
     * @return 处理结果
     */
    public String handleCallback(String body) {
        try {
            JSONObject jsonObj = JSONObject.parseObject(body);
            log.info("OnlyOffice回调状态: {}", documentStatusMap.get(jsonObj.get("status")));

            // 检查状态值，2表示保存文档, 6表示强制保存
            if (jsonObj != null && (jsonObj.getIntValue("status") == 2 || jsonObj.getIntValue("status") == 6)) {
                String downloadUri = jsonObj.getString("url");
                String key = jsonObj.getString("key");
                String fileType = jsonObj.getString("filetype");

                // 创建临时文件保存
                URL url = new URL(downloadUri);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                try (InputStream stream = connection.getInputStream()) {
                    // 确保临时目录存在
                    Path tempDirPath = Paths.get(System.getProperty("java.io.tmpdir"), "onlyoffice");
                    if (!Files.exists(tempDirPath)) {
                        Files.createDirectories(tempDirPath);
                    }

                    // 将文件保存到临时目录
                    String tempFile = tempDirPath.toString() + File.separator + key + "." + fileType;
                    try (FileOutputStream out = new FileOutputStream(tempFile)) {
                        byte[] buffer = new byte[4096];
                        int read;
                        while ((read = stream.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                        out.flush();
                    }

                    // 将临时文件上传到MinIO
                    File file = new File(tempFile);

                    String objectName = key + "." + fileType;

                    // 确定文件类型
                    String contentType;
                    switch (fileType.toLowerCase()) {
                        case "docx":
                            contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                            break;
                        case "xlsx":
                            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                            break;
                        case "pptx":
                            contentType = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
                            break;
                        case "pdf":
                            contentType = "application/pdf";
                            break;
                        default:
                            contentType = "application/octet-stream";
                    }

                    // 上传到MinIO并替换原文件
                    minioService.uploadFile(file, objectName, contentType);

                    // 删除临时文件
                    if (file.exists()) {
                        file.delete();
                    }
                }
                connection.disconnect();
            }

            // 返回成功响应，很重要！
            return "{\"error\":0}";
        } catch (Exception e) {
            log.error("处理OnlyOffice回调失败", e);
            return "{\"error\":1,\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 获取文件扩展名
     *
     * @param filename 文件名
     * @return 扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? "" : filename.substring(dotIndex + 1);
    }

}
