package com.hmall.es;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.item.domain.dto.ItemDocDTO;
import com.hmall.item.domain.po.Item;
import com.hmall.item.service.IItemService;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.Stats;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

// 【修改点1】添加 seata.enabled=false 解决 JDK17 兼容性问题
@SpringBootTest(classes = com.hmall.item.ItemApplication.class, properties = {"spring.profiles.active=local", "seata.enabled=false"})
public class ElasticDocumentTest {
    private RestHighLevelClient client;

    @Autowired
    private IItemService itemService;

    private static final String MAPPING_TEMPLATE = "{\n" +
            "  \"mappings\": {\n" +
            "    \"properties\": {\n" +
            "      \"id\": {\n" +
            "        \"type\": \"long\"\n" +
            "      },\n" +
            "      \"name\": {\n" +
            "        \"type\": \"text\",\n" +
            "        \"analyzer\": \"ik_smart\"\n" +
            "      },\n" +
            "      \"category\": {\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"brand\": {\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"price\": {\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"sold\": {\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"image\": {\n" +
            "        \"type\": \"keyword\",\n" +
            "        \"index\": false\n" +
            "      },\n" +
            "      \"commentCount\":{\n" +
            "        \"type\": \"integer\",\n" +
            "        \"index\": false\n" +
            "      },\n" +
            "      \"isAD\":{\n" +
            "        \"type\": \"boolean\"\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

    @Test
    void testCreateIndex() throws IOException {
        // 1.准备Request对象
        CreateIndexRequest request = new CreateIndexRequest("items");
        // 2.准备请求参数
        request.source(MAPPING_TEMPLATE, XContentType.JSON);
        // 3.发送请求
        client.indices().create(request, RequestOptions.DEFAULT);
    }

    @Test
    void testDeleteIndex() throws IOException {
        // 1.准备Request对象
        DeleteIndexRequest request = new DeleteIndexRequest("items");
        // 2.准备请求参数
        // request.source(MAPPING_TEMPLATE, XContentType.JSON);
        // 3.发送请求
        client.indices().delete(request, RequestOptions.DEFAULT);
    }

    @Test
    void testGetIndex() throws IOException {
        // 1.准备Request对象
        GetIndexRequest request = new GetIndexRequest("items");
        // 2.准备请求参数
        // request.source(MAPPING_TEMPLATE, XContentType.JSON);
        // 3.发送请求
        boolean exists = client.indices().exists(request, RequestOptions.DEFAULT);
        System.out.println("exists = " + exists);
    }
    @Test
    void testSaveDocument() throws Exception {
        Item item = itemService.getById(317578L);
        // 【修改点2】增加判空保护
        if (item == null) return;

        IndexRequest request = new IndexRequest("items").id(item.getId().toString());
        request.source(JSONUtil.toJsonStr(BeanUtil.copyProperties(item, ItemDocDTO.class)), XContentType.JSON);
        IndexResponse response = client.index(request, RequestOptions.DEFAULT);
        System.out.println("response: " + response);
    }

    @Test
    void testupdateDocument() throws Exception {
        Item item = itemService.getById(317578L);
        if (item == null) return;

        UpdateRequest request = new UpdateRequest("items", item.getId().toString());
        request.doc(
                "price", 2000,
                "stock", 200
        );
        UpdateResponse response = client.update(request, RequestOptions.DEFAULT);
        System.out.println("response: " + response);
    }

    @Test
    void testGetDocumentById() throws Exception {
        GetRequest request = new GetRequest("items").id("317578");
        GetResponse response = client.get(request, RequestOptions.DEFAULT);
        // 建议增加判断 response.isExists()
        if (response.isExists()) {
            String json = response.getSourceAsString();
            ItemDocDTO result = JSONUtil.toBean(json, ItemDocDTO.class);
            System.out.println(result);
        }
    }

