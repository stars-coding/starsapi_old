package com.stars.starsapiclientsdk.client;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.stars.starsapiclientsdk.utils.SignUtils;
import com.stars.starsapicommon.model.entity.User;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * StarsApiClient是一个用于与API网关交互的客户端类。
 *
 * @author stars
 */
public class StarsApiClient {

    // todo 开发环境-本地地址-网关地址
    private static final String GATEWAY_HOST = "http://localhost:11030";
    // todo 线上环境-服务器外网地址-网关地址

    private String accessKey;
    private String secretKey;

    /**
     * 构造StarsApiClient的新实例。
     *
     * @param accessKey 公钥
     * @param secretKey 密钥
     */
    public StarsApiClient(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    /**
     * 使用GET请求获取名称。
     *
     * @param name 要获取的名称
     * @return 获取的名称
     */
    public String getNameByGet(String name) {
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("name", name);
        String result = HttpUtil.get(GATEWAY_HOST + "/api/name/", paramMap);
        return result;
    }

    /**
     * 使用POST请求获取名称。
     *
     * @param name 要获取的名称
     * @return 获取的名称
     */
    public String getNameByPost(String name) {
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("name", name);
        String result = HttpUtil.post(GATEWAY_HOST + "/api/name/", paramMap);
        return result;
    }

    /**
     * 获取请求头信息。
     *
     * @param body   请求体
     * @param isPost 是否为POST请求
     * @return 请求头信息的哈希映射
     */
    private Map<String, String> getHeadMap(String body, boolean isPost) {
        String encode = isPost ? encodeBody(body) : body;
        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("accessKey", accessKey);
        hashMap.put("body", encode);
        hashMap.put("nonce", RandomUtil.randomNumbers(4));
        hashMap.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000L));
        hashMap.put("sign", SignUtils.getSign(encode, secretKey));
        return hashMap;
    }

    /**
     * 获取用户名称。
     *
     * @param user 包含用户信息的User对象
     * @return 用户名称
     */
    public String getUsername(User user) {
        String json = JSONUtil.toJsonStr(user);
        HttpResponse httpResponse = HttpRequest.post(GATEWAY_HOST + "/name/user")
                .addHeaders(getHeadMap(json, true))
                .body(json)
                .execute();
        return httpResponse.body();
    }

    /**
     * 处理参数字符串并返回哈希映射。
     *
     * @param parameters 包含参数的字符串
     * @return 哈希映射
     */
    public HashMap<String, String> handleParameters(String parameters) {
        HashMap<String, String> map = new HashMap<>(32);
        String[] split = parameters.split("&");
        for (String s : split) {
            String[] split1 = s.split("=");
            String a = split1[0];
            String b = split1[1];
            map.put(a, b);
        }
        return map;
    }

    /**
     * 在线调用指定的方法。
     *
     * @param parameters 包含参数的字符串
     * @param url        调用的URL路径
     * @param method     请求方法 (GET或POST)
     * @return 调用结果
     */
    public String onlineInvoke(String parameters, String url, String method) {
        if ("POST".equals(method)) {
            HttpResponse httpResponse = HttpRequest.post(GATEWAY_HOST + url)
                    .addHeaders(getHeadMap(parameters, true))
                    .body(parameters)
                    .execute();
            return httpResponse.body();
        }
        if ("GET".equals(method)) {
            HashMap<String, String> stringObjectHashMap = handleParameters(parameters);
            String baseUrl = GATEWAY_HOST + url + "?" + getEncodedParams(stringObjectHashMap);
            HttpRequest httpRequest = HttpRequest.get(baseUrl)
                    .addHeaders(getHeadMap(parameters, false));
            HttpResponse httpResponse = httpRequest.execute();
            return httpResponse.body();
        }
        return "没有此请求方法";
    }

    /**
     * 将参数哈希映射编码为字符串。
     *
     * @param params 参数哈希映射
     * @return 编码后的参数字符串
     */
    private static String getEncodedParams(Map<String, String> params) {
        try {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                String encodedKey = URLEncoder.encode(key, "UTF-8");
                String encodedValue = URLEncoder.encode(value, "UTF-8");
                sb.append(encodedKey).append("=").append(encodedValue).append("&");
            }
            return sb.substring(0, sb.length() - 1);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 编码请求体。
     *
     * @param body 请求体
     * @return 编码后的请求体
     */
    private String encodeBody(String body) {
        try {
            return URLEncoder.encode(body, "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
