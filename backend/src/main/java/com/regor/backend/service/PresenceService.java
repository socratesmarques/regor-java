package com.regor.backend.service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
@Service
public class PresenceService {
    private final Map<String,UUID> sessions=new ConcurrentHashMap<>();
    public void connected(String session,UUID user){sessions.put(session,user);}
    public void disconnected(String session){sessions.remove(session);}
    public boolean online(UUID user){return sessions.containsValue(user);}
}
