import java.util.Stack;

public interface OperationsIntf {
	void select(String tableName, String[] colNames, Stack<String[]> select_condition);
	void dropTable(String tableName);
	void update(String tableName, Stack<String[]> set_condition, Stack<String[]> where_condition);
	void createTable(String tableName, String[] colNames);
	void insertInto(String tableName, String[] insertValues);
	void delete(String tableName, Stack<String[]> delete_condition);
	void createIndex(String tableName, String[] colNames);
}
