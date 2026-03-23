package com.cartandcook.selfhosted.contracts;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RuntimeDbTestResponse {
    private boolean success;
    private String message;
}
