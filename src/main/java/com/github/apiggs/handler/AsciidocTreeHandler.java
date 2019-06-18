package com.github.apiggs.handler;

import com.github.apiggs.Environment;
import com.github.apiggs.http.HttpMessage;
import com.github.apiggs.http.HttpRequest;
import com.github.apiggs.http.HttpResponse;
import com.github.apiggs.markup.MarkupBuilder;
import com.github.apiggs.markup.asciidoc.AsciiDoc;
import com.github.apiggs.markup.asciidoc.Color;
import com.github.apiggs.schema.Appendix;
import com.github.apiggs.schema.Bucket;
import com.github.apiggs.schema.Group;
import com.github.apiggs.schema.Tree;
import com.github.apiggs.util.Cell;
import com.github.apiggs.util.loging.Logger;
import com.github.apiggs.util.loging.LoggerFactory;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.github.apiggs.markup.asciidoc.AsciiDoc.*;

/**
 * adoc文件构建器
 */
public class AsciidocTreeHandler implements TreeHandler {

    private Logger log = LoggerFactory.getLogger(this.getClass());
    private MarkupBuilder builder = MarkupBuilder.getInstance();

    private Environment env;
    private List<CharSequence> attrs;

    @Override
    public void handle(Tree tree, Environment env) {

        attrs = Lists.newArrayList(
                attr(DOCTYPE, BOOK),
                attr(TOC, LEFT), attr(TOC_LEVEL, 3), attr(TOC_TITLE, "目录"),
                attr(SOURCE_HIGHLIGHTER, HIGHLIGHTJS));

        builder.header(tree.getName(), attrs.toArray(new CharSequence[0]));
        if (Objects.nonNull(tree.getVersion())) {
            builder.paragraph("version:" + tree.getVersion());
        }
        if (Objects.nonNull(tree.getDescription())) {
            builder.paragraph(tree.getDescription());
        }

        if (!Strings.isNullOrEmpty(tree.getReadme())) {
            builder.title(1, "文档说明");
            builder.paragraph(tree.getReadme());
        }

        this.env = env;

        if (tree.getBuckets().isEmpty()) {
            for (Group group : tree.getBucket().getGroups()) {
                //build 成功时，章节号往后加1，否则加0
                buildGroup(group, 1);
            }
        } else {
            buildBucket(tree.getBucket());
            for (Bucket bucket : tree.getBuckets().values()) {
                buildBucket(bucket);
            }
        }


        if (!tree.getAppendices().isEmpty()) {
            builder.title(1, "附录");
            for (Appendix appendix : tree.getAppendices()) {
                if (!appendix.isEmpty()) {
                    builder.title(2, appendix.getName());
                    table(appendix.getCells());
                }
            }
        }
        //buildAdoc(env);
    }

    private void buildAdoc(Environment env) {
        try {
            Path adoc = env.getOutPath().resolve(env.getId() + EXTENSION);
            write(adoc, builder.getContent(), StandardCharsets.UTF_8);
            log.info("Build {}", adoc);
        } finally {
            builder.clean();
        }
    }

    private void buildBucket(Bucket bucket) {
        if (!bucket.isEmpty()) {
            builder.title(1, bucket.getName());
            for (Group group : bucket.getGroups()) {
                buildGroup(group, 2);
            }
        }
    }


    private void buildGroup(Group group, int level) {
        if (!group.isEmpty()) {
            builder.title(level, group.getName());
            if (Objects.nonNull(group.getDescription())) {
                builder.paragraph(group.getDescription());
            }
            for (HttpMessage httpMessage : group.getNodes()) {
                buildHttpMessage(httpMessage, level + 1);
            }

            env.id(group.getId());
            buildAdoc(env);
            builder.header("目录", attrs.toArray(new CharSequence[0]));
        }
    }

    private void buildHttpMessage(HttpMessage message, int level) {
        builder.title(level, message.getName());
        if (Objects.nonNull(message.getDescription())) {
            builder.paragraph(message.getDescription());
        }

        HttpRequest request = message.getRequest();
        builder.textLine("请求");
        builder.listing(builder -> {
            for (String uri : request.getUris()) {
                builder.textLine(request.getMethod()
                        + " "
                        + uri
                        + request.queryString()
                        + " "
                        + message.getVersion());
            }
            request.getHeaders().forEach((k, v) -> builder.textLine(k + ": " + v));
            if (request.hasBody()) {
                builder.br();
                builder.text(request.bodyString());
            }
        }, "source,HTTP");

        ntcdd(request.getCells());

        HttpResponse response = message.getResponse();
        if (!response.isEmpty()) {
            builder.textLine("响应");
            builder.listing(builder -> {
                builder.textLine(message.getVersion() + " " + response.getStatus());
                response.getHeaders().forEach((k, v) -> builder.textLine(k + ": " + v));
                if (response.hasBody()) {
                    builder.br();
                    builder.text(response.bodyString());
                }
            }, "source,HTTP");
            ntcdd(response.getCells());
        }

    }

    private void ntcdd(List<Cell<String>> cells) {
        if (cells.size() > 0) {
            List<List<String>> responseTable = new ArrayList<>();
            responseTable.add(Arrays.asList("名称", "类型", "描述"));
//            responseTable.add(Arrays.asList("名称", "类型", "校验", "默认", "描述"));
//            cells.forEach(parameter -> responseTable.add(parameter.toList()));
            cells.forEach(parameter -> responseTable.add(parameter.selectByIndex(0,1,4)));
            builder.table(responseTable);
        }
    }

    private void table(List<Cell<String>> cells) {
        if (cells.size() > 0) {
            List<List<String>> responseTable = new ArrayList<>();
            cells.forEach(parameter -> responseTable.add(parameter.toList()));
            builder.table(responseTable, false, false);
        }
    }

}
