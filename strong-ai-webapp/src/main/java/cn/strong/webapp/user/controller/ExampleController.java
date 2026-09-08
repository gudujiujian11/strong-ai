package cn.strong.webapp.user.controller;

import cn.strong.common.base.Result;
import cn.strong.common.exception.ParamValiateException;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:wf2311@163.com">wf2311</a>
 * @since 2021/6/8 16:41.
 */

@RestController
@RequestMapping("/example")
@Slf4j
@Api(tags = {"v1.0", "example"}, description = "示例控制器，可以删除")
public class ExampleController {

    @ApiOperation("哈喽")
    @GetMapping("/hello")
    public Result<String> hello(String message) {
        if ("error".equals(message)) {
            throw new ParamValiateException("参数错误");
        }
        return Result.ok(message);
    }


    @ApiOperation("格式化1")
    @GetMapping("/format")
    public Result<DateModel> format(LocalDate day, Date date) {
        DateModel m = new DateModel();
        m.setDay(day);
        m.setUpdateDate(LocalDateTime.now());
        m.setOther(LocalDateTime.now().toLocalDate());
        if (date != null) {
            m.setDefaultDate(date);
            m.setOtherFormatDate(date);
        }
        return Result.ok(m);
    }


    @ApiOperation("格式化2")
    @GetMapping("/format2")
    public Result<DateModel> format2(DateModel m) {
        return Result.ok(m);
    }


    @ApiOperation("格式化3")
    @PostMapping("/format3")
    public Result<DateModel> format3(@RequestBody DateModel m) {
        return Result.ok(m);
    }

    @GetMapping("/logger")
    public Result<List<String>> logInfo() {
        List<String> levels = new ArrayList<>();
        if (log.isDebugEnabled()) {
            log.debug("logging debug info");
            levels.add("debug");
        }
        if (log.isInfoEnabled()) {
            log.info("logging info info");
            levels.add("info");
        }
        if (log.isWarnEnabled()) {
            log.warn("logging warn info");
            levels.add("warn");
        }
        if (log.isErrorEnabled()) {
            log.error("logging error info");
            levels.add("error");
        }
        return Result.ok(levels);
    }

    @Data
    @ApiModel("时间类")
    public static class DateModel implements Serializable {

        @ApiModelProperty("日期")
        private LocalDate day;

        @ApiModelProperty("更新时间")
        @DateTimeFormat(pattern = "yyyy/MM/dd HH:mm:ss")
        private LocalDateTime updateDate;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy年MM月dd日")
        @ApiModelProperty("其他")
        private LocalDate other;

        @ApiModelProperty("时间Date")
        private Date defaultDate;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy年MM月dd号 HH点mm分ss秒", timezone = "GMT+8")
        @ApiModelProperty("自定义时间格式的Date")
        private Date otherFormatDate;

    }
}
