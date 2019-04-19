
import java.io.RandomAccessFile;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Date;
import java.text.SimpleDateFormat;

public class Table{
	
	public static int pageSize = 512;
	public static String datePattern = "yyyy-MM-dd_HH:mm:ss";
	public static String dir_catalog = "data/catalog/";
	public static String dir_userdata = "data/user_data/";
	
	public static int no_of_records;
	
	public static int pages(RandomAccessFile file){
		int num_pages = 0;
		try{
			num_pages = (int)(file.length()/(new Long(pageSize)));
		}catch(Exception e){
			System.out.println(e);
		}

		return num_pages;
	}

	public static void drop(String table){
		try{
			
			RandomAccessFile file = new RandomAccessFile(dir_catalog+"davisbase_tables.tbl", "rw");
			int numOfPages = pages(file);
			for(int page = 1; page <= numOfPages; page ++){
				file.seek((page-1)*pageSize);
				byte fileType = file.readByte();
				if(fileType == 0x0D)
				{
					short[] cellsAddr = BPlusTree.getCellArray(file, page);
					int k = 0;
					for(int i = 0; i < cellsAddr.length; i++)
					{
						long loc = BPlusTree.getCellLoc(file, page, i);
						String[] vals = retrieveValues(file, loc);
						String tb = vals[1];
						if(!tb.equals(table))
						{
							BPlusTree.setCellOffset(file, page, k, cellsAddr[i]);
							k++;
						}
					}
					BPlusTree.setCellNumber(file, page, (byte)k);
				}
				else
					continue;
			}

			file = new RandomAccessFile(dir_catalog+"davisbase_columns.tbl", "rw");
			numOfPages = pages(file);
			for(int page = 1; page <= numOfPages; page ++){
				file.seek((page-1)*pageSize);
				byte fileType = file.readByte();
				if(fileType == 0x0D)
				{
					short[] cellsAddr = BPlusTree.getCellArray(file, page);
					int k = 0;
					for(int i = 0; i < cellsAddr.length; i++)
					{
						long loc = BPlusTree.getCellLoc(file, page, i);
						String[] vals = retrieveValues(file, loc);
						String tb = vals[1];
						if(!tb.equals(table))
						{
							BPlusTree.setCellOffset(file, page, k, cellsAddr[i]);
							k++;
						}
					}
					BPlusTree.setCellNumber(file, page, (byte)k);
				}
				else
					continue;
			}

			File anOldFile = new File(dir_userdata, table+".tbl"); 
			anOldFile.delete();
		}catch(Exception e){
			System.out.println(e);
		}

	}
    
	public static void delete(String table, String[] cmp){
		try{
		ArrayList<Integer> rowids = new ArrayList<Integer>();
		
		if(!"rowid".equals(cmp[0])) {
			//get the rowids to be updated
			Records records = select(table, new String[] {"*"}, cmp, false);
			rowids.addAll(records.content.keySet());
		}
		else
			rowids.add(Integer.parseInt(cmp[2]));
		
		for(int key : rowids) {
			RandomAccessFile file = new RandomAccessFile(dir_userdata+table+".tbl", "rw");
			int numPages = pages(file);
			int page = 0;
			for(int p = 1; p <= numPages; p++)
				if(BPlusTree.hasKey(file, p, key)&BPlusTree.getPageType(file, p)==0x0D){
					page = p;
					break;
				}
			
			if(page==0)
			{
				System.out.println("The given key value does not exist");
				return;
			}
			
			short[] cellsAddr = BPlusTree.getCellArray(file, page);
			int k = 0;
			for(int i = 0; i < cellsAddr.length; i++)
			{
				long loc = BPlusTree.getCellLoc(file, page, i);
				String[] vals = retrieveValues(file, loc);
				int x = new Integer(vals[0]);
				if(x!=key)
				{
					BPlusTree.setCellOffset(file, page, k, cellsAddr[i]);
					k++;
				}
			}
			BPlusTree.setCellNumber(file, page, (byte)k);
		}
		
		}catch(Exception e)
		{
			System.out.println(e);
		}
		
	}
	
