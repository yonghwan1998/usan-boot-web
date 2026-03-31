package com.usanmap.usan.dto;

public record LoginUserSnapshot(
        Long userId,
        String email,
        String nickname
) {}
