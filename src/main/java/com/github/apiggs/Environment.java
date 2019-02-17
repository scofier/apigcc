package com.github.apiggs;

import com.github.apiggs.handler.AsciidocTreeHandler;
import com.github.apiggs.handler.HtmlTreeHandler;
import com.github.apiggs.handler.TreeHandler;
import com.github.apiggs.handler.postman.PostmanTreeHandler;
import com.github.apiggs.util.loging.Logger;
import com.github.apiggs.util.loging.LoggerFactory;
import com.github.apiggs.visitor.NodeVisitor;
import com.github.apiggs.visitor.springmvc.SpringVisitor;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

public class Environment {

    Logger log = LoggerFactory.getLogger(this.getClass());

    public static final String NAME = "apiggs";

    public static final Path DEFAULT_PRODUCTION = Paths.get(NAME);
    public static final Path DEFAULT_SOURCE_STRUCTURE = Paths.get("src", "main", "java");
    public static final Path DEFAULT_PROJECT_PATH = Paths.get(System.getProperty("user.dir"));
    public static final Path DEFAULT_OUT = Paths.get("build");

    public static final Integer DEFAULT_NODE_INDEX = 99;

    /**
     * 默认的文档构建器
     */
//    public static Iterable<TreeHandler> DEFAULT_PIPELINE = Lists.newArrayList(new PostmanTreeHandler(), new AsciidocTreeHandler(), new HtmlTreeHandler());
    public static Iterable<TreeHandler> DEFAULT_PIPELINE = Lists.newArrayList(new AsciidocTreeHandler(), new HtmlTreeHandler());


    private enum Framework {

        SPRINGMVC(new SpringVisitor());

        private NodeVisitor visitor;

        Framework(NodeVisitor visitor) {
            this.visitor = visitor;
        }

        public NodeVisitor visitor() {
            return visitor;
        }

    }

    /**
     * project根目录
     */
    private Path project = DEFAULT_PROJECT_PATH;

    /**
     * 项目名称 生成 index.json index.adoc index.html
     */
    private String id = "index";

    /**
     * 文档标题
     */
    private String title;

    /**
     * 文档描述
     */
    private String description;
    /**
     * 文档版本
     */
    private String version;

    /**
     * source code folder wait for parse
     * or just some code
     * default: parse user.dir 's code
     */
    private Set<Path> sources = Sets.newHashSet();

    /**
     * dependency source code folders
     */
    private Set<Path> dependencies = Sets.newHashSet();

    /**
     * dependency third jars
     */
    private Set<Path> jars = Sets.newHashSet();

    /**
     * 输出文件包裹的文件夹
     */
    private Path production = DEFAULT_PRODUCTION;

    /**
     * 输出目录
     */
    private Path out = DEFAULT_OUT;

    private String css;

    private Set<String> controller = Sets.newHashSet();

    /**
     * 忽略哪些类型的参数、类解析
     */
    public static ThreadLocal<Set<String>> ignoreTypes = new ThreadLocal<>();

    /**
     * 当前项目使用了什么框架
     */
    private Framework currentFramework = Framework.SPRINGMVC;

    private CombinedTypeSolver typeSolver;

    public Environment source(Path... values) {
        for (Path value : values) {
            if(!value.isAbsolute()){
                value = project.resolve(value);
            }
            if (Files.exists(value)) {
                this.sources.add(value);
            }
        }
        dependency(values);
        return this;
    }

    public Environment dependency(Path... values) {
        for (Path value : values) {
            if(!value.isAbsolute()){
                value = project.resolve(value);
            }
            if (Files.exists(value)) {
                this.dependencies.add(value);
            }
        }
        return this;
    }

    public Environment jar(Path... values) {
        for (Path value : values) {
            if(!value.isAbsolute()){
                value = project.resolve(value);
            }
            if (!Files.exists(value)) {
                continue;
            }
            if (!Files.isDirectory(value) && value.toString().endsWith("jar")){
                this.jars.add(value);
            }
            try {
                Files.list(value).forEach(this::jar);
            } catch (IOException e) {
                log.debug("read list of {} error", value);
            }
        }
        return this;
    }

    public Environment id(String value) {
        this.id = value;
        return this;
    }

    public Environment project(Path value) {
        this.project = value;
        return this;
    }

    public Environment production(Path value) {
        this.production = value;
        return this;
    }

    public Environment out(Path value) {
        this.out = value;
        return this;
    }

    public Environment title(String value) {
        this.title = value;
        return this;
    }

    public Environment description(String value) {
        this.description = value;
        return this;
    }

    public Environment version(String value) {
        this.version = value;
        return this;
    }

    public Environment ignore(String... values) {
        getIgnoreTypes().addAll(Sets.newHashSet(values));
        return this;
    }

    public Environment controller(String... values) {
        getController().addAll(Sets.newHashSet(values));
        return this;
    }

    public Set<String> getController() {
        return controller;
    }

    public Environment css(String css) {
        this.css = css;
        return this;
    }

    public Iterable<TreeHandler> pipeline() {
        return DEFAULT_PIPELINE;
    }

    /**
     * 通过某种方法判断当前项目采用了什么框架
     *
     * @return
     */
    public Framework currentFramework() {
        return currentFramework;
    }

    public ParserConfiguration buildParserConfiguration() {
        if (sources.isEmpty()) {
            source(project.resolve(DEFAULT_SOURCE_STRUCTURE));
        }
        typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());

        dependencies.forEach(value -> typeSolver.add(new JavaParserTypeSolver(value)));
        jars.forEach(value -> {
            try {
                typeSolver.add(new JarTypeSolver(value));
            } catch (IOException e) {
                log.debug("read jar fail:{}",value);
            }
        });

        ParserConfiguration parserConfiguration = new ParserConfiguration();
        parserConfiguration.setSymbolResolver(new JavaSymbolSolver(typeSolver));
        return parserConfiguration;
    }

    public NodeVisitor visitor() {
        return currentFramework().visitor();
    }

    public Set<Path> getSources() {
        return sources;
    }

    public Set<Path> getDependencies() {
        return dependencies;
    }

    public Set<Path> getJars() {
        return jars;
    }

    public Path getOutPath() {
        if(out.isAbsolute()){
            return out.resolve(production);
        }
        return project.resolve(out).resolve(production);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public static Set<String> getIgnoreTypes() {
        Set<String> ignores = ignoreTypes.get();
        if (ignores == null) {
            ignores = Sets.newHashSet();
            ignoreTypes.set(ignores);
        }
        return ignores;
    }

    public String getVersion() {
        return version;
    }

    public CombinedTypeSolver getTypeSolver() {
        return typeSolver;
    }

    public Path getProject() {
        return project;
    }

    public String getCss() {
        return css;
    }
}
