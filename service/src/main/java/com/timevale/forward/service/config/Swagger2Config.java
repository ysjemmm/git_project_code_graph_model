package com.timevale.forward.service.config;

import io.swagger.annotations.ApiOperation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.service.Parameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yuankai
 * @date 2020/11/3 17:45
 */
@EnableSwagger2
@Configuration
public class Swagger2Config extends WebMvcConfigurerAdapter {
    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .enable(true)
                .apiInfo(apiInfo())
                .groupName("forward")
                .select()
                // 为当前包路径
                .apis(RequestHandlerSelectors.withMethodAnnotation(ApiOperation.class))
                .paths(PathSelectors.any())
                .build().globalOperationParameters(getParameterList());
    }

    /**
     * 构建 api 文档的详细信息函数, 注意这里的注解引用的是哪个
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                // 页面标题
                .title("产研项目管理系统接口文档")
                // 创建人
                .contact(new Contact("yuankai", "", "yuankai@tsign.cn"))
                // 版本号
                .version("1.0")
                // 描述
                .description("接口文档")
                .build();
    }

    /**
     * 标准化工程结合 Swagger2，访问显示 404
     * 有可能是某处把默认的静态资源路径覆盖了
     * 目前尝试手动设置再重新指定一次
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("swagger-ui.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    /**
     * header 参数配置
     *
     * @return 参数
     */
    private List<Parameter> getParameterList() {
        List<Parameter> ps = new ArrayList<>();

        ParameterBuilder pb = new ParameterBuilder();
        pb.name("X-Tsign-Open-App-Id").description("appID").modelRef(new ModelRef("string"))
                .parameterType("header").required(false).build();
        ps.add(pb.build());

        return ps;
    }
}