    @Test
    void testSearchDynamicHeight() throws IOException {
        // 1. 准备 Request
        SearchRequest request = new SearchRequest("items");

        // 2. 准备 DSL 参数
        // 2.1 搜索条件
        request.source().query(QueryBuilders.matchQuery("name", "脱脂牛奶"));
        // 2.2 高亮设置 (假设我们不知道具体是哪个字段，或者配置了多个字段高亮)
        request.source().highlighter(
                SearchSourceBuilder.highlight()
                        .field("name")
                // .field("brand") // 甚至可以同时高亮多个字段
        );

        // 3. 发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        // 4. 结果解析
        SearchHits hits = response.getHits();
        for (SearchHit hit : hits) {
            //4.1得到原始的JSON
            String json = hit.getSourceAsString();
            //4.2反序列化为对象
            ItemDocDTO itemDoc = JSONUtil.toBean(json, ItemDocDTO.class);


            //4.3获取高亮结果
            Map<String, HighlightField> hfs = hit.getHighlightFields();
            if (!CollUtil.isEmpty(hfs)){
                //遍历高亮结果，Key就是高亮字段的字段名
                for (Map.Entry<String, HighlightField> entry : hfs.entrySet()) {
                    String fieldName = entry.getKey();//获取字段名称
                    HighlightField hf = entry.getValue();//获取字段属性


                    if (hf != null){
                        try {
                            //1 获取DTO中对应的Field对象
                            Field field = ItemDocDTO.class.getDeclaredField(fieldName);
                            //2. 暴力反射，允许访问私有属性
                            field.setAccessible(true);
                            //3 获取高亮内容
                            String highlightValue = hf.getFragments()[0].toString();
                            //4 动态将高亮值set到对象中
                            field.set(itemDoc, highlightValue);

                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            // 如果 DTO 中没有这个字段，或者赋值失败，则忽略或打印日志
                            // 防止因为个别字段高亮导致整个程序崩溃
                            e.printStackTrace();
                        }
                    }
                }
            }
            // 5. 输出最终结果
            System.out.println("处理后的结果: " + itemDoc);
        }
    }

    @Test
    void testBulk() throws Exception {
        int pageNo = 1, pageSize = 1000;
        while (true) {
            // 【修改点3】建议增加排序，防止分页数据错乱
            Page<Item> page = itemService.lambdaQuery()
                    .eq(Item::getStatus, 1)
                    .orderByAsc(Item::getId)
                    .page(Page.of(pageNo, pageSize));

            List<Item> records = page.getRecords();
            if (records == null || records.size() == 0) {
                break; // 使用 break 比 return 更符合循环逻辑
            }

            BulkRequest request = new BulkRequest();
            for (Item item : records) {
                request.add(new IndexRequest("items")
                        .id(item.getId().toString())
                        .source(JSONUtil.toJsonStr(BeanUtil.copyProperties(item, ItemDocDTO.class)), XContentType.JSON));
            }
            client.bulk(request, RequestOptions.DEFAULT);
            pageNo++;
        }
    }

    @Test
    void testDeleteDocument() throws Exception {
        Item item = itemService.getById(317578L);
        if (item == null) return;

        DeleteRequest request = new DeleteRequest("items").id(item.getId().toString());
        DeleteResponse delete = client.delete(request, RequestOptions.DEFAULT);
        System.out.println(delete.toString());
    }

    @Test
    void testAggs() throws IOException {
        //1.准备request
        SearchRequest request = new SearchRequest("items");
        //2.准备DSL请求参数
        //2.1搜索条件
        request.source().query(QueryBuilders.termQuery("category", "手机"));
        //2.2 设置搜索结果的数量
        request.source().size(0);
        //2.3 对品牌进行聚合(并进行嵌套）
        String aggsName = "brand_aggs";
        request.source().aggregation(
                AggregationBuilders
                        .terms(aggsName)
                        .field("brand")
                        .size(20)
                        .subAggregation(
                                AggregationBuilders
                                        .stats("price_statis")
                                        .field("price")
                        )
        );
        //3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //4.结果解析
        Aggregations aggregations = response.getAggregations();
        //4.1 根据名称获取聚合结果
        Terms brandTerms = aggregations.get(aggsName);
        List<? extends Terms.Bucket> buckets = brandTerms.getBuckets();
        //4.2 遍历
        for (Terms.Bucket bucket : buckets) {
            //获取key
            String brandName = bucket.getKeyAsString();
            long docCount = bucket.getDocCount();
            //获取嵌套的stats聚合结果
            Stats priceStats = bucket.getAggregations().get("price_statis");
            double avgPrice = priceStats.getAvg();
            double maxPrice = priceStats.getMax();
            double minPrice = priceStats.getMin();
            long count = priceStats.getCount();
            double sum = priceStats.getSum();

            System.out.printf("品牌: %s, 商品数: %d, 平均价: %.2f, 最高价: %.2f, 最低价: %.2f%n",
                    brandName, docCount, avgPrice, maxPrice, minPrice);
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        // 【修改点4】请确认这里是否真的是 9201，通常默认是 9200
        client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.10.100:9200")
        ));
    }

    @AfterEach
    void tearDown() throws Exception {
        if (client != null) {
            client.close();
        }
    }
}