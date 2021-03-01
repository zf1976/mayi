package com.zf1976.ant.auth.controller;

import com.zf1976.ant.common.core.foundation.ResultData;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author mac
 * @date 2021/2/19
 **/
@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping("/test")
    @PreAuthorize("hasAuthority('admin')")
    public ResultData<?> test(){
        return ResultData.success();
    }
}