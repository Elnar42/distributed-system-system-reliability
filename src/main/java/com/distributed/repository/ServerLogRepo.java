package com.distributed.repository;

import com.distributed.model.ServerLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServerLogRepo extends JpaRepository<ServerLog, Long> {


    @Query(value = "SELECT sl FROM ServerLog sl WHERE sl.isError =?1")
    List<ServerLog> getServerLogByError(boolean error);

}