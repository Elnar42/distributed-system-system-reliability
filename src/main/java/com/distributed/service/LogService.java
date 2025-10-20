package com.distributed.service;

import com.distributed.model.ServerLog;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface LogService {

    void fetchAndSaveLogs(Integer count);

    List<ServerLog> getErrorLogs();

    List<ServerLog> getSuccessfulLogs();


    Map<String, Object> getErrorDistribution();
}
