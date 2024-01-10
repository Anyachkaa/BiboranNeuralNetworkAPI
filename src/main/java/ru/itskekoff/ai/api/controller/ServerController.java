package ru.itskekoff.ai.api.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.itskekoff.ai.api.types.CaptchaData;
import ru.itskekoff.ai.api.types.CaptchaResponse;
import ru.itskekoff.ai.api.types.CaptchaType;
import ru.itskekoff.ai.api.utils.OperationUtils;
import ru.itskekoff.ai.worker.AIWorker;

import java.util.*;
import java.util.concurrent.*;

@RestController
public class ServerController {
    private static final Logger logger = LoggerFactory.getLogger(ServerController.class);
    private final Map<String, CaptchaData> imageDataMap = new HashMap<>();
    private final AIWorker aiWorker;

    @Autowired
    public ServerController(AIWorker aiWorker) throws Exception {
        this.aiWorker = aiWorker;
        this.aiWorker.initAI();

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::solveCaptcha, 3, 3, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::cleanupImageData, 0, 10, TimeUnit.MINUTES);
    }

    @PostMapping("/in.php")
    public ResponseEntity<?> handleUpload(@RequestParam(value = "method") String method,
                                          @RequestParam(value = "file", required = false) MultipartFile file,
                                          @RequestParam(value = "json", required = false, defaultValue = "0") int json,
                                          @RequestParam(value = "body", required = false) String body) {
        CaptchaResponse response = new CaptchaResponse();
        String captchaID;
        byte[] data;
        if (method.equals("post")) {
            data = OperationUtils.getBytesFromFile(file);
        } else if (method.equals("base64")) {
            data = Base64.getDecoder().decode(body);
        } else {
            return ResponseEntity.badRequest().body("INVALID_REQUEST");
        }
        captchaID = OperationUtils.getIdFromBytes(data);

        if (data == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("FILE_UPLOAD_FAILED");
        }

        logger.info("Received captcha {} with data length {} bytes", captchaID, data.length);

        CaptchaData captchaData = new CaptchaData(data, CaptchaType.PROCESSING);
        imageDataMap.put(captchaID, captchaData);
        response.setStatus(1);
        response.setRequest(captchaID);
        if (json == 1) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.ok("OK|" + captchaID);
    }

    @GetMapping("/res.php")
    public ResponseEntity<?> provideAnswer(@RequestParam(value = "id") String captchaIDParam,
                                           @RequestParam(value = "json", required = false, defaultValue = "0") int json) {
        CaptchaResponse response = new CaptchaResponse();
        if (captchaIDParam == null) {
            return ResponseEntity.badRequest().body("INVALID_REQUEST");
        }

        CaptchaData captchaData = imageDataMap.get(captchaIDParam);
        if (captchaData == null) {
            return ResponseEntity.notFound().build();
        }

        if (captchaData.getCaptchaType() == CaptchaType.SOLVED) {
            String answer = captchaData.getAnswer();
            if (json == 1) {
                response.setStatus(1);
                response.setRequest(answer);
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.ok("OK|" + answer);
        } else {
            if (json == 1) {
                response.setStatus(0);
                response.setRequest("CAPCHA_NOT_READY");
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.ok("CAPCHA_NOT_READY");
        }
    }

    private void solveCaptcha() {
        List<CompletableFuture<Void>> futures = imageDataMap.entrySet().stream()
                .filter(entry -> entry.getValue().getCaptchaType() == CaptchaType.PROCESSING)
                .map(entry -> {
                    String captchaID = entry.getKey();
                    CaptchaData captchaData = entry.getValue();
                    byte[] imageData = captchaData.getImageData();

                    return CompletableFuture.runAsync(() -> {
                        String answer = aiWorker.getAnswer(imageData);
                        captchaData.setAnswer(answer);
                        captchaData.setCaptchaType(CaptchaType.SOLVED);
                        logger.info("Solved captcha with id {}, answer {}", captchaID, answer);
                    });
                }).toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private void cleanupImageData() {
        imageDataMap.entrySet().removeIf(entry -> entry.getValue().getCaptchaType() == CaptchaType.SOLVED);
    }
}
