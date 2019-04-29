
import java.io.RandomAccessFile;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;


public class DavisBase {
	

	static boolean exit = false;
			
	static Scanner scanner = new Scanner(System.in).useDelimiter(";");
	
    public static void main(String[] args) {
    	init();
		
		splashScreen();

		String userCommand = ""; 

		while(!exit) {
			System.out.print(Constants.PROMPT);
			userCommand = scanner.next().replace("\n", " ").replace("\r", "").trim().toLowerCase();
			parseUserCommand(userCommand);
		}
		System.out.println("Exiting...");


	}
	
    public static void splashScreen() {
		System.out.println(line("*",80));
        System.out.println("Welcome to DavisBase");
		System.out.println("DavisBase Version " + Constants.VERSION);
//		System.out.println(copyright);
		System.out.println("\nType \"help;\" to display supported commands.");
		System.out.println(line("*",80));
	}
	

	
	public static String line(String s,int num) {
		String a = "";
		for(int i=0;i<num;i++) {
			a += s;
		}
		return a;
	}
	
	
	public static void help() {
		System.out.println(line("*",80));
		System.out.println("SUPPORTED COMMANDS");
		System.out.println("All commands below are case insensitive");
		System.out.println();
		System.out.println("\tSHOW TABLES;                                               Display all the tables in the database.");
		System.out.println("\tCREATE TABLE table_name (<column_name datatype> <NOT NULL/UNIQUE>);   Create a new table in the database. First record should be primary key of type Int.");
		System.out.println("\tCREATE INDEX ON table_name (<column_name>);       	     Create a new index for the table in the database.");
		System.out.println("\tINSERT INTO table_name VALUES (value1,value2,..);          Insert a new record into the table. First Column is primary key which has inbuilt auto increment function.");
		System.out.println("\tDELETE FROM TABLE table_name WHERE row_id = key_value;     Delete a record from the table whose rowid is <key_value>.");
		System.out.println("\tUPDATE table_name SET column_name = value WHERE condition; Modifies the records in the table.");
		System.out.println("\tSELECT * FROM table_name;                                  Display all records in the table.");
		System.out.println("\tSELECT * FROM table_name WHERE column_name operator value; Display records in the table where the given condition is satisfied.");
		System.out.println("\tDROP TABLE table_name;                                     Remove table data and its schema.");
		System.out.println("\tVERSION;                                                   Show the program version.");
		System.out.println("\tHELP;                                                      Show this help information.");
		System.out.println("\tEXIT;                                                      Exit the program.");
		System.out.println();
		System.out.println();
		System.out.println(line("*",80));
	}


	
	public static boolean tableExists(String tableName){
		tableName = tableName+".tbl";
		
		try {
			
			
			File data_dir = new File(Constants.dirUserdata);
			if (tableName.equalsIgnoreCase(Constants.TABLE_CATALOG+Constants.FILE_TYPE) || tableName.equalsIgnoreCase(Constants.COLUMN_CATALOG+Constants.FILE_TYPE))
				data_dir = new File(Constants.dirCatalog) ;
			
			String[] oldTables = data_dir.list();
			for (int i=0; i<oldTables.length; i++) {
				if(oldTables[i].equals(tableName))
					return true;
			}
		}
		catch (Exception e) {
			System.out.println("Unable to create directory");
			System.out.println(e);
		}

		return false;
	}

