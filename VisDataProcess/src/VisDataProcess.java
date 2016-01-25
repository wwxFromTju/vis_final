import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;


public class VisDataProcess {
	public static String problem[] = {"LEAK", "LOW", "NEIGHBOR", "AGAIN"};
	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException{
		Class.forName("com.mysql.jdbc.Driver");
		System.out.println("加载成功");
		//allHere();
		separate();
		//deleteErrorPosition();
		//setRegion();
		//total2();
	}
	
	public static Connection getConnection() throws SQLException{
		return DriverManager.getConnection("jdbc:mysql://localhost:3306/heating", "root", null);
	}
	
	
	public static void setRegion() throws SQLException{
		Connection conn1 = getConnection();
		Connection conn2 = getConnection();
		System.out.println("连接成功");
		String str1 = "select workformid, standardaddress from workformok";
		ResultSet rs = conn1.createStatement().executeQuery(str1);
		while(rs.next()){
			String workformid = rs.getString(1);
			String standardaddress = rs.getString(2);
			String region = standardaddress.substring(0,3);
			String parameter = "";
			String str2 = "update workformok set region= ? where workformid=?";
			if(region.equals("南开区")){
				parameter = "1";
			}
			else if(region.equals("红桥区")){
				parameter = "2";
			}
			else if(region.equals("河西区")){
				parameter = "3";
			}
			else if(region.equals("河东区")){
				parameter = "4";
			}
			else if(region.equals("河北区")){
				parameter = "5";
			}
			else{
				parameter = "6";
			}
			PreparedStatement ps = conn2.prepareStatement(str2);
			ps.setString(1,parameter);
			ps.setString(2, workformid);
			ps.executeUpdate();
		}
		conn2.close();
		conn1.close();
		System.out.println("region: the end");
	}
	public static void total2() throws SQLException, IOException{
		File file = new File("total2.json");
		Connection conn = getConnection();
		System.out.println("连接成功");
		OutputStreamWriter ow = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
		ow.write("[\r\n");
		for(int k = 0; k < 4; k++){
			ow.write("{problem:\"" + problem[k] + "\",sum:[\r\n");
			for(int i = 0; i <= 2; i++){
				for(int j = 1; j <= 12; j++){
					String time = "1" + i;
					if(j < 10){
						time = time + "0";
					}
					time = time + j;
					int t = fortotal2(problem[k], "20" + time, conn);
					//System.out.println(t);
					ow.write("[" + (i*12+j) + "," + t + "]");
					if(i==2&&j==12)
						ow.write("\r\n");
					else
						ow.write(",");
				}
			}
			ow.write("]}");
			if(k < 3)
				ow.write(",");
			ow.write("\r\n");
				
		}
		ow.write("]");
		conn.close();
		ow.close();
		
	}
	
	public static int fortotal2(String str, String time, Connection conn) throws SQLException{
		String sql = "select count(*) from workformok where " + str +"=1 and workformid like \"" + time +"%\"";
		System.out.println(str);
		System.out.println(time);
		System.out.println(sql);
		ResultSet rs = conn.createStatement().executeQuery(sql);
		rs.next();
		int total = rs.getInt(1);
		rs.close();
		return total;
		
		
	}
	public static void allHere() throws SQLException, IOException{
		File file = new File("vis.json");
		String all = "select * from workformok";
		Connection conn = getConnection();
		System.out.println("连接成功");
		ResultSet rs = conn.createStatement().executeQuery(all);
		 OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(file),"UTF-8"); 
		fw.write("{\r\n\"data\":[\r\n");
		String WORKFORMID = null;
		Timestamp ACCEPTTIME = null;
		String EVENTCAUSE = null;
		String STANDARDADDRESS = null;
		Float LNG;
		Float LAT;
		
