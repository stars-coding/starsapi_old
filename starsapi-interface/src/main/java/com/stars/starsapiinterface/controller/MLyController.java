package com.stars.starsapiinterface.controller;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.stars.starsapicommon.model.entity.MLyParams;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 茉莉机器人控制器
 *
 * @author stars
 */

@RestController
@RequestMapping("/mly")
public class MLyController {

    @Resource
    private RestTemplate restTemplate;

    /**
     * 处理POST请求，根据请求参数获取茉莉机器人的回复
     *
     * @param mLyParams 请求体中的MLyParams对象，包含请求参数
     * @param request   HttpServletRequest对象，用于获取请求信息
     * @return 字符串，表示茉莉机器人的回复
     */
    @PostMapping("/reply")
    public String getMLyByPost(@RequestBody MLyParams mLyParams, HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        String content = mLyParams.getContent();
        if ("".equals(content) || content == null) {
            ErrorResponse errorResponse = new ErrorResponse("请输入正确的请求参数！");
            return JSONUtil.toJsonStr(errorResponse);
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Api-Key", "qifrxxj5ih0x7lis");
        headers.add("Api-Secret", "w308lv4c");
        JSONObject body = new JSONObject();
        body.set("content", content);
        body.set("type", 1);
        body.set("from", "StarsApi");
        body.set("fromName", "StarsApi");
        HttpEntity<String> formEntity = new HttpEntity<>(body.toString(), headers);
        JSONObject jsonObject = restTemplate.postForEntity(
                "https://api.mlyai.com/reply",
                formEntity,
                JSONObject.class).getBody();

        String data = jsonObject.getStr("data");
        return data;
    }
}
