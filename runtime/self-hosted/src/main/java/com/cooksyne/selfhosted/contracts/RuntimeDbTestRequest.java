package com.cooksyne.selfhosted.contracts;

import lombok.Data;

@Data
public class RuntimeDbTestRequest {
    private String dbUrl;
    private String dbUsername;
    private String dbPassword;
}
