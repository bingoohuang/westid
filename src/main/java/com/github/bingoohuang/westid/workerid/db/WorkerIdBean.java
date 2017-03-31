package com.github.bingoohuang.westid.workerid.db;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * Created by bingoohuang on 2017/3/26.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkerIdBean {
    private int workerId;
    private int pid;
    private String ip;
    private String hostname;
    private Timestamp createTime;
}
