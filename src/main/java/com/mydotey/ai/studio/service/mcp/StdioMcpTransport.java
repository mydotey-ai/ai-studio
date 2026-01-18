package com.mydotey.ai.studio.service.mcp;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class StdioMcpTransport implements McpTransport {

    private final ProcessBuilder processBuilder;
    private Process process;
    private PrintWriter writer;
    private BufferedReader reader;
    private ExecutorService errorStreamExecutor;

    public StdioMcpTransport(String command, String workingDir) {
        this.processBuilder = new ProcessBuilder();
        if (workingDir != null && !workingDir.isEmpty()) {
            processBuilder.directory(new java.io.File(workingDir));
        }
        processBuilder.command(command.split("\\s+"));
    }

    @Override
    public String sendRequest(String jsonRequest) throws Exception {
        if (process == null) {
            startProcess();
        }

        log.debug("Sending MCP request: {}", jsonRequest);
        writer.println(jsonRequest);
        writer.flush();

        String response = reader.readLine();
        if (response == null) {
            throw new Exception("MCP process terminated unexpectedly");
        }
        log.debug("Received MCP response: {}", response);

        return response;
    }

    private void startProcess() throws Exception {
        log.info("Starting MCP process: {}", processBuilder.command());
        process = processBuilder.start();

        writer = new PrintWriter(process.getOutputStream());
        reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        errorStreamExecutor = Executors.newSingleThreadExecutor();
        errorStreamExecutor.submit(() -> {
            try {
                BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()));
                String line;
                while ((line = errorReader.readLine()) != null) {
                    log.error("MCP process error: {}", line);
                }
            } catch (Exception e) {
                log.error("Error reading MCP process error stream", e);
            }
        });
    }

    @Override
    public void close() {
        if (writer != null) {
            writer.close();
            writer = null;
        }
        if (reader != null) {
            try {
                reader.close();
            } catch (Exception e) {
                log.error("Error closing reader", e);
            }
            reader = null;
        }
        if (errorStreamExecutor != null) {
            errorStreamExecutor.shutdownNow();
            errorStreamExecutor = null;
        }
        if (process != null) {
            process.destroy();
            process = null;
        }
    }
}
