package com.nsqre.insquare.Square;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.Locale;

/**
 * SquareDeserializer is the class that Gson uses to deserialize the JSON strings that represent squares
 * @see com.nsqre.insquare.Activities.MapActivity
 * @see com.nsqre.insquare.Fragments.MapFragment
 */
public class SquareDeserializer implements JsonDeserializer<Square> {

    private static final String TAG = "SquareDeserializer";
    private Locale locale;

    public SquareDeserializer(Locale l)
    {
        this.locale = l;
    }

    /**
     * Manages the particular format of square's JSON representation, so it has sufficient data to instantiate a Square object
     * @param json
     * @param typeOfT
     * @param context
     * @return A Square object based on the data deserialized
     * @throws JsonParseException
     * @see Square
     */
    @Override
    public Square deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

        /*
       Input Example:
       ==============
       {
        "_index": "squares",
        "_type": "square",
        "_id": "56cc47e2ee9a60fe92ca47a2",
        "_score": null,
        "_source": {
          "name": "Prova",
          "description": "cose",
          "searchName": "Prova",
          "geo_loc": "41.566872185995614,12.440877668559551",
          "messages": [
            "56cc48d5ee9a60fe92ca47a4",
            "56d0651fda98e2a49986c2a5"...
          ],
          "ownerId": "56bf62fb01469357eaabb167",
          "views": 50,
          "favouredBy": 0,
          "state": "AWOKEN",
          "lastMessageDate": "2016-02-28T17:57:44.357Z"
        }
       ==============
         */

//        Log.d(TAG, "deserialize: " + json.toString());

        final JsonObject jsonObject = json.getAsJsonObject();
        final String id = jsonObject.get("_id").getAsString();
        final JsonObject source = jsonObject.get("_source").getAsJsonObject();
        final String name = source.get("name").getAsString();
        final String description;
        if(source.get("description") != null){
            description = source.get("description").getAsString();
        }
        else {
            description = "";
        }
        final String geoloc = source.get("geo_loc").getAsString();
        final String ownerid;
        if(source.get("ownerId") != null){
            ownerid = source.get("ownerId").getAsString();
        }
        else {
            ownerid = "";
        }
        final String favouredby = source.get("favouredBy").getAsString();
        final String views = source.get("views").getAsString();
        final String state = source.get("state").getAsString();
        String lmd = "";
        try {
            final String lastMessageDate = source.get("lastMessageDate").getAsString();
            lmd = lastMessageDate;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        final Square square = new Square(id, name, description, geoloc, ownerid, favouredby, views, state, lmd, this.locale);
        return square;
    }
}
