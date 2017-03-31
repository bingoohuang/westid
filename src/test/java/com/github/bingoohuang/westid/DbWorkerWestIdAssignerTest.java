package com.github.bingoohuang.westid;

import com.github.bingoohuang.westid.workerid.DbWorkerIdAssigner;
import com.github.bingoohuang.westid.workerid.db.DefaultWorkerIdAssignDao;
import lombok.val;
import org.junit.Test;
import org.n3r.eql.eqler.EqlerFactory;

import static com.google.common.truth.Truth.assertThat;

/**
 * Created by bingoohuang on 2017/3/26.
 */
public class DbWorkerWestIdAssignerTest {
    @Test
    public void test1() {
        val dao = EqlerFactory.getEqler(DefaultWorkerIdAssignDao.class);
        dao.createTable();

        val assigner = new DbWorkerIdAssigner(dao);
        assertThat(assigner.assignWorkerId(WestId.DEFAULT_ID_CONFIG)).isAtLeast(0);
    }
}
