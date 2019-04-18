import java.io.RandomAccessFile;
import java.io.File;
import java.io.FileReader;
import java.util.Scanner;
import java.util.SortedMap;
import java.util.ArrayList;
import java.util.Arrays;
import static java.lang.System.out;
import java.util.Stack;
public class DavisBase {

	/* This can be changed to whatever you like */
	static String prompt = "davisql> ";
	static String version = "v1.0b(example)";
	static String copyright = "©2016 Chris Irwin Davis";
	static boolean isExit = false;
	static String catalog_dir = "data/catalog";
	static String userdata_dir = "data/user_data";
	/*
	 * Page size for all files is 512 bytes by default.
	 * You may choose to make it user modifiable
	 */
	static long pageSize = 512; 

	/* 
	 *  The Scanner class is used to collect user commands from the prompt
	 *  There are many ways to do this. This is just one.
	 *
	 *  Each time the semicolon (;) delimiter is entered, the userCommand 
	 *  String is re-populated.
	 */
	static Scanner scanner = new Scanner(System.in).useDelimiter(";");
	
	/** ***********************************************************************
	 *  Main method
	 */
    public static void main(String[] args) {

		/* Display the welcome screen */
		splashScreen();

		/* Variable to collect user input from the prompt */
		String userCommand = ""; 

		while(!isExit) {
			System.out.print(prompt);
			/* toLowerCase() renders command case insensitive */
			userCommand = scanner.next().replace("\n", " ").replace("\r", "").trim().toLowerCase();
			// userCommand = userCommand.replace("\n", "").replace("\r", "");
			parseUserCommand(userCommand);
		}
		System.out.println("Exiting...");


	}

	/** ***********************************************************************
	 *  Static method definitions
	 */

	/**
	 *  Display the splash screen
	 */
	public static void splashScreen() {
		System.out.println(line("-",80));
        System.out.println("Welcome to DavisBaseLite"); // Display the string.
		System.out.println("DavisBaseLite Version " + getVersion());
		System.out.println(getCopyright());
		System.out.println("\nType \"help;\" to display supported commands.");
		System.out.println(line("-",80));
	}
	
	/**
	 * @param s The String to be repeated
	 * @param num The number of time to repeat String s.
	 * @return String A String object, which is the String s appended to itself num times.
	 */
	public static String line(String s,int num) {
		String a = "";
		for(int i=0;i<num;i++) {
			a += s;
		}
		return a;
	}
	
	public static void printCmd(String s) {
		System.out.println("\n\t" + s + "\n");
	}
	public static void printDef(String s) {
		System.out.println("\t\t" + s);
	}
	
		/**
		 *  Help: Display supported commands
		 */
		public static void help() {
			out.println(line("*",80));
			out.println("SUPPORTED COMMANDS\n");
			out.println("All commands below are case insensitive\n");
			out.println("SHOW TABLES;");
			out.println("\tDisplay the names of all tables.\n");
			//printCmd("SELECT * FROM <table_name>;");
			//printDef("Display all records in the table <table_name>.");
			out.println("SELECT <column_list> FROM <table_name> [WHERE <condition>];");
			out.println("\tDisplay table records whose optional <condition>");
			out.println("\tis <column_name> = <value>.\n");
			out.println("DROP TABLE <table_name>;");
			out.println("\tRemove table data (i.e. all records) and its schema.\n");
			out.println("UPDATE TABLE <table_name> SET <column_name> = <value> [WHERE <condition>];");
			out.println("\tModify records data whose optional <condition> is\n");
			out.println("VERSION;");
			out.println("\tDisplay the program version.\n");
			out.println("HELP;");
			out.println("\tDisplay this help information.\n");
			out.println("EXIT;");
			out.println("\tExit the program.\n");
			out.println(line("*",80));
		}

	/** return the DavisBase version */
	public static String getVersion() {
		return version;
	}
	
	public static String getCopyright() {
		return copyright;
	}
	
	public static void displayVersion() {
		System.out.println("DavisBaseLite Version " + getVersion());
		System.out.println(getCopyright());
	}
		
