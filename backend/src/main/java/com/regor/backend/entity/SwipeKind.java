package com.regor.backend.entity;
public enum SwipeKind { LIKE, DISLIKE, SUPERLIKE;
    public boolean isPositive() { return this != DISLIKE; }
}
