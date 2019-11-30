package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PmsBaseCatalog1;
import com.atguigu.gmall.bean.PmsBaseCatalog2;
import com.atguigu.gmall.bean.PmsBaseCatalog3;

import java.util.List;

public interface CatalogService {

    List<PmsBaseCatalog1> getCataLog1();

    List<PmsBaseCatalog2> getCataLog2(String catalog1Id);

    List<PmsBaseCatalog3> getCataLog3(String catalog2Id);
}
