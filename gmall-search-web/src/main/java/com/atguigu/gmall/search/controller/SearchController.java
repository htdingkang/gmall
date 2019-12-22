package com.atguigu.gmall.search.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.AttrService;
import com.atguigu.gmall.service.SearchService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;

@Controller
public class SearchController {

    @Reference
    SearchService searchService;
    @Reference
    AttrService attrService;


    @RequestMapping("index")
    @LoginRequired(loginSuccess = false)  //首页主动登录，重定向到首页，需要写入token到cookie
    public String index(){
        return "index";
    }

    @RequestMapping("list.html")
    public String list(PmsSearchParam pmsSearchParam, ModelMap modelMap){
        //调用搜索服务，返回搜索结果
        List<PmsSearchSkuInfo> pmsSearchSkuInfoList = searchService.list(pmsSearchParam);
        modelMap.put("skuLsInfoList", pmsSearchSkuInfoList);

        //点击属性时，新请求的url=当前请求的url（urlParam）+属性Id
        //<li  th:each="attrValue:${attrInfo.attrValueList}"><a th:href="'/list.html?'+${urlParam}+'&valueId='+${attrValue.id}"  th:text="${attrValue.valueName}">属性值</a></li>
        //当前请求就存在于pmsSearchParam中
        String urlParam = getUrlParam(pmsSearchParam);
        modelMap.put("urlParam", urlParam);

        //面包屑中展示搜索关键字
        String keyword = pmsSearchParam.getKeyword();
        if (StringUtils.isNotBlank(keyword)) {
            modelMap.put("keyword", keyword);
        }

        //抽取检索结果包含的平台属性集合
        Set<String> valueIdSet = new HashSet<String>();
        for (PmsSearchSkuInfo pmsSearchSkuInfo : pmsSearchSkuInfoList) {
            List<PmsSkuAttrValue> skuAttrValueList = pmsSearchSkuInfo.getSkuAttrValueList();
            for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
                String valueId = pmsSkuAttrValue.getValueId();
                valueIdSet.add(valueId);
            }
        }
        //根据valueId查询平台属性信息
        if(valueIdSet.isEmpty()){
            return "list";  //搜索结果为空，下面查属性值会报错
        }
        List<PmsBaseAttrInfo> pmsBaseAttrInfos = attrService.getAttrValueListByValueId(valueIdSet);

        //对平台属性集合进行进一步处理，去掉当前条件中valueId所在的属性组
        String[] currValueIds = pmsSearchParam.getValueId();
        if (currValueIds != null && currValueIds.length > 0) {
            //面包屑
            List<PmsSearchCrumb> pmsSearchCrumbs = new ArrayList<>();
            for (String currValueId : currValueIds) {
                //如果currValueIds不为空，当前请求中包含属性的参数，每一个属性参数都会生成一个面包屑
                PmsSearchCrumb pmsSearchCrumb = new PmsSearchCrumb();
                pmsSearchCrumb.setValueId(currValueId);
                pmsSearchCrumb.setUrlParam(getUrlParam(pmsSearchParam, currValueId));
                Iterator<PmsBaseAttrInfo> iterator = pmsBaseAttrInfos.iterator();
                label:
                //跳出循环标记点
                while (iterator.hasNext()) {
                    PmsBaseAttrInfo next = iterator.next();
                    List<PmsBaseAttrValue> attrValueList = next.getAttrValueList();
                    for (PmsBaseAttrValue pmsBaseAttrValue : attrValueList) {
                        if (currValueId.equals(pmsBaseAttrValue.getId())) {
                            //通过面包屑的id找到name
                            pmsSearchCrumb.setValueName(pmsBaseAttrValue.getValueName());
                            //删除该属性值所在的属性组
                            iterator.remove();
                            break label; //直接跳出info的循环
                        }
                    }
                }
                pmsSearchCrumbs.add(pmsSearchCrumb);
            }
            modelMap.put("attrValueSelectedList", pmsSearchCrumbs);
        }
        modelMap.put("attrList", pmsBaseAttrInfos);

        return "list";
    }

    private String getUrlParam(PmsSearchParam pmsSearchParam,String... delValueId) {
        String catalog3Id = pmsSearchParam.getCatalog3Id();
        String keyword = pmsSearchParam.getKeyword();
        String[] valueIds = pmsSearchParam.getValueId();
        String urlParam="";
        if(StringUtils.isNotBlank(catalog3Id)){
            if(StringUtils.isNotBlank(urlParam)){
                urlParam=urlParam+"&";
            }
            urlParam=urlParam+"catalog3Id="+catalog3Id;
        }
        if(StringUtils.isNotBlank(keyword)){
            if(StringUtils.isNotBlank(urlParam)){
                urlParam=urlParam+"&";
            }
            urlParam=urlParam+"keyword="+keyword;
        }
        if (valueIds != null && valueIds.length > 0) {
            for (String valueId : valueIds) {
                if (delValueId.length>0) {
                    //点击面包屑时，urlParam=当前url-面包屑valueId
                    if (!valueId.equals(delValueId[0])) {
                        urlParam = urlParam + "&valueId=" + valueId;
                    }
                } else {
                    urlParam = urlParam + "&valueId=" + valueId;
                }
            }
        }
        return urlParam;
    }
}
