package com.mall.service;

import java.util.Map;

public interface PrivacyService {
    Map<String, Object> exportPersonalData(Long userId);
    void closeAccount(Long userId);
}
