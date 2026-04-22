package com.hmall.search.test;

import cn.hutool.json.JSONUtil;
import com.hmall.search.domain.po.ItemDoc;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.Stats;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ElasticTest {

    private RestHighLevelClient client;

    @Test
    void testSearchDemo() throws IOException {
        // 1.准备Request
        SearchRequest request = new SearchRequest("items");
        // 2.准备DSL参数
        request.source()
                .query(QueryBuilders.matchAllQuery());
        // 3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // 4.结果解析
        parseResponseResult(response);
    }

    @Test
    void testSearchMilk() throws IOException {
        // 1.准备Request
        SearchRequest request = new SearchRequest("items");
        // 2.准备DSL参数
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        // 2.1.关键字搜索
        queryBuilder.must(QueryBuilders.matchQuery("name", "脱脂牛奶"));
        // 2.2.过滤条件
        queryBuilder.filter(QueryBuilders.termQuery("brand", "德亚"));
        queryBuilder.filter(QueryBuilders.rangeQuery("price").lt(30000));

        request.source().query(queryBuilder);
        // 3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // 4.结果解析
        parseResponseResult(response);
    }

    @Test
    void testSortAndPage() throws IOException {
        int pageNo = 1, pageSize = 5;

        // 1.准备Request
        SearchRequest request = new SearchRequest("items");
        // 2.准备DSL参数
        // 2.1.搜索条件
        request.source().query(QueryBuilders.matchAllQuery());
        // 2.2.分页条件
        request.source().from((pageNo - 1) * pageSize).size(pageSize);
        // 2.3.排序条件
        request.source().sort("price", SortOrder.DESC);
        // 3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // 4.结果解析
        parseResponseResult(response);
    }

    @Test
    void testHighlight() throws IOException {
        // 1.准备Request
        SearchRequest request = new SearchRequest("items");
        // 2.准备DSL参数
        // 2.1.query条件
        request.source().query(QueryBuilders.matchQuery("name", "脱脂牛奶"));
        // 2.2.高亮条件
        request.source().highlighter(new HighlightBuilder().field("name"));
        // 3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // 4.结果解析
        parseResponseResult(response);
    }

    private void parseResponseResult(SearchResponse response) {
        SearchHits searchHits = response.getHits();
        // 4.1.获取total
        long total = searchHits.getTotalHits().value;
        System.out.println("total = " + total);
        // 4.2.取数据
        SearchHit[] hits = searchHits.getHits();
        // 4.3.遍历
        for (SearchHit hit : hits) {
            // 4.4.获取source
            String json = hit.getSourceAsString();
            ItemDoc itemDoc = JSONUtil.toBean(json, ItemDoc.class);
            // 4.5.处理高亮结果
            Map<String, HighlightField> hfs = hit.getHighlightFields();
            HighlightField hf = hfs.get("name");
            String hfName = hf.getFragments()[0].string();
            itemDoc.setName(hfName);
            System.out.println("itemDoc = " + itemDoc);
        }
    }

    @Test
    void testAggs() throws IOException {
        // 1.准备Request
        SearchRequest request = new SearchRequest("items");
        // 2.准备参数DSL
        // 2.1.query条件，限定聚合范围
        request.source().query(QueryBuilders.termQuery("category", "手机"));
        // 2.2.不要放的文档
        request.source().size(0);
        // 2.3.聚合条件
        String aggName = "brandAgg";
        request.source().aggregation(
                AggregationBuilders.terms(aggName).field("brand").subAggregation(
                        AggregationBuilders.stats("priceStats").field("price")
                )
        ); 
        // 3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // 4.解析结果
        Aggregations aggregations = response.getAggregations();
        // 4.1.根据聚合名称获取聚合结果
        Terms brandTerms = aggregations.get(aggName);
        // 4.2.获取聚合中的buckets
        List<? extends Terms.Bucket> buckets = brandTerms.getBuckets();
        // 4.3.遍历
        for (Terms.Bucket bucket : buckets) {
            // 4.4.获取key，聚合的字段的值，本例中是品牌
            String brand = bucket.getKeyAsString();
            // 4.5.获取桶内的文档的数量
            long count = bucket.getDocCount();
            System.out.println(brand + " : " + count);

            Aggregations aggregations1 = bucket.getAggregations();
            Stats priceStats = aggregations1.get("priceStats");
            double min = priceStats.getMin();
            System.out.println("\tmin = " + min);
            double max = priceStats.getMax();
            System.out.println("\tmax = " + max);
            double avg = priceStats.getAvg();
            System.out.println("\tavg = " + avg);
        }
    }

    @BeforeEach
    void setUp() {
        client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.150.101:9200")
        ));
    }

    @AfterEach
    void tearDown() throws IOException {
        client.close();
    }
}