	public static void parseUserCommand (String userCommand) {
		
		/* commandTokens is an array of Strings that contains one token per array element 
		 * The first token can be used to determine the type of command 
		 * The other tokens can be used to pass relevant parameters to each command-specific
		 * method inside each case statement */
		// String[] commandTokens = userCommand.split(" ");
		ArrayList<String> commandTokens = new ArrayList<String>(Arrays.asList(userCommand.split(" ")));
		

		/*
		*  This switch handles a very small list of hardcoded commands of known syntax.
		*  You will want to rewrite this method to interpret more complex commands. 
		*/
		switch (commandTokens.get(0)) {
			case "show":
				Stack<String[]> condition=new Stack<>();
				String[] columnNames={"*"};
				Operations.select("davisbase_tables",columnNames,condition);
				break;
			case "select":
				parseSelect(userCommand);
				break;
			case "drop":
				dropTable(userCommand);
				break;
			case "create":
		    	switch (commandTokens.get(1)) {
		    	case "table": 
		    		parseCreateTable(userCommand);
		    		break;
		    		
		    	case "index":
		    		parseIndex(userCommand);
		    		break;
		    		
		    	default:
					System.out.println("I didn't understand the command: \"" + userCommand + "\"");
					System.out.println();
					break;
		    	}
		    	break;
			case "update":
				parseUpdate(userCommand);
				break;
			case "help":
				help();
				break;
			case "version":
				displayVersion();
				break;
			case "exit":
				isExit = true;
				break;
			case "quit":
				isExit = true;
			default:
				System.out.println("I didn't understand the command: \"" + userCommand + "\"");
				break;
		}
	}
	

	/**
	 *  Stub method for dropping tables
	 *  @param dropTableString is a String of the user input
	 */
	public static void dropTable(String dropTableString) {
		System.out.println("STUB: Calling the method to process the command");
		System.out.println("Parsing the string:\"" + dropTableString + "\"");
		
		String[] tokens=dropTableString.split(" ");
		String tableName = tokens[2];
		if(!tableExists(tableName)){
			System.out.println("Table "+tableName+" does not exist.");
		}
		else
		{
			Operations.dropTable(tableName);
		}		

	}
	
	// Helper function to check if a table already exists.
	public static boolean tableExists(String tablename){
		tablename = tablename+".tbl";
		
		try {
			
			
			File dataDir = new File(userdata_dir);
			if (tablename.equalsIgnoreCase("davisbase_tables.tbl") || tablename.equalsIgnoreCase("davisbase_columns.tbl"))
				dataDir = new File(catalog_dir) ;
			
			String[] oldTableFiles;
			oldTableFiles = dataDir.list();
			for (int i=0; i<oldTableFiles.length; i++) {
				if(oldTableFiles[i].equals(tablename))
					return true;
			}
		}
		catch (SecurityException se) {
			System.out.println("Unable to create data container directory");
			System.out.println(se);
		}

		return false;
	}
	
	/**
	 *  Method for executing queries
	 *  @param queryString is a String of the user input
	 */
	public static void parseSelect(String queryString) {
		System.out.println("\tParsing the string:\"" + queryString + "\"");

		String str[] = queryString.split("where");		//where condition in str[1] and before that in str[0]
		String tableName = str[0].split("from")[1].trim();		//get table name
		String[] colNames = str[0].split("from")[0].replaceAll("select"," ").split(",");
		
		for(int i=0;i<colNames.length;i++) {
			colNames[i] = colNames[i].trim();
		}
		
		Operations.select(tableName,colNames,parseCondition(str[1]));
	}

	/**
	 *  Stub method for updating records
	 *  @param updateString is a String of the user input
	 */
	public static void parseUpdate(String updateString) {
		String[]updateTokens = updateString.toLowerCase().split("set");
		String[] temp = updateTokens[0].trim().split(" ");
		String tableName = temp[1].trim();
		String set_condition;
		String where_condition = null;
		if(!tableExists(tableName))
		{
			System.out.println("Table not present");
			return;
		}
		if(updateTokens[1].contains("where"))
		{
			String[] set_where = updateTokens[1].split("where");
			set_condition = set_where[0].trim();
			where_condition = set_where[1].trim();
			Operations.update(tablename, parseCondition(set_condition),  parseCondition((where_condition)));
		}
		else{ 
			set_condition=updateTokens[1].trim();
		
		String[] no_where = new String[0];
		Operations.update(tableName,  parseCondition(set_condition),no_where);
		}
	}
	
