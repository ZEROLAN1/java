package com.cloudstorage.service;

import com.cloudstorage.model.File;
import com.cloudstorage.model.User;
import com.cloudstorage.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    
    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
            System.out.println("✅ 文件上传目录已初始化: " + uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize folder for upload!");
        }
    }

    public File uploadFile(MultipartFile file, User user) throws IOException {
        return uploadFile(file, user, null);
    }
    
    public File uploadFile(MultipartFile file, User user, Long parentId) throws IOException {
        // 获取原始文件名（可能包含路径）
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IOException("文件名不能为空");
        }
        
        // 处理文件路径，统一使用/作为分隔符
        String fileName = originalFilename.replace("\\", "/");
        
        // 分解路径：提取文件夹路径和纯文件名
        String[] pathParts = fileName.split("/");
        String pureFileName = pathParts[pathParts.length - 1]; // 最后一部分是文件名
        
        // 如果有子目录，需要创建文件夹结构
        Long currentParentId = parentId;
        if (pathParts.length > 1) {
            // 逐层创建文件夹
            for (int i = 0; i < pathParts.length - 1; i++) {
                String folderName = pathParts[i];
                // 检查文件夹是否已存在
                Optional<File> existingFolder = fileRepository.findByUserAndParentIdAndFileName(user, currentParentId, folderName);
                if (existingFolder.isPresent() && existingFolder.get().getIsFolder()) {
                    currentParentId = existingFolder.get().getId();
                } else {
                    // 创建新文件夹
                    File newFolder = createFolder(folderName, user, currentParentId);
                    currentParentId = newFolder.getId();
                }
            }
        }
        
        // Create user-specific directory
        String userDir = uploadDir + "/" + user.getId() + "/";
        
        // 如果有父文件夹，获取父文件夹路径
        String relativePath = "/";
        if (currentParentId != null) {
            Optional<File> parentFolder = fileRepository.findByIdAndUser(currentParentId, user);
            if (parentFolder.isPresent() && parentFolder.get().getIsFolder()) {
                relativePath = parentFolder.get().getPath() + "/";
                userDir += relativePath.substring(1); // 移除开头的/
            }
        }
        
        // 构建完整的文件路径
        String filePath = userDir + pureFileName;
        Path targetPath = Paths.get(filePath);
        
        // 创建所有必要的父目录
        Files.createDirectories(targetPath.getParent());
        
        // 如果文件已存在，先删除
        if (Files.exists(targetPath)) {
            Files.delete(targetPath);
        }
        
        Files.copy(file.getInputStream(), targetPath);
        
        // 检查数据库中是否已存在同名文件（使用纯文件名）
        Optional<File> existingFile = fileRepository.findByUserAndParentIdAndFileName(user, currentParentId, pureFileName);
        
        File fileEntity;
        if (existingFile.isPresent()) {
            // 如果文件已存在，更新元数据
            fileEntity = existingFile.get();
            fileEntity.setFileType(file.getContentType());
            fileEntity.setFileSize(file.getSize());
            fileEntity.setFilePath(filePath);
        } else {
            // 如果文件不存在，创建新记录
            fileEntity = new File();
            fileEntity.setFileName(pureFileName); // 使用纯文件名
            fileEntity.setFileType(file.getContentType());
            fileEntity.setFileSize(file.getSize());
            fileEntity.setFilePath(filePath);
            fileEntity.setUser(user);
            fileEntity.setIsFolder(false);
            fileEntity.setParentId(currentParentId); // 使用最终的父文件夹ID
            fileEntity.setPath(relativePath + pureFileName);
        }
        
        return fileRepository.save(fileEntity);
    }

    public List<File> getUserFiles(User user) {
        return fileRepository.findByUserOrderByUploadedAtDesc(user);
    }

    public Optional<File> getFileById(Long id, User user) {
        return fileRepository.findByIdAndUser(id, user);
    }

    public void deleteFile(Long id, User user) throws IOException {
        Optional<File> file = fileRepository.findByIdAndUser(id, user);
        if (file.isPresent()) {
            File fileEntity = file.get();
            
            if (fileEntity.getIsFolder()) {
                // 如果是文件夹，递归删除所有子文件和子文件夹
                deleteFolderRecursively(fileEntity, user);
            } else {
                // 如果是文件，直接删除
                Files.deleteIfExists(Paths.get(fileEntity.getFilePath()));
                fileRepository.delete(fileEntity);
            }
        }
    }
    
    private void deleteFolderRecursively(File folder, User user) throws IOException {
        // 获取文件夹下的所有子文件和子文件夹
        List<File> children = fileRepository.findByUserAndParentIdOrderByIsFolderDescUploadedAtDesc(user, folder.getId());
        
        // 递归删除所有子项
        for (File child : children) {
            if (child.getIsFolder()) {
                deleteFolderRecursively(child, user);
            } else {
                Files.deleteIfExists(Paths.get(child.getFilePath()));
                fileRepository.delete(child);
            }
        }
        
        // 删除文件夹本身
        Path folderPath = Paths.get(folder.getFilePath());
        if (Files.exists(folderPath)) {
            Files.delete(folderPath);
        }
        fileRepository.delete(folder);
    }

    public Path getFilePath(Long id, User user) {
        Optional<File> file = fileRepository.findByIdAndUser(id, user);
        return file.map(value -> Paths.get(value.getFilePath())).orElse(null);
    }
    
    // 重命名文件或文件夹
    public File renameFile(Long id, String newName, User user) throws IOException {
        Optional<File> fileOpt = fileRepository.findByIdAndUser(id, user);
        if (!fileOpt.isPresent()) {
            throw new IOException("文件不存在");
        }
        
        File file = fileOpt.get();
        String oldPath = file.getFilePath();
        Path oldFilePath = Paths.get(oldPath);
        
        // 构建新路径
        Path newFilePath = oldFilePath.getParent().resolve(newName);
        
        // 重命名物理文件/文件夹
        Files.move(oldFilePath, newFilePath);
        
        // 更新数据库
        file.setFileName(newName);
        file.setFilePath(newFilePath.toString());
        
        // 更新path
        String oldFileName = file.getPath().substring(file.getPath().lastIndexOf('/') + 1);
        file.setPath(file.getPath().replace(oldFileName, newName));
        
        return fileRepository.save(file);
    }
    
    // 移动文件或文件夹到另一个文件夹
    public File moveFile(Long fileId, Long targetFolderId, User user) throws IOException {
        Optional<File> fileOpt = fileRepository.findByIdAndUser(fileId, user);
        if (!fileOpt.isPresent()) {
            throw new IOException("文件不存在");
        }
        
        File file = fileOpt.get();
        
        // 验证目标文件夹
        String newRelativePath = "/";
        if (targetFolderId != null) {
            Optional<File> targetFolderOpt = fileRepository.findByIdAndUser(targetFolderId, user);
            if (!targetFolderOpt.isPresent() || !targetFolderOpt.get().getIsFolder()) {
                throw new IOException("目标文件夹不存在");
            }
            newRelativePath = targetFolderOpt.get().getPath() + "/";
        }
        
        // 不能将文件夹移动到它自己或它的子文件夹中
        if (file.getIsFolder() && targetFolderId != null) {
            if (fileId.equals(targetFolderId) || isDescendant(targetFolderId, fileId, user)) {
                throw new IOException("不能将文件夹移动到它自己或它的子文件夹中");
            }
        }
        
        // 移动物理文件/文件夹
        String oldPhysicalPath = file.getFilePath();
        String userDir = uploadDir + "/" + user.getId() + "/";
        String newPhysicalPath = userDir + (targetFolderId != null ? newRelativePath.substring(1) : "") + file.getFileName();
        
        Path oldPath = Paths.get(oldPhysicalPath);
        Path newPath = Paths.get(newPhysicalPath);
        
        // 确保目标目录存在
        Files.createDirectories(newPath.getParent());
        
        // 移动文件/文件夹
        Files.move(oldPath, newPath);
        
        // 更新数据库
        file.setParentId(targetFolderId);
        file.setPath(newRelativePath + file.getFileName());
        file.setFilePath(newPhysicalPath);
        
        // 如果是文件夹，需要递归更新所有子文件的路径
        if (file.getIsFolder()) {
            updateChildrenPaths(file, user);
        }
        
        return fileRepository.save(file);
    }
    
    // 检查是否为子文件夹
    private boolean isDescendant(Long potentialDescendantId, Long ancestorId, User user) {
        Optional<File> file = fileRepository.findByIdAndUser(potentialDescendantId, user);
        while (file.isPresent() && file.get().getParentId() != null) {
            if (file.get().getParentId().equals(ancestorId)) {
                return true;
            }
            file = fileRepository.findByIdAndUser(file.get().getParentId(), user);
        }
        return false;
    }
    
    // 递归更新子文件的路径
    private void updateChildrenPaths(File folder, User user) throws IOException {
        List<File> children = fileRepository.findByUserAndParentIdOrderByIsFolderDescUploadedAtDesc(user, folder.getId());
        
        for (File child : children) {
            String oldPath = child.getFilePath();
            String newPath = folder.getFilePath() + "/" + child.getFileName();
            
            // 移动物理文件
            Files.move(Paths.get(oldPath), Paths.get(newPath));
            
            // 更新数据库
            child.setFilePath(newPath);
            child.setPath(folder.getPath() + "/" + child.getFileName());
            fileRepository.save(child);
            
            // 如果是文件夹，递归更新
            if (child.getIsFolder()) {
                updateChildrenPaths(child, user);
            }
        }
    }
    
    // 创建文件夹
    public File createFolder(String folderName, User user, Long parentId) throws IOException {
        // Create user-specific directory
        String userDir = uploadDir + "/" + user.getId() + "/";
        
        // 如果有父文件夹，获取父文件夹路径
        String relativePath = "/";
        if (parentId != null) {
            Optional<File> parentFolder = fileRepository.findByIdAndUser(parentId, user);
            if (parentFolder.isPresent() && parentFolder.get().getIsFolder()) {
                relativePath = parentFolder.get().getPath() + "/";
                userDir += relativePath.substring(1); // 移除开头的/
            }
        }
        
        // 创建物理文件夹
        String folderPath = userDir + folderName;
        Files.createDirectories(Paths.get(folderPath));
        
        // 保存文件夹元数据到数据库
        File folderEntity = new File();
        folderEntity.setFileName(folderName);
        folderEntity.setFileType("folder");
        folderEntity.setFileSize(0L);
        folderEntity.setFilePath(folderPath);
        folderEntity.setUser(user);
        folderEntity.setIsFolder(true);
        folderEntity.setParentId(parentId);
        folderEntity.setPath(relativePath + folderName);
        
        return fileRepository.save(folderEntity);
    }
    
    // 获取指定文件夹下的文件和子文件夹
    public List<File> getFilesInFolder(User user, Long parentId) {
        if (parentId == null) {
            // 获取根目录下的文件
            return fileRepository.findByUserAndParentIdIsNullOrderByIsFolderDescUploadedAtDesc(user);
        } else {
            return fileRepository.findByUserAndParentIdOrderByIsFolderDescUploadedAtDesc(user, parentId);
        }
    }
    
    // 读取文本文件内容
    public String readTextFileContent(Long id, User user) throws IOException {
        Optional<File> file = fileRepository.findByIdAndUser(id, user);
        if (file.isPresent() && !file.get().getIsFolder()) {
            Path filePath = Paths.get(file.get().getFilePath());
            if (Files.exists(filePath)) {
                // 限制文件大小，避免读取过大文件
                if (Files.size(filePath) > 1024 * 1024) { // 1MB
                    throw new IOException("文件过大，无法在线预览");
                }
                return Files.readString(filePath);
            }
        }
        throw new IOException("文件不存在或无法读取");
    }
}