	public static void init(){
		try {
			File data_dir = new File("data");
			if(data_dir.mkdir()){
				System.out.println("Initializing...");
				initialize();
			}
			else {
				data_dir = new File(Constants.dirCatalog);
				String[] oldTables = data_dir.list();
				boolean tableExists = false;
				boolean colExists = false;
				for (int i=0; i<oldTables.length; i++) {
					if(oldTables[i].equals(Constants.TABLE_CATALOG+Constants.FILE_TYPE))
						tableExists = true;
					if(oldTables[i].equals(Constants.COLUMN_CATALOG+Constants.FILE_TYPE))
						colExists = true;
				}
				
				if(!tableExists){
					System.out.println("Davisbase_tables does not exist, initializing...");
					System.out.println();
					initialize();
				}
				
				if(!colExists){
					System.out.println("Davisbase_columns table does not exist, initializing...");
					System.out.println();
					initialize();
				}
				
			}
		}
		catch (Exception e) {
			System.out.println(e);
		}

	}
	
public static void initialize() {

		
		try {
			File data_dir = new File(Constants.dirUserdata);
			data_dir.mkdir();
			data_dir = new File(Constants.dirCatalog);
			data_dir.mkdir();
			String[] oldTables;
			oldTables = data_dir.list();
			for (int i=0; i<oldTables.length; i++) {
				File oldFile = new File(data_dir, oldTables[i]); 
				oldFile.delete();
			}
		}
		catch (Exception e) {
			System.out.println(e);
		}

		try {
			RandomAccessFile catalogTable = new RandomAccessFile(Constants.dirCatalog+"/davisbase_tables.tbl", "rw");
			catalogTable.setLength(Constants.PAGE_SIZE);
			catalogTable.seek(0);
			catalogTable.write(0x0D);
			catalogTable.writeByte(0x02);
									
			//creating davisbase_tables
			catalogTable.writeShort(Constants.COLUMN_OFFSET);
			catalogTable.writeInt(0);
			catalogTable.writeInt(0);
			catalogTable.writeShort(Constants.TABLE_OFFSET);
			catalogTable.writeShort(Constants.COLUMN_OFFSET);
			
			catalogTable.seek(Constants.TABLE_OFFSET);
			catalogTable.writeShort(20);
			catalogTable.writeInt(1); 
			catalogTable.writeByte(1);
			catalogTable.writeByte(28);
			catalogTable.writeBytes(Constants.TABLE_CATALOG);
			
			catalogTable.seek(Constants.COLUMN_OFFSET);
			catalogTable.writeShort(21);
			catalogTable.writeInt(2); 
			catalogTable.writeByte(1);
			catalogTable.writeByte(29);
			catalogTable.writeBytes(Constants.COLUMN_CATALOG);
			
			catalogTable.close();
		}
		catch (Exception e) {
			System.out.println(e);
		}
		
		try {
			RandomAccessFile catalogColumn = new RandomAccessFile(Constants.dirCatalog+"/davisbase_columns.tbl", "rw");
			catalogColumn.setLength(Constants.PAGE_SIZE);
			catalogColumn.seek(0);       
			catalogColumn.writeByte(0x0D); 
			catalogColumn.writeByte(0x09); //no of records

			
			int[] offset=new int[9];
			offset[0]=Constants.PAGE_SIZE-45;
			offset[1]=offset[0]-49;
			offset[2]=offset[1]-46;
			offset[3]=offset[2]-50;
			offset[4]=offset[3]-51;
			offset[5]=offset[4]-49;
			offset[6]=offset[5]-59;
			offset[7]=offset[6]-51;
			offset[8]=offset[7]-49;
			
			catalogColumn.writeShort(offset[8]); 
			catalogColumn.writeInt(0); 
			catalogColumn.writeInt(0); 
			
			for(int i=0;i<offset.length;i++)
				catalogColumn.writeShort(offset[i]);

			
			//creating davisbase_columns
			catalogColumn.seek(offset[0]);
			catalogColumn.writeShort(36);
			catalogColumn.writeInt(1); //key
			catalogColumn.writeByte(6); //no of columns
			catalogColumn.writeByte(28); //16+12next file lines indicate the code for datatype/length of the 5 columns
			catalogColumn.writeByte(17); //5+12
			catalogColumn.writeByte(15); //3+12
			catalogColumn.writeByte(4);
			catalogColumn.writeByte(14);
			catalogColumn.writeByte(14);
			catalogColumn.writeBytes(Constants.TABLE_CATALOG); 
			catalogColumn.writeBytes(Constants.HEADER_ROWID); 
			catalogColumn.writeBytes("INT"); 
			catalogColumn.writeByte(1); 
			catalogColumn.writeBytes(Constants.FALSE); 
			catalogColumn.writeBytes(Constants.FALSE); 
			catalogColumn.writeBytes(Constants.FALSE);
			
			catalogColumn.seek(offset[1]);
			catalogColumn.writeShort(42); 
			catalogColumn.writeInt(2); 
			catalogColumn.writeByte(6);
			catalogColumn.writeByte(28);
			catalogColumn.writeByte(22);
			catalogColumn.writeByte(16);
			catalogColumn.writeByte(4);
			catalogColumn.writeByte(14);
			catalogColumn.writeByte(14);
			catalogColumn.writeBytes(Constants.TABLE_CATALOG); 
			catalogColumn.writeBytes(Constants.HEADER_TABLE_NAME); 
			catalogColumn.writeBytes(Constants.HEADER_TEXT); 
			catalogColumn.writeByte(2);
			catalogColumn.writeBytes(Constants.FALSE); 
			catalogColumn.writeBytes(Constants.FALSE);
			
			catalogColumn.seek(offset[2]);
			catalogColumn.writeShort(37); 
			catalogColumn.writeInt(3); 
			catalogColumn.writeByte(6);
			catalogColumn.writeByte(29);
			catalogColumn.writeByte(17);
			catalogColumn.writeByte(15);
			catalogColumn.writeByte(4);
			catalogColumn.writeByte(14);
			catalogColumn.writeByte(14);
			catalogColumn.writeBytes(Constants.COLUMN_CATALOG);
			catalogColumn.writeBytes(Constants.HEADER_ROWID);
			catalogColumn.writeBytes("INT");
			catalogColumn.writeByte(1);
			catalogColumn.writeBytes(Constants.FALSE);
			catalogColumn.writeBytes(Constants.FALSE);
			
			catalogColumn.seek(offset[3]);
			catalogColumn.writeShort(43);
			catalogColumn.writeInt(4); 
			catalogColumn.writeByte(6);
			catalogColumn.writeByte(29);
			catalogColumn.writeByte(22);
			catalogColumn.writeByte(16);
			catalogColumn.writeByte(4);
			catalogColumn.writeByte(14);
			catalogColumn.writeByte(14);
			catalogColumn.writeBytes(Constants.COLUMN_CATALOG);
			catalogColumn.writeBytes(Constants.HEADER_TABLE_NAME);
			catalogColumn.writeBytes(Constants.HEADER_TEXT);
			catalogColumn.writeByte(2);
			catalogColumn.writeBytes(Constants.FALSE);
			catalogColumn.writeBytes(Constants.FALSE);
			
			catalogColumn.seek(offset[4]);
			catalogColumn.writeShort(44);
			catalogColumn.writeInt(5); 
			catalogColumn.writeByte(6);
			catalogColumn.writeByte(29);
			catalogColumn.writeByte(23);
			catalogColumn.writeByte(16);
			catalogColumn.writeByte(4);
			catalogColumn.writeByte(14);
			catalogColumn.writeByte(14);
			catalogColumn.writeBytes(Constants.COLUMN_CATALOG);
			catalogColumn.writeBytes("column_name");
			catalogColumn.writeBytes(Constants.HEADER_TEXT);
			catalogColumn.writeByte(3);
			catalogColumn.writeBytes(Constants.FALSE);
			catalogColumn.writeBytes(Constants.FALSE);
			
			catalogColumn.seek(offset[5]);
			catalogColumn.writeShort(42);
			catalogColumn.writeInt(6); 
			catalogColumn.writeByte(6);
			catalogColumn.writeByte(29);
			catalogColumn.writeByte(21);
			catalogColumn.writeByte(16);
			catalogColumn.writeByte(4);
			catalogColumn.writeByte(14);
			catalogColumn.writeByte(14);
			catalogColumn.writeBytes(Constants.COLUMN_CATALOG);
			catalogColumn.writeBytes("data_type");
			catalogColumn.writeBytes(Constants.HEADER_TEXT);
			catalogColumn.writeByte(4);
			catalogColumn.writeBytes(Constants.FALSE);
			catalogColumn.writeBytes(Constants.FALSE);
			
			catalogColumn.seek(offset[6]);
			catalogColumn.writeShort(52); 
			catalogColumn.writeInt(7); 
			catalogColumn.writeByte(6);
			catalogColumn.writeByte(29);
			catalogColumn.writeByte(28);
			catalogColumn.writeByte(19);
			catalogColumn.writeByte(4);
			catalogColumn.writeByte(14);
			catalogColumn.writeByte(14);
			catalogColumn.writeBytes(Constants.COLUMN_CATALOG);
			catalogColumn.writeBytes("ordinal_position");
			catalogColumn.writeBytes("TINYINT");
			catalogColumn.writeByte(5);
			catalogColumn.writeBytes(Constants.FALSE);
			catalogColumn.writeBytes(Constants.FALSE);
			
			catalogColumn.seek(offset[7]);
			catalogColumn.writeShort(44); 
			catalogColumn.writeInt(8); 
			catalogColumn.writeByte(6);
			catalogColumn.writeByte(29);
			catalogColumn.writeByte(23);
			catalogColumn.writeByte(16);
			catalogColumn.writeByte(4);
			catalogColumn.writeByte(14);
			catalogColumn.writeByte(14);
			catalogColumn.writeBytes(Constants.COLUMN_CATALOG);
			catalogColumn.writeBytes(Constants.HEADER_IS_NULLABLE);
			catalogColumn.writeBytes(Constants.HEADER_TEXT);
			catalogColumn.writeByte(6);
			catalogColumn.writeBytes(Constants.FALSE);
			catalogColumn.writeBytes(Constants.FALSE);
		

			catalogColumn.seek(offset[8]);
			catalogColumn.writeShort(42); 
			catalogColumn.writeInt(9); 
			catalogColumn.writeByte(6);
			catalogColumn.writeByte(29);
			catalogColumn.writeByte(21);
			catalogColumn.writeByte(16);
			catalogColumn.writeByte(4);
			catalogColumn.writeByte(14);
			catalogColumn.writeByte(14);
			catalogColumn.writeBytes(Constants.COLUMN_CATALOG);
			catalogColumn.writeBytes(Constants.HEADER_IS_UNIQUE);
			catalogColumn.writeBytes(Constants.HEADER_TEXT);
			catalogColumn.writeByte(7);
			catalogColumn.writeBytes(Constants.FALSE);
			catalogColumn.writeBytes(Constants.FALSE);
			
			catalogColumn.close();
			
			String[] cur_row_id_value = {"10", Constants.TABLE_CATALOG,"cur_row_id","INT","3",Constants.FALSE,Constants.FALSE};		
			Table.insertInto(Constants.COLUMN_CATALOG,cur_row_id_value,Constants.dirCatalog);			//add current row_id column to davisbase_columns
		}
		catch (Exception e) {
			System.out.println(e);
		}
}



