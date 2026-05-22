package com.zz.usercenter;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.HttpURLConnection;
import java.net.URL;

@SpringBootApplication
@MapperScan("com.zz.usercenter.mapper")
@EnableScheduling
@EnableAsync
@Slf4j
public class UserCenterApplication {

    @Value("${chroma.auto-start:false}")
    private boolean chromaAutoStart;

    @Value("${chroma.exec-path:}")
    private String chromaExecPath;

    @Value("${spring.ai.vectorstore.chroma.client.port:8000}")
    private int chromaPort;

    public static void main(String[] args) {
        SpringApplication.run(UserCenterApplication.class, args);
    }

    @PostConstruct
    public void startChromaIfNeeded() {
        if (!chromaAutoStart) return;

        // 已经在跑就不重复启动
        if (isChromaRunning()) {
            log.info("Chroma 已在端口 {} 运行，跳过启动", chromaPort);
            return;
        }

        String execPath = chromaExecPath;
        if (execPath == null || execPath.isBlank()) {
            // 自动查找 chroma 可执行文件
            String pythonDir = System.getenv().getOrDefault("PYTHON_PATH",
                    "C:\\Users\\zhouzhou\\AppData\\Local\\Programs\\Python\\Python313");
            execPath = pythonDir + "\\Scripts\\chroma.exe";
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    execPath, "run", "--host", "127.0.0.1", "--port", String.valueOf(chromaPort)
            );
            pb.redirectErrorStream(true);
            pb.directory(null);
            Process process = pb.start();
            // 用守护线程消费进程输出，避免 transferTo 阻塞主线程导致 Spring 启动卡死
            Thread outputConsumer = new Thread(() -> {
                try { process.inputReader().transferTo(java.io.Writer.nullWriter()); } catch (Exception ignored) {}
            });
            outputConsumer.setDaemon(true);
            outputConsumer.start();

            // 等 Chroma 就绪（最多 15 秒）
            for (int i = 0; i < 30; i++) {
                Thread.sleep(500);
                if (isChromaRunning()) {
                    log.info("Chroma 自动启动成功，端口 {}", chromaPort);
                    return;
                }
            }
            log.warn("Chroma 启动超时，RAG 功能可能不可用");
        } catch (Exception e) {
            log.warn("Chroma 自动启动失败: {}", e.getMessage());
        }
    }

    private boolean isChromaRunning() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(
                    "http://127.0.0.1:" + chromaPort + "/api/v2/heartbeat").openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(1000);
            conn.setReadTimeout(1000);
            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
