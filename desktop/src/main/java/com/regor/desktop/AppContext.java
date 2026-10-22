package com.regor.desktop;
import com.regor.desktop.service.ApiClient;
import java.util.concurrent.*;
public final class AppContext implements AutoCloseable {
    public AppContext() {}
    public final ApiClient api=new ApiClient();
    public final ExecutorService worker=Executors.newVirtualThreadPerTaskExecutor();
    public Navigator navigator;
    @Override public void close() { api.close();worker.shutdownNow(); }
}
