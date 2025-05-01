package com.ethereal.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckEmailResponse {
    private boolean exists;
    private boolean enabled;
}
