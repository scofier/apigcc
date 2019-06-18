package com.github.apiggs.example;

import com.github.apiggs.Apiggs;
import com.github.apiggs.Environment;
import com.github.apiggs.example.diff.MatchUtil;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @title Apiggs示例文档
 * @description 通过javadoc设置文档描述信息
 * 优先级大于通过Environment.description()设置的值
 * @readme 所有接口均使用Https调用
 * /app路径下的接口为app专用
 * /mini路径下的接口为小程序专用
 */
public class ApiggsTest {

    @Test
    public void testApiggs() {
        Environment env = new Environment()
                .project(Paths.get("/Users/sk/Work/xlab/apigcc"))
                .source(Paths.get("src", "test", "java"))
                .ignore("ResponseEntity")
                .jar(Paths.get("src/test/resources/lib/apiggs-model-1.0-SNAPSHOT.jar"))
                .id("apiggs")
                .title("示例接口文档")
                .description("示例接口文档，使用默认模板");
        Apiggs apiggs = new Apiggs(env);
        apiggs.lookup().build();

//        Path buildAdoc = env.getOutPath().resolve(env.getId() + ".adoc");
//        Path template = env.getOutPath().resolve("../../src/test/resources/template.adoc");
//        Path templateHtml = env.getOutPath().resolve("../../src/test/resources/template.html");
//        Path resultHtml = env.getOutPath().resolve("result.html");
//
//        new MatchUtil(templateHtml, resultHtml).compare(template, buildAdoc);
    }

}