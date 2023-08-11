package com.stars.starsapiinterface.controller;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONUtil;
import com.stars.starsapicommon.model.entity.MD5Params;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * MD5控制器
 *
 * @author stars
 */
@RestController
@RequestMapping("/md5")
public class MD5Controller {

    /**
     * 处理POST请求，进行MD5转换
     *
     * @param md5Params          请求体中的MD5Params对象，包含待转换的内容
     * @param httpServletRequest HttpServletRequest对象，用于获取请求信息
     * @return 字符串，表示MD5转换后的结果
     */
    @PostMapping("/conversion")
    public String myMD5(@RequestBody MD5Params md5Params, HttpServletRequest httpServletRequest) {
        String content = md5Params.getContent();
        if ("".equals(content) || content == null) {
            ErrorResponse errorResponse = new ErrorResponse("请输入正确的请求参数！");
            return JSONUtil.toJsonStr(errorResponse);
        }
        String s = SecureUtil.md5(content);
        MD5Params resultMD5 = new MD5Params();
        resultMD5.setContent(s);
        String result = JSONUtil.toJsonStr(resultMD5);
        return result;
    }
}
