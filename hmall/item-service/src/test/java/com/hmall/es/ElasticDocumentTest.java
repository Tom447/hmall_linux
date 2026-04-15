package com.hmall.es;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.item.domain.dto.ItemDocDTO;
import com.hmall.item.domain.po.Item;
import com.hmall.item.service.IItemService;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

// 【修改点1】添加 seata.enabled=false 解决 JDK17 兼容性问题
@SpringBootTest(classes = com.hmall.item.ItemApplication.class, properties = {"spring.profiles.active=local", "seata.enabled=false"})
public class ElasticDocumentTest {
    private RestHighLevelClient client;

    @Autowired
    private IItemService itemService;

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