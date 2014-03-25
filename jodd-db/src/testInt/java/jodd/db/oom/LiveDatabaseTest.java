// Copyright (c) 2003-2014, Jodd Team (jodd.org). All Rights Reserved.

package jodd.db.oom;

import jodd.db.DbSession;
import jodd.db.oom.sqlgen.DbEntitySql;
import jodd.db.oom.tst.Tester;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Live database test. Requires database services to be started.
 * There must exist the database: "jodd-test".
 */
public class LiveDatabaseTest extends DbBaseTest {

	/**
	 * DATABASES TO TEST!
	 */
	DbAccess[] databases = new DbAccess[]{
			new MySql(),
			new PostgreSql(),
			new HsqlDb(),
	};

	/**
	 * MySql.
	 */
	public class MySql extends MySqlDbAccess {

		@Override
		public String getCreateTableSql() {
			return "create table TESTER (" +
						"ID			INT UNSIGNED NOT NULL AUTO_INCREMENT," +
						"NAME		VARCHAR(20)	not null," +
						"VALUE		INT NULL," +
						"primary key (ID)" +
						')';
		}

		@Override
		public String getTableName() {
			return "TESTER";
		}
	}

	/**
	 * PostgreSql.
	 */
	public class PostgreSql extends PostgreSqlDbAccess {

		@Override
		public String getCreateTableSql() {
			return "create table TESTER (" +
						"ID			SERIAL," +
						"NAME		varchar(20)	NOT NULL," +
						"VALUE		integer NULL," +
						"primary key (ID)" +
						')';
		}

		@Override
		public String getTableName() {
			return "TESTER";
		}
	}

	/**
	 * HsqlDB.
	 */
	public class HsqlDb extends HsqlDbAccess {

		@Override
		public String getCreateTableSql() {
			return "create table TESTER (" +
						"ID			integer GENERATED BY DEFAULT AS IDENTITY(START WITH 1) PRIMARY KEY," +
						"NAME		varchar(20)	NOT NULL," +
						"VALUE		integer NULL" +
						')';
		}

		@Override
		public String getTableName() {
			return "TESTER";
		}
	}

	// ---------------------------------------------------------------- test

	@Test
	public void testLiveDb() throws Exception {
		for (DbAccess db : databases) {
			System.out.println("\t" + db.getClass().getSimpleName());
			init();
			db.initDb();
			connect();

			dboom.registerEntity(Tester.class);

			db.createTables();

			try {
				workoutEntity();
			} finally {
				db.close();
			}
		}
	}

	// ---------------------------------------------------------------- workout

	protected void workoutEntity() {
		DbSession session = new DbSession();

		Tester tester = new Tester();
		tester.setName("one");
		tester.setValue(Integer.valueOf(7));

		DbOomQuery dbOomQuery = DbOomQuery.query(session, DbEntitySql.insert(tester));
		dbOomQuery.setGeneratedKey();
		dbOomQuery.executeUpdate();
		assertDb(session, "{1,one,7}");

		long key = dbOomQuery.getGeneratedKey();
		tester.setId(Long.valueOf(key));
		dbOomQuery.close();

		assertEquals(1, tester.getId().longValue());

		tester.setName("seven");
		DbOomQuery.query(session, DbEntitySql.updateAll(tester)).executeUpdate();
		assertDb(session, "{1,seven,7}");

		tester.setName("SEVEN");
		DbOomQuery.query(session, DbEntitySql.update(tester)).executeUpdate();
		assertDb(session, "{1,SEVEN,7}");

		tester.setName("seven");
		DbOomQuery.query(session, DbEntitySql.updateColumn(tester, "name")).executeUpdate();
		assertDb(session, "{1,seven,7}");

		tester = new Tester();
		tester.setId(Long.valueOf(2));
		tester.setName("two");
		tester.setValue(Integer.valueOf(2));
		DbOomQuery.query(session, DbEntitySql.insert(tester)).executeUpdate();
		assertDb(session, "{1,seven,7}{2,two,2}");

		long count = DbOomQuery.query(session, DbEntitySql.count(Tester.class)).executeCount();
		assertEquals(2, count);

		tester = DbOomQuery.query(session, DbEntitySql.findById(Tester.class, Integer.valueOf(2))).find(Tester.class);
		assertNotNull(tester);
		assertEquals("{2,two,2}", tester.toString());

		tester = DbOomQuery
				.query(session, DbEntitySql
						.findById(Tester.class, Integer.valueOf(2))
						.aliasColumnsAs(ColumnAliasType.COLUMN_CODE))
				.find(Tester.class);
		assertNotNull(tester);
		assertEquals("{2,two,2}", tester.toString());

		tester = DbOomQuery
				.query(session, DbEntitySql
						.findById(Tester.class, Integer.valueOf(2))
						.aliasColumnsAs(ColumnAliasType.TABLE_REFERENCE))
				.find(Tester.class);
		assertNotNull(tester);
		assertEquals("{2,two,2}", tester.toString());

		tester = DbOomQuery
				.query(session, DbEntitySql
						.findById(Tester.class, Integer.valueOf(2))
						.aliasColumnsAs(ColumnAliasType.TABLE_NAME))
				.find(Tester.class);
		assertNotNull(tester);
		assertEquals("{2,two,2}", tester.toString());

		tester = DbOomQuery
				.query(session, DbEntitySql
						.findById(Tester.class, Integer.valueOf(2))
						.aliasColumnsAs(ColumnAliasType.COLUMN_CODE))    // fixes POSTGRESQL
				.find();
		assertEquals("{2,two,2}", tester.toString());

		tester = new Tester();
		tester.setName("seven");
		tester = DbOomQuery.query(session, DbEntitySql.find(tester)).find(Tester.class);
		assertEquals("{1,seven,7}", tester.toString());

		DbOomQuery.query(session, DbEntitySql.findByColumn(Tester.class, "name", "seven")).find(Tester.class);
		assertEquals("{1,seven,7}", tester.toString());

		DbOomQuery.query(session, DbEntitySql.deleteById(Tester.class, Integer.valueOf(1))).executeUpdate();

		count = DbOomQuery.query(session, DbEntitySql.count(Tester.class)).executeCount();
		assertEquals(1, count);

		session.closeSession();
	}

	// ---------------------------------------------------------------- util

	protected void assertDb(DbSession dbSession, String expected) {
		DbOomQuery query = new DbOomQuery(dbSession, "select * from TESTER order by ID");
		List<Tester> testerList = query.list(Tester.class);

		StringBuilder sb = new StringBuilder();
		for (Tester tester : testerList) {
			sb.append(tester.toString());
		}

		assertEquals(expected, sb.toString());
	}

}