	public static String[] parseCondition(String condition){
		String parsedCondition[] = new String[3];
		String temp[] = new String[2];
		if(condition.contains(Constants.EQUALS_SIGN)) {
			temp = condition.split(Constants.EQUALS_SIGN);
			parsedCondition[0] = temp[0].trim();
			parsedCondition[1] = Constants.EQUALS_SIGN;
			parsedCondition[2] = temp[1].trim();
		}
		
		if(condition.contains(Constants.LESS_THAN_SIGN)) {
			temp = condition.split(Constants.LESS_THAN_SIGN);
			parsedCondition[0] = temp[0].trim();
			parsedCondition[1] = Constants.LESS_THAN_SIGN;
			parsedCondition[2] = temp[1].trim();
		}
		
		if(condition.contains(Constants.GREATER_THAN_SIGN)) {
			temp = condition.split(Constants.GREATER_THAN_SIGN);
			parsedCondition[0] = temp[0].trim();
			parsedCondition[1] = Constants.GREATER_THAN_SIGN;
			parsedCondition[2] = temp[1].trim();
		}
		
		if(condition.contains(Constants.LESS_THAN_EQUAL_SIGN)) {
			temp = condition.split(Constants.LESS_THAN_EQUAL_SIGN);
			parsedCondition[0] = temp[0].trim();
			parsedCondition[1] = Constants.LESS_THAN_EQUAL_SIGN;
			parsedCondition[2] = temp[1].trim();
		}

		if(condition.contains(Constants.GREATER_THAN_EQUAL_SIGN)) {
			temp = condition.split(Constants.GREATER_THAN_EQUAL_SIGN);
			parsedCondition[0] = temp[0].trim();
			parsedCondition[1] = Constants.GREATER_THAN_EQUAL_SIGN;
			parsedCondition[2] = temp[1].trim();
		}
		
		if(condition.contains(Constants.NOT_EQUAL_SIGN)) {
			temp = condition.split(Constants.NOT_EQUAL_SIGN);
			parsedCondition[0] = temp[0].trim();
			parsedCondition[1] = Constants.NOT_EQUAL_SIGN;
			parsedCondition[2] = temp[1].trim();
		}

		return parsedCondition;
	}
		
