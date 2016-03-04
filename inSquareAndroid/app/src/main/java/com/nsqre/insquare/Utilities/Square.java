package com.nsqre.insquare.Utilities;/* Created by umbertosonnino on 6/2/16  */

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Square implements Serializable {

    private static final String TAG = "Square";
    private String id;
    private String name;
    private String description;
    private double lat, lon;
    private String type;
    private String ownerId;
    private long favouredBy, views;
    private SquareState squareState;
    private Calendar lastMessageDate;
    private String lastMessageDateString;
    private Locale myLocale;

    public Square(String id, String name, double lat, double lon, String type, String ownerId) {
        this.id = id;
        this.name = name;
        this.lat = lat;
        this.lon = lon;
        this.type = type;
        this.ownerId = ownerId;
    }

    public Square(String id, String name, String description, String geoloc, String ownerId, String favouredBy, String views, String state, String lastMessageDate, Locale l) {
        this.id = id;
        this.name = name;
        this.description = description;

        String[] parts = geoloc.split(",", 2);
        lat = Double.parseDouble(parts[0]);
        lon = Double.parseDouble(parts[1]);

        this.ownerId = ownerId;
        this.favouredBy = Long.parseLong(favouredBy);
        this.views = Long.parseLong(views);

        switch (state) {
            case "asleep":
                this.squareState = SquareState.asleep;
                break;
            case "awoken":
                this.squareState = SquareState.awoken;
                break;
            case "caffeinated":
                this.squareState = SquareState.caffeinated;
                break;
            default:
                break;
        }
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", l);
        try {
            Date d = df.parse(lastMessageDate);
            this.lastMessageDate = Calendar.getInstance();
            this.lastMessageDate.setTime(d);

            this.lastMessageDateString = df.format(d);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        this.myLocale = l;
//        Log.d(TAG, "NEWSQUARE:\n" + this.toString());
    }

    @Override
    public String toString() {
        return "Square{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", lat=" + lat +
                ", lon=" + lon +
                ", type='" + type + '\'' +
                ", ownerId='" + ownerId + '\'' +
                ", favouredBy=" + favouredBy +
                ", views=" + views +
                ", squareState=" + squareState +
                ", lastMessageDate=" + lastMessageDate +
                ", lastMessageDateString='" + lastMessageDateString + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        return this.name.equals(((Square)o).getName());
//        return this.id.equals(((Square)o).getId());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public long getFavouredBy() {
        return favouredBy;
    }

    public long getViews() {
        return views;
    }

    public SquareState getSquareState() {
        return squareState;
    }

    public String getLastMessageDateString() {
        return lastMessageDateString;
    }

    public Calendar getLastMessageDate() {
        return lastMessageDate;
    }

    public String formatTime()
    {
        String timetoShow = "";
        Calendar c = Calendar.getInstance();
        int tYear = c.get(Calendar.YEAR);
        int tDay = c.get(Calendar.DAY_OF_MONTH);

        Calendar msgCal = this.getLastMessageDate();
        int mYear = msgCal.get(Calendar.YEAR);
        int mDay = msgCal.get(Calendar.DAY_OF_MONTH);

        DateFormat df;
        if(mYear != tYear)
        {
            df = new SimpleDateFormat("MMM d, ''yy, HH:mm", this.myLocale);
        }else if(mDay != tDay)
        {
            df = new SimpleDateFormat("MMM d, HH:mm", this.myLocale);
        }else
        {
            df = new SimpleDateFormat("HH:mm", this.myLocale);
            timetoShow += "Oggi, ";
        }

        timetoShow += df.format(msgCal.getTime());

        return "Ultimo messaggio: " + timetoShow;
    }
}
