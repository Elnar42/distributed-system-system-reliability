package com.distributed.service;

import com.distributed.model.ServerLog;
import com.distributed.repository.ServerLogRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class LogServiceImpl implements LogService {

    private final ServerLogRepo serverLogRepo;
    private static final ExecutorService executor = Executors.newFixedThreadPool(15);
    private static final Logger log = LoggerFactory.getLogger(LogServiceImpl.class);
    private final RestTemplate restTemplate;

    public LogServiceImpl(ServerLogRepo serverLogRepo, RestTemplate restTemplate) {
        this.serverLogRepo = serverLogRepo;
        this.restTemplate = restTemplate;
    }

    private static final String LOG_FETCH_URL = "https://latest-960957615762.me-central1.run.app/getlogs";
    private static final String BALANCE_CHECK_URL = "https://latest-960957615762.me-central1.run.app/getbalance";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    public void fetchAndSaveLogs(Integer count) {
        serverLogRepo.deleteAll();
        sendBalanceCheckApiRequest(count);
        try {

            String logs = restTemplate.getForObject(LOG_FETCH_URL, String.class);

            if (logs == null || logs.isEmpty()) {
                return;
            }

            List<ServerLog> entries = parseLogs(logs);

            serverLogRepo.saveAll(entries);

        } catch (Exception e) {
            log.warn("LOG SERVER IS DOWN (OR SOMETHING IS WRONG) AND THE REQUEST CAN NOT BE PROCEED: {}", LocalDateTime.now());
            new ArrayList<>();
        }
    }

    private List<ServerLog> parseLogs(String content) {
        List<ServerLog> entries = new ArrayList<>();
        String[] lines = content.split("\\r?\\n");

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            try {
                ServerLog entry = parseIndividualLine(line);
                entries.add(entry);
            } catch (Exception e) {
                log.warn("THIS LINE OF THE CODE CAN NOT BE PROCEED UNFORTUNATELY: {}", line);
            }
        }

        return entries;
    }


    private ServerLog parseIndividualLine(String line) {

        if (line.length() < 20) {
            return new ServerLog();
        }

        String dateTimeStr = line.substring(0, 19);
        LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, FORMATTER);
        String message = line.substring(20).trim();
        boolean isError = message.startsWith("ERROR") || message.startsWith("WARNING");

        return ServerLog.builder()
                .dateTime(dateTime)
                .logMessage(message)
                .isError(isError)
                .build();
    }


    private void sendBalanceCheckApiRequest(Integer count) {
        List<CompletableFuture<Void>> futures = IntStream.range(0, count)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    try {
                        restTemplate.getForObject(BALANCE_CHECK_URL, String.class);
                    } catch (Exception e) {
                        log.info("EXCEPTION HAPPENED, BUT IT IS DESIRABLE ACTION");
                    }
                }, executor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    public List<ServerLog> getErrorLogs() {
        return serverLogRepo.getServerLogByError(true);
    }


    public List<ServerLog> getSuccessfulLogs() {
        return serverLogRepo.getServerLogByError(false);
    }


    public Map<String, Object> getErrorDistribution() {
        List<ServerLog> serverLogs = serverLogRepo.getServerLogByError(true);

        Map<String, List<ServerLog>> groupedByMainCategory = serverLogs.stream()
                .collect(Collectors.groupingBy(log -> classifyErrorComingFromDatabase(log.getLogMessage())));

        long totalErrors = serverLogs.size();
        Map<String, Object> result = new LinkedHashMap<>();

        for (Map.Entry<String, List<ServerLog>> entry : groupedByMainCategory.entrySet()) {
            String mainCategory = entry.getKey();
            List<ServerLog> logsInCategory = entry.getValue();
            double mainPercent = logsInCategory.size() * 100.0 / totalErrors;

            Map<String, Double> subDistribution = getSubDistribution(mainCategory, logsInCategory);

            Map<String, Object> categoryData = new LinkedHashMap<>();
            categoryData.put("percentage", mainPercent);
            categoryData.put("subCategories", subDistribution);

            result.put(mainCategory, categoryData);
        }

        return result;
    }


    private String classifyErrorComingFromDatabase(String message) {
        if (message == null || message.isBlank()) return "Unknown";

        String lowerMsg = message.toLowerCase();

        if (lowerMsg.contains("database connection pool empty"))
            return "Database Error";
        else if (lowerMsg.contains("internal server error"))
            return "Server Error";
        else if (lowerMsg.contains("latency"))
            return "Performance Warning";
        else if (lowerMsg.contains("timeout"))
            return "Network Timeout";
        else
            return "Other";
    }

    private Map<String, Double> getSubDistribution(String mainCategory, List<ServerLog> logs) {
        Map<String, Long> subCounts = new HashMap<>();

        for (ServerLog log : logs) {
            String msg = log.getLogMessage().toLowerCase();
            String subCategory;

            switch (mainCategory) {
                case "Server Error":
                    if (msg.contains("nullpointer"))
                        subCategory = "Null Pointer Exception";
                    else if (msg.contains("illegal"))
                        subCategory = "Illegal State";
                    else if (msg.contains("timeout"))
                        subCategory = "Timeout Exception";
                    else if (msg.contains("internal server"))
                        subCategory = "Internal Server";
                    else
                        subCategory = "Other Server Error";
                    break;

                case "Database Error":
                    if (msg.contains("pool empty"))
                        subCategory = "Connection Pool Exhaustion";
                    else if (msg.contains("timeout"))
                        subCategory = "DB Timeout";
                    else if (msg.contains("sqlsyntax"))
                        subCategory = "SQL Syntax Error";
                    else
                        subCategory = "Other Database Error";
                    break;

                case "Performance Warning":
                    subCategory = (msg.contains("latency")) ? "High Latency" : "Other Performance Issue";
                    break;

                default:
                    subCategory = "Other";
            }

            subCounts.put(subCategory, subCounts.getOrDefault(subCategory, 0L) + 1);
        }

        long total = logs.size();
        Map<String, Double> subDistribution = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : subCounts.entrySet()) {
            subDistribution.put(entry.getKey(), (entry.getValue() * 100.0) / total);
        }
        return subDistribution;
    }


}