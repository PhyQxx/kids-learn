package com.kidslearn.common.ftp;

import com.kidslearn.common.exception.BusinessException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FtpTool {

    private static final int TIMEOUT_MILLIS = 30_000;

    private final FtpProperties properties;

    public String upload(String serviceDir, String fileName, InputStream inputStream) {
        if (!properties.isConfigured()) {
            throw new BusinessException("FTP is not configured");
        }

        FTPClient ftpClient = new FTPClient();
        try (inputStream) {
            connect(ftpClient);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            String remoteDirectory = normalizeRemoteDirectory(serviceDir);
            createDirectory(ftpClient, remoteDirectory);
            if (!ftpClient.changeWorkingDirectory(encodePath(remoteDirectory))) {
                throw new IOException("Cannot enter FTP directory: " + remoteDirectory);
            }

            String encodedFileName = encodePath(fileName);
            if (!ftpClient.storeFile(encodedFileName, inputStream)) {
                throw new IOException("FTP store failed with reply: " + ftpClient.getReplyString());
            }
            return fileName;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("FTP upload failed: {}", e.getMessage(), e);
            throw new BusinessException("FTP upload failed");
        } finally {
            close(ftpClient);
        }
    }

    public String buildPublicUrl(String serviceDir, String fileName) {
        String normalizedDir = normalizeServiceDir(serviceDir);
        return properties.normalizedSiteUrl() + normalizedDir + "/" + fileName;
    }

    private void connect(FTPClient ftpClient) throws IOException {
        ftpClient.setConnectTimeout(TIMEOUT_MILLIS);
        ftpClient.connect(properties.getHost(), properties.getPort());
        ftpClient.setRemoteVerificationEnabled(false);
        if (!ftpClient.login(properties.getUsername(), properties.getPassword())) {
            throw new IOException("FTP login failed");
        }
        if (FTPReply.isPositiveCompletion(ftpClient.sendCommand("OPTS UTF8", "ON"))) {
            ftpClient.setControlEncoding(StandardCharsets.UTF_8.name());
        } else {
            ftpClient.setControlEncoding(StandardCharsets.ISO_8859_1.name());
        }
        if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
            throw new IOException("FTP connection rejected: " + ftpClient.getReplyString());
        }
    }

    private void createDirectory(FTPClient ftpClient, String remoteDirectory) throws IOException {
        String[] parts = remoteDirectory.split("/");
        StringBuilder current = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            current.append('/').append(part);
            String path = current.toString();
            String encodedPath = encodePath(path);
            if (ftpClient.changeWorkingDirectory(encodedPath)) {
                continue;
            }
            if (!ftpClient.makeDirectory(encodedPath) && !ftpClient.changeWorkingDirectory(encodedPath)) {
                throw new IOException("Cannot create FTP directory: " + path);
            }
        }
    }

    private String normalizeRemoteDirectory(String serviceDir) {
        return properties.normalizedBasePath() + normalizeServiceDir(serviceDir);
    }

    private String normalizeServiceDir(String serviceDir) {
        String normalized = serviceDir == null ? "" : serviceDir.replace("\\", "/").trim();
        if (normalized.isBlank()) {
            return "";
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String encodePath(String path) {
        return new String(path.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
    }

    private void close(FTPClient ftpClient) {
        if (!ftpClient.isConnected()) {
            return;
        }
        try {
            ftpClient.logout();
        } catch (IOException e) {
            log.debug("FTP logout failed: {}", e.getMessage());
        }
        try {
            ftpClient.disconnect();
        } catch (IOException e) {
            log.debug("FTP disconnect failed: {}", e.getMessage());
        }
    }
}
