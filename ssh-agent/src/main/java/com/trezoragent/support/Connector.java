package com.trezoragent.support;

public interface Connector {
  String getName();
  boolean isAvailable();
  void query(Buffer buffer) throws AgentProxyException;
}
