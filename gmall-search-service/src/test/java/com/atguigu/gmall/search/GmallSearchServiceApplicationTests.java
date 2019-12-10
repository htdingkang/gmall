package com.atguigu.gmall.search;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsSearchSkuInfo;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.service.SkuService;
import io.searchbox.client.JestClient;

import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallSearchServiceApplicationTests {

    @Reference
    SkuService skuService;

    @Autowired
    JestClient jestClient;

    @Test
    public void contextLoads() throws IOException {
        //put();
        search();
    }
    public void search() throws IOException {
        List<PmsSearchSkuInfo> pmsSearchSkuInfos=new ArrayList<>();
//        String searchStr="{\n" +
//                "  \"query\": {\n" +
//                "    \"bool\": {\n" +
//                "      \"filter\": [\n" +
//                "        {\n" +
//                "          \"terms\":{ \n" +
//                "            \"skuAttrValueList.valueId\": [\"48\",\"51\"]\n" +
//                "          }\n" +
//                "        },\n" +
//                "        {\n" +
//                "          \"term\": {\n" +
//                "            \"skuAttrValueList.valueId\": \"39\"\n" +
//                "          }\n" +
//                "        },\n" +
//                "        {\n" +
//                "          \"term\": {\n" +
//                "            \"skuAttrValueList.valueId\": \"43\"\n" +
//                "          }\n" +
//                "        }\n" +
//                "      ]\n" +
//                "      , \"must\": [\n" +
//                "        {\n" +
//                "          \"match\": {\n" +
//                "            \"skuName\": \"华为\"\n" +
//                "          }\n" +
//                "        }\n" +
//                "      ]\n" +
//                "    }\n" +
//                "  }\n" +
//                "}";

        //jest的dsl工具
        SearchSourceBuilder searchSourceBuilder=new SearchSourceBuilder();
            //bool
            BoolQueryBuilder boolQueryBuilder=new BoolQueryBuilder();
            TermQueryBuilder queryBuilder=new TermQueryBuilder("skuAttrValueList.valueId","39");
            //filter
            boolQueryBuilder.filter(queryBuilder);
            MatchQueryBuilder matchQueryBuilder=new MatchQueryBuilder("skuName","华为");
            //must
            boolQueryBuilder.must(matchQueryBuilder);
        //query
        searchSourceBuilder.query(boolQueryBuilder);
        //from
        searchSourceBuilder.from(0);
        //size
        searchSourceBuilder.size(20);
        //highlight
        searchSourceBuilder.highlight(null);

        String searchStr = searchSourceBuilder.toString();

        System.out.println(searchStr);


        Search search = new Search.Builder(searchStr)
                .addIndex("gmall")
                .addType("PmsSkuInfo")
                .build();
        SearchResult result = jestClient.execute(search);
        List<SearchResult.Hit<PmsSearchSkuInfo, Void>> hits = result.getHits(PmsSearchSkuInfo.class);
        for (SearchResult.Hit<PmsSearchSkuInfo, Void> hit : hits) {
            PmsSearchSkuInfo source = hit.source;
            pmsSearchSkuInfos.add(source);
        }
        System.out.println(pmsSearchSkuInfos);
    }

    public  void put() throws IOException {
        //查询mysql
        List<PmsSkuInfo> pmsSkuInfos = skuService.getAllSku();
        //转化为es数据结构
        List<PmsSearchSkuInfo> pmsSearchSkuInfos = new ArrayList<>();
        for (PmsSkuInfo pmsSkuInfo : pmsSkuInfos) {
            PmsSearchSkuInfo pmsSearchSkuInfo = new PmsSearchSkuInfo();
            BeanUtils.copyProperties(pmsSkuInfo, pmsSearchSkuInfo);
            pmsSearchSkuInfos.add(pmsSearchSkuInfo);
        }

        //导入es
        for (PmsSearchSkuInfo pmsSearchSkuInfo : pmsSearchSkuInfos) {
            Index index = new Index.Builder(pmsSearchSkuInfo)
                    .index("gmall")
                    .type("PmsSkuInfo")
                    .id(pmsSearchSkuInfo.getId())
                    .build();
            jestClient.execute(index);
        }
    }

}
