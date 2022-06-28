/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rocks.gravili.notquests.paper.managers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;

public class WebManager {
  private final NotQuests main;
  Gson gson = new Gson();

  public WebManager(final NotQuests main) {
    this.main = main;

    /*Assert.assertTrue(jsonObject.isJsonObject());
    Assert.assertTrue(jsonObject.get("name").getAsString().equals("Baeldung"));
    Assert.assertTrue(jsonObject.get("java").getAsBoolean() == true);*/

  }

  String convertYamlToJson(String yaml) throws JsonProcessingException {
    ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
    Object obj = yamlReader.readValue(yaml, Object.class);

    ObjectMapper jsonWriter = new ObjectMapper();
    return jsonWriter.writeValueAsString(obj);
  }

  public String sendRequest(JsonObject jsonObject) throws IOException {
    // String       postURL       = "editor.notquests.com/webeditornew";// put in your url
    String postURL = "https://editor.notquests.com/api/webeditors";
    HttpClient httpClient = HttpClientBuilder.create().build();
    HttpPost post = new HttpPost(postURL);
    StringEntity postingString =
        new StringEntity(gson.toJson(jsonObject)); // gson.tojson() converts your pojo to json
    post.setEntity(postingString);
    post.setHeader("Content-type", "application/json");

    HttpResponse result = httpClient.execute(post);

    return EntityUtils.toString(result.getEntity(), "UTF-8");

    // main.getLogManager().info("Response: " + response.toString());
  }

  public String openEditor() {
    JsonObject jsonObject = toJson();

    try {
      String jsonReturnObject = sendRequest(jsonObject);
      main.getLogManager().info("JSON returned: \n" + jsonReturnObject);
      return jsonReturnObject;
      // main.getLogManager().info("<success>Sent web request!");
    } catch (IOException e) {
      main.getLogManager().warn("Cannot send web request!");
      e.printStackTrace();
    }

    return "Cannot send web request!";
  }

  public JsonObject toJson() {
    /*String jsonDefaultCategoryQuests = "{}";

    try{
        jsonDefaultCategoryQuests = convertYamlToJson(main.getDataManager().getDefaultCategory().getQuestsConfig().saveToString());
    }catch (JsonProcessingException e){
        main.getLogManager().warn("Cannot convert Quests YAML to web-ready JSON.");
        e.printStackTrace();
    }

    main.getLogManager().info("JSON to send: \n" + jsonDefaultCategoryQuests);*/

    JsonObject categories = new JsonObject();

    try {
      for (Category category : main.getDataManager().getCategories()) {
        JsonObject categoryObject = new JsonObject();

        String ymlQuestsString = category.getQuestsConfig().saveToString();
        String ymlActionsString = category.getActionsConfig().saveToString();
        String ymlConditionsString = category.getConditionsConfig().saveToString();

        if (!ymlQuestsString.isBlank()) {
          String jsonQuestsString = convertYamlToJson(ymlQuestsString);
          categoryObject.add("Quests", JsonParser.parseString(jsonQuestsString));
        }
        if (!ymlActionsString.isBlank()) {
          String jsonActionsString = convertYamlToJson(ymlActionsString);
          categoryObject.add("Actions", JsonParser.parseString(jsonActionsString));
        }
        if (!ymlConditionsString.isBlank()) {
          String jsonConditionsString = convertYamlToJson(ymlConditionsString);
          categoryObject.add("Conditions", JsonParser.parseString(jsonConditionsString));
        }

        categories.add(category.getCategoryName(), categoryObject);
      }
    } catch (Exception e) {
      main.getLogManager().warn("Cannot convert Quests YAML to web-ready JSON.");
      e.printStackTrace();
    }

    return categories;
  }
}