	public static void parseUserCommand (String userCommand) {
		
		ArrayList<String> commandTokens = new ArrayList<String>(Arrays.asList(userCommand.split(" ")));

		switch (commandTokens.get(0)) {

		    case "show":
		    	System.out.println("CASE: SHOW");
			    showTables();
			    break;
			
		    case "create":
		    	switch (commandTokens.get(1)) {
		    	case "table": 
		    		System.out.println("CASE: CREATE TABLE");
		    		parseCreateString(userCommand);
		    		break;
		    		
		    	case "index":
		    		System.out.println("CASE: CREATE INDEX");
		    		parseIndexString(userCommand);
		    		break;
		    		
		    	default:
					System.out.println("I didn't understand the command: \"" + userCommand + "\"");
					System.out.println();
					break;
		    	}
		    	break;

			case "insert":
				System.out.println("CASE: INSERT");
				parseInsertString(userCommand);
				break;
				
			case "delete":
				System.out.println("CASE: DELETE");
				parseDeleteString(userCommand);
				break;	

			case "update":
				System.out.println("CASE: UPDATE");
				parseUpdateString(userCommand);
				break;
				
			case "select":
				System.out.println("CASE: SELECT");
				parseQueryString(userCommand);
				break;

			case "drop":
				System.out.println("CASE: DROP");
				dropTable(userCommand);
				break;	

			case "help":
				help();
				break;

			case "version":
				System.out.println("DavisBase Version " + Constants.VERSION);
				break;

			case "exit":
				exit=true;
				break;
				
			case "quit":
				exit=true;
				break;
	
			default:
				System.out.println("I didn't understand the command: \"" + userCommand + "\"");
				System.out.println();
				break;
		}
	} 

