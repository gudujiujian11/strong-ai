//package cn.strong.webapp.user.code.generator;
//
//import com.baomidou.mybatisplus.generator.AutoGenerator;
//import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
//import com.baomidou.mybatisplus.generator.config.PackageConfig;
//import com.baomidou.mybatisplus.generator.config.converts.MySqlTypeConvert;
//import com.baomidou.mybatisplus.generator.config.querys.MySqlQuery;
//import com.baomidou.mybatisplus.generator.keywords.MySqlKeyWordsHandler;
//import org.junit.jupiter.api.Test;
//
///**
// * MySQL 代码生成
// *
// * @author lanjerry
// * @since 3.5.3
// */
//public class MySQLGeneratorTest extends BaseGeneratorTest {
//
//
//
//    @Test
//    public void testSimple() {
//        generate("ac_", "ac_app_type");
//    }
//
//    protected void generate(String tablePrefix, String... tables) {
//        AutoGenerator mpg = new AutoGenerator(dc);
//        sc = strategyConfig(tablePrefix, tables).build();
//        mpg.global(gc);
//        mpg.packageInfo(pc);
//        mpg.strategy(sc);
//        mpg.template(tc);
//
//        mpg.execute();
//
//    }
//
//    //TODO 修改真实的数据源
//    public static  DataSourceConfig.Builder dataSourceConfig(){
//        return new DataSourceConfig.Builder("jdbc:mysql://192.168.21.80:3306/letao_app_center?useSSL=false",
//                "leke",
//                "leke@@@")
//                .dbQuery(new MySqlQuery())
//                .schema("mybatis-plus")
//                .typeConvert(new MySqlTypeConvert())
//                .keyWordsHandler(new MySqlKeyWordsHandler());
//    }
//
//    public static PackageConfig.Builder packageConfig() {
//        //TODO 替换 app 为真实的模块名
//        return new PackageConfig.Builder()
//                .parent("cn.strong.webapp.user")
//                .moduleName("app")
//                .entity("model")
//                .service("service")
//                .serviceImpl("service.impl")
//                .mapper("dao.mybatis")
//                .xml("dao.mybatis")
//                .controller("controller");
//
//    }
//}