	public static String[] retrieveValues(RandomAccessFile file, long loc){
		
		String[] values = null;
		try{
			
			SimpleDateFormat dateFormat = new SimpleDateFormat (datePattern);

			file.seek(loc+2);
			int key = file.readInt();
			int num_cols = file.readByte();
			
			byte[] stc = new byte[num_cols];
			file.read(stc);
			
			values = new String[num_cols+1];
			
			values[0] = Integer.toString(key);
			
			for(int i=1; i <= num_cols; i++){
				switch(stc[i-1]){
					case 0x00:  file.readByte();
					            values[i] = "null";
								break;

					case 0x01:  file.readShort();
					            values[i] = "null";
								break;

					case 0x02:  file.readInt();
					            values[i] = "null";
								break;

					case 0x03:  file.readLong();
					            values[i] = "null";
								break;

					case 0x04:  values[i] = Integer.toString(file.readByte());
								break;

					case 0x05:  values[i] = Integer.toString(file.readShort());
								break;

					case 0x06:  values[i] = Integer.toString(file.readInt());
								break;

					case 0x07:  values[i] = Long.toString(file.readLong());
								break;

					case 0x08:  values[i] = String.valueOf(file.readFloat());
								break;

					case 0x09:  values[i] = String.valueOf(file.readDouble());
								break;

					case 0x0A:  Long temp = file.readLong();
								Date dateTime = new Date(temp);
								values[i] = dateFormat.format(dateTime);
								break;

					case 0x0B:  temp = file.readLong();
								Date date = new Date(temp);
								values[i] = dateFormat.format(date).substring(0,10);
								break;

					default:    int len = new Integer(stc[i-1]-0x0C); //why 12 is subtracted?
								byte[] bytes = new byte[len];
								file.read(bytes);
								values[i] = new String(bytes);
								break;
				}
			}

		}catch(Exception e){
			System.out.println(e);
		}

		return values;
	}

	public static void createTable(String table, String[] col){
		try{	
			String[] new_col = new String[col.length+1];
			new_col[0] = "rowid INT UNIQUE";
			for(int i=0;i<col.length;i++) {
				new_col[i+1] = col[i];
			}
			
			RandomAccessFile file = new RandomAccessFile(dir_userdata+table+".tbl", "rw");
			file.setLength(pageSize);
			file.seek(0);
			file.writeByte(0x0D);
			file.close();
			
			file = new RandomAccessFile(dir_catalog+"davisbase_tables.tbl", "rw");
			
			int numOfPages = pages(file);
			int page=1;
			for(int p = 1; p <= numOfPages; p++){
				int rm = BPlusTree.getRightMost(file, p);
				if(rm == 0)
					page = p;
			}
			
			int[] keys = BPlusTree.getKeyArray(file, page);
			int l = keys[0];
			for(int i = 0; i < keys.length; i++)
				if(keys[i]>l)
					l = keys[i];
			file.close();
			
			String[] values = {Integer.toString(l+1), table};
			insertInto("davisbase_tables", values,dir_catalog);

			file = new RandomAccessFile(dir_catalog+"davisbase_columns.tbl", "rw");
			
			numOfPages = pages(file);
			page=1;
			for(int p = 1; p <= numOfPages; p++){
				int rm = BPlusTree.getRightMost(file, p);
				if(rm == 0)
					page = p;
			}
			
			keys = BPlusTree.getKeyArray(file, page);
			l = keys[0];
			for(int i = 0; i < keys.length; i++)
				if(keys[i]>l)
					l = keys[i];
			file.close();
			
			String[] cur_row_id_value = {"10", "davisbase_tables","cur_row_id","INT","3","NO","NO"};		
			insertInto("davisbase_columns",cur_row_id_value,dir_catalog);			//add current row_id column to davisbase_columns
			
			for(int i = 0; i < col.length; i++){
				l = l + 1;
				String[] token = col[i].split(" ");
				String col_name = token[0];
				String dt = token[1].toUpperCase();
				String pos = Integer.toString(i+1);
				String nullable;
				String unique="NO";
				if(token.length > 2)
				{
					nullable = "NO";
					if(token[2].toUpperCase().trim().equals("UNIQUE"))
						unique = "YES";
					else
						unique = "NO";
				}
				else
					 nullable = "YES";
				
				
				String[] value = {Integer.toString(l), table, col_name, dt, pos, nullable, unique};
				insertInto("davisbase_columns", value,dir_catalog);
			}
	
		}catch(Exception e){
			System.out.println(e);
		}
	}

