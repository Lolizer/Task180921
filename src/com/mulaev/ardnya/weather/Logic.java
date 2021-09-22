package com.mulaev.ardnya.weather;

import java.lang.Exception;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


class Logic {
	private String city;
	private String appId;
	private String country;
	private int days;
	private String url = "https://api.openweathermap.org/data/2.5/onecall?lat=%1$s&lon=%2$s&exclude=current,minutely,hourly&appid=%3$s&units=metric";
	private String geocode = "https://nominatim.geocoding.ai/search.php?q=%1$s&countrycodes=%2$s";
	
	Logic(String city, String appId, int days) {
		this("ru", city, appId,days);
	}
	
	Logic(String country, String city, String appId, int days) {
		init(country, city, appId, days);
	}
	
	Logic() {
		
	}
	
	void doAction(String country, String city, String appId, int days) throws Exception {
		init(country, city, appId, days);
		doAction();
	}
	
	void doAction(String city, String appId, int days) throws Exception {
		doAction("ru", city, appId, days);
	}
	
	void doAction() throws Exception {
		if (city.isBlank() || appId.isBlank() || days <= 0 || days > 6)
			throw new Exception("Check if all required variables are set properly!");
		
		doRequest();
	}
	
	void setCity (String cityName) {
		this.city = cityName;
	}
	
	void setAppId (String appId) {
		this.appId = appId;
	}
	
	void setDays (int days) {
		this.days = days;
	}
	
	void setCountry (String country) {
		this.country = country;
	}
	
	private void init(String country, String city, String appId, int days) {
		if (city.contains(" "))
			city = city.replaceAll(" ", "%20");
		setCity(city);
		setAppId(appId);
		setDays(days);
		setCountry(country);
	}
	
	private void doRequest() throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request;
		HttpRequest lonlatjson = HttpRequest.newBuilder(URI.create(String.format(geocode,city, country))).build();
		float[] lonlat;
		
		JsonObject minCGap = null;
		JsonObject fiveDaysMaxD = null;
		int fiveDaysCnt = 0;
		
		lonlat = getLonLat(client, lonlatjson);
		
		request = HttpRequest.newBuilder(URI.create(String.format(url,lonlat[1],lonlat[0],appId))).build();
		
		HttpResponse response = client.send(request, BodyHandlers.ofString());

		JsonObject ob = new JsonParser().parse(response.body().toString()).getAsJsonObject();
		JsonArray allDates = ob.get("daily").getAsJsonArray();
		
		for (JsonElement element : allDates) {
			JsonObject day = element.getAsJsonObject();
			
			if (fiveDaysMaxD == null || fiveDaysCnt < days && getDayDuration(fiveDaysMaxD) < getDayDuration(day))
				fiveDaysMaxD = day;
			
			if (minCGap == null || getCGap(day) < getCGap(minCGap))
				minCGap = day;
			
			fiveDaysCnt++;
		}
		
		
		Date minCGapD = new Date(minCGap.get("dt").getAsLong() * 1000L);
		Date maxDurationDay = new Date(fiveDaysMaxD.get("dt").getAsLong() * 1000L);
		
		System.out.printf("Данная информация актуальна для широты %1f и долготы %2f (Запрашиваемый город (страна): %3s (%s))\n", 
				lonlat[1], lonlat[0], city.replaceAll("%20", " "), country);
		System.out.printf(
				"1. День с минимальной разницей \"ощутимой\" и фактической температуры ночью за 7 дней (ограничение бесплатного API): %1s\nРазница температур в этот день составляет %2.1f C.\n",
				getLocalDateFormat(minCGapD), getCGap(minCGap));
		
		System.out.printf("2. Максимальная продолжительность свотового дня за ближайшие %1d дней составит %2s %3s.", 
				days, getHoursAndMinutesFromF(getDayDuration(fiveDaysMaxD)), getLocalDateFormat(maxDurationDay));
		}
	
	private String getHoursAndMinutesFromF(float hoursMins) {
		int hours = (int) hoursMins;
		int mins = (int) (60 * (hoursMins - hours));
		
		return String.format("%1$d ч. %2$d мин.", hours, mins);
	}
	
	private String getLocalDateFormat(Date date) {
		LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		return localDate.format(DateTimeFormatter.ofPattern("dd.MM.YYYY"));
	}
	
	private float getCGap(JsonObject day) {
		return Math.abs(day.get("temp").getAsJsonObject().get("night").getAsFloat() -
				day.get("feels_like").getAsJsonObject().get("night").getAsFloat());
	}

	private float getDayDuration(JsonObject day) {
		return ((float) (day.get("sunset").getAsInt() - day.get("sunrise").getAsInt())/60)/60;
	}
	
	private float[] getLonLat(HttpClient client, HttpRequest request) throws Exception {
		HttpResponse response = client.send(request, BodyHandlers.ofString());
		JsonArray obj = new JsonParser().parse(response.body().toString()).getAsJsonArray();
		JsonObject jsonO = obj.get(0).getAsJsonObject();

		return new float[] {Float.valueOf(jsonO.get("lon").getAsFloat()), Float.valueOf(jsonO.get("lat").getAsFloat())};
	}
}
