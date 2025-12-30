package com.testexecutionnode.scheduler;

import com.testexecutionnode.config.NodeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Component
public class TempFileCleanupScheduler {
    private static final Logger logger = LoggerFactory.getLogger(TempFileCleanupScheduler.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    private NodeConfig nodeConfig;

    @Value("${project.root.dir}")
    private String projectRootDir;

    /**
     * 初始化临时目录
     */
    public void initTempDirectory() {
        try {
            Path projectRoot = Paths.get(projectRootDir);
            Path tempDir = projectRoot.resolve(nodeConfig.getTempScriptDirectory());
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
                logger.info("Created temporary directory: {}", tempDir.toAbsolutePath());
            } else {
                logger.info("Using existing temporary directory: {}", tempDir.toAbsolutePath());
            }
        } catch (IOException e) {
            logger.error("Failed to create or access temporary directory: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取临时文件路径
     * @param prefix 文件名前缀
     * @param suffix 文件扩展名
     * @return 临时文件路径
     * @throws IOException IO异常
     */
    public Path getTempFilePath(String prefix, String suffix) throws IOException {
        initTempDirectory();
        
        String currentDate = LocalDate.now().format(DATE_FORMATTER);
        
        Path projectRoot = Paths.get(projectRootDir);
        
        Path tempDir = projectRoot.resolve(nodeConfig.getTempScriptDirectory());
        Path dateDir = tempDir.resolve(currentDate);
        if (!Files.exists(dateDir)) {
            Files.createDirectories(dateDir);
            logger.info("Created date directory: {}", dateDir.toAbsolutePath());
        }
        
        File tempFile = File.createTempFile(prefix, suffix, dateDir.toFile());
        logger.info("Created temporary file: {}", tempFile.getAbsolutePath());
        return tempFile.toPath();
    }

    /**
     * 定时任务：每天凌晨1点清理一次当天往前的所有日期文件夹
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void cleanupExpiredTempFiles() {
        logger.info("Starting cleanup of expired temporary directories");
        try {
            Path projectRoot = Paths.get(projectRootDir);
            Path tempDir = projectRoot.resolve(nodeConfig.getTempScriptDirectory());
            if (!Files.exists(tempDir)) {
                logger.info("Temporary directory does not exist, skipping cleanup");
                return;
            }

            LocalDate currentDate = LocalDate.now();
            int deletedDirsCount = 0;
            int deletedFilesCount = 0;

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempDir)) {
                for (Path dir : stream) {
                    if (Files.isDirectory(dir)) {
                        String dirName = dir.getFileName().toString();
                        try {
                            LocalDate dirDate = LocalDate.parse(dirName, DATE_FORMATTER);
                            
                            if (dirDate.isBefore(currentDate)) {
                                try (DirectoryStream<Path> fileStream = Files.newDirectoryStream(dir)) {
                                    for (Path file : fileStream) {
                                        Files.deleteIfExists(file);
                                        deletedFilesCount++;
                                    }
                                }
                                
                                Files.deleteIfExists(dir);
                                deletedDirsCount++;
                                logger.info("Deleted expired date directory: {}", dir.toAbsolutePath());
                            }
                        } catch (Exception e) {
                            logger.warn("Skipping directory with invalid date format: {}", dirName);
                        }
                    }
                }

                logger.info("Cleanup completed: deleted {} directories and {} files", deletedDirsCount, deletedFilesCount);
            }
        } catch (IOException e) {
            logger.error("Error during temporary directories cleanup: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 定时任务：每小时清理一次过期的临时文件（超过24小时的文件）
     */
    @Scheduled(fixedRate = 3600000)
    public void cleanupExpiredTempFilesHourly() {
        logger.info("Starting cleanup of expired temporary files");
        try {
            Path projectRoot = Paths.get(projectRootDir);
            Path tempDir = projectRoot.resolve(nodeConfig.getTempScriptDirectory());
            if (!Files.exists(tempDir)) {
                logger.info("Temporary directory does not exist, skipping cleanup");
                return;
            }

            long twentyFourHoursAgo = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24);
            int deletedCount = 0;

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempDir)) {
                for (Path file : stream) {
                    BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
                    long creationTime = attributes.creationTime().toMillis();

                    if (creationTime < twentyFourHoursAgo) {
                        Files.deleteIfExists(file);
                        deletedCount++;
                        logger.info("Deleted expired temporary file: {}", file.toAbsolutePath());
                    }
                }

                logger.info("Cleanup completed: deleted {} expired temporary files", deletedCount);
            }
        } catch (IOException e) {
            logger.error("Error during temporary files cleanup: {}", e.getMessage());
            e.printStackTrace();
        }
    }
}