	public static void update(String table, String[] cmp, String[] set){
		try{
			
			//int key = new Integer(cmp[2]);
			ArrayList<Integer> rowids = new ArrayList<Integer>();
			
			if(!"rowid".equals(cmp[0])) {
				//get the rowids to be updated
				System.out.println("Found the following records:");
				Records records = select(table, new String[] {"*"}, cmp, false);
				rowids.addAll(records.content.keySet());
			}
			else
				rowids.add(Integer.parseInt(cmp[2]));
			
			for(int key : rowids) {
				RandomAccessFile file = new RandomAccessFile(dir_userdata+table+".tbl", "rw");
				int numPages = pages(file);
				int page = 0;
				for(int p = 1; p <= numPages; p++)
					if(BPlusTree.hasKey(file, p, key)&BPlusTree.getPageType(file, p)==0x0D){
						page = p;
					}
				
				if(page==0)
				{
					System.out.println("The given key value does not exist");
					return;
				}
				
				int[] keys = BPlusTree.getKeyArray(file, page);
				int x = 0;
				for(int i = 0; i < keys.length; i++)
					if(keys[i] == key)
						x = i;
				int offset = BPlusTree.getCellOffset(file, page, x);
				long loc = BPlusTree.getCellLoc(file, page, x);
				
				String[] cols = getColName(table);
				String[] values = retrieveValues(file, loc);
	
				String[] type = getDataType(table);
				for(int i=0; i < type.length; i++)
					if(type[i].equals("DATE") || type[i].equals("DATETIME"))
						values[i] = "'"+values[i]+"'";
	
				for(int i = 0; i < cols.length; i++)
					if(cols[i].equals(set[0]))
						x = i;
				values[x] = set[2];
	
				String[] nullable = getNullable(table);
				for(int i = 0; i < nullable.length; i++){
					if(values[i].equals("null") && nullable[i].equals("NO")){
						System.out.println("NULL-value constraint violation");
						return;
					}
				}
	
				byte[] stc = new byte[cols.length-1];
				int plsize = calPayloadSize(table, values, stc);
				BPlusTree.updateLeafCell(file, page, offset, plsize, key, stc, values);
	
				file.close();
			}

		}catch(Exception e){
			System.out.println(e);
		}
	}

	public static void insertInto(String table, String[] values,String dir_s){
		try{
			RandomAccessFile file = new RandomAccessFile(dir_s + table+".tbl", "rw");
			insertInto(file, table, values);
			file.close();

		}catch(Exception e){
			System.out.println(e);
		}
	}
	
	public static void insertInto(RandomAccessFile file, String table, String[] values){
		String[] dtype = getDataType(table);
		String[] nullable = getNullable(table);
		String[] unique = getUnique(table);
		
		int numOfPages = pages(file);
		int pages=1;
		for(int p = 1; p <= numOfPages; p++){
			int rm = BPlusTree.getRightMost(file, p);
			if(rm == 0)
				pages = p;
		}
		
		int[] keys = BPlusTree.getKeyArray(file, pages);
		int l = 0;
		for(int i = 0; i < keys.length; i++)
			if(keys[i]>l)
				l = keys[i];
		
		if (values[0].isEmpty())
			values[0]=String.valueOf(l+1);

		for(int i = 0; i < nullable.length; i++)
			if(values[i].equals("null") && nullable[i].equals("NO")){
				System.out.println("NULL-value constraint violation");
				System.out.println();
				return;
			}
		
		for(int i = 0; i < unique.length; i++)
			if(unique[i].equals("YES")){
				System.out.println("Checking for unique constraint violation");
				System.out.println();
				String path = dir_userdata ;
				
				try {
				
				RandomAccessFile uni = new RandomAccessFile(path+table+".tbl", "rw");
				
				String[] columnName = getColName(table);
				String[] type = getDataType(table);
				
				Records records = new Records();
				
				String[] cmp = {columnName[i],"=",values[i]};
				
				filter(uni, cmp, columnName, type, records);
				
				if (records.num_row>0)
				{
					System.out.println("Duplicate key found for "+columnName[i].toString());
					
					System.out.println();
					return;
				}
				 
				uni.close();
				
				}catch (Exception e)
				{
					System.out.println(e);
				}
				
			}

		int key = new Integer(values[0]);
		int page = searchKeyPage(file, key);
		if(page != 0)
			if(BPlusTree.hasKey(file, page, key)){
				System.out.println("Uniqueness constraint violation");
				return;
			}
		if(page == 0)
			page = 1;
		
		byte[] stc = new byte[dtype.length-1];
		short plSize = (short) calPayloadSize(table, values, stc);
		int cellSize = plSize + 6;
		int offset = BPlusTree.checkLeafSpace(file, page, cellSize);
		
		if(offset != -1){
			BPlusTree.insertLeafCell(file, page, offset, plSize, key, stc, values);

		}else{
			BPlusTree.splitLeaf(file, page);
			insertInto(file, table, values);
		}
	}

