package com.jjdx.xoj.controller;

import com.jjdx.xoj.common.BaseResponse;
import com.jjdx.xoj.common.ErrorCode;
import com.jjdx.xoj.common.ResultUtils;
import com.jjdx.xoj.exception.BusinessException;
import com.jjdx.xoj.model.entity.FileRecord;
import com.jjdx.xoj.model.entity.User;
import com.jjdx.xoj.service.FileRecordService;
import com.jjdx.xoj.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 文件管理接口
 */
@RestController
@RequestMapping("/file")
@Slf4j
public class FileRecordController {

    @Resource
    private UserService userService;

    @Resource
    private FileRecordService fileRecordService;

    @Value("${file.upload-dir:E:\\ideaProject\\JavaWebProjects\\xoj\\xoj-back\\uploads}")
    private String uploadDir;

    /**
     上传文件

     @param file    上传的文件
     @param request HTTP请求
     @return 文件访问路径
     */
    @PostMapping("/upload/avatar")
    public BaseResponse<String> uploadFile(@RequestPart MultipartFile file,
                                           HttpServletRequest request) {
        // 1. 校验文件
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }

        // 2. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        try {
            // 3. 创建上传目录(如果不存在)
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 4. 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = FilenameUtils.getExtension(originalFilename);
            String storageName = UUID.randomUUID() + "." + fileExtension;
            Path filePath = uploadPath.resolve(storageName);

            // 5. 保存文件
            file.transferTo(filePath.toFile());

            // 6. 保存文件记录到数据库
            FileRecord fileRecord = new FileRecord();
            fileRecord.setUserId(loginUser.getId());
            fileRecord.setOriginalName(originalFilename);
            fileRecord.setStorageName(storageName);
            fileRecord.setFileSize(file.getSize());
            fileRecord.setFileType(file.getContentType());
            fileRecord.setFileExtension(fileExtension);
            fileRecord.setFileCategory("avatar"); // 可以根据需要修改分类

            boolean saved = fileRecordService.save(fileRecord);
            if (!saved) {
                // 如果数据库保存失败，删除已上传的文件
                Files.deleteIfExists(filePath);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件保存失败");
            }

            // 7. 返回文件访问路径
            return ResultUtils.success(storageName);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        }
    }

    /**
     获取头像

     @param storageName 存储文件名
     @return 头像的二进制数据
     */
    @GetMapping("/getAvatar")
    public BaseResponse<byte[]> getAvatar(String storageName) {
        try {
            Path filePath = Paths.get(uploadDir, storageName);
            if (Files.exists(filePath)) {
                return ResultUtils.success(Files.readAllBytes(filePath));
            } else {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "头像文件未找到");
            }
        } catch (IOException e) {
            log.error("获取头像失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取头像失败");
        }
    }
}
