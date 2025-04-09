/*
 * IGinX - the polystore system with high performance
 * Copyright (C) Tsinghua University
 * TSIGinX@gmail.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package cn.edu.tsinghua.iginx.integration.expansion.neo4j;

import static cn.edu.tsinghua.iginx.integration.expansion.neo4j.Neo4jHistoryDataGenerator.LOCAL_IP;

import cn.edu.tsinghua.iginx.exception.SessionException;
import cn.edu.tsinghua.iginx.integration.controller.Controller;
import cn.edu.tsinghua.iginx.integration.expansion.BaseCapacityExpansionIT;
import cn.edu.tsinghua.iginx.integration.expansion.constant.Constant;
import cn.edu.tsinghua.iginx.integration.expansion.utils.SQLTestTools;
import cn.edu.tsinghua.iginx.integration.tool.ConfLoader;
import cn.edu.tsinghua.iginx.integration.tool.DBConf;
import cn.edu.tsinghua.iginx.thrift.RemovedStorageEngineInfo;
import cn.edu.tsinghua.iginx.thrift.StorageEngineType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Neo4jCapacityExpansionIT extends BaseCapacityExpansionIT {

  private static final Logger LOGGER = LoggerFactory.getLogger(Neo4jCapacityExpansionIT.class);

  public Neo4jCapacityExpansionIT() {
    super(
        StorageEngineType.neo4j,
        "username=neo4j, password=neo4jtest",
        new Neo4jHistoryDataGenerator());
    ConfLoader conf = new ConfLoader(Controller.CONFIG_FILE);
    DBConf dbConf = conf.loadDBConf(conf.getStorageType());
    Constant.oriPort = dbConf.getDBCEPortMap().get(Constant.ORI_PORT_NAME);
    Constant.expPort = dbConf.getDBCEPortMap().get(Constant.EXP_PORT_NAME);
    Constant.readOnlyPort = dbConf.getDBCEPortMap().get(Constant.READ_ONLY_PORT_NAME);
  }

  @Override
  protected void updateParams(int port) {}

  @Override
  protected void restoreParams(int port) {}

  @Override
  protected void shutdownDatabase(int port) {
    shutOrRestart(port, true, "neo4j");
  }

  @Override
  protected void startDatabase(int port) {
    shutOrRestart(port, false, "neo4j");
  }

  @Override
  protected void testQuerySpecialHistoryData() {
    testFloatData();
    testFilter();
    try {
      testShowDummyColumns();
    } catch (SessionException e) {
      e.printStackTrace();
    }
  }

  /** 测试float类型数据 */
  private void testFloatData() {
    String statement = "select wt02.float from tm.wf05 where wt02.float <= 44.55;";
    List<String> pathList = Constant.READ_ONLY_FLOAT_PATH_LIST;
    List<List<Object>> valuesList = Constant.READ_ONLY_FLOAT_VALUES_LIST;
    SQLTestTools.executeAndCompare(session, statement, pathList, valuesList);
  }

  private void testFilter() {
    String statement = "select i from d1.c1 where i < 3;";
    String expect =
        "ResultSets:\n"
            + "+---+-------+\n"
            + "|key|d1.c1.i|\n"
            + "+---+-------+\n"
            + "|  1|      0|\n"
            + "|  2|      1|\n"
            + "|  3|      2|\n"
            + "+---+-------+\n"
            + "Total line number = 3\n";
    SQLTestTools.executeAndCompare(session, statement, expect);

    statement = "select i from d1.c1 where key > 0 and key <= 2;";
    expect =
        "ResultSets:\n"
            + "+---+-------+\n"
            + "|key|d1.c1.i|\n"
            + "+---+-------+\n"
            + "|  1|      0|\n"
            + "|  2|      1|\n"
            + "+---+-------+\n"
            + "Total line number = 2\n";
    SQLTestTools.executeAndCompare(session, statement, expect);

    statement = "select b from d1.c1 where b = true;";
    expect =
        "ResultSets:\n"
            + "+---+-------+\n"
            + "|key|d1.c1.b|\n"
            + "+---+-------+\n"
            + "|  1|   true|\n"
            + "|  3|   true|\n"
            + "|  5|   true|\n"
            + "+---+-------+\n"
            + "Total line number = 3\n";
    SQLTestTools.executeAndCompare(session, statement, expect);

    statement = "select f from d1.c1 where f > 2;";
    expect =
        "ResultSets:\n"
            + "+---+-------+\n"
            + "|key|d1.c1.f|\n"
            + "+---+-------+\n"
            + "|  3|    2.1|\n"
            + "|  4|    3.1|\n"
            + "|  5|    4.1|\n"
            + "+---+-------+\n"
            + "Total line number = 3\n";
    SQLTestTools.executeAndCompare(session, statement, expect);

    statement = "select f from d1.c1 where f > 1.0 and f < 2.9;";
    expect =
        "ResultSets:\n"
            + "+---+-------+\n"
            + "|key|d1.c1.f|\n"
            + "+---+-------+\n"
            + "|  2|    1.1|\n"
            + "|  3|    2.1|\n"
            + "+---+-------+\n"
            + "Total line number = 2\n";
    SQLTestTools.executeAndCompare(session, statement, expect);

    statement = "select s from d1.c1 where s = '1st';";
    expect =
        "ResultSets:\n"
            + "+---+-------+\n"
            + "|key|d1.c1.s|\n"
            + "+---+-------+\n"
            + "|  1|    1st|\n"
            + "+---+-------+\n"
            + "Total line number = 1\n";
    SQLTestTools.executeAndCompare(session, statement, expect);

    statement = "select s from d1.c1 where s LIKE '.*th';";
    expect =
        "ResultSets:\n"
            + "+---+-------+\n"
            + "|key|d1.c1.s|\n"
            + "+---+-------+\n"
            + "|  3|    3th|\n"
            + "|  4|    4th|\n"
            + "|  5|    5th|\n"
            + "+---+-------+\n"
            + "Total line number = 3\n";
    SQLTestTools.executeAndCompare(session, statement, expect);
  }

  private void testShowDummyColumns() throws SessionException {
    String schemaPrefix = "dummy";
    int readOnlyPort = 19532;
    Map<String, String> extraParams = new HashMap<>();
    extraParams.put("has_data", "true");
    extraParams.put("is_read_only", "true");
    extraParams.put("schema_prefix", schemaPrefix);
    try {
      session.addStorageEngine(LOCAL_IP, readOnlyPort, StorageEngineType.vectordb, extraParams);
    } catch (SessionException e) {
    }
    String statement = "SHOW COLUMNS dummy.*;";
    String expected =
        "Columns:\n"
            + "+------------------------------+--------+\n"
            + "|                          Path|DataType|\n"
            + "+------------------------------+--------+\n"
            + "|                 dummy.d1.c1.b| BOOLEAN|\n"
            + "|                 dummy.d1.c1.f|   FLOAT|\n"
            + "|                 dummy.d1.c1.i|    LONG|\n"
            + "|                 dummy.d1.c1.s|  BINARY|\n"
            + "|            dummy.d1.c1.vector|  BINARY|\n"
            + "|                 dummy.d2.c1.b| BOOLEAN|\n"
            + "|                 dummy.d2.c1.f|  DOUBLE|\n"
            + "|                 dummy.d2.c1.i|    LONG|\n"
            + "|                 dummy.d2.c1.s|  BINARY|\n"
            + "|            dummy.d2.c1.vector|  BINARY|\n"
            + "|     dummy.tm.wf05.wt01.status|    LONG|\n"
            + "|dummy.tm.wf05.wt01.temperature|  DOUBLE|\n"
            + "|      dummy.tm.wf05.wt02.float|   FLOAT|\n"
            + "|     dummy.tm.wf05.wt02.vector|  BINARY|\n"
            + "+------------------------------+--------+\n"
            + "Total line number = 14\n";
    SQLTestTools.executeAndCompare(session, statement, expected);
    session.removeStorageEngine(
        Arrays.asList(new RemovedStorageEngineInfo(LOCAL_IP, readOnlyPort, schemaPrefix, "")));
  }
}
