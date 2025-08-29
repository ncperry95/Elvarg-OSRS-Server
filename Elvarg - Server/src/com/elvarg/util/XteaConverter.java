import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class XteaConverter {
    public static void main(String[] args) throws IOException {
        String jsonPath = "./Elvarg - Server/data/xtea/validated_keys.json";
        String outputDir = "./Elvarg - Server/data/xtea/";

        JsonParser parser = new JsonParser();
        JsonArray array = parser.parse(new java.io.FileReader(jsonPath)).getAsJsonArray();

        new File(outputDir).mkdirs();

        for (int i = 0; i < array.size(); i++) {
            JsonObject obj = array.get(i).getAsJsonObject();
            int regionId = obj.get("region_id").getAsInt();
            JsonArray keys = obj.get("keys").getAsJsonArray();
            FileWriter writer = new FileWriter(outputDir + regionId + ".txt");
            for (int k = 0; k < 4; k++) {
                writer.write(keys.get(k).getAsString() + "\n");
            }
            writer.close();
        }
    }
}