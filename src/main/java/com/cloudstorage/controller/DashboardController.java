package com.cloudstorage.controller;

import com.cloudstorage.model.File;
import com.cloudstorage.model.User;
import com.cloudstorage.repository.UserRepository;
import com.cloudstorage.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final FileService fileService;
    private final UserRepository userRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        List<File> files = fileService.getUserFiles(user);
        model.addAttribute("files", files);
        return "dashboard";
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file, 
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
            fileService.uploadFile(file, user);
            redirectAttributes.addFlashAttribute("message", "File uploaded successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Could not upload the file: " + e.getMessage());
        }
        return "redirect:/dashboard";
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id, Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        Path filePath = fileService.getFilePath(id, user);
        
        if (filePath == null) {
            return ResponseEntity.notFound().build();
        }
        
        Resource resource = new FileSystemResource(filePath);
        File file = fileService.getFileById(id, user).orElseThrow();
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
                .body(resource);
    }

    @GetMapping("/delete/{id}")
    public String deleteFile(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
            fileService.deleteFile(id, user);
            redirectAttributes.addFlashAttribute("message", "File deleted successfully!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("message", "Could not delete the file: " + e.getMessage());
        }
        return "redirect:/dashboard";
    }
}
