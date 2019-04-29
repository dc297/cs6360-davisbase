
public final class Constants {
	
	public static final int pageSize = 512;
	public static final String datePattern = "yyyy-MM-dd_HH:mm:ss";
	public static final String dirCatalog = "data/catalog/";
	public static final String dirUserdata = "data/user_data/";
	
	//catalog filenames
	public static final String TABLE_CATALOG = "davisbase_tables";
	public static final String COLUMN_CATALOG = "davisbase_columns";
	
	
	//page types
	public static final int recordsPage = 0x0D;
	
	//data types
	
	//NULL
	public static final byte NULL = 0x00;
	public static final byte SHORTNULL = 0x01;
	public static final byte INTNULL = 0x02;
	public static final byte LONGNULL = 0x03;
	
	//Numeric
	public static final byte TINYINT = 0x04;
	public static final byte SHORTINT = 0x05;
	public static final byte INT = 0x06;
	public static final byte LONG = 0x07;
	public static final byte FLOAT = 0x08;
	public static final byte DOUBLE = 0x09;
	
	//DateTime
	public static final byte DATETIME = 0x0A;
	public static final byte DATE = 0x0B;
	
	//TEXT
	public static final byte TEXT = 0x0C;
}
