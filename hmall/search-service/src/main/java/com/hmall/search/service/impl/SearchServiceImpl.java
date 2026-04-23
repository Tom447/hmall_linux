package com.hmall.search.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmall.api.client.ItemClient;
import com.hmall.api.dto.ItemDTO;
import com.hmall.search.domain.po.ItemDoc;
import com.hmall.search.domain.query.ItemPageQuery;
import com.hmall.search.domain.vo.PageVO;
import com.hmall.search.service.ISearchService;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHost;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements ISearchService {

    private final ItemClient itemClient;
    private RestHighLevelClient client;

    private final String INDEX_NAME = "items";
    {
        client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.10.100:9200")
        ));
    }

    @Override
    public void saveItemById(Long itemId) {
        // 根据id查询商品
        ItemDTO itemDTO = itemClient.queryItemById(itemId);

        try {
            // 1.准备Request对象
            IndexRequest request = new IndexRequest(INDEX_NAME).id(itemId.toString());
            // 2.准备请求参数
            request.source(JSONUtil.toJsonStr(BeanUtil.copyProperties(itemDTO, ItemDoc.class)), XContentType.JSON);
            // 3.发送请求
            client.index(request, RequestOptions.DEFAULT);
            System.out.println("search-service——上架商品:itemid : " + itemId);
        } catch (IOException e) {
            throw new RuntimeException("更新商品失败，商品id：" + itemId, e);
        }
    }

    @Override
    public void deleteItemById(Long itemId) {
        try {
            // 1.准备Request对象
            DeleteRequest request = new DeleteRequest(INDEX_NAME, itemId.toString());
            // 3.发送请求
            client.delete(request, RequestOptions.DEFAULT);
            System.out.println("search-service——下架商品:itemid : " + itemId);
        } catch (IOException e) {
            throw new RuntimeException("删除商品失败，商品id：" + itemId, e);
        }
    }

   /* private QueryBuilder buildQuery(ItemPageQuery query) {
        //1.初始化布尔查询
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        //1 关键字搜索
        String key = query.getKey();
        if (StrUtil.isBlank(key)){
            boolQuery.must(QueryBuilders.matchAllQuery());
        }else {
            boolQuery.must(QueryBuilders.matchQuery("name", key));
        }

        //2 过滤条件
        //2.1 分类过滤
        String category = query.getCategory();
        if (StrUtil.isNotBlank(category)) {
            boolQuery.filter(QueryBuilders.termQuery("category", category));
        }

        //2.2品牌过滤
        String brand = query.getBrand();
        if (StrUtil.isNotBlank(brand)) {
            boolQuery.filter(QueryBuilders.termQuery("brand", brand));
        }

        //2.3价格过滤
        Integer minPrice = query.getMinPrice();
        Integer maxPrice = query.getMaxPrice();
        if (minPrice != null || maxPrice != null) {
            boolQuery.filter(QueryBuilders.rangeQuery("price").gte(minPrice).lte(maxPrice));
        }

        return boolQuery;
    }*/

    private QueryBuilder buildQueryAndIsAD(ItemPageQuery query) {
        //1.初始化布尔查询
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        //1 关键字搜索
        String key = query.getKey();
        if (StrUtil.isBlank(key)){
            boolQuery.must(QueryBuilders.matchAllQuery());
        }else {
            boolQuery.must(QueryBuilders.matchQuery("name", key));
        }

        //2 过滤条件
        //2.1 分类过滤
        String category = query.getCategory();
        if (StrUtil.isNotBlank(category)) {
            boolQuery.filter(QueryBuilders.termQuery("category", category));
        }

        //2.2品牌过滤
        String brand = query.getBrand();
        if (StrUtil.isNotBlank(brand)) {
            boolQuery.filter(QueryBuilders.termQuery("brand", brand));
        }

        //2.3价格过滤
        Integer minPrice = query.getMinPrice();
        Integer maxPrice = query.getMaxPrice();
        if (minPrice != null || maxPrice != null) {
            boolQuery.filter(QueryBuilders.rangeQuery("price").gte(minPrice).lte(maxPrice));
        }

        //添加竞价的广告
        FunctionScoreQueryBuilder functionScoreQuery = QueryBuilders.functionScoreQuery(
                boolQuery, // 第一个参数：原始查询
                new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                QueryBuilders.termQuery("isAD", true), // 过滤条件：广告商品
                                //设置一个初始值
                                ScoreFunctionBuilders.weightFactorFunction(100) // 算分函数：权重 100
                        )
                }
        )/*
            针对文档的最终得分，boostMode(CombineFunction.MULTIPLY)是一个怎么样组合的策略
            1. 原始相关性得分，也是文档相关性得分（比如搜索华为手机，相关度5.6）
            2. 函数得分，上面 weightFactorFunction 计算出来的得分（广告为 100，非广告为 0 或 1）。
            boostMode 决定了这两个分数怎么“合体”。我选择了 MULTIPLY（相乘）。

        */
        .boostMode(CombineFunction.MULTIPLY);
        return functionScoreQuery;
    }

    @Override
    public PageVO<ItemDoc> search(ItemPageQuery query){
        try {
            //1.发送请求
            SearchRequest request = new SearchRequest("items");
            //2.准备DSL
            //2.1查询条件
            request.source().query(buildQueryAndIsAD(query));
            //2.2分页条件
            request.source().from((query.getPageNo() - 1) * query.getPageSize()).size(query.getPageSize());

            //2.3排序条件
            if (StrUtil.isNotBlank(query.getSortBy())) {
                request.source().sort(query.getSortBy(), query.getIsAsc() ? SortOrder.ASC : SortOrder.DESC);
            }

            //2.4高亮条件
            request.source().highlighter(new HighlightBuilder().field("name"));
            //3.发送请求
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);

            //4. 解析结果
            SearchHits searchHits = response.getHits();
            // 4.1.总条数
            long total = searchHits.getTotalHits().value;
            // 4.2.数据
            SearchHit[] hits = searchHits.getHits();
            // 4.3.遍历
            List<ItemDoc> list = new ArrayList<>(hits.length);
            for (SearchHit hit : hits) {
                // 4.4.获取source，转为Doc
                String json = hit.getSourceAsString();
                ItemDoc itemDoc = JSONUtil.toBean(json, ItemDoc.class);
                list.add(itemDoc);
                // 4.5.获取高亮
                Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                if (CollUtil.isNotEmpty(highlightFields)) {
                    String hfName = highlightFields.get("name").getFragments()[0].string();
                    itemDoc.setName(hfName);
                }
            }
            // 5.封装返回
            return PageVO.of(total, 0L, list);
        } catch (IOException e) {
            return PageVO.empty(0L, 0L);
        }
    }

    @Override
    public Map<String, List<String>> getFilters(ItemPageQuery query) {
        try {
            // 1.创建request
            SearchRequest request = new SearchRequest("items");
            // 2.准备DSL
            // 2.1.查询条件
            request.source().query(buildQueryAndIsAD(query));
            // 2.2.分页条件
            request.source().size(0);
            // 2.3.聚合条件
            String brandAgg = "brandAgg";
            String categoryAgg = "categoryAgg";
            // 2.3.1.品牌聚合
            request.source().aggregation(AggregationBuilders.terms(brandAgg).field("brand"));
            // 2.3.2.分类聚合
            request.source().aggregation(AggregationBuilders.terms(categoryAgg).field("category"));

            // 3.发送请求
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            // 4.解析结果
            Aggregations aggregations = response.getAggregations();
            // 4.1.获取品牌聚合
            Terms brandTerms = aggregations.get(brandAgg);
            List<String> brands = brandTerms.getBuckets()
                    .stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
            // 4.2.获取分类聚合
            Terms categoryTerms = aggregations.get(categoryAgg);
            List<String> categories = categoryTerms.getBuckets()
                    .stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
            return Map.of("brand", brands, "category", categories);
        } catch (IOException e) {
            return Collections.emptyMap();
        }
    }


}






