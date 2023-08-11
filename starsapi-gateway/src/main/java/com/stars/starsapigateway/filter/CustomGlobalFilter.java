package com.stars.starsapigateway.filter;

import com.stars.starsapiclientsdk.utils.SignUtils;
import com.stars.starsapicommon.model.entity.Interf;
import com.stars.starsapicommon.model.entity.User;
import com.stars.starsapicommon.service.InnerInterfService;
import com.stars.starsapicommon.service.InnerUserInvokeInterfService;
import com.stars.starsapicommon.service.InnerUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 自定义全局过滤器
 *
 * @author stars
 */
@Component
@Slf4j
public class CustomGlobalFilter implements GlobalFilter, Ordered {

    @DubboReference
    private InnerUserService innerUserService;

    @DubboReference
    private InnerInterfService innerInterfService;

    @DubboReference
    private InnerUserInvokeInterfService innerUserInvokeInterfService;

    // todo 黑白名单，访问控制
    // private static final List<String> IP_WHITE_LIST = Arrays.asList(new String[] { "127.0.0.1" });

    // todo 开发、线上环境-本地地址-模拟接口地址
    private static final String INTERFACE_HOST = "http://localhost:11020";

    /**
     * 对请求进行过滤处理
     *
     * @param exchange ServerWebExchange对象，用于访问请求和响应
     * @param chain    GatewayFilterChain对象，用于继续请求链的处理
     * @return 字段类型为Mono<Void>的对象，表示异步处理结果
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = INTERFACE_HOST + request.getPath().value();
        String method = request.getMethod().toString();
        log.info("请求唯一标识" + request.getId());
        log.info("请求路径" + path);
        log.info("请求方法" + method);
        log.info("请求参数" + request.getQueryParams());
        String sourceAddress = request.getLocalAddress().getHostString();
        log.info("请求来源地址" + sourceAddress);
        log.info("请求来源地址" + request.getRemoteAddress());
        // 拿到封装完成的请求头的东西
        ServerHttpResponse response = exchange.getResponse();
        HttpHeaders headers = request.getHeaders();
        String accessKey = headers.getFirst("accessKey");
        String nonce = headers.getFirst("nonce");
        String timestamp = headers.getFirst("timestamp");
        String sign = headers.getFirst("sign");
        String body = headers.getFirst("body");
        User invokeUser = null;
        // 根据accessKey获取调用对象，方便后续拿secretKey
        try {
            invokeUser = innerUserService.getInvokeUser(accessKey);
        } catch (Exception e) {
            log.error("getInvokeUser error", e);
        }
        if (invokeUser == null) {
            return handleNoAuth(response);
        }
        // 判断随机数，需要有一定规则
        if (nonce == null || Long.parseLong(nonce) > 10000) {
            return handleNoAuth(response);
        }
        // 判断时间，防止间隔时间重复调用
        long currentTime = System.currentTimeMillis() / 1000;
        final long FIVE_MINUTES = 60 * 5L;
        if ((currentTime - Long.parseLong(timestamp)) >= FIVE_MINUTES) {
            return handleNoAuth(response);
        }
        // 得到secretKey
        String secretKey = invokeUser.getSecretKey();
        // 签名认证算法校验，目的是判断secretKey是否正确
        String serverSign = SignUtils.getSign(body, secretKey);
        if (sign == null || !sign.equals(serverSign)) {
            return handleNoAuth(response);
        }
        Interf interf = null;
        // 根据路径和方法查看是否有此接口
        try {
            String requestPath = request.getPath().value();
            interf = innerInterfService.getInterf(requestPath, method);
        } catch (Exception e) {
            log.error("getInterf error", e);
        }
        if (interf == null) {
            return handleNoAuth(response);
        }
        // 接口是否还剩调用次数，该用户是否有此接口的调用次数(鉴权)
        try {
            innerUserInvokeInterfService.validLeftNum(invokeUser.getId(), interf.getId());
        } catch (Exception e) {
            log.error("剩余次数不足！", e);
            return handleNoAuth(response);
        }
        // 对原有请求进行添加请求头的操作，流量染色
        ServerHttpRequest modifyRequest = request.mutate().header("Info", "StarsFlowStaining").build();
        ServerWebExchange newExchange = exchange.mutate().request(modifyRequest).build();
        return handleResponse(newExchange, chain, interf.getId(), invokeUser.getId());
    }

    /**
     * 处理响应的方法
     *
     * @param exchange ServerWebExchange对象，用于访问请求和响应
     * @param chain    GatewayFilterChain对象，用于继续请求链的处理
     * @param interfId 接口ID
     * @param userId   用户ID
     * @return 字段类型为Mono<Void>的对象，表示异步处理结果
     */
    public Mono<Void> handleResponse(ServerWebExchange exchange, GatewayFilterChain chain, long interfId, long userId) {
        try {
            ServerHttpResponse originalResponse = exchange.getResponse();
            // 缓冲区工厂
            DataBufferFactory bufferFactory = originalResponse.bufferFactory();
            // 拿到响应码
            HttpStatus statusCode = originalResponse.getStatusCode();
            if (statusCode == HttpStatus.OK) {
                // 装饰，增强能力
                ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
                    // 等调用完转发的接口后才会执行
                    @Override
                    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                        // log.info("body instanceof Flux: {}", (body instanceof Flux));
                        if (body instanceof Flux) {
                            Flux<? extends DataBuffer> fluxBody = Flux.from(body);
                            // 往返回值里面写数据
                            // 拼接字符串
                            return super.writeWith(
                                    fluxBody.map(dataBuffer -> {
                                        // 调用成功，接口调用次数+1 invokeCount
                                        try {
                                            innerUserInvokeInterfService.invokeCount(userId, interfId);
                                        } catch (Exception e) {
                                            log.error("invokeCount出错!", e);
                                        }
                                        byte[] content = new byte[dataBuffer.readableByteCount()];
                                        dataBuffer.read(content);
                                        // 释放内存
                                        DataBufferUtils.release(dataBuffer);
                                        // 构建日志
                                        StringBuilder sb2 = new StringBuilder(200);
                                        sb2.append("<--- {} {} \n");
                                        List<Object> rspArgs = new ArrayList<>();
                                        rspArgs.add(originalResponse.getStatusCode());
                                        String data = new String(content, StandardCharsets.UTF_8);
                                        sb2.append(data);
                                        // 打印日志
                                        log.info(sb2.toString(), rspArgs.toArray(), data);
                                        return bufferFactory.wrap(content);
                                    }));
                        } else {
                            log.error("<--- {} 响应code异常", getStatusCode());
                        }
                        return super.writeWith(body);
                    }
                };
                // 设置response对象为装饰过的
                return chain.filter(exchange.mutate().response(decoratedResponse).build());
            }
            // 降级处理返回数据
            return chain.filter(exchange);
        } catch (Exception e) {
            log.error("网关处理响应错误.\n" + e);
            return chain.filter(exchange);
        }
    }

    /**
     * 获取过滤器的顺序值
     *
     * @return 过滤器顺序值，返回-1
     */
    public int getOrder() {
        return -1;
    }

    /**
     * 处理无权限的情况
     *
     * @param response ServerHttpResponse对象，用于设置响应
     * @return 字段类型为Mono<Void>的对象，表示异步处理结果
     */
    public Mono<Void> handleNoAuth(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.FORBIDDEN);
        return response.setComplete();
    }

    /**
     * 处理调用错误的情况
     *
     * @param response ServerHttpResponse对象，用于设置响应
     * @return 字段类型为Mono<Void>的对象，表示异步处理结果
     */
    public Mono<Void> handleInvokeError(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        return response.setComplete();
    }
}
