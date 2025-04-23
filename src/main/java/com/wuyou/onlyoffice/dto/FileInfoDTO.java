package cn.superlu.onlyoffice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class FileInfoDTO {
    // 文件名
    private String fileName;
    // 文件URL
    private String fileUrl;
    // 文件类型
    private String fileType;
    // 文件键
    private String fileKey;
    // 上传时间（时间戳）
    private long uploadTime;

    // 格式化的上传时间，用于前端显示
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    public String getUploadTimeFormatted() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date(uploadTime));
    }
}
