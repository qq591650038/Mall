package com.mall.controller;

import com.mall.common.result.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {
    private final Path root;

    public FileController(@Value("${mall.file.root:./uploads}") String root) {
        this.root = Paths.get(root).toAbsolutePath().normalize();
    }

    @PostMapping("/images")
    public Result<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024) throw new IllegalArgumentException("图片为空或超过5MB");
        String type = file.getContentType();
        if (type == null || !type.startsWith("image/")) throw new IllegalArgumentException("仅支持图片文件");
        Files.createDirectories(root);
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String name = UUID.randomUUID() + "." + (ext == null ? "bin" : ext);
        Files.copy(file.getInputStream(), root.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        return Result.success("上传成功", "/uploads/" + name);
    }
}
