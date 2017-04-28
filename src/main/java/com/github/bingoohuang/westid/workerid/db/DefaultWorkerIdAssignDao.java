package com.github.bingoohuang.westid.workerid.db;

import org.n3r.eql.eqler.annotations.EqlerConfig;
import org.n3r.eql.eqler.annotations.Sql;

@EqlerConfig("workerId")
public interface DefaultWorkerIdAssignDao extends WorkerIdAssignDao {
    @Sql({"drop table if exists workerid_assign",
            "create table workerid_assign(" +
                    "worker_id int primary key, " +
                    "pid int not null, " +
                    "ip varchar(15) not null, " +
                    "hostname varchar(60) not null, " +
                    "create_time timestamp not null)"})
    void createTable();
}
