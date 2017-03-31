package com.github.bingoohuang.westid.workerid.db;

import org.n3r.eql.eqler.annotations.Sql;

import java.sql.Timestamp;
import java.util.List;

/**
 * create table workerid_assign( worker_id int primary key, pid int not null, ip varchar(15) not null, hostname varchar(60), create_time timestamp);
 */
public interface WorkerIdAssignDao {
    @Sql("select worker_id, pid, ip, hostname, create_time  " +
            "from workerid_assign " +
            "where ip = ##")
    List<WorkerIdBean> queryWorkerIds(String ip);

    @Sql("update workerid_assign " +
            "set pid = #4#, create_time = #5# " +
            "where worker_id = #1# " +
            "and ip = #2# " +
            "and pid = #3#")
    int updateWorkerId(int workerId, String ip, int oldPid, int newPid, Timestamp timestamp);

    @Sql("select worker_id from workerid_assign")
    List<Integer> queryAllWorkerIds();

    @Sql("insert into workerid_assign(worker_id, pid, ip, hostname, create_time) " +
            "values(#workerId#, #pid#, #ip#, #hostname#, #_time#)")
    void addWorkerId(WorkerIdBean bean);

    @Sql("delete from workerid_assign " +
            "where worker_id = #workerId# " +
            "and pid = #pid# " +
            "and ip = #ip# ")
    void deleteWorkerId(WorkerIdBean bean);
}
