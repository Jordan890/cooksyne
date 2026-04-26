package com.cooksyne.adapters.aiollama;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cooksyne.ai.ollama")
public class OllamaProperties {

    private String baseUrl;
    private String model;

    /** Context window size. Lower = less RAM, faster. 0 = model default. */
    private int numCtx = 2048;

    /**
     * CPU threads for inference. Set to physical core count (not hyperthreads). 0 =
     * Ollama auto-detect.
     */
    private int numThread = 0;

    /**
     * Max tokens to generate. Caps response length to prevent runaway generation. 0
     * = unlimited.
     */
    private int numPredict = 512;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getNumCtx() {
        return numCtx;
    }

    public void setNumCtx(int numCtx) {
        this.numCtx = numCtx;
    }

    public int getNumThread() {
        return numThread;
    }

    public void setNumThread(int numThread) {
        this.numThread = numThread;
    }

    public int getNumPredict() {
        return numPredict;
    }

    public void setNumPredict(int numPredict) {
        this.numPredict = numPredict;
    }
}