	public static void showTables() {
		System.out.println("Parsing the string:\"show tables\"");
		

		String table = Constants.TABLE_CATALOG;
		String[] cols = {Constants.HEADER_TABLE_NAME};
		String[] condition = new String[0];
		Table.select(table, cols, condition,true);
	}
	
    public static void parseCreateString(String createString) {
		
		System.out.println("Parsing the string:\"" + createString + "\"");
		
		String tableName = createString.split(" ")[2];
		String cols = createString.split(tableName)[1].trim();
		String[] create_cols = cols.substring(1, cols.length()-1).split(",");
		
		for(int i = 0; i < create_cols.length; i++)
			create_cols[i] = create_cols[i].trim();
		
		if(tableExists(tableName)){
			System.out.println("Table "+tableName+" already exists.");
		}
		else
			{
			Table.createTable(tableName, create_cols);		
			}

	}
    
    public static void parseInsertString(String insertString) {
    	try{
		System.out.println("Parsing the string:\"" + insertString + "\"");
		
		String table = insertString.split(" ")[2];
		String rawCols=insertString.split("values")[1].trim();
		String[] insert_vals_init = rawCols.substring(1, rawCols.length()-1).split(",");
		String[] insert_vals = new String[insert_vals_init.length + 1];
		for(int i = 1; i <= insert_vals_init.length; i++)
			insert_vals[i] = insert_vals_init[i-1].trim();
	
		if(tableExists(table)){
			Table.insertInto(table, insert_vals,Constants.dirUserdata+"/");
		}
		else
		{
			System.out.println("Table "+table+" does not exist.");
		}
    	}
    	catch(Exception e)
    	{
    		System.out.println(e+e.toString());
    	}

	}
    
