import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;


public class Position {
//	public static void main(String[] args){
//		System.out.println(LToP(new Float(117.11599731445312),new Float(39.130287170410156)));
//		System.exit(0);
//	}
	
	//LNG and LAT to Position(百度经纬度坐标)
	public static String LToP(Float LNG, Float LAT, boolean type){
		String url = "http://api.map.baidu.com/geocoder/v2/";
		String ak = "ak=" + "Y8H7Bqiic9Zp0tIFSAV8yQk2";
		String callback = "&callback=" + "renderReverse";
		String coordtype = "";
		if(type){
			coordtype = "&coordtype=" + "wgs84ll";
		}
		String location = "&location=" + LAT +"," + LNG;
		String output = "&output=" + "json";
		String pois = "&pois=" + 0;
		String param = ak + location + coordtype + output + pois;
		//System.out.println(param);
		return sendGet(url, param);
	}
	
	public static String sendGet(String url, String param) {
		String str = null; 
        
        BufferedReader bufferedReader = null;
        try {
            String urlNameString = url + "?" + param;
            URL realUrl = new URL(urlNameString);
            // 打开和URL之间的连接
            URLConnection connection = realUrl.openConnection();
            
            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent",
                    "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.80 Safari/537.36");
            
            // 建立实际的连接
            connection.connect();
            if((((HttpURLConnection)connection).getResponseCode()) == 500){
            	Thread.sleep(5000);
            	return sendGet(url, param);
            }
            InputStream inputStream = connection.getInputStream();  
            //对应的字符编码转换  
            Reader reader = new InputStreamReader(inputStream, "UTF-8");  
            bufferedReader = new BufferedReader(reader);  
             
            StringBuffer sb = new StringBuffer();  
            while ((str = bufferedReader.readLine()) != null) {  
                sb.append(str);  
            } 
            reader.close();
            bufferedReader.close();
            str = sb.toString();
            int index = str.indexOf("address");
            str = str.substring(index + 13, index + 16);
            //System.out.println(str);
            
        } catch (Exception e) {
            System.out.println("发送GET请求出现异常！" + e);
            e.printStackTrace();
        }
        // 使用finally块来关闭输入流
        return str;
    }
}
