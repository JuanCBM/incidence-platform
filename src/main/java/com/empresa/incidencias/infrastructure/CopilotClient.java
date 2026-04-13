package com.empresa.incidencias.infrastructure;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Component
public class CopilotClient {

    public String ejecutarPrompt(String prompt) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "gh", "copilot", "suggest", "-t", "generic", prompt
        );
        pb.redirectErrorStream(true);
        Process proceso = pb.start();

        boolean terminado = proceso.waitFor(30, TimeUnit.SECONDS);
        if (!terminado) {
            proceso.destroy();
            throw new IllegalStateException("Copilot CLI timeout");
        }

        try (InputStream is = proceso.getInputStream()) {
            return new String(is.readAllBytes());
        }
    }
}
