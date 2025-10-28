package com.cloudstorage.repository;

import com.cloudstorage.model.File;
import com.cloudstorage.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {
    List<File> findByUserOrderByUploadedAtDesc(User user);
    Optional<File> findByIdAndUser(Long id, User user);
    boolean existsByFileNameAndUser(String fileName, User user);
    
    // 文件夹相关查询
    List<File> findByUserAndParentIdIsNullOrderByIsFolderDescUploadedAtDesc(User user);
    List<File> findByUserAndParentIdOrderByIsFolderDescUploadedAtDesc(User user, Long parentId);
    
    // 查找指定用户、父文件夹和文件名的文件
    Optional<File> findByUserAndParentIdAndFileName(User user, Long parentId, String fileName);
}
