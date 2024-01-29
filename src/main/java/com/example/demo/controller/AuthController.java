package com.example.demo.controller;

import com.example.demo.exceptions.GlobalExceptionHandler;
import com.example.demo.service.AuthorizeService;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

@RestController
public class AuthController {

    final static String TOKEN = "/token";
    final static String TOKEN_VALIDATE = "/validate";

    @Autowired
    AuthorizeService authorizeService;

    @RequestMapping(method = RequestMethod.GET, value = TOKEN)
    ResponseEntity getToken() {
        String token;
        try {
            token = authorizeService.generateToken();
            JSONObject obj = new JSONObject();
            obj.put("token", token);
            GlobalExceptionHandler r = new GlobalExceptionHandler("Token successfully created!!", "", 201, obj);
            return new ResponseEntity<>(obj, HttpStatus.CREATED);
        } catch (Exception e) {
            GlobalExceptionHandler r = new GlobalExceptionHandler("Bad Request", "", 404, new ArrayList<>());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    @RequestMapping(method = RequestMethod.POST, value = TOKEN_VALIDATE)
    ResponseEntity validateToken(@RequestHeader(value = "authorization") String token) {

        try {
            String isToken = authorizeService.authorize(token);
            GlobalExceptionHandler r;
            if (isToken.equals("VALID_TOKEN")) {
                r = new GlobalExceptionHandler("Token verified!!", "", 200, true);
            } else {
                r = new GlobalExceptionHandler(isToken, "", 200, false);
            }
            return new ResponseEntity<>(r, HttpStatus.OK);
        } catch (Exception e) {
            GlobalExceptionHandler r = new GlobalExceptionHandler("Bad Request", "", 404, new ArrayList<>());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
