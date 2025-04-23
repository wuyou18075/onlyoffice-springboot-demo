package cn.superlu.onlyoffice.controller;


import cn.superlu.onlyoffice.dto.FileInfoDTO;
import cn.superlu.onlyoffice.service.MinioService;
import cn.superlu.onlyoffice.service.OnlyOfficeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@Slf4j
@RestController
@RequestMapping("/api/onlyoffice")
@RequiredArgsConstructor
public class OnlyOfficeController {

    private final OnlyOfficeService onlyOfficeService;

    private final MinioService minioService;


    /**
     * 上传文件
     *
     * @param file 上传的文件
     * @return 上传结果
     */
    @PostMapping("/upload")
    public ResponseEntity<FileInfoDTO> uploadFile(@RequestParam("file") MultipartFile file) {
        FileInfoDTO fileInfo = onlyOfficeService.uploadFile(file);
        if (fileInfo == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(fileInfo);
    }

    /**
     * OnlyOffice回调接口
     * @param request HTTP请求
     * @param response HTTP响应
     * @return 处理结果
     * @throws IOException IO异常
     */
    @RequestMapping("/callback")
    @ResponseBody
    public String callback(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 设置正确的响应内容类型
        response.setContentType("application/json");

        // 读取请求体
        Scanner scanner = new Scanner(request.getInputStream()).useDelimiter("\\A");
        String body = scanner.hasNext() ? scanner.next() : "";

        // 处理回调
        return onlyOfficeService.handleCallback(body);
    }

    /**
     * 删除文件
     *
     * @param fileKey 文件键
     * @return 删除结果
     */
    @DeleteMapping("/delete/{fileKey}")
    public ResponseEntity<Map<String, Object>> deleteFile(@PathVariable String fileKey) {
        Map<String, Object> response = new HashMap<>();
        boolean deleted = minioService.deleteFile(fileKey);

        if (deleted) {
            response.put("success", true);
            response.put("message", "文件删除成功");
        } else {
            response.put("success", false);
            response.put("message", "文件删除失败");
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 获取文件列表
     *
     * @return 文件列表
     */
    @GetMapping("/files")
    public ResponseEntity<List<FileInfoDTO>> getFiles() {
        List<FileInfoDTO> files = minioService.listFiles();
        return ResponseEntity.ok(files);
    }
}
