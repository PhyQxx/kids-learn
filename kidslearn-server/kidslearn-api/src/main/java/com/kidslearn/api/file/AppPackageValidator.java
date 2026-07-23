package com.kidslearn.api.file;

import com.kidslearn.common.exception.BusinessException;
import java.util.Locale;
import java.util.Set;
import org.springframework.web.multipart.MultipartFile;

/**
 * App 升级包（wgt / apk / ipa）校验器
 */
final class AppPackageValidator {

    static final long MAX_SIZE_BYTES = 200L * 1024 * 1024; // 200MB
    static final Set<String> ALLOWED_EXTENSIONS = Set.of("wgt", "apk", "ipa");

    private AppPackageValidator() {
    }

    static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("升级包文件不能为空");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BusinessException("升级包文件不能超过 200MB");
        }
        String extension = extension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("仅支持 .wgt / .apk / .ipa 文件");
        }
    }

    static String extension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException("文件名不能为空");
        }
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            throw new BusinessException("文件缺少扩展名");
        }
        return originalFilename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
