package com.example.demo.jsonSchemaBase;

import com.example.demo.controller.PlanController;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.InputStream;

@Service
public class BaseJsonSchemaValidator {


    final String JSON_PATH = "/model/plan.schema.json";


    public void getJsonSchemaFromJsonNode(JSONObject jsonObject) throws ValidationException {

        JSONObject jsonSchema = new JSONObject(new JSONTokener(PlanController.class.getResourceAsStream(JSON_PATH)));

        Schema schema = SchemaLoader.load(jsonSchema);
        schema.validate(jsonObject);
    }

    public boolean validateSchema(JSONObject data) throws FileNotFoundException {
        InputStream inputStream = getClass().getResourceAsStream(JSON_PATH);
        JSONObject schemaJson = new JSONObject(new JSONTokener(inputStream));
        Schema schema = SchemaLoader.load(schemaJson);
        try {
            schema.validate(data);
            return true;
        } catch (ValidationException e) {
            System.out.println(e.getErrorMessage());
        }
        return false;
    }
    
    public String getErrorMessage(JSONObject data) throws FileNotFoundException {
    	InputStream inputStream = getClass().getResourceAsStream(JSON_PATH);
        JSONObject schemaJson = new JSONObject(new JSONTokener(inputStream));
        Schema schema = SchemaLoader.load(schemaJson);
        StringBuilder sb = new StringBuilder();
        try {
            schema.validate(data);
        } catch (ValidationException e) {
            sb.append(e.getErrorMessage());
        }
        return sb.toString();
    }

}
