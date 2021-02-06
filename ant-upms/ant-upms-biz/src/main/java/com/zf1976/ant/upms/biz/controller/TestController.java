package com.zf1976.ant.upms.biz.controller;

import com.zf1976.ant.common.core.util.RequestUtils;
import com.zf1976.ant.common.log.annotation.Log;
import com.zf1976.ant.common.log.dao.SysLogDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * @author mac
 * @date 2020/12/24
 **/
@Slf4j
@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private SysLogDao sysLogDao;

    @Log(description = "测试A接口")
    @RequestMapping(method = RequestMethod.GET, path = "/testA")
    public void testA(@RequestParam String description) {
        log.info("{}", description);
        throw new RuntimeException(this.getClass().getName());
    }

    @Log(description = "测试B接口")
    @GetMapping("/testB")
    public void testB(){
        HttpServletRequest request = RequestUtils.getRequest();
        System.out.println(request.getParameter("hanbi"));
    }
}