	public static int calPayloadSize(String table, String[] vals, byte[] stc){
		String[] dataType = getDataType(table);
		int size =dataType.length;
		for(int i = 1; i < dataType.length; i++){
			stc[i - 1]= getStc(vals[i], dataType[i]);
			size = size + feildLength(stc[i - 1]);
		}
		return size;
	}
	

	public static byte getStc(String value, String dataType){
		if(value.equals("null")){
			switch(dataType){
				case "TINYINT":     return 0x00;
				case "SMALLINT":    return 0x01;
				case "INT":			return 0x02;
				case "BIGINT":      return 0x03;
				case "REAL":        return 0x02;
				case "DOUBLE":      return 0x03;
				case "DATETIME":    return 0x03;
				case "DATE":        return 0x03;
				case "TEXT":        return 0x03;
				default:			return 0x00;
			}							
		}else{
			switch(dataType){
				case "TINYINT":     return 0x04;
				case "SMALLINT":    return 0x05;
				case "INT":			return 0x06;
				case "BIGINT":      return 0x07;
				case "REAL":        return 0x08;
				case "DOUBLE":      return 0x09;
				case "DATETIME":    return 0x0A;
				case "DATE":        return 0x0B;
				case "TEXT":        return (byte)(value.length()+0x0C);
				default:			return 0x00;
			}
		}
	}
	

    public static short feildLength(byte stc){
		switch(stc){
			case 0x00: return 1;
			case 0x01: return 2;
			case 0x02: return 4;
			case 0x03: return 8;
			case 0x04: return 1;
			case 0x05: return 2;
			case 0x06: return 4;
			case 0x07: return 8;
			case 0x08: return 4;
			case 0x09: return 8;
			case 0x0A: return 8;
			case 0x0B: return 8;
			default:   return (short)(stc - 0x0C);
		}
	}


	
public static int searchKeyPage(RandomAccessFile file, int key){
		int val = 1;
		try{
			int numPages = pages(file);
			for(int page = 1; page <= numPages; page++){
				file.seek((page - 1)*pageSize);
				byte pageType = file.readByte();
				if(pageType == 0x0D){
					int[] keys = BPlusTree.getKeyArray(file, page);
					if(keys.length == 0)
						return 0;
					int rm = BPlusTree.getRightMost(file, page);
					if(keys[0] <= key && key <= keys[keys.length - 1]){
						return page;
					}else if(rm == 0 && keys[keys.length - 1] < key){
						return page;
					}
				}
			}
		}catch(Exception e){
			System.out.println(e);
		}

		return val;
	}


	
	public static String[] getDataType(String table){
		String[] dataType = new String[0];
		try{
			RandomAccessFile file = new RandomAccessFile(dir_catalog+"davisbase_columns.tbl", "rw");
			Records records = new Records();
			String[] columnName = {"rowid", "table_name", "column_name", "data_type", "ordinal_position", "is_nullable","is_unique"};
			String[] cmp = {"table_name","=",table};
			filter(file, cmp, columnName, records);
			HashMap<Integer, String[]> content = records.content;
			ArrayList<String> array = new ArrayList<String>();
			for(String[] x : content.values()){
				array.add(x[3]);
			}
			int size=array.size();
			dataType = array.toArray(new String[size]);
			file.close();
			return dataType;
		}catch(Exception e){
			System.out.println(e);
		}
		return dataType;
	}

	public static String[] getColName(String table){
		String[] cols = new String[0];
		try{
			RandomAccessFile file = new RandomAccessFile(dir_catalog+"davisbase_columns.tbl", "rw");
			Records records = new Records();
			String[] columnName = {"rowid", "table_name", "column_name", "data_type", "ordinal_position", "is_nullable","is_unique"};
			String[] cmp = {"table_name","=",table};
			filter(file, cmp, columnName, records);
			HashMap<Integer, String[]> content = records.content;
			ArrayList<String> array = new ArrayList<String>();
			for(String[] i : content.values()){
				array.add(i[2]);
			}
			int size=array.size();
			cols = array.toArray(new String[size]);
			file.close();
			return cols;
		}catch(Exception e){
			System.out.println(e);
		}
		return cols;
	}

