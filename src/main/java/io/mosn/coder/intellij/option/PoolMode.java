package io.mosn.coder.intellij.option;

/**
 * @author yiji@apache.org
 */
public enum PoolMode {
    /**
     * Mosn connection pool model, reuse tcp connection pool
     */
    Multiplex,
    PingPong;
}
