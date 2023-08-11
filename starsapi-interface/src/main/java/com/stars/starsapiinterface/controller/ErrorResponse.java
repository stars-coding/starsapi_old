package com.stars.starsapiinterface.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 错误响应
 *
 * @author stars
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {

    private String resultData;
}