		boolean LEAK = false;
		boolean LOW = false;
		boolean NEIGHBOR = false;
		boolean AGAIN = false;
		while(rs.next())
		{
			EVENTCAUSE = rs.getString(3);
			LEAK = (EVENTCAUSE.indexOf("水") > -1 ||
					EVENTCAUSE.indexOf("漏") > -1 ||
					EVENTCAUSE.indexOf("气") > -1 ||
					EVENTCAUSE.indexOf("汽") > -1 ||
					EVENTCAUSE.indexOf("冒") > -1 ) ? true : false;
			
			LOW = (EVENTCAUSE.indexOf("度") > -1 ||
					EVENTCAUSE.indexOf("低") > -1 ||
					EVENTCAUSE.indexOf("温") > -1 ||
					EVENTCAUSE.indexOf("温热") > -1 ||
					EVENTCAUSE.indexOf("不热") > -1 ||
					EVENTCAUSE.indexOf("冰") > -1 ||
					EVENTCAUSE.indexOf("冷") > -1) ? true : false;
			NEIGHBOR = (EVENTCAUSE.indexOf("邻居") > -1) ? true : false;
			int pos = EVENTCAUSE.indexOf("同20");
			if(pos > -1){
				pos++;
				AGAIN = true;
				String number = EVENTCAUSE.substring(pos, EVENTCAUSE.indexOf("同20")+15);
				String s1 = "select * from workformok where WORKFORMID = " + number;
				ResultSet rs1 = conn.createStatement().executeQuery(s1);
				if(rs1.next()){
					if(rs1.getInt(7) == 1)
						LEAK = true;
					if(rs1.getInt(8) == 1)
						LOW = true;
					if(rs1.getInt(9) == 1)
						NEIGHBOR = true;	
				}
			}
			else
				AGAIN = false;
			if(LEAK || LOW || NEIGHBOR || AGAIN){
				WORKFORMID = rs.getString(1);
				ACCEPTTIME = rs.getTimestamp(2);
				STANDARDADDRESS = rs.getString(4);
				LNG = rs.getFloat(5);
				LAT = rs.getFloat(6);
				
				int a = LEAK? 1:0, b = LOW? 1:0, c = NEIGHBOR? 1:0, d = AGAIN? 1:0;
				String s = "update workformok set LEAK = ?, LOW = ?, NEIGHBOR = ?, AGAIN = ? where WORKFORMOK.WORKFORMID = ?";
				PreparedStatement ps = conn.prepareStatement(s);
				ps.setInt(1, a);
				ps.setInt(2, b);
				ps.setInt(3, c);
				ps.setInt(4, d);
				ps.setString(5, WORKFORMID);
				ps.executeUpdate();
				
				
				s =  "{" + 
						    "\"WORKFORMID\":\"" + WORKFORMID + "\"," +
							"\"ACCEPTTIME\":\"" + ACCEPTTIME +"\"," +
							"\"STANDARDADDRESS\":\"" + STANDARDADDRESS + "\"," +
							"\"LNG\":\"" + LNG + "\"," + 
							"\"LAT\":\"" + LAT + "\"," + 
							"\"LEAK\":" + (LEAK ? 1 : 0) + "," +
							"\"LOW\":" + (LOW ? 1 : 0) + "," + 
							"\"NEIGHBOR\":" + (NEIGHBOR ? 1 : 0) + "," +
							"\"AGAIN\":" + (AGAIN ? 1 : 0) +  
					  "},\r\n";
				fw.write(s);
			}
		}
		fw.write("]}");	
		fw.close();
		System.out.println("The End! (1)");
		
	}
	
	public static void deleteErrorPosition() throws SQLException{
		String sql = "select workformid, standardaddress, lng, lat from workformok";
		Connection conn = getConnection();
		Connection conn1 = getConnection();
		Connection conn2 = getConnection();	
		System.out.println("连接成功");
		ResultSet rs = conn.createStatement().executeQuery(sql);
		int replace=0, delete = 0;
		while(rs.next()){
			
			String workformid = rs.getString(1);
			Float lng  = rs.getFloat(3);
			Float lat = rs.getFloat(4);
		
			String standardaddress = rs.getString(2);
			String regionFromDatabase = standardaddress.substring(0,3);
			String regionFromBaiDu_gps = Position.LToP(lng, lat, false);
			String regionFromBaiDu_baidu = Position.LToP(lng, lat, true);
			boolean in1 = inSixRegion(regionFromBaiDu_gps);
			boolean in2 = inSixRegion(regionFromBaiDu_baidu);
			if(in1 || in2){
//				if(!regionFromDatabase.equals(regionFromBaiDu_gps) && !regionFromDatabase.equals(regionFromBaiDu_baidu)){//up
//					String newAddress = "";
//					if(in1)newAddress = regionFromBaiDu_gps + standardaddress.substring(3);
//					else newAddress = regionFromBaiDu_baidu + standardaddress.substring(3);
//					String str = "update workformok set standardaddress=\"" + newAddress + "\" where workformid=" + workformid;
//					//conn1.createStatement().execute(str);
//					System.out.println(workformid + " " + regionFromBaiDu_gps + " " + regionFromDatabase);
//					replace++;
//				}
			}
			else{//delete data
				String str = "delete from workformok where workformid=" + workformid;
						
				conn2.createStatement().execute(str);
				System.out.println(regionFromBaiDu_gps + " " + regionFromBaiDu_baidu);
				delete++;
				System.out.println("delete " + delete);
//				System.out.println(workformid+ " " + regionFromBaiDu_gps + " " + regionFromDatabase);
			}
			
			
		}
		System.out.println(delete);
		conn.close();
		conn1.close();
		conn2.close();
		System.out.println("delete error position: the end");
	}
	public static boolean inSixRegion(String str){
		if(str.equals("和平区")||
				str.equals("河东区")||
				str.equals("南开区")||
				str.equals("河西区")||
				str.equals("河北区")||
				str.equals("红桥区")){
			return true;
		}
		return false;
	}
	//
	public static void separate() throws SQLException, IOException{
		File file = new File("separate.json");
		OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(file),"UTF-8");
		fw.write("{\r\n");
		
		query("LEAK", fw);
		fw.write(",\r\n");
		
		query("LOW", fw);
		fw.write(",\r\n");
		
		query("NEIGHBOR", fw);
		fw.write(",\r\n");
		
		query("AGAIN", fw);
		fw.write("\r\n}");
		fw.close();
		System.out.println("The End! (2)");
		
	}
	
	public static void query(String str, OutputStreamWriter fw) throws SQLException, IOException{
		String WORKFORMID = null;
		Timestamp ACCEPTTIME = null;
		String STANDARDADDRESS = null;
		Float LNG;
		Float LAT;
		Connection conn = getConnection();
		String s = "";
		
		String s1 = "select count(*) ct from workformok where " + str + "= 1";
		String s2 = "select * from workformok where " + str + "= 1";
		ResultSet rs = conn.createStatement().executeQuery(s1);
		rs.next();
		int total = rs.getInt("ct");
		rs = conn.createStatement().executeQuery(s2);
		fw.write("\"" + str + "\":[\r\n");
		int ct = 0;
		while(rs.next()){
			WORKFORMID = rs.getString(1);
			ACCEPTTIME = rs.getTimestamp(2);
			STANDARDADDRESS = rs.getString(4);
			LNG = rs.getFloat(5);
			LAT = rs.getFloat(6);
			
			s =  "{" + 
				    "\"WORKFORMID\":\"" + WORKFORMID + "\"," +
					"\"ACCEPTTIME\":\"" + ACCEPTTIME +"\"," +
					"\"STANDARDADDRESS\":\"" + STANDARDADDRESS + "\"," +
					"\"LNG\":\"" + LNG + "\"," + 
					"\"LAT\":\"" + LAT + "\"" + 
			     "}";
			ct++;
			if(ct != total){
				s += ",";
			}
			s += "\r\n";
			fw.write(s);
		}
		fw.write("]");
	}
}
