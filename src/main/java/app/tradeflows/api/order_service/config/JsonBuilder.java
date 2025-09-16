package app.tradeflows.api.order_service.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class JsonBuilder {

    public Gson gson(){
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter());
        gsonBuilder.registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter());
        return gsonBuilder.create();
    }
}
