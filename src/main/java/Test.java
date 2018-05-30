
import org.json.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.*;

public class Test {

    private static final String  getAllGroupUrl = "http://schedule.sumdu.edu.ua/index/json?method=getGroups";
    public static List<String>  getGroupID ()throws Exception{

        File file = new File("group_name.txt");
        List<String> params = new LinkedList<String>();
        BufferedReader reader = new BufferedReader(new FileReader(file.getAbsoluteFile()));
        try {
            String s;
            while ((s = reader.readLine()) != null) {
                params.add(s);
            }
        } finally {
            if (reader != null)
                reader.close();
        }

        String groupsJSON = "";
        URL url = new URL(getAllGroupUrl);
        URLConnection urlConnection = url.openConnection();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(
                        urlConnection.getInputStream()));
        String inputLine;

        while ((inputLine = in.readLine()) != null)

            groupsJSON+=inputLine;

        in.close();

        JSONObject jObj = new JSONObject(groupsJSON);
        Iterator<?> keys = jObj.keySet().iterator();
        while(keys.hasNext()) {
            Object obj = keys.next();
            String tmpGroupName = jObj.get(obj.toString()).toString();
            System.out.print(obj+ " -> ");
            System.out.println(tmpGroupName);
            if (tmpGroupName.equals(params.get(0))) {
                params.add(obj.toString());
                return params;
            }
        }
     return null;
    }

    public static void main(String[] args) throws Exception{


        List<String> params = getGroupID();
        if (params != null) {
            System.out.println("Hello");
            String result = "";
            StringBuilder sb = new StringBuilder();
            sb.append("http://schedule.sumdu.edu.ua/index/json?method=getSchedules&id_grp=")
                    .append(params.get(3))
                    .append("&id_fio=0&id_aud=0&date_beg=").append(params.get(1))
                    .append("&date_end=").append(params.get(2));

            URL url = new URL(sb.toString());
            URLConnection urlConnection = url.openConnection();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                    		urlConnection.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null)

                result+=inputLine;

            in.close();
            result = result.substring(1, result.length()-1);
        //System.out.println(result);
            String[] strings = result.split("\\,\\{");
            List<String> lessons = Arrays.asList(strings);
            EventItem item = new EventItem();
            for (String s: lessons) {
            	s = (s.indexOf("{")==-1)?("{" + s):s;
            	JSONObject jObj = new JSONObject(s);
            	item.setDate(jObj.getString("DATE_REG"));
            	item.setDiscipline(jObj.getString("ABBR_DISC"));
            	item.setDay(jObj.getString("NAME_WDAY"));
            	item.setTime(jObj.getString("TIME_PAIR"));
            	item.setTeacher(jObj.getString("NAME_FIO"));
            	item.setClassroom(jObj.getString("NAME_AUD"));
            	item.setGroups(jObj.getString("NAME_GROUP"));
            	item.setStud(jObj.getString("NAME_STUD"));
                GoogleCalendarHelper.postEvent(item);

           /* System.out.println("DATE_REG: "+jObj.getString("DATE_REG"));
            System.out.println("ABBR_DISC: "+jObj.getString("ABBR_DISC"));
            System.out.println("NAME_WDAY: "+jObj.getString("NAME_WDAY"));
            System.out.println("TIME_PAIR: "+jObj.getString("TIME_PAIR"));
            System.out.println("NAME_FIO: "+jObj.getString("NAME_FIO"));
            System.out.println("NAME_AUD: "+jObj.getString("NAME_AUD"));
            System.out.println("NAME_GROUP: "+jObj.getString("NAME_GROUP"));
            System.out.println("NAME_STUD: "+jObj.getString("NAME_STUD"));
        	System.out.println("-------------------------");
         */
        }
        }
    }
}
