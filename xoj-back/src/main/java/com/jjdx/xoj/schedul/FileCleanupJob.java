package com.jjdx.xoj.schedul;

import com.jjdx.xoj.model.entity.FileRecord;
import com.jjdx.xoj.service.FileRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 文件清理定时任务
 */
@Component
@Slf4j
public class FileCleanupJob {

    @Value("${file.upload-dir:E:\\ideaProject\\JavaWebProjects\\xoj\\xoj-back\\uploads}")
    private String uploadDir;

    private final FileRecordService fileRecordService;

    public FileCleanupJob(FileRecordService fileRecordService) {
        this.fileRecordService = fileRecordService;
    }


    /**
     每天凌晨3点执行文件清理任务
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOrphanedFiles() {
        log.info("开始执行文件清理任务...");
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            log.warn("上传目录不存在: {}", uploadDir);
            return;
        }
        //  获取数据库中所有有效的文件记录
        List<FileRecord> validRecords = fileRecordService.lambdaQuery()
                .eq(FileRecord::getIsDelete, 0)
                .list();
        Set<String> validFileNames = validRecords.stream()
                .map(FileRecord::getStorageName)
                .collect(Collectors.toSet());
        // 遍历删除
        try {
            doClean(uploadPath, validFileNames);
            log.info("文件清理任务完成");
        } catch (Exception e) {
            log.error("文件清理任务执行失败", e);
        }
    }

    private void doClean(Path uploadPath, Set<String> validFileNames) throws IOException {
        try (Stream<Path> paths = Files.list(uploadPath)) {
            Consumer<Path> delFile = filePath -> {
                String fileName = filePath.getFileName().toString();
                // 检查文件是否在有效记录中
                if (validFileNames.contains(fileName)) return;
                try {
                    Files.delete(filePath);
                    log.info("删除无效文件: {}", fileName);
                } catch (IOException e) {
                    log.error("删除文件失败: {}", fileName, e);
                }
            };
            paths.filter(Files::isRegularFile).forEach(delFile);
        }
    }
}
