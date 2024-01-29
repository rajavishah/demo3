package com.example.demo.configuration;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackages = "*")
public class ElasticSearchConfiguration extends AbstractElasticsearchConfiguration {

//    @Override
//    @Bean
//    public RestHighLevelClient elasticsearchClient() {
//
//        final ClientConfiguration clientConfiguration = ClientConfiguration.builder()
//                .connectedTo("localhost:9200")
//                .build();
//
//        return RestClients.create(clientConfiguration).rest();
//    }

//	@Value("${spring.data.elasticsearch.host}")
//	private String host;
//
//	@Value("${spring.data.elasticsearch.port}")
//	private int port;
//
//	@Value("${spring.data.elasticsearch.protocol}")
//	private String protocol;
//
//	@Value("${spring.data.elasticsearch.username}")
//	private String userName;
//
//	@Value("${spring.data.elasticsearch.password}")
//	private String password;

	@Bean(destroyMethod = "close")
	public RestHighLevelClient elasticsearchClient() {

		final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("admin", "admin"));

		RestClientBuilder builder = RestClient.builder(new HttpHost("localhost", 9200, "http"))
				.setHttpClientConfigCallback(
						httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
				.setDefaultHeaders(compatibilityHeaders());

		return new RestHighLevelClient(builder);
	}

	private Header[] compatibilityHeaders() {
		return new Header[] {
				new BasicHeader(HttpHeaders.ACCEPT, "application/vnd.elasticsearch+json;compatible-with=7"),
				new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/vnd.elasticsearch+json;compatible-with=7") };
	}
}
