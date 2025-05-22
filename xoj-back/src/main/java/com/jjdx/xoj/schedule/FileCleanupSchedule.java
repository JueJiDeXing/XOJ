package com.jjdx.xoj.schedule;

import com.jjdx.xoj.service.QuestionService;
import com.jjdx.xoj.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class FileCleanupSchedule {

    @Value("${file.upload-dir:E:\\ideaProject\\JavaWebProjects\\xoj\\xoj-back\\uploads}")
    private String uploadDir;

    @Value("${file.prefix.user-dir:user}")
    private String userDirPrefix;

    @Value("${file.prefix.judge-case-dir:judge-case}")
    private String judgeCaseDirPrefix;

    @Resource
    private UserService userService;

    @Resource
    private QuestionService questionService;


    /**
     每天凌晨3点执行清理任务
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupUnusedFiles() {
        log.info("[定时任务]开始清理未被引用的文件...");
        try {
            cleanupUserAvatars();  // 清理用户头像
            cleanupJudgeCases();   // 清理测试用例
            log.info("[定时任务]文件清理完成");
        } catch (Exception e) {
            log.error("[定时任务]文件清理失败", e);
        }
    }

    private int cleanupUnusedDir(Path rootPath, Set<String> validVersions) throws IOException {
        File[] dirs = rootPath.toFile().listFiles();
        if (dirs == null) return 0;
        int count = 0;
        for (File dir : dirs) {
            String id = dir.getName();
            // 遍历版本目录
            File[] versionDirs = dir.listFiles();
            if (versionDirs == null) continue;
            for (File versionDir : versionDirs) {
                String versionKey = id + File.separator + versionDir.getName();
                // 删除未被引用的版本目录
                if (!validVersions.contains(versionKey)) {
                    count++;
                    FileUtils.deleteDirectory(versionDir);
                }
            }
        }
        return count;
    }

    private void cleanupUserAvatars() throws IOException {
        Path userRootPath = Paths.get(uploadDir, userDirPrefix);
        if (!Files.exists(userRootPath)) return;
        // 获取所有有效的用户ID及其头像版本
        Set<String> validVersions = userService.list()
                .stream()
                .map(user -> user.getId() + File.separator + user.getUserAvatarVersion())
                .collect(Collectors.toSet());
        int cnt = cleanupUnusedDir(userRootPath, validVersions);
        log.info("[定时任务]用户文件清理完成,共删除{}个文件夹", cnt);
    }

    private void cleanupJudgeCases() throws IOException {
        Path judgeCaseRootPath = Paths.get(uploadDir, judgeCaseDirPrefix);
        if (!Files.exists(judgeCaseRootPath)) return;
        // 获取所有有效的题目ID及其测试用例版本
        Set<String> validVersions = questionService.list()
                .stream()
                .map(question -> question.getId() + File.separator + question.getJudgeCaseVersion())
                .collect(Collectors.toSet());

        int cnt = cleanupUnusedDir(judgeCaseRootPath, validVersions);
        log.info("[定时任务]测试用例文件清理完成,共删除{}个文件夹", cnt);
    }
}
