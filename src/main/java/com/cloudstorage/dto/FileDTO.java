package com.cloudstorage.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FileDTO {
    private Long id;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private LocalDateTime uploadedAt;
    private Boolean isFolder;
    private Long parentId;
    private String path;
}
