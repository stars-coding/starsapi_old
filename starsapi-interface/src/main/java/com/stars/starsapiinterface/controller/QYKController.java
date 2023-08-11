package com.stars.starsapiinterface.controller;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.stars.starsapicommon.model.entity.QYKParams;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 青云客机器人控制器
 *
 * @author stars
 */
@RestController
@RequestMapping("/starsapi")
public class QYKController {

    /**
     * 处理POST请求，根据请求参数获取IP
     *
     * @param qykParams 请求体中的QYKParams对象，包含请求参数
     * @param request   HttpServletRequest对象，用于获取请求信息
     * @return 字符串，表示根据请求参数获取的IP结果
     */
    @PostMapping("/qyk")
    public String getIPByPost(@RequestBody QYKParams qykParams, HttpServletRequest request) {
        String msg = qykParams.getContent();
        if ("".equals(msg) || msg == null) {
            ErrorResponse errorResponse = new ErrorResponse("请输入正确的请求参数！");
            return JSONUtil.toJsonStr(errorResponse);
        }
        String url = "http://api.qingyunke.com/api.php?key=free&appid=0&msg=" + msg;
        HttpResponse execute = HttpUtil.createGet(url).execute();
        return execute.body().toString();
    }
}
