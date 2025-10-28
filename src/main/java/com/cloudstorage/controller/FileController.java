package com.cloudstorage.controller;

import com.cloudstorage.dto.ApiResponse;
import com.cloudstorage.dto.FileDTO;
import com.cloudstorage.model.File;
import com.cloudstorage.model.User;
import com.cloudstorage.repository.UserRepository;
import com.cloudstorage.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> getUserFiles(
            @RequestParam(required = false) Long parentId,
            Authentication authentication) {
        try {
            User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            List<File> files = fileService.getFilesInFolder(user, parentId);
            List<FileDTO> fileDTOs = files.stream().map(file -> {
                FileDTO dto = new FileDTO();
                dto.setId(file.getId());
                dto.setFileName(file.getFileName());
                dto.setFileType(file.getFileType());
                dto.setFileSize(file.getFileSize());
                dto.setUploadedAt(file.getUploadedAt());
                dto.setIsFolder(file.getIsFolder());
                dto.setParentId(file.getParentId());
                dto.setPath(file.getPath());
                return dto;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(new ApiResponse(true, "获取文件列表成功", fileDTOs));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "获取文件列表失败: " + e.getMessage()));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long parentId,
            Authentication authentication) {
        try {
            User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            File uploadedFile = fileService.uploadFile(file, user, parentId);
            
            FileDTO dto = new FileDTO();
            dto.setId(uploadedFile.getId());
            dto.setFileName(uploadedFile.getFileName());
            dto.setFileType(uploadedFile.getFileType());
            dto.setFileSize(uploadedFile.getFileSize());
            dto.setUploadedAt(uploadedFile.getUploadedAt());
            dto.setIsFolder(uploadedFile.getIsFolder());
            dto.setParentId(uploadedFile.getParentId());
            dto.setPath(uploadedFile.getPath());
            
            return ResponseEntity.ok(new ApiResponse(true, "文件上传成功", dto));
        } catch (Exception e) {
            e.printStackTrace(); // 打印完整堆栈信息
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "文件上传失败: " + e.getMessage()));
        }
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<?> downloadFile(@PathVariable Long id, Authentication authentication) {
        try {
            User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            Path filePath = fileService.getFilePath(id, user);
            
            if (filePath == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "文件不存在"));
            }
            
            Resource resource = new FileSystemResource(filePath);
            File file = fileService.getFileById(id, user).orElseThrow();
            
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + file.getFileName() + "\"")
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "文件下载失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFile(@PathVariable Long id, Authentication authentication) {
        try {
            User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            fileService.deleteFile(id, user);
            
            return ResponseEntity.ok(new ApiResponse(true, "文件删除成功"));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "文件删除失败: " + e.getMessage()));
        }
    }
    
    @PostMapping("/folder")
    public ResponseEntity<?> createFolder(
            @RequestParam String folderName,
            @RequestParam(required = false) Long parentId,
            Authentication authentication) {
        try {
            User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            File folder = fileService.createFolder(folderName, user, parentId);
            
            FileDTO dto = new FileDTO();
            dto.setId(folder.getId());
            dto.setFileName(folder.getFileName());
            dto.setFileType(folder.getFileType());
            dto.setFileSize(folder.getFileSize());
            dto.setUploadedAt(folder.getUploadedAt());
            dto.setIsFolder(folder.getIsFolder());
            dto.setParentId(folder.getParentId());
            dto.setPath(folder.getPath());
            
            return ResponseEntity.ok(new ApiResponse(true, "文件夹创建成功", dto));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "文件夹创建失败: " + e.getMessage()));
        }
    }
    
    @GetMapping("/preview/{id}")
    public ResponseEntity<?> previewTextFile(@PathVariable Long id, Authentication authentication) {
        try {
            User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            String content = fileService.readTextFileContent(id, user);
            
            return ResponseEntity.ok(new ApiResponse(true, "获取文件内容成功", content));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "获取文件内容失败: " + e.getMessage()));
        }
    }
    
    @PutMapping("/move/{id}")
    public ResponseEntity<?> moveFile(
            @PathVariable Long id,
            @RequestParam(required = false) Long targetFolderId,
            Authentication authentication) {
        try {
            User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            File movedFile = fileService.moveFile(id, targetFolderId, user);
            
            FileDTO dto = new FileDTO();
            dto.setId(movedFile.getId());
            dto.setFileName(movedFile.getFileName());
            dto.setFileType(movedFile.getFileType());
            dto.setFileSize(movedFile.getFileSize());
            dto.setUploadedAt(movedFile.getUploadedAt());
            dto.setIsFolder(movedFile.getIsFolder());
            dto.setParentId(movedFile.getParentId());
            dto.setPath(movedFile.getPath());
            
            return ResponseEntity.ok(new ApiResponse(true, "移动成功", dto));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "移动失败: " + e.getMessage()));
        }
    }
    
    @PutMapping("/rename/{id}")
    public ResponseEntity<?> renameFile(
            @PathVariable Long id,
            @RequestParam String newName,
            Authentication authentication) {
        try {
            User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            File renamedFile = fileService.renameFile(id, newName, user);
            
            FileDTO dto = new FileDTO();
            dto.setId(renamedFile.getId());
            dto.setFileName(renamedFile.getFileName());
            dto.setFileType(renamedFile.getFileType());
            dto.setFileSize(renamedFile.getFileSize());
            dto.setUploadedAt(renamedFile.getUploadedAt());
            dto.setIsFolder(renamedFile.getIsFolder());
            dto.setParentId(renamedFile.getParentId());
            dto.setPath(renamedFile.getPath());
            
            return ResponseEntity.ok(new ApiResponse(true, "重命名成功", dto));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "重命名失败: " + e.getMessage()));
        }
    }
}