	public static String[] getNullable(String table){
		String[] nullable = new String[0];
		try{
			RandomAccessFile file = new RandomAccessFile(dir_catalog+"davisbase_columns.tbl", "rw");
			Records records = new Records();
			String[] columnName = {"rowid", "table_name", "column_name", "data_type", "ordinal_position", "is_nullable"};
			String[] cmp = {"table_name","=",table};
			filter(file, cmp, columnName, records);
			HashMap<Integer, String[]> content = records.content;
			ArrayList<String> array = new ArrayList<String>();
			for(String[] i : content.values()){
				array.add(i[5]);
			}
			int size=array.size();
			nullable = array.toArray(new String[size]);
			file.close();
			return nullable;
		}catch(Exception e){
			System.out.println(e);
		}
		return nullable;
	}
	
	public static String[] getUnique(String table){
		String[] unique = new String[0];
		try{
			RandomAccessFile file = new RandomAccessFile(dir_catalog+"davisbase_columns.tbl", "rw");
			Records records = new Records();
			String[] columnName = {"rowid", "table_name", "column_name", "data_type", "ordinal_position", "is_nullable", "is_unique"};
			String[] cmp = {"table_name","=",table};
			filter(file, cmp, columnName, records);
			HashMap<Integer, String[]> content = records.content;
			ArrayList<String> array = new ArrayList<String>();
			for(String[] i : content.values()){
				array.add(i[6]);
			}
			int size=array.size();
			unique = array.toArray(new String[size]);
			file.close();
			return unique;
		}catch(Exception e){
			System.out.println(e);
		}
		return unique;
	}

	public static Records select(String table, String[] cols, String[] cmp, boolean display){
		try{
			String path = dir_userdata ;
			if (table.equalsIgnoreCase("davisbase_tables") || table.equalsIgnoreCase("davisbase_columns"))
				path = dir_catalog ;
			
			RandomAccessFile file = new RandomAccessFile(path+table+".tbl", "rw");
			String[] columnName = getColName(table);
			String[] type = getDataType(table);
			
			Records records = new Records();
			
			if (cmp.length > 0 && cmp[1].equals("=") && cmp[2].equalsIgnoreCase("null")) 
			{
				System.out.println("Empty Set");
				return null;
			}
			
			if (cmp.length > 0 && cmp[1].equals("!=") && cmp[2].equalsIgnoreCase("null")) 
			{
				cmp = new String[0];
			}
			
			filter(file, cmp, columnName, type, records);
			if(display) records.display(cols); 
			file.close();
			return records;
		}catch(Exception e){
			System.out.println(e);
			return null;
		}
	}


	public static void filter(RandomAccessFile file, String[] cmp, String[] columnName, String[] type, Records records){
		try{
			
			int numOfPages = pages(file);
			
			for(int page = 1; page <= numOfPages; page++){
				
				file.seek((page-1)*pageSize);
				byte pageType = file.readByte();
				
					if(pageType == 0x0D){
						
					byte numOfCells = BPlusTree.getCellNumber(file, page);

					 for(int i=0; i < numOfCells; i++){
						long loc = BPlusTree.getCellLoc(file, page, i);
						String[] vals = retrieveValues(file, loc);
						int rowid=Integer.parseInt(vals[0]);
						
						for(int j=0; j < type.length; j++)
							if(type[j].equals("DATE") || type[j].equals("DATETIME"))
								vals[j] = "'"+vals[j]+"'";
						
						boolean check = cmpCheck(vals, rowid , cmp, columnName);

						
						for(int j=0; j < type.length; j++)
							if(type[j].equals("DATE") || type[j].equals("DATETIME"))
								vals[j] = vals[j].substring(1, vals[j].length()-1);

						if(check)
							records.add(rowid, vals);
					 }
				   }
				    else
						continue;
			}

			records.columnName = columnName;
			records.format = new int[columnName.length];

		}catch(Exception e){
			System.out.println("Error at filter");
			e.printStackTrace();
		}

	}


