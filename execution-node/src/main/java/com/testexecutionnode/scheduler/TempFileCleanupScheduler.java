package com.testexecutionnode.scheduler;

import com.testexecutionnode.config.NodeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    /**
     * 初始化临时目录
     */
    public void initTempDirectory() {
        try {
            // 获取项目根目录（当在execution-node目录运行时，父目录就是项目根目录）
            Path projectRoot = Paths.get(System.getProperty("user.dir")).getParent();
            if (projectRoot == null) {
                logger.error("Failed to get project root directory");
                return;
            }
            
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
        // 确保临时目录存在
        initTempDirectory();
        
        // 获取当前日期，格式化为yyyyMMdd
        String currentDate = LocalDate.now().format(DATE_FORMATTER);
        
        // 获取项目根目录（当在execution-node目录运行时，父目录就是项目根目录）
        Path projectRoot = Paths.get(System.getProperty("user.dir")).getParent();
        if (projectRoot == null) {
            // 如果获取不到父目录，使用当前目录
            projectRoot = Paths.get(System.getProperty("user.dir"));
        }
        
        // 在项目根目录下创建日期子目录
        Path tempDir = projectRoot.resolve(nodeConfig.getTempScriptDirectory());
        Path dateDir = tempDir.resolve(currentDate);
        if (!Files.exists(dateDir)) {
            Files.createDirectories(dateDir);
            logger.info("Created date directory: {}", dateDir.toAbsolutePath());
        }
        
        // 在日期子目录中创建临时文件
        File tempFile = File.createTempFile(prefix, suffix, dateDir.toFile());
        logger.info("Created temporary file: {}", tempFile.getAbsolutePath());
        return tempFile.toPath();
    }

    /**
     * 定时任务：每天凌晨1点清理一次当天往前的所有日期文件夹
     */
    @Scheduled(cron = "0 0 1 * * ?") // 每天凌晨1点执行一次
    public void cleanupExpiredTempFiles() {
        logger.info("Starting cleanup of expired temporary directories");
        try {
            // 获取项目根目录
            Path projectRoot = Paths.get(System.getProperty("user.dir")).getParent();
            Path tempDir = projectRoot.resolve(nodeConfig.getTempScriptDirectory());
            if (!Files.exists(tempDir)) {
                logger.info("Temporary directory does not exist, skipping cleanup");
                return;
            }

            // 获取当前日期
            LocalDate currentDate = LocalDate.now();
            int deletedDirsCount = 0;
            int deletedFilesCount = 0;

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempDir)) {
                for (Path dir : stream) {
                    if (Files.isDirectory(dir)) {
                        String dirName = dir.getFileName().toString();
                        try {
                            // 解析日期目录名
                            LocalDate dirDate = LocalDate.parse(dirName, DATE_FORMATTER);
                            
                            // 如果目录日期在当前日期之前，则删除该目录及其所有内容
                            if (dirDate.isBefore(currentDate)) {
                                // 计算删除的文件数量
                                try (DirectoryStream<Path> fileStream = Files.newDirectoryStream(dir)) {
                                    for (Path file : fileStream) {
                                        Files.deleteIfExists(file);
                                        deletedFilesCount++;
                                    }
                                }
                                
                                // 删除目录
                                Files.deleteIfExists(dir);
                                deletedDirsCount++;
                                logger.info("Deleted expired date directory: {}", dir.toAbsolutePath());
                            }
                        } catch (Exception e) {
                            // 如果目录名不是有效日期格式，跳过该目录
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
    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    public void cleanupExpiredTempFilesHourly() {
        logger.info("Starting cleanup of expired temporary files");
        try {
            // 获取项目根目录
            Path projectRoot = Paths.get(System.getProperty("user.dir")).getParent();
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