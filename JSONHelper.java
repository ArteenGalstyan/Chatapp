
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class JSONHelper {

    public static String parse(String jsonString, String key) {
        JSONParser parser = new JSONParser();
        try {
            JSONObject jsonObject = (JSONObject) parser.parse(jsonString);
            return jsonObject.get(key).toString();
        } catch (ParseException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static JSONObject makeJson(Type type, String ip, int port,
            String message) {
        JSONObject jsonObject = makeJson(type, ip, port);
        jsonObject.put("message", message);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject makeJson(Type type, String ip, int port) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", type.name());
        jsonObject.put("ip", ip);
        jsonObject.put("port", port);
        return jsonObject;
    }
}
