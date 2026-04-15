package com.hmall.es;


import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ElasticTest {
    private RestHighLevelClient client;

    @Test
    void testConnection(){
        System.out.println("client = " + client);
    }

    @BeforeEach
    void setUp() throws Exception {
        client = new RestHighLevelClient(
                RestClient.builder(
                        HttpHost.create("http://192.168.10.100:9200")
                )
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        client.close();
    }
}
