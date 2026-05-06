package com.kidslearn.api.file;

import com.kidslearn.common.ftp.FtpTool;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Admin File")
@RestController
@RequestMapping("/api/v1/admin/file")
@RequiredArgsConstructor
public class AdminFileController {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final FtpTool ftpTool;

    @Operation(summary = "Upload question image")
    @PostMapping("/upload-image")
    public R<Map<String, String>> uploadQuestionImage(@RequestParam("file") MultipartFile file) throws IOException {
        ImageUploadValidator.validate(file);

        String serviceDir = "/question/images/" + LocalDate.now().format(DAY_FORMAT);
        String fileName = UUID.randomUUID().toString().replace("-", "")
            + "." + ImageUploadValidator.extension(file.getOriginalFilename());

        String storedFileName = ftpTool.upload(serviceDir, fileName, file.getInputStream());
        return R.ok(Map.of(
            "url", ftpTool.buildPublicUrl(serviceDir, storedFileName),
            "fileName", storedFileName
        ));
    }
}