    public static void parseDeleteString(String deleteString) {
		System.out.println("Parsing the string:\"" + deleteString + "\"");
		
		String table = deleteString.split(" ")[2];
		String[] rawConditionArray = deleteString.split("where");
		String rawCondition = rawConditionArray.length>1?rawConditionArray[1]:"";
		String[] parsedCondition = rawConditionArray.length>1?parseCondition(rawCondition) : new String[0];
		if(tableExists(table)){
			Table.delete(table, parsedCondition, Constants.dirUserdata);
		}
		else
		{
			System.out.println("Table "+table+" does not exist.");
		}
		
		
	}
    
    public static void parseUpdateString(String updateString) {
		System.out.println("Parsing the string:\"" + updateString + "\"");
		
		String table = updateString.split(" ")[1];
		String whereCondition = updateString.split("set")[1].split("where")[1];
		String setCondition = updateString.split("set")[1].split("where")[0];
		String[] parsedCondition = parseCondition(whereCondition);
		String[] parsedSetCondition = parseCondition(setCondition);
		if(!tableExists(table)){
			System.out.println("Table "+table+" does not exist.");
		}
		else
		{
			Table.update(table, parsedCondition, parsedSetCondition, Constants.dirUserdata);
		}
		
	}
    
    public static void parseQueryString(String queryString) {
		System.out.println("Parsing the string:\"" + queryString + "\"");
		
		String[] parsedCondition;
		String[] columns;
		String[] cols_condition = queryString.split("where");
		if(cols_condition.length > 1){
			parsedCondition = parseCondition(cols_condition[1].trim());
		}
		else{
			parsedCondition = new String[0];
		}
		String[] select = cols_condition[0].split("from");
		String tableName = select[1].trim();
		String cols = select[0].replace("select", "").trim();
		if(cols.contains("*")){
			columns = new String[1];
			columns[0] = "*";
		}
		else{
			columns = cols.split(",");
			for(int i = 0; i < columns.length; i++)
				columns[i] = columns[i].trim();
		}
		
		if(!tableExists(tableName)){
			System.out.println("Table "+tableName+" does not exist.");
		}
		else
		{
		    Table.select(tableName, columns, parsedCondition,true);
		}
	}
	
	public static void dropTable(String dropTableString) {
		System.out.println("Parsing the string:\"" + dropTableString + "\"");
		
		String[] tokens=dropTableString.split(" ");
		String tableName = tokens[2];
		if(tableExists(tableName)){
			Table.drop(tableName);
		}
		else{
			System.out.println("Table "+tableName+" does not exist.");
		}		
	}
	
public static void parseIndexString(String createString) {
		System.out.println("Parsing the string:\"" + createString + "\"");
		
		String[] tokens=createString.split(" ");
		String tableName = tokens[3];
		String[] temp = createString.split(tableName);
		String cols = temp[1].trim();
		String[] create_cols = cols.substring(1, cols.length()-1).split(",");
		
		for(int i = 0; i < create_cols.length; i++)
			create_cols[i] = create_cols[i].trim();
		
		
		Table.createIndex(tableName, create_cols);		
			

	}
		

}
