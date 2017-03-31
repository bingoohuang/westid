package com.github.bingoohuang.westid.workerid;

import com.github.bingoohuang.westid.Os;
import com.github.bingoohuang.westid.WestIdConfig;
import com.github.bingoohuang.westid.WorkerIdAssigner;
import com.github.bingoohuang.westid.workerid.db.DefaultWorkerIdAssignDao;
import com.github.bingoohuang.westid.workerid.db.WorkerIdAssignDao;
import com.github.bingoohuang.westid.workerid.db.WorkerIdBean;
import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import lombok.val;
import org.n3r.eql.eqler.EqlerFactory;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public class DbWorkerIdAssigner implements WorkerIdAssigner {
    private final WorkerIdAssignDao dao;

    public DbWorkerIdAssigner() {
        this(EqlerFactory.getEqler(DefaultWorkerIdAssignDao.class));
    }

    public DbWorkerIdAssigner(WorkerIdAssignDao dao) {
        this.dao = dao;
    }

    private Integer createNewWorkerId(WorkerIdAssignDao dao, WestIdConfig westIdConfig,
                                      List<Integer> usableWorkerIds, int size) {
        for (val usableWorkerId : usableWorkerIds) {
            if (addWorkerId(dao, usableWorkerId)) {
                return usableWorkerId;
            }
        }

        for (int id = size; id <= westIdConfig.getMaxWorkerId(); ++id) {
            if (addWorkerId(dao, id)) {
                return id;
            }
        }

        return null;
    }

    @SneakyThrows
    private boolean addWorkerId(WorkerIdAssignDao dao, Integer usableWorkerId) {
        try {
            dao.addWorkerId(new WorkerIdBean(usableWorkerId, Os.PID_INT,
                    Os.IP_STRING, Os.HOSTNAME, null));
            return true;
        } catch (Exception ex) {
            if (isConstraintViolation(ex)) {
                return false;
            }
            throw ex;
        }
    }

    /*
     * Determine if SQLException#getSQLState() of the catched SQLException
     * starts with 23 which is a constraint violation as per the SQL specification.
     * It can namely be caused by more factors than "just" a constraint violation.
     * You should not amend every SQLException as a constraint violation.
     * ORACLE:
     * [2017-03-26 15:13:07] [23000][1] ORA-00001: 违反唯一约束条件 (SYSTEM.SYS_C007109)
     * MySQL:
     * [2017-03-26 15:17:27] [23000][1062] Duplicate entry '1' for key 'PRIMARY'
     * H2:
     * [2017-03-26 15:19:52] [23505][23505] Unique index or primary key violation:
     * "PRIMARY KEY ON PUBLIC.TT(A)"; SQL statement:
     *
     */
    public static boolean isConstraintViolation(Exception e) {
        return e instanceof SQLException
                && ((SQLException) e).getSQLState().startsWith("23");
    }

    private Integer reuseWorkerId(WorkerIdAssignDao dao) {
        WorkerIdBean workerIdBean = null;
        for (val bean : dao.queryWorkerIds(Os.IP_STRING)) {
            val process = Os.OPERATING_SYSTEM.getProcess(bean.getPid());
            if (process != null) {
                continue;
            }

            if (workerIdBean != null) dao.deleteWorkerId(workerIdBean);
            workerIdBean = bean;
        }

        if (workerIdBean != null) {
            int updatedRows = dao.updateWorkerId(workerIdBean.getWorkerId(),
                    Os.IP_STRING, workerIdBean.getPid(), Os.PID_INT,
                    new Timestamp(System.currentTimeMillis()));
            if (updatedRows == 1) {
                return workerIdBean.getWorkerId();
            }
        }

        return null;
    }

    public static List<Integer> findJumpedNumbers(List<Integer> oldNumbers) {
        List<Integer> jumpedNumbers = Lists.newArrayList();
        int number = -1;
        for (Integer oldNumber : oldNumbers) {
            for (; ++number < oldNumber; ) {
                jumpedNumbers.add(number);
            }
        }
        return jumpedNumbers;
    }

    @Override
    public int assignWorkerId(WestIdConfig westIdConfig) {
        val workerId = reuseWorkerId(dao);
        if (workerId != null) {
            return workerId;
        }

        val workerIds = dao.queryAllWorkerIds();
        int oldWorkerIdsSize = workerIds.size();

        val usableWorkerIds = findJumpedNumbers(workerIds);
        val id = createNewWorkerId(dao, westIdConfig, usableWorkerIds, oldWorkerIdsSize);
        if (id != null) {
            return id;
        }

        throw new RuntimeException("workerId used up");
    }

}
