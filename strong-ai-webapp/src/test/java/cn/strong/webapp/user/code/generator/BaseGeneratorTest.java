//package cn.strong.webapp.user.code.generator;
//
//import cn.strong.mybatis.model.MyBatisPlusBaseModel;
//import cn.strong.mybatis.plus.extend.CustomBaseMapper;
//import cn.strong.mybatis.plus.extend.CustomServiceImpl;
//import com.baomidou.mybatisplus.generator.config.*;
//import com.baomidou.mybatisplus.generator.config.converts.MySqlTypeConvert;
//import com.baomidou.mybatisplus.generator.config.querys.MySqlQuery;
//import com.baomidou.mybatisplus.generator.config.rules.DateType;
//import com.baomidou.mybatisplus.generator.keywords.MySqlKeyWordsHandler;
//import org.junit.jupiter.api.BeforeEach;
//
///**
// * 基础测试类
// *
// * @author lanjerry
// * @since 3.5.3
// */
//public class BaseGeneratorTest {
//
//    protected GlobalConfig gc;
//    protected PackageConfig pc;
//    protected DataSourceConfig dc;
//    protected StrategyConfig sc;
//    protected TemplateConfig tc;
//
//    @BeforeEach
//    public void init(){
//        dc = MySQLGeneratorTest.dataSourceConfig().build();
//        gc = globalConfig().build();
//        pc = MySQLGeneratorTest.packageConfig().build();
//        tc = templateConfig();
//
//    }
//
//
//
//    /**
//     * 策略配置
//     */
//    protected static StrategyConfig.Builder strategyConfig(String tablePrefix, String... tables) {
//        StrategyConfig.Builder builder= new StrategyConfig.Builder();
//        builder.controllerBuilder()
//                .formatFileName("%sController");
//        builder.entityBuilder()
//                .superClass(MyBatisPlusBaseModel.class)
//                .enableLombok()
//                .formatFileName("%sModel");
//        builder.mapperBuilder()
//                .superClass(CustomBaseMapper.class)
//                .enableBaseResultMap()
//                .enableBaseColumnList()
//                .formatXmlFileName("%sMapper")
//                .formatMapperFileName("%sMapper");
//        builder.serviceBuilder()
//                .superServiceImplClass(CustomServiceImpl.class)
//                .formatServiceFileName("%sService");
//        builder.addTablePrefix(tablePrefix);
//        builder.addInclude(tables);
//        return builder;
//    }
//
//    /**
//     * 全局配置
//     */
//    protected static GlobalConfig.Builder globalConfig() {
//        String outputDir = System.getProperty("user.dir") + "/src/main/java";
//        return new GlobalConfig.Builder()
//                .fileOverride()
//                .outputDir(outputDir)
//                .author("baomidou")
//                .enableSwagger()
//                .dateType(DateType.TIME_PACK)
//                .commentDate("yyyy-MM-dd HH:mm:ss");
//    }
//
//
//
//    /**
//     * 模板配置
//     */
//    protected static TemplateConfig templateConfig() {
//        return new TemplateConfig.Builder().build();
//    }
//
//}