package com.cloudstorage.model;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "files")
public class File {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String fileName;
    
    private String fileType;
    
    private Long fileSize;
    
    private String filePath;
    
    // 文件夹支持
    @Column(nullable = false)
    private Boolean isFolder = false;
    
    // 父文件夹ID（null表示根目录）
    private Long parentId;
    
    // 文件路径（用于显示层级结构，如：/folder1/folder2）
    private String path;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @CreationTimestamp
    private LocalDateTime uploadedAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