	public static void filter(RandomAccessFile file, String[] cmp, String[] columnName, Records records){
		try{
			
			int numOfPages = pages(file);
			for(int page = 1; page <= numOfPages; page++){
				
				file.seek((page-1)*pageSize);
				byte pageType = file.readByte();
				if(pageType == 0x0D)
				{
					byte numOfCells = BPlusTree.getCellNumber(file, page);

					for(int i=0; i < numOfCells; i++){
						
						long loc = BPlusTree.getCellLoc(file, page, i);	
						String[] vals = retrieveValues(file, loc);
						int rowid=Integer.parseInt(vals[0]);

						boolean check = cmpCheck(vals, rowid, cmp, columnName);
						
						if(check)
							records.add(rowid, vals);
					}
				}
				else
					continue;
			}

			records.columnName = columnName;
			records.format = new int[columnName.length];

		}catch(Exception e){
			System.out.println("Error at filter");
			e.printStackTrace();
		}

	}


	
	public static boolean cmpCheck(String[] values, int rowid, String[] cmp, String[] columnName){

		boolean check = false;
		
		if(cmp.length == 0){
			check = true;
		}
		else{
			int colPos = 1;
			for(int i = 0; i < columnName.length; i++){
				if(columnName[i].equals(cmp[0])){
					colPos = i + 1;
					break;
				}
			}
			
			if(colPos == 1){
				int val = Integer.parseInt(cmp[2]);
				String operator = cmp[1];
				switch(operator){
					case "=": if(rowid == val) 
								check = true;
							  else
							  	check = false;
							  break;
					case ">": if(rowid > val) 
								check = true;
							  else
							  	check = false;
							  break;
					case ">=": if(rowid >= val) 
						        check = true;
					          else
					  	        check = false;	
					          break;
					case "<": if(rowid < val) 
								check = true;
							  else
							  	check = false;
							  break;
					case "<=": if(rowid <= val) 
								check = true;
							  else
							  	check = false;	
							  break;
					case "!=": if(rowid != val)  
								check = true;
							  else
							  	check = false;	
							  break;						  							  							  							
				}
			}else{
				if(cmp[2].equals(values[colPos-1]))
					check = true;
				else
					check = false;
			}
		}
		return check;
	}
	
	public static void createIndex(String table, String[] cols){
		try{
			String path = dir_userdata ;
			
			RandomAccessFile file = new RandomAccessFile(path+table+".tbl", "rw");
			String[] columnName = getColName(table);
			String[] type = getDataType(table);
			String [] cmp = new String[0];
			HashMap<String, String> content = new HashMap<String, String>();
			
			Records records = new Records();
			
			BTree b = new BTree(new RandomAccessFile(path+table+".idx", "rw"));
			
			int control=0; // = new int[cols.length];
			for(int j = 0; j < cols.length; j++)
				for(int i = 0; i < columnName.length; i++)
					if(cols[j].equals(columnName[i]))
						control = i;
			
			//filter(file, cmp, columnName, type, buffer);
			
			try{
				
				int numOfPages = pages(file);
				for(int page = 1; page <= numOfPages; page++){
					
					file.seek((page-1)*pageSize);
					byte pageType = file.readByte();
					if(pageType == 0x0D)
					{
						byte numOfCells = BPlusTree.getCellNumber(file, page);

						for(int i=0; i < numOfCells; i++){
							
							long loc = BPlusTree.getCellLoc(file, page, i);	
							String[] vals = retrieveValues(file, loc);
							int rowid=Integer.parseInt(vals[0]);
							

//							content.put(String.valueOf(vals[control]), String.valueOf(loc));
							b.add(String.valueOf(vals[control]), String.format("%04x",loc));
							
							//String.valueOf(Long.toHexString(0xFFFF &loc))
						}
					}
					else
						continue;
				}

//				buffer.columnName = columnName;
//				buffer.format = new int[columnName.length];

			}catch(Exception e){
				System.out.println("Error at indexing");
				e.printStackTrace();
			}
			
			
//			for(String[] i : buffer.content.values()){
//				
//				try {
//					
//				b.add(i[0],i[1]);
//				
//				}
//				catch (Exception e)
//				{ System.out.println(e);
//				
//				}
//				
//				System.out.println("Added");
//			}
			
			file.close();
			
		}catch(Exception e){
			System.out.println(e);
		}
	}
	
}


