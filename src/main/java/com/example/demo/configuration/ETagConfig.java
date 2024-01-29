package com.example.demo.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.servlet.Filter;

@EnableWebMvc
@Configuration
public class ETagConfig {

    @Bean
    public Filter etagFilter() {
        return new ShallowEtagHeaderFilter();
    }
}