	private static Stack<String[]> parseCondition(String condition) {
		String[] conditions = condition.trim().split(",");
		Stack<String[]> comparators = new Stack<>();
		for(int i=0;i<conditions.length;i++) {
			if(conditions[i].length()!=0) {
				String curCondition = conditions[i];
				String comparator[] = new String[3];
				String temp[] = new String[2];
				if(curCondition.contains("=")) {
					temp = curCondition.split("=");
					comparator[0] = temp[0].trim();
					comparator[1] = "=";
					comparator[2] = temp[1].trim();
				}
				
				if(curCondition.contains("<")) {
					temp = curCondition.split("<");
					comparator[0] = temp[0].trim();
					comparator[1] = "<";
					comparator[2] = temp[1].trim();
				}
				
				if(curCondition.contains(">")) {
					temp = curCondition.split(">");
					comparator[0] = temp[0].trim();
					comparator[1] = ">";
					comparator[2] = temp[1].trim();
				}
				
				if(curCondition.contains("<=")) {
					temp = curCondition.split("<=");
					comparator[0] = temp[0].trim();
					comparator[1] = "<=";
					comparator[2] = temp[1].trim();
				}
		
				if(curCondition.contains(">=")) {
					temp = curCondition.split(">=");
					comparator[0] = temp[0].trim();
					comparator[1] = ">=";
					comparator[2] = temp[1].trim();
				}
				
				if(curCondition.contains("!=")) {
					temp = curCondition.split("!=");
					comparator[0] = temp[0].trim();
					comparator[1] = "!=";
					comparator[2] = temp[1].trim();
				}
				comparators.push(comparator);
			}
	}

		return comparators;
	}

	
	/**
	 *  Method for creating new tables
	 *  @param createTableString is a String of the user input
	 */
	public static void parseCreateTable(String createTableString) {
		
		System.out.println("Parsing the string:\"" + createTableString + "\"");
		ArrayList<String> createTableTokens = new ArrayList<String>(Arrays.asList(createTableString.split(" ")));

		String[] tokens=createTableString.split(" ");
		String tableName = tokens[2];
		String[] temp = createTableString.split(tableName);
		String cols = temp[1].trim();
		String[] create_cols = cols.substring(1, cols.length()-1).split(",");
		
		for(int i = 0; i < create_cols.length; i++)
			create_cols[i] = create_cols[i].trim();
		
		if(tableExists(tableName)){
			System.out.println("Table "+tableName+" already exists.");
		}
		else
			{
			Operations.createTable(tableName, create_cols);		
			}
	}
	
	public static void parseInsert(String insertString) {
    	try{
		System.out.println("STUB: Calling the method to process the command");
		System.out.println("Parsing the string:\"" + insertString + "\"");
		
		String[] insertTokens=insertString.split(" ");
		String table = insertTokens[2];
		String[] temp = insertString.split("values");
		String temp2=temp[1].trim();
		String[] insert_vals = temp2.substring(1, temp2.length()-1).split(",");
		for(int i = 0; i < insert_vals.length; i++)
			insert_vals[i] = insert_vals[i].trim();
	
		if(!tableExists(table)){
			System.out.println("Table "+table+" does not exist.");
		}
		else
		{
			Operations.insertInto(table, insert_vals);
		}
    	}
    	catch(Exception e)
    	{
    		System.out.println(e+e.toString());
    	}

	}
    
    public static void parseDeleteString(String deleteString) {
		System.out.println("STUB: Calling the method to process the command");
		System.out.println("Parsing the string:\"" + deleteString + "\"");
		
		String[] deleteTokens=deleteString.split(" ");
		String table = deleteTokens[3];
		String[] temp = deleteString.split("where");
		String delete_condition = temp[1];
		if(!tableExists(table)){
			System.out.println("Table "+table+" does not exist.");
		}
		else
		{
			Operations.delete(table, parseCondition(delete_condition));
		}
		
		
	}
    
public static void parseIndex(String createString) {
		
		System.out.println("STUB: Calling your method to process the command");
		System.out.println("Parsing the string:\"" + createString + "\"");
		
		String[] tokens=createString.split(" ");
		String tableName = tokens[3];
		String[] temp = createString.split(tableName);
		String cols = temp[1].trim();
		String[] index_cols = cols.substring(1, cols.length()-1).split(",");
		
		for(int i = 0; i <index_cols.length; i++)
			index_cols[i] = index_cols[i].trim();
		
		Operations.createIndex(tableName, index_cols);
	}
